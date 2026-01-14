package mephi.hotelservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final RoomMapper roomMapper;

    private final Map<String, AvailabilityResponse> processedRequests = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public List<RoomResponse> getAllRooms() {
        log.debug("Fetching all rooms");

        List<Room> rooms = roomRepository.findAll();

        return roomMapper.toResponseList(rooms);
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long id) {
        log.debug("Fetching room with id: {}", id);

        Room room = roomRepository.findByIdWithHotel(id)
            .orElseThrow(() -> new ResourceNotFoundException("Room", id));

        return roomMapper.toResponse(room);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByHotelId(Long hotelId) {
        log.debug("Fetching rooms for hotel: {}", hotelId);

        if (!hotelRepository.existsById(hotelId)) {
            throw new ResourceNotFoundException("Hotel", hotelId);
        }

        List<Room> rooms = roomRepository.findByHotelId(hotelId);

        return roomMapper.toResponseList(rooms);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getAvailableRooms() {
        log.debug("Fetching all available rooms");

        List<Room> rooms = roomRepository.findByAvailable(true);

        return roomMapper.toResponseList(rooms);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getAvailableRoomsByHotelId(Long hotelId) {
        log.debug("Fetching available rooms for hotel: {}", hotelId);

        if (!hotelRepository.existsById(hotelId)) {
            throw new ResourceNotFoundException("Hotel", hotelId);
        }

        List<Room> rooms = roomRepository.findAvailableRoomsByHotelId(hotelId);

        return roomMapper.toResponseList(rooms);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getAvailableRoomsByType(RoomType roomType) {
        log.debug("Fetching available rooms of type: {}", roomType);

        List<Room> rooms = roomRepository.findAvailableRoomsByTypeOrderByTimesBookedAsc(roomType);

        return roomMapper.toResponseList(rooms);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getAvailableRoomsByCapacity(Integer guestCount) {
        log.debug("Fetching available rooms for {} guests", guestCount);

        List<Room> rooms = roomRepository.findAvailableRoomsByCapacity(guestCount);

        return roomMapper.toResponseList(rooms);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getAvailableRoomsByMaxPrice(BigDecimal maxPrice) {
        log.debug("Fetching available rooms with max price: {}", maxPrice);

        List<Room> rooms = roomRepository.findAvailableRoomsByMaxPrice(maxPrice);

        return roomMapper.toResponseList(rooms);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getAvailableRoomsByHotelIdAndType(Long hotelId, RoomType roomType) {
        log.debug("Fetching available rooms for hotel: {} and type: {}", hotelId, roomType);

        if (!hotelRepository.existsById(hotelId)) {
            throw new ResourceNotFoundException("Hotel", hotelId);
        }

        List<Room> rooms = roomRepository.findAvailableRoomsByHotelIdAndType(hotelId, roomType);

        return roomMapper.toResponseList(rooms);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getRecommendedRooms(Long hotelId, RoomType roomType, Integer guestCount) {
        log.debug("Getting recommended rooms: hotelId={}, roomType={}, guestCount={}", hotelId, roomType, guestCount);

        List<Room> rooms;

        if (hotelId != null && roomType != null) {
            rooms = roomRepository.findAvailableRoomsByHotelIdAndType(hotelId, roomType);
        }
        else if (hotelId != null) {
            rooms = roomRepository.findAvailableRoomsByHotelIdOrderByTimesBookedAsc(hotelId);
        }
        else if (roomType != null) {
            rooms = roomRepository.findAvailableRoomsByTypeOrderByTimesBookedAsc(roomType);
        }
        else if (guestCount != null) {
            rooms = roomRepository.findAvailableRoomsByCapacityOrderByTimesBookedAsc(guestCount);
        }
        else {
            rooms = roomRepository.findAvailableRoomsOrderByTimesBookedAsc();
        }

        if (guestCount != null) {
            rooms = rooms.stream()
                .filter(r -> r.getMaxOccupancy() >= guestCount)
                .toList();
        }

        return roomMapper.toResponseList(rooms);
    }

    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        log.info("Creating new room: {} in hotel {}", request.getRoomNumber(), request.getHotelId());

        Hotel hotel = hotelRepository.findById(request.getHotelId())
            .orElseThrow(() -> new ResourceNotFoundException("Hotel", request.getHotelId()));

        if (roomRepository.existsByHotelIdAndRoomNumber(request.getHotelId(), request.getRoomNumber())) {
            throw new DuplicateResourceException(
                "Room", "room number in hotel",
                request.getRoomNumber()
            );
        }

        Room room = roomMapper.toEntity(request);
        room.setHotel(hotel);
        if (room.getAvailable() == null) {
            room.setAvailable(true);
        }
        room.setTimesBooked(0);

        Room savedRoom = roomRepository.save(room);

        log.info("Room created successfully with id: {}", savedRoom.getId());

        return roomMapper.toResponse(savedRoom);
    }

    @Transactional
    public RoomResponse updateRoom(Long id, RoomRequest request) {
        log.info("Updating room with id: {}", id);

        Room room = roomRepository.findByIdWithHotel(id)
            .orElseThrow(() -> new ResourceNotFoundException("Room", id));

        if (request.getHotelId() != null && !request.getHotelId().equals(room.getHotel().getId())) {
            Hotel newHotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", request.getHotelId()));
            room.setHotel(newHotel);
        }

        if (request.getRoomNumber() != null && !request.getRoomNumber().equals(room.getRoomNumber())) {
            Long hotelId = request.getHotelId() != null ? request.getHotelId() : room.getHotel().getId();
            if (roomRepository.existsByHotelIdAndRoomNumber(hotelId, request.getRoomNumber())) {
                throw new DuplicateResourceException(
                    "Room", "room number in hotel",
                    request.getRoomNumber()
                );
            }
        }

        roomMapper.updateEntityFromRequest(request, room);
        Room updatedRoom = roomRepository.save(room);

        log.info("Room updated successfully: {}", updatedRoom.getId());

        return roomMapper.toResponse(updatedRoom);
    }

    @Transactional
    public void deleteRoom(Long id) {
        log.info("Deleting room with id: {}", id);

        if (!roomRepository.existsById(id)) {
            throw new ResourceNotFoundException("Room", id);
        }

        roomRepository.deleteById(id);

        log.info("Room deleted successfully: {}", id);
    }

    @Transactional
    public AvailabilityResponse confirmAvailability(Long roomId, AvailabilityRequest request) {
        log.info("Confirming availability for room {} with requestId: {}", roomId, request.getRequestId());

        if (processedRequests.containsKey(request.getRequestId())) {
            log.info("Request {} already processed, returning cached response", request.getRequestId());

            return processedRequests.get(request.getRequestId());
        }

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        Room room = roomRepository.findByIdWithLock(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));

        if (!Boolean.TRUE.equals(room.getAvailable())) {
            AvailabilityResponse response = AvailabilityResponse.builder()
                .roomId(roomId)
                .hotelId(room.getHotel().getId())
                .requestId(request.getRequestId())
                .confirmed(false)
                .message("Room is not available")
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

            processedRequests.put(request.getRequestId(), response);

            return response;
        }

        if (request.getGuestCount() != null && request.getGuestCount() > room.getMaxOccupancy()) {
            AvailabilityResponse response = AvailabilityResponse.builder()
                .roomId(roomId)
                .hotelId(room.getHotel().getId())
                .requestId(request.getRequestId())
                .confirmed(false)
                .message("Room capacity (" + room.getMaxOccupancy() + ") is less than guest count (" + request.getGuestCount() + ")")
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

            processedRequests.put(request.getRequestId(), response);

            return response;
        }

        long nights = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        BigDecimal totalPrice = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));

        room.setAvailable(false);
        room.incrementTimesBooked();
        roomRepository.save(room);

        log.info("Room {} confirmed for booking, requestId: {}", roomId, request.getRequestId());

        AvailabilityResponse response = AvailabilityResponse.builder()
            .roomId(roomId)
            .hotelId(room.getHotel().getId())
            .requestId(request.getRequestId())
            .confirmed(true)
            .message("Room availability confirmed")
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .totalPrice(totalPrice)
            .nights((int) nights)
            .build();

        processedRequests.put(request.getRequestId(), response);

        return response;
    }

    @Transactional
    public AvailabilityResponse releaseRoom(Long roomId, String requestId) {
        log.info("Releasing room {} for requestId: {}", roomId, requestId);

        Room room = roomRepository.findByIdWithLock(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));

        room.setAvailable(true);
        roomRepository.save(room);

        processedRequests.remove(requestId);

        log.info("Room {} released successfully", roomId);

        return AvailabilityResponse.builder()
            .roomId(roomId)
            .hotelId(room.getHotel().getId())
            .requestId(requestId)
            .confirmed(false)
            .message("Room released successfully")
            .build();
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return roomRepository.existsById(id);
    }

    @Transactional(readOnly = true)
    public Long countAvailableRoomsByHotelId(Long hotelId) {
        return roomRepository.countAvailableRoomsByHotelId(hotelId);
    }
}
