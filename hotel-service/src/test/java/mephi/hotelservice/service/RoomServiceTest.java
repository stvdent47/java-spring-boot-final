package mephi.hotelservice.service;

import mephi.hotelservice.dto.AvailabilityRequest;
import mephi.hotelservice.dto.AvailabilityResponse;
import mephi.hotelservice.dto.RoomRequest;
import mephi.hotelservice.dto.RoomResponse;
import mephi.hotelservice.entity.Hotel;
import mephi.hotelservice.entity.Room;
import mephi.hotelservice.entity.RoomType;
import mephi.hotelservice.exception.DuplicateResourceException;
import mephi.hotelservice.exception.ResourceNotFoundException;
import mephi.hotelservice.mapper.RoomMapper;
import mephi.hotelservice.repository.HotelRepository;
import mephi.hotelservice.repository.RoomRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomService Unit Tests")
class RoomServiceTest {
    @Mock
    private RoomRepository roomRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private RoomService roomService;

    private Hotel testHotel;
    private Room testRoom;
    private RoomRequest testRequest;
    private RoomResponse testResponse;

    @BeforeEach
    void setUp() {
        testHotel = new Hotel();
        testHotel.setId(1L);
        testHotel.setName("Grand Hotel");

        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setRoomNumber("101");
        testRoom.setRoomType(RoomType.STANDARD);
        testRoom.setPricePerNight(BigDecimal.valueOf(100));
        testRoom.setMaxOccupancy(2);
        testRoom.setAvailable(true);
        testRoom.setTimesBooked(0);
        testRoom.setHotel(testHotel);

        testRequest = new RoomRequest();
        testRequest.setHotelId(1L);
        testRequest.setRoomNumber("101");
        testRequest.setRoomType(RoomType.STANDARD);
        testRequest.setPricePerNight(BigDecimal.valueOf(100));
        testRequest.setMaxOccupancy(2);

        testResponse = RoomResponse.builder()
            .id(1L)
            .roomNumber("101")
            .roomType(RoomType.STANDARD)
            .pricePerNight(BigDecimal.valueOf(100))
            .maxOccupancy(2)
            .available(true)
            .hotelId(1L)
            .hotelName("Grand Hotel")
            .build();
    }

    @Nested
    @DisplayName("getRoomById")
    class GetRoomById {
        @Test
        @DisplayName("should return room when room exists")
        void should_ReturnRoom_When_RoomExists() {
            given(roomRepository.findByIdWithHotel(1L)).willReturn(Optional.of(testRoom));
            given(roomMapper.toResponse(testRoom)).willReturn(testResponse);

            RoomResponse result = roomService.getRoomById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getRoomNumber()).isEqualTo("101");
            verify(roomRepository, times(1)).findByIdWithHotel(1L);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when room does not exist")
        void should_ThrowResourceNotFoundException_When_RoomDoesNotExist() {
            given(roomRepository.findByIdWithHotel(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> roomService.getRoomById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Room")
                .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("getRoomsByHotelId")
    class GetRoomsByHotelId {
        @Test
        @DisplayName("should return rooms when hotel exists and has rooms")
        void should_ReturnRooms_When_HotelHasRooms() {
            given(hotelRepository.existsById(1L)).willReturn(true);
            given(roomRepository.findByHotelId(1L)).willReturn(List.of(testRoom));
            given(roomMapper.toResponseList(List.of(testRoom))).willReturn(List.of(testResponse));

            List<RoomResponse> result = roomService.getRoomsByHotelId(1L);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getHotelId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when hotel does not exist")
        void should_ThrowResourceNotFoundException_When_HotelDoesNotExist() {
            given(hotelRepository.existsById(999L)).willReturn(false);

            assertThatThrownBy(() -> roomService.getRoomsByHotelId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Hotel");
        }
    }

    @Nested
    @DisplayName("getAvailableRooms")
    class GetAvailableRooms {
        @Test
        @DisplayName("should return only available rooms")
        void should_ReturnAvailableRooms_When_AvailableRoomsExist() {
            given(roomRepository.findByAvailable(true)).willReturn(List.of(testRoom));
            given(roomMapper.toResponseList(List.of(testRoom))).willReturn(List.of(testResponse));

            List<RoomResponse> result = roomService.getAvailableRooms();

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getAvailable()).isTrue();
        }

        @Test
        @DisplayName("should return empty list when no available rooms")
        void should_ReturnEmptyList_When_NoAvailableRooms() {
            given(roomRepository.findByAvailable(true)).willReturn(List.of());
            given(roomMapper.toResponseList(List.of())).willReturn(List.of());

            List<RoomResponse> result = roomService.getAvailableRooms();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createRoom")
    class CreateRoom {
        @Test
        @DisplayName("should create room when hotel exists and room number is unique")
        void should_CreateRoom_When_HotelExistsAndRoomNumberUnique() {
            given(hotelRepository.findById(1L)).willReturn(Optional.of(testHotel));
            given(roomRepository.existsByHotelIdAndRoomNumber(1L, "101")).willReturn(false);
            given(roomMapper.toEntity(testRequest)).willReturn(testRoom);
            given(roomRepository.save(any(Room.class))).willReturn(testRoom);
            given(roomMapper.toResponse(testRoom)).willReturn(testResponse);

            RoomResponse result = roomService.createRoom(testRequest);

            assertThat(result).isNotNull();
            assertThat(result.getRoomNumber()).isEqualTo("101");
            verify(roomRepository, times(1)).save(any(Room.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when hotel does not exist")
        void should_ThrowResourceNotFoundException_When_HotelDoesNotExist() {
            given(hotelRepository.findById(999L)).willReturn(Optional.empty());
            testRequest.setHotelId(999L);

            assertThatThrownBy(() -> roomService.createRoom(testRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Hotel");

            verify(roomRepository, never()).save(any(Room.class));
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when room number exists in hotel")
        void should_ThrowDuplicateResourceException_When_RoomNumberExists() {
            given(hotelRepository.findById(1L)).willReturn(Optional.of(testHotel));
            given(roomRepository.existsByHotelIdAndRoomNumber(1L, "101")).willReturn(true);

            assertThatThrownBy(() -> roomService.createRoom(testRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("101");

            verify(roomRepository, never()).save(any(Room.class));
        }
    }

    @Nested
    @DisplayName("deleteRoom")
    class DeleteRoom {
        @Test
        @DisplayName("should delete room when room exists")
        void should_DeleteRoom_When_RoomExists() {
            given(roomRepository.existsById(1L)).willReturn(true);
            doNothing().when(roomRepository).deleteById(1L);

            roomService.deleteRoom(1L);

            verify(roomRepository, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when room does not exist")
        void should_ThrowResourceNotFoundException_When_RoomDoesNotExist() {
            given(roomRepository.existsById(999L)).willReturn(false);

            assertThatThrownBy(() -> roomService.deleteRoom(999L))
                .isInstanceOf(ResourceNotFoundException.class);

            verify(roomRepository, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("confirmAvailability")
    class ConfirmAvailability {
        @Test
        @DisplayName("should confirm availability when room is available and capacity is sufficient")
        void should_ConfirmAvailability_When_RoomAvailableAndCapacitySufficient() {
            String requestId = UUID.randomUUID().toString();
            AvailabilityRequest request = new AvailabilityRequest();
            request.setRequestId(requestId);
            request.setStartDate(LocalDate.now().plusDays(1));
            request.setEndDate(LocalDate.now().plusDays(3));
            request.setGuestCount(2);

            given(roomRepository.findByIdWithLock(1L)).willReturn(Optional.of(testRoom));
            given(roomRepository.save(any(Room.class))).willReturn(testRoom);

            AvailabilityResponse result = roomService.confirmAvailability(1L, request);

            assertThat(result).isNotNull();
            assertThat(result.isConfirmed()).isTrue();
            assertThat(result.getRoomId()).isEqualTo(1L);
            assertThat(result.getNights()).isEqualTo(2);
            assertThat(result.getTotalPrice()).isEqualTo(BigDecimal.valueOf(200));
            verify(roomRepository, times(1)).save(any(Room.class));
        }

        @Test
        @DisplayName("should reject when room is not available")
        void should_RejectAvailability_When_RoomNotAvailable() {
            String requestId = UUID.randomUUID().toString();
            AvailabilityRequest request = new AvailabilityRequest();
            request.setRequestId(requestId);
            request.setStartDate(LocalDate.now().plusDays(1));
            request.setEndDate(LocalDate.now().plusDays(3));

            testRoom.setAvailable(false);
            given(roomRepository.findByIdWithLock(1L)).willReturn(Optional.of(testRoom));

            AvailabilityResponse result = roomService.confirmAvailability(1L, request);

            assertThat(result.isConfirmed()).isFalse();
            assertThat(result.getMessage()).contains("not available");
        }

        @Test
        @DisplayName("should reject when guest count exceeds room capacity")
        void should_RejectAvailability_When_GuestCountExceedsCapacity() {
            String requestId = UUID.randomUUID().toString();
            AvailabilityRequest request = new AvailabilityRequest();
            request.setRequestId(requestId);
            request.setStartDate(LocalDate.now().plusDays(1));
            request.setEndDate(LocalDate.now().plusDays(3));
            request.setGuestCount(5); // Room capacity is 2

            given(roomRepository.findByIdWithLock(1L)).willReturn(Optional.of(testRoom));

            AvailabilityResponse result = roomService.confirmAvailability(1L, request);

            assertThat(result.isConfirmed()).isFalse();
            assertThat(result.getMessage()).contains("capacity");
        }

        @Test
        @DisplayName("should throw exception when start date is after end date")
        void should_ThrowException_When_StartDateAfterEndDate() {
            String requestId = UUID.randomUUID().toString();
            AvailabilityRequest request = new AvailabilityRequest();
            request.setRequestId(requestId);
            request.setStartDate(LocalDate.now().plusDays(5));
            request.setEndDate(LocalDate.now().plusDays(1)); // Before start date

            assertThatThrownBy(() -> roomService.confirmAvailability(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start date must be before end date");
        }
    }

    @Nested
    @DisplayName("releaseRoom")
    class ReleaseRoom {
        @Test
        @DisplayName("should release room and make it available")
        void should_ReleaseRoom_When_RoomExists() {
            String requestId = UUID.randomUUID().toString();
            testRoom.setAvailable(false);

            given(roomRepository.findByIdWithLock(1L)).willReturn(Optional.of(testRoom));
            given(roomRepository.save(any(Room.class))).willReturn(testRoom);

            AvailabilityResponse result = roomService.releaseRoom(1L, requestId);

            assertThat(result.isConfirmed()).isFalse();
            assertThat(result.getMessage()).contains("released");
            verify(roomRepository, times(1)).save(any(Room.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when room does not exist")
        void should_ThrowResourceNotFoundException_When_RoomDoesNotExist() {
            given(roomRepository.findByIdWithLock(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> roomService.releaseRoom(999L, "req-123"))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getRecommendedRooms")
    class GetRecommendedRooms {
        @Test
        @DisplayName("should return rooms ordered by times booked when no filters")
        void should_ReturnRoomsOrderedByTimesBooked_When_NoFilters() {
            given(roomRepository.findAvailableRoomsOrderByTimesBookedAsc()).willReturn(List.of(testRoom));
            given(roomMapper.toResponseList(List.of(testRoom))).willReturn(List.of(testResponse));

            List<RoomResponse> result = roomService.getRecommendedRooms(null, null, null);

            assertThat(result).hasSize(1);
            verify(roomRepository, times(1)).findAvailableRoomsOrderByTimesBookedAsc();
        }

        @Test
        @DisplayName("should filter by hotel when hotel ID provided")
        void should_FilterByHotel_When_HotelIdProvided() {
            given(roomRepository.findAvailableRoomsByHotelIdOrderByTimesBookedAsc(1L)).willReturn(List.of(testRoom));
            given(roomMapper.toResponseList(List.of(testRoom))).willReturn(List.of(testResponse));

            List<RoomResponse> result = roomService.getRecommendedRooms(1L, null, null);

            assertThat(result).hasSize(1);
            verify(roomRepository, times(1)).findAvailableRoomsByHotelIdOrderByTimesBookedAsc(1L);
        }
    }
}
