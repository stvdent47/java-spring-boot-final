package mephi.hotelservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mephi.hotelservice.dto.HotelStatisticsResponse;
import mephi.hotelservice.dto.SystemStatisticsResponse;
import mephi.hotelservice.entity.Hotel;
import mephi.hotelservice.entity.Room;
import mephi.hotelservice.entity.RoomType;
import mephi.hotelservice.exception.ResourceNotFoundException;
import mephi.hotelservice.repository.HotelRepository;
import mephi.hotelservice.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    @Transactional(readOnly = true)
    public HotelStatisticsResponse getHotelStatistics(Long hotelId) {
        log.debug("Generating statistics for hotel: {}", hotelId);

        Hotel hotel = hotelRepository.findByIdWithRooms(hotelId)
            .orElseThrow(() -> new ResourceNotFoundException("Hotel", hotelId));

        List<Room> rooms = hotel.getRooms();
        int totalRooms = rooms.size();
        int availableRooms = (int) rooms.stream().filter(r -> Boolean.TRUE.equals(r.getAvailable())).count();
        int occupiedRooms = totalRooms - availableRooms;
        int totalBookings = rooms.stream().mapToInt(Room::getTimesBooked).sum();

        BigDecimal occupancyRate = totalRooms > 0
            ? BigDecimal.valueOf(occupiedRooms).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalRooms), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        BigDecimal averageTimesBooked = totalRooms > 0
            ? BigDecimal.valueOf(totalBookings).divide(BigDecimal.valueOf(totalRooms), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        List<HotelStatisticsResponse.RoomStatistics> roomStats = rooms.stream()
            .map(room -> HotelStatisticsResponse.RoomStatistics.builder()
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .roomType(room.getRoomType().name())
                .available(room.getAvailable())
                .timesBooked(room.getTimesBooked())
                .pricePerNight(room.getPricePerNight())
                .build()
            )
                .sorted(Comparator.comparing(HotelStatisticsResponse.RoomStatistics::getTimesBooked).reversed())
                .toList();

        return HotelStatisticsResponse.builder()
            .hotelId(hotel.getId())
            .hotelName(hotel.getName())
            .totalRooms(totalRooms)
            .availableRooms(availableRooms)
            .occupiedRooms(occupiedRooms)
            .occupancyRate(occupancyRate)
            .totalBookings(totalBookings)
            .averageTimesBooked(averageTimesBooked)
            .roomStatistics(roomStats)
            .build();
    }

    @Transactional(readOnly = true)
    public SystemStatisticsResponse getSystemStatistics() {
        log.debug("Generating system-wide statistics");

        List<Hotel> hotels = hotelRepository.findAllWithRooms();
        List<Room> allRooms = roomRepository.findAll();

        int totalHotels = hotels.size();
        int totalRooms = allRooms.size();
        int availableRooms = (int) allRooms.stream().filter(r -> Boolean.TRUE.equals(r.getAvailable())).count();
        int occupiedRooms = totalRooms - availableRooms;
        int totalBookings = allRooms.stream().mapToInt(Room::getTimesBooked).sum();

        BigDecimal overallOccupancyRate = totalRooms > 0
            ? BigDecimal.valueOf(occupiedRooms).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalRooms), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        Map<String, Integer> roomsByType = new HashMap<>();
        Map<String, BigDecimal> occupancyByType = new HashMap<>();

        for (RoomType type : RoomType.values()) {
            List<Room> roomsOfType = allRooms.stream()
                .filter(r -> r.getRoomType() == type)
                .toList();

            int countOfType = roomsOfType.size();
            roomsByType.put(type.name(), countOfType);

            if (countOfType > 0) {
                int occupiedOfType = (int) roomsOfType.stream()
                    .filter(r -> !Boolean.TRUE.equals(r.getAvailable()))
                    .count();

                BigDecimal typeOccupancy = BigDecimal.valueOf(occupiedOfType)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(countOfType), 2, RoundingMode.HALF_UP);

                occupancyByType.put(type.name(), typeOccupancy);
            }
            else {
                occupancyByType.put(type.name(), BigDecimal.ZERO);
            }
        }

        HotelStatisticsResponse mostBooked = null;
        HotelStatisticsResponse leastBooked = null;

        if (!hotels.isEmpty()) {
            Hotel mostBookedHotel = hotels.stream()
                .max(Comparator.comparingInt(h -> h.getRooms().stream().mapToInt(Room::getTimesBooked).sum()))
                .orElse(null);

            Hotel leastBookedHotel = hotels.stream()
                .min(Comparator.comparingInt(h -> h.getRooms().stream().mapToInt(Room::getTimesBooked).sum()))
                .orElse(null);

            if (mostBookedHotel != null) {
                mostBooked = getHotelStatistics(mostBookedHotel.getId());
            }
            if (leastBookedHotel != null) {
                leastBooked = getHotelStatistics(leastBookedHotel.getId());
            }
        }

        return SystemStatisticsResponse.builder()
            .totalHotels(totalHotels)
            .totalRooms(totalRooms)
            .availableRooms(availableRooms)
            .occupiedRooms(occupiedRooms)
            .overallOccupancyRate(overallOccupancyRate)
            .totalBookings(totalBookings)
            .roomsByType(roomsByType)
            .occupancyByType(occupancyByType)
            .mostBookedHotel(mostBooked)
            .leastBookedHotel(leastBooked)
            .build();
    }

    @Transactional(readOnly = true)
    public List<HotelStatisticsResponse> getAllHotelStatistics() {
        log.debug("Generating statistics for all hotels");

        List<Hotel> hotels = hotelRepository.findAllWithRooms();

        return hotels.stream()
            .map(hotel -> getHotelStatistics(hotel.getId()))
            .sorted(Comparator.comparing(HotelStatisticsResponse::getTotalBookings).reversed())
            .toList();
    }
}
