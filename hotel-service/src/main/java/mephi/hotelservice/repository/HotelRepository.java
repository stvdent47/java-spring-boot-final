package mephi.hotelservice.repository;

import mephi.hotelservice.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {
    List<Hotel> findByCity(String city);

    List<Hotel> findByCountry(String country);

    List<Hotel> findByStarRatingGreaterThanEqual(Integer starRating);

    @Query("SELECT h FROM Hotel h WHERE LOWER(h.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Hotel> findByNameContainingIgnoreCase(@Param("name") String name);

    @Query("SELECT h FROM Hotel h LEFT JOIN FETCH h.rooms WHERE h.id = :id")
    Optional<Hotel> findByIdWithRooms(@Param("id") Long id);

    @Query("SELECT DISTINCT h FROM Hotel h LEFT JOIN FETCH h.rooms")
    List<Hotel> findAllWithRooms();

    boolean existsByNameAndAddress(String name, String address);
}
