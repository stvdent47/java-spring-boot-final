package mephi.bookingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mephi.bookingservice.client.HotelServiceClient;
import mephi.bookingservice.dto.BookingRequest;
import mephi.bookingservice.dto.BookingResponse;
import mephi.bookingservice.dto.hotel.AvailabilityRequest;
import mephi.bookingservice.dto.hotel.AvailabilityResponse;
import mephi.bookingservice.dto.hotel.RoomResponse;
import mephi.bookingservice.entity.Booking;
import mephi.bookingservice.entity.BookingStatus;
import mephi.bookingservice.entity.User;
import mephi.bookingservice.exception.BookingException;
import mephi.bookingservice.exception.ResourceNotFoundException;
import mephi.bookingservice.mapper.BookingMapper;
import mephi.bookingservice.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final HotelServiceClient hotelServiceClient;
    private final UserService userService;

    @Transactional
    public BookingResponse createBooking(BookingRequest request, String username) {
        log.info("Creating booking for user: {}, room: {}", username, request.getRoomId());

        if (!request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new BookingException("Check-out date must be after check-in date");
        }

        User user = userService.findByUsername(username);

        List<Booking> overlapping = bookingRepository.findOverlappingBookings(
            request.getRoomId(),
            request.getCheckInDate(),
            request.getCheckOutDate(),
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
        );

        if (!overlapping.isEmpty()) {
            throw new BookingException("Room is already booked for the selected dates");
        }

        Booking booking = bookingMapper.toEntity(request);
        booking.setUser(user);
        booking.setStatus(BookingStatus.PENDING);
        booking = bookingRepository.save(booking);

        String requestId = UUID.randomUUID().toString();
        log.info("Booking created with reference: {}, requestId: {}", booking.getBookingReference(), requestId);

        try {
            AvailabilityRequest availRequest = AvailabilityRequest.builder()
                .requestId(requestId)
                .startDate(request.getCheckInDate())
                .endDate(request.getCheckOutDate())
                .guestCount(request.getGuestCount())
                .build();

            AvailabilityResponse availResponse = hotelServiceClient.confirmAvailability(request.getRoomId(), availRequest);

            if (availResponse.isConfirmed()) {
                booking.setStatus(BookingStatus.CONFIRMED);
                booking.setTotalPrice(availResponse.getTotalPrice());
                booking = bookingRepository.save(booking);

                log.info(
                    "Booking confirmed: ref={}, totalPrice={}",
                    booking.getBookingReference(),
                    booking.getTotalPrice()
                );
            }
            else {
                booking.setStatus(BookingStatus.FAILED);
                booking.setCancellationReason(availResponse.getMessage());
                booking = bookingRepository.save(booking);

                log.warn(
                    "Booking failed - room not available: ref={}, reason={}",
                    booking.getBookingReference(),
                    availResponse.getMessage()
                );

                throw new BookingException(
                    "Room is not available: " + availResponse.getMessage(),
                    booking.getBookingReference()
                );
            }
        }
        catch (BookingException e) {
            throw e;
        }
        catch (Exception e) {
            log.error("Error during booking confirmation, compensating: {}", e.getMessage());

            try {
                hotelServiceClient.releaseRoom(request.getRoomId(), requestId);
            }
            catch (Exception releaseEx) {
                log.error("Failed to release room during compensation: {}", releaseEx.getMessage());
            }

            booking.setStatus(BookingStatus.FAILED);
            booking.setCancellationReason("Booking failed due to system error");
            bookingRepository.save(booking);

            throw new BookingException(
                "Failed to complete booking. Please try again.",
                booking.getBookingReference(),
                "SYSTEM_ERROR"
            );
        }

        return bookingMapper.toResponse(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findByIdWithUser(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", id));

        return bookingMapper.toResponse(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingByReference(String reference) {
        Booking booking = bookingRepository.findByBookingReferenceWithUser(reference)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", "reference", reference));

        return bookingMapper.toResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByUser(String username) {
        User user = userService.findByUsername(username);
        List<Booking> bookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        return bookingMapper.toResponseList(bookings);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {
        List<Booking> bookings = bookingRepository.findAll();

        return bookingMapper.toResponseList(bookings);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByStatus(BookingStatus status) {
        List<Booking> bookings = bookingRepository.findByStatus(status);

        return bookingMapper.toResponseList(bookings);
    }

    @Transactional
    public BookingResponse cancelBooking(Long id, String username, String reason, boolean isAdmin) {
        Booking booking = bookingRepository.findByIdWithUser(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", id));

        if (!isAdmin && !booking.getUser().getUsername().equals(username)) {
            throw new BookingException("You are not authorized to cancel this booking");
        }

        if (
            booking.getStatus() != BookingStatus.PENDING &&
            booking.getStatus() != BookingStatus.CONFIRMED
        ) {
            throw new BookingException(
                "Cannot cancel booking in " + booking.getStatus() + " status",
                booking.getBookingReference()
            );
        }

        try {
            String requestId = UUID.randomUUID().toString();
            hotelServiceClient.releaseRoom(booking.getRoomId(), requestId);
            log.info("Room released for cancelled booking: ref={}", booking.getBookingReference());
        }
        catch (Exception e) {
            log.warn(
                "Failed to release room for booking {}: {}",
                booking.getBookingReference(),
                e.getMessage()
            );
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason != null ? reason : "Cancelled by user");
        booking = bookingRepository.save(booking);

        log.info("Booking cancelled: ref={}, reason={}", booking.getBookingReference(), reason);

        return bookingMapper.toResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getRecommendedRooms(Long hotelId, String roomType, Integer guestCount) {
        return hotelServiceClient.getRecommendedRooms(hotelId, roomType, guestCount);
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoomDetails(Long roomId) {
        return hotelServiceClient.getRoomById(roomId);
    }
}
