package mephi.bookingservice.repository;

import mephi.bookingservice.entity.Booking;
import mephi.bookingservice.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingReference(String bookingReference);

    List<Booking> findByUserId(Long userId);

    List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status);

    List<Booking> findByRoomId(Long roomId);

    List<Booking> findByHotelId(Long hotelId);

    List<Booking> findByStatus(BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId ORDER BY b.createdAt DESC")
    List<Booking> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT b FROM Booking b WHERE b.roomId = :roomId AND b.status IN :statuses " +
        "AND ((b.checkInDate <= :checkOut AND b.checkOutDate >= :checkIn))")
    List<Booking> findOverlappingBookings(
        @Param("roomId") Long roomId,
        @Param("checkIn") LocalDate checkIn,
        @Param("checkOut") LocalDate checkOut,
        @Param("statuses") List<BookingStatus> statuses
    );

    @Query("SELECT b FROM Booking b JOIN FETCH b.user WHERE b.id = :id")
    Optional<Booking> findByIdWithUser(@Param("id") Long id);

    @Query("SELECT b FROM Booking b JOIN FETCH b.user WHERE b.bookingReference = :reference")
    Optional<Booking> findByBookingReferenceWithUser(@Param("reference") String reference);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.user.id = :userId AND b.status = :status")
    Long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") BookingStatus status);

    boolean existsByBookingReference(String bookingReference);
}
