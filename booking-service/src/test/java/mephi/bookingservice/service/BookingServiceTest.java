package mephi.bookingservice.service;

import mephi.bookingservice.client.HotelServiceClient;
import mephi.bookingservice.dto.BookingRequest;
import mephi.bookingservice.dto.BookingResponse;
import mephi.bookingservice.dto.hotel.AvailabilityRequest;
import mephi.bookingservice.dto.hotel.AvailabilityResponse;
import mephi.bookingservice.entity.Booking;
import mephi.bookingservice.entity.BookingStatus;
import mephi.bookingservice.entity.Role;
import mephi.bookingservice.entity.User;
import mephi.bookingservice.exception.BookingException;
import mephi.bookingservice.exception.ResourceNotFoundException;
import mephi.bookingservice.mapper.BookingMapper;
import mephi.bookingservice.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService Unit Tests")
class BookingServiceTest {
    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private HotelServiceClient hotelServiceClient;

    @Mock
    private UserService userService;

    @InjectMocks
    private BookingService bookingService;

    private User testUser;
    private Booking testBooking;
    private BookingRequest bookingRequest;
    private BookingResponse bookingResponse;
    private AvailabilityResponse availabilityResponse;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("john_doe");
        testUser.setEmail("john@example.com");
        testUser.setRole(Role.USER);

        bookingRequest = new BookingRequest();
        bookingRequest.setRoomId(1L);
        bookingRequest.setHotelId(1L);
        bookingRequest.setCheckInDate(LocalDate.now().plusDays(1));
        bookingRequest.setCheckOutDate(LocalDate.now().plusDays(3));
        bookingRequest.setGuestCount(2);

        testBooking = new Booking();
        testBooking.setId(1L);
        testBooking.setBookingReference("BK-123456");
        testBooking.setRoomId(1L);
        testBooking.setHotelId(1L);
        testBooking.setUser(testUser);
        testBooking.setCheckInDate(LocalDate.now().plusDays(1));
        testBooking.setCheckOutDate(LocalDate.now().plusDays(3));
        testBooking.setGuestCount(2);
        testBooking.setStatus(BookingStatus.PENDING);
        testBooking.setTotalPrice(BigDecimal.valueOf(200));

        bookingResponse = BookingResponse.builder()
            .id(1L)
            .bookingReference("BK-123456")
            .roomId(1L)
            .hotelId(1L)
            .status(BookingStatus.CONFIRMED)
            .checkInDate(LocalDate.now().plusDays(1))
            .checkOutDate(LocalDate.now().plusDays(3))
            .totalPrice(BigDecimal.valueOf(200))
            .build();

        availabilityResponse = AvailabilityResponse.builder()
            .roomId(1L)
            .hotelId(1L)
            .confirmed(true)
            .message("Room availability confirmed")
            .totalPrice(BigDecimal.valueOf(200))
            .nights(2)
            .build();
    }

    @Nested
    @DisplayName("createBooking - Saga Pattern")
    class CreateBooking {
        @Test
        @DisplayName("should create booking and confirm with hotel service when room available")
        void should_CreateAndConfirmBooking_When_RoomAvailable() {
            given(userService.findByUsername("john_doe")).willReturn(testUser);
            given(bookingRepository.findOverlappingBookings(anyLong(), any(), any(), any()))
                .willReturn(List.of());
            given(bookingMapper.toEntity(bookingRequest)).willReturn(testBooking);
            given(bookingRepository.save(any(Booking.class))).willReturn(testBooking);
            given(hotelServiceClient.confirmAvailability(eq(1L), any(AvailabilityRequest.class)))
                .willReturn(availabilityResponse);
            given(bookingMapper.toResponse(any(Booking.class))).willReturn(bookingResponse);

            BookingResponse result = bookingService.createBooking(bookingRequest, "john_doe");

            assertThat(result).isNotNull();
            assertThat(result.getBookingReference()).isEqualTo("BK-123456");
            verify(hotelServiceClient, times(1)).confirmAvailability(eq(1L), any());
            verify(bookingRepository, times(2)).save(any(Booking.class));
        }

        @Test
        @DisplayName("should throw BookingException when check-out date is not after check-in")
        void should_ThrowBookingException_When_InvalidDates() {
            bookingRequest.setCheckOutDate(LocalDate.now().plusDays(1));
            bookingRequest.setCheckInDate(LocalDate.now().plusDays(3)); // After checkout

            assertThatThrownBy(() -> bookingService.createBooking(bookingRequest, "john_doe"))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("Check-out date must be after check-in date");

            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("should throw BookingException when room has overlapping bookings")
        void should_ThrowBookingException_When_OverlappingBookingsExist() {
            Booking existingBooking = new Booking();
            existingBooking.setStatus(BookingStatus.CONFIRMED);

            given(userService.findByUsername("john_doe")).willReturn(testUser);
            given(bookingRepository.findOverlappingBookings(anyLong(), any(), any(), any()))
                .willReturn(List.of(existingBooking));

            assertThatThrownBy(() -> bookingService.createBooking(bookingRequest, "john_doe"))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("already booked");

            verify(hotelServiceClient, never()).confirmAvailability(anyLong(), any());
        }

        @Test
        @DisplayName("should set booking to FAILED and throw when room not available")
        void should_SetBookingToFailed_When_RoomNotAvailable() {
            AvailabilityResponse failedResponse = AvailabilityResponse.builder()
                .roomId(1L)
                .confirmed(false)
                .message("Room is not available")
                .build();

            given(userService.findByUsername("john_doe")).willReturn(testUser);
            given(bookingRepository.findOverlappingBookings(anyLong(), any(), any(), any()))
                .willReturn(List.of());
            given(bookingMapper.toEntity(bookingRequest)).willReturn(testBooking);
            given(bookingRepository.save(any(Booking.class))).willReturn(testBooking);
            given(hotelServiceClient.confirmAvailability(eq(1L), any(AvailabilityRequest.class)))
                .willReturn(failedResponse);

            assertThatThrownBy(() -> bookingService.createBooking(bookingRequest, "john_doe"))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("not available");

            verify(bookingRepository, times(2)).save(any(Booking.class));
        }

        @Test
        @DisplayName("should compensate and release room on system error")
        void should_CompensateAndReleaseRoom_When_SystemError() {
            given(userService.findByUsername("john_doe")).willReturn(testUser);
            given(bookingRepository.findOverlappingBookings(anyLong(), any(), any(), any()))
                .willReturn(List.of());
            given(bookingMapper.toEntity(bookingRequest)).willReturn(testBooking);
            given(bookingRepository.save(any(Booking.class))).willReturn(testBooking);
            given(hotelServiceClient.confirmAvailability(eq(1L), any(AvailabilityRequest.class)))
                .willThrow(new RuntimeException("Connection timeout"));

            assertThatThrownBy(() -> bookingService.createBooking(bookingRequest, "john_doe"))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("Failed to complete booking");

            verify(hotelServiceClient, times(1)).releaseRoom(eq(1L), anyString());
        }
    }

    @Nested
    @DisplayName("getBookingById")
    class GetBookingById {
        @Test
        @DisplayName("should return booking when found")
        void should_ReturnBooking_When_Found() {
            given(bookingRepository.findByIdWithUser(1L)).willReturn(Optional.of(testBooking));
            given(bookingMapper.toResponse(testBooking)).willReturn(bookingResponse);

            BookingResponse result = bookingService.getBookingById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when booking not found")
        void should_ThrowResourceNotFoundException_When_BookingNotFound() {
            given(bookingRepository.findByIdWithUser(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.getBookingById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Booking");
        }
    }

    @Nested
    @DisplayName("getBookingsByUser")
    class GetBookingsByUser {
        @Test
        @DisplayName("should return user's bookings")
        void should_ReturnUserBookings() {
            given(userService.findByUsername("john_doe")).willReturn(testUser);
            given(bookingRepository.findByUserIdOrderByCreatedAtDesc(1L)).willReturn(List.of(testBooking));
            given(bookingMapper.toResponseList(List.of(testBooking))).willReturn(List.of(bookingResponse));

            List<BookingResponse> result = bookingService.getBookingsByUser("john_doe");

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getBookingReference()).isEqualTo("BK-123456");
        }
    }

    @Nested
    @DisplayName("cancelBooking")
    class CancelBooking {
        @BeforeEach
        void setUpCancelTests() {
            testBooking.setStatus(BookingStatus.CONFIRMED);
        }

        @Test
        @DisplayName("should cancel booking when user is owner")
        void should_CancelBooking_When_UserIsOwner() {
            BookingResponse cancelledResponse = BookingResponse.builder()
                .id(1L)
                .status(BookingStatus.CANCELLED)
                .build();

            given(bookingRepository.findByIdWithUser(1L)).willReturn(Optional.of(testBooking));
            given(bookingRepository.save(any(Booking.class))).willReturn(testBooking);
            given(bookingMapper.toResponse(any(Booking.class))).willReturn(cancelledResponse);

            BookingResponse result = bookingService.cancelBooking(1L, "john_doe", "Change of plans", false);

            assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            verify(hotelServiceClient, times(1)).releaseRoom(eq(1L), anyString());
        }

        @Test
        @DisplayName("should cancel booking when user is admin")
        void should_CancelBooking_When_UserIsAdmin() {
            BookingResponse cancelledResponse = BookingResponse.builder()
                .id(1L)
                .status(BookingStatus.CANCELLED)
                .build();

            given(bookingRepository.findByIdWithUser(1L)).willReturn(Optional.of(testBooking));
            given(bookingRepository.save(any(Booking.class))).willReturn(testBooking);
            given(bookingMapper.toResponse(any(Booking.class))).willReturn(cancelledResponse);

            BookingResponse result = bookingService.cancelBooking(1L, "admin", "Policy violation", true);

            assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        }

        @Test
        @DisplayName("should throw BookingException when user is not owner and not admin")
        void should_ThrowBookingException_When_NotAuthorized() {
            given(bookingRepository.findByIdWithUser(1L)).willReturn(Optional.of(testBooking));

            assertThatThrownBy(() ->
                bookingService.cancelBooking(1L, "other_user", "Reason", false)
            )
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("not authorized");

            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("should throw BookingException when booking already cancelled")
        void should_ThrowBookingException_When_AlreadyCancelled() {
            testBooking.setStatus(BookingStatus.CANCELLED);
            given(bookingRepository.findByIdWithUser(1L)).willReturn(Optional.of(testBooking));

            assertThatThrownBy(() ->
                bookingService.cancelBooking(1L, "john_doe", "Reason", false)
            )
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("Cannot cancel booking in CANCELLED status");
        }
    }

    @Nested
    @DisplayName("getBookingsByStatus")
    class GetBookingsByStatus {
        @Test
        @DisplayName("should return bookings filtered by status")
        void should_ReturnBookings_FilteredByStatus() {
            testBooking.setStatus(BookingStatus.CONFIRMED);
            given(bookingRepository.findByStatus(BookingStatus.CONFIRMED)).willReturn(List.of(testBooking));
            given(bookingMapper.toResponseList(List.of(testBooking))).willReturn(List.of(bookingResponse));

            List<BookingResponse> result = bookingService.getBookingsByStatus(BookingStatus.CONFIRMED);

            assertThat(result).hasSize(1);
            verify(bookingRepository, times(1)).findByStatus(BookingStatus.CONFIRMED);
        }
    }
}
