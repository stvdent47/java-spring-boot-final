package mephi.hotelservice.repository;

import jakarta.persistence.LockModeType;
import mephi.hotelservice.entity.Room;
import mephi.hotelservice.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByHotelId(Long hotelId);

    List<Room> findByHotelIdAndAvailable(Long hotelId, Boolean available);

    List<Room> findByRoomType(RoomType roomType);

    List<Room> findByAvailable(Boolean available);

    @Query("SELECT r FROM Room r WHERE r.hotel.id = :hotelId AND r.available = true")
    List<Room> findAvailableRoomsByHotelId(@Param("hotelId") Long hotelId);

    @Query("SELECT r FROM Room r WHERE r.available = true AND r.maxOccupancy >= :guestCount")
    List<Room> findAvailableRoomsByCapacity(@Param("guestCount") Integer guestCount);

    @Query("SELECT r FROM Room r WHERE r.available = true AND r.pricePerNight <= :maxPrice")
    List<Room> findAvailableRoomsByMaxPrice(@Param("maxPrice") BigDecimal maxPrice);

    @Query("SELECT r FROM Room r WHERE r.hotel.id = :hotelId AND r.available = true AND r.roomType = :roomType")
    List<Room> findAvailableRoomsByHotelIdAndType(
        @Param("hotelId") Long hotelId,
        @Param("roomType") RoomType roomType
    );

    @Query("SELECT r FROM Room r WHERE r.available = true ORDER BY r.timesBooked ASC")
    List<Room> findAvailableRoomsOrderByTimesBookedAsc();

    @Query("SELECT r FROM Room r WHERE r.hotel.id = :hotelId AND r.available = true ORDER BY r.timesBooked ASC")
    List<Room> findAvailableRoomsByHotelIdOrderByTimesBookedAsc(@Param("hotelId") Long hotelId);

    @Query("SELECT r FROM Room r WHERE r.available = true AND r.roomType = :roomType ORDER BY r.timesBooked ASC")
    List<Room> findAvailableRoomsByTypeOrderByTimesBookedAsc(@Param("roomType") RoomType roomType);

    @Query("SELECT r FROM Room r WHERE r.available = true AND r.maxOccupancy >= :guestCount ORDER BY r.timesBooked ASC")
    List<Room> findAvailableRoomsByCapacityOrderByTimesBookedAsc(@Param("guestCount") Integer guestCount);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdWithLock(@Param("id") Long id);

    boolean existsByHotelIdAndRoomNumber(Long hotelId, String roomNumber);

    @Query("SELECT COUNT(r) FROM Room r WHERE r.hotel.id = :hotelId AND r.available = true")
    Long countAvailableRoomsByHotelId(@Param("hotelId") Long hotelId);

    @Query("SELECT r FROM Room r JOIN FETCH r.hotel WHERE r.id = :id")
    Optional<Room> findByIdWithHotel(@Param("id") Long id);
}
