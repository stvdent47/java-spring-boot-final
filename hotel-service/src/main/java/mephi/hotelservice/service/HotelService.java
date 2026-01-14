package mephi.hotelservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mephi.hotelservice.dto.HotelRequest;
import mephi.hotelservice.dto.HotelResponse;
import mephi.hotelservice.entity.Hotel;
import mephi.hotelservice.exception.DuplicateResourceException;
import mephi.hotelservice.exception.ResourceNotFoundException;
import mephi.hotelservice.mapper.HotelMapper;
import mephi.hotelservice.repository.HotelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotelService {
    private final HotelRepository hotelRepository;
    private final HotelMapper hotelMapper;

    @Transactional(readOnly = true)
    public List<HotelResponse> getAllHotels() {
        log.debug("Fetching all hotels");

        List<Hotel> hotels = hotelRepository.findAll();

        return hotels.stream()
            .map(hotelMapper::toResponseWithoutRooms)
            .toList();
    }

    @Transactional(readOnly = true)
    public HotelResponse getHotelById(Long id) {
        log.debug("Fetching hotel with id: {}", id);

        Hotel hotel = hotelRepository.findByIdWithRooms(id)
            .orElseThrow(() -> new ResourceNotFoundException("Hotel", id));

        return hotelMapper.toResponse(hotel);
    }

    @Transactional(readOnly = true)
    public HotelResponse getHotelByIdWithoutRooms(Long id) {
        log.debug("Fetching hotel (without rooms) with id: {}", id);

        Hotel hotel = hotelRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Hotel", id));

        return hotelMapper.toResponseWithoutRooms(hotel);
    }

    @Transactional(readOnly = true)
    public List<HotelResponse> getHotelsByCity(String city) {
        log.debug("Fetching hotels in city: {}", city);

        List<Hotel> hotels = hotelRepository.findByCity(city);

        return hotels.stream()
            .map(hotelMapper::toResponseWithoutRooms)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<HotelResponse> getHotelsByCountry(String country) {
        log.debug("Fetching hotels in country: {}", country);

        List<Hotel> hotels = hotelRepository.findByCountry(country);

        return hotels.stream()
            .map(hotelMapper::toResponseWithoutRooms)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<HotelResponse> getHotelsByMinStarRating(Integer minStars) {
        log.debug("Fetching hotels with minimum {} stars", minStars);

        List<Hotel> hotels = hotelRepository.findByStarRatingGreaterThanEqual(minStars);

        return hotels.stream()
            .map(hotelMapper::toResponseWithoutRooms)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<HotelResponse> searchHotelsByName(String name) {
        log.debug("Searching hotels by name: {}", name);

        List<Hotel> hotels = hotelRepository.findByNameContainingIgnoreCase(name);

        return hotels.stream()
            .map(hotelMapper::toResponseWithoutRooms)
            .toList();
    }

    @Transactional
    public HotelResponse createHotel(HotelRequest request) {
        log.info("Creating new hotel: {}", request.getName());

        if (hotelRepository.existsByNameAndAddress(request.getName(), request.getAddress())) {
            throw new DuplicateResourceException(
                "Hotel", "name and address",
                request.getName() + " at " + request.getAddress()
            );
        }

        Hotel hotel = hotelMapper.toEntity(request);
        Hotel savedHotel = hotelRepository.save(hotel);

        log.info("Hotel created successfully with id: {}", savedHotel.getId());

        return hotelMapper.toResponseWithoutRooms(savedHotel);
    }

    @Transactional
    public HotelResponse updateHotel(Long id, HotelRequest request) {
        log.info("Updating hotel with id: {}", id);

        Hotel hotel = hotelRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Hotel", id));

        if (!hotel.getName().equals(request.getName()) || !hotel.getAddress().equals(request.getAddress())) {
            if (hotelRepository.existsByNameAndAddress(request.getName(), request.getAddress())) {
                throw new DuplicateResourceException(
                    "Hotel", "name and address",
                    request.getName() + " at " + request.getAddress()
                );
            }
        }

        hotelMapper.updateEntityFromRequest(request, hotel);
        Hotel updatedHotel = hotelRepository.save(hotel);

        log.info("Hotel updated successfully: {}", updatedHotel.getId());

        return hotelMapper.toResponseWithoutRooms(updatedHotel);
    }

    @Transactional
    public void deleteHotel(Long id) {
        log.info("Deleting hotel with id: {}", id);

        if (!hotelRepository.existsById(id)) {
            throw new ResourceNotFoundException("Hotel", id);
        }

        hotelRepository.deleteById(id);

        log.info("Hotel deleted successfully: {}", id);
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return hotelRepository.existsById(id);
    }
}
