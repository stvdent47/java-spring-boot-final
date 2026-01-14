package mephi.hotelservice.service;

import mephi.hotelservice.dto.HotelRequest;
import mephi.hotelservice.dto.HotelResponse;
import mephi.hotelservice.entity.Hotel;
import mephi.hotelservice.exception.DuplicateResourceException;
import mephi.hotelservice.exception.ResourceNotFoundException;
import mephi.hotelservice.mapper.HotelMapper;
import mephi.hotelservice.repository.HotelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HotelService Unit Tests")
class HotelServiceTest {
    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private HotelMapper hotelMapper;

    @InjectMocks
    private HotelService hotelService;

    private Hotel testHotel;
    private HotelRequest testRequest;
    private HotelResponse testResponse;

    @BeforeEach
    void setUp() {
        testHotel = new Hotel();
        testHotel.setId(1L);
        testHotel.setName("Grand Hotel");
        testHotel.setCity("Moscow");
        testHotel.setCountry("Russia");
        testHotel.setAddress("Red Square 1");
        testHotel.setStarRating(5);

        testRequest = new HotelRequest(
            "Grand Hotel",
            "Red Square 1",
            "Moscow",
            "Russia",
            5
        );

        testResponse = HotelResponse.builder()
            .id(1L)
            .name("Grand Hotel")
            .city("Moscow")
            .country("Russia")
            .address("Red Square 1")
            .starRating(5)
            .build();
    }

    @Nested
    @DisplayName("getAllHotels")
    class GetAllHotels {
        @Test
        @DisplayName("should return all hotels when hotels exist")
        void should_ReturnAllHotels_When_HotelsExist() {
            Hotel secondHotel = new Hotel();
            secondHotel.setId(2L);
            secondHotel.setName("Budget Inn");

            List<Hotel> hotels = Arrays.asList(testHotel, secondHotel);
            given(hotelRepository.findAll()).willReturn(hotels);
            given(hotelMapper.toResponseWithoutRooms(any(Hotel.class))).willReturn(testResponse);

            List<HotelResponse> result = hotelService.getAllHotels();

            assertThat(result).hasSize(2);
            verify(hotelRepository, times(1)).findAll();
            verify(hotelMapper, times(2)).toResponseWithoutRooms(any(Hotel.class));
        }

        @Test
        @DisplayName("should return empty list when no hotels exist")
        void should_ReturnEmptyList_When_NoHotelsExist() {
            given(hotelRepository.findAll()).willReturn(List.of());

            List<HotelResponse> result = hotelService.getAllHotels();

            assertThat(result).isEmpty();
            verify(hotelRepository, times(1)).findAll();
            verify(hotelMapper, never()).toResponseWithoutRooms(any(Hotel.class));
        }
    }

    @Nested
    @DisplayName("getHotelById")
    class GetHotelById {
        @Test
        @DisplayName("should return hotel with rooms when hotel exists")
        void should_ReturnHotelWithRooms_When_HotelExists() {
            given(hotelRepository.findByIdWithRooms(1L)).willReturn(Optional.of(testHotel));
            given(hotelMapper.toResponse(testHotel)).willReturn(testResponse);

            HotelResponse result = hotelService.getHotelById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Grand Hotel");
            verify(hotelRepository, times(1)).findByIdWithRooms(1L);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when hotel does not exist")
        void should_ThrowResourceNotFoundException_When_HotelDoesNotExist() {
            given(hotelRepository.findByIdWithRooms(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> hotelService.getHotelById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Hotel")
                .hasMessageContaining("999");

            verify(hotelRepository, times(1)).findByIdWithRooms(999L);
            verify(hotelMapper, never()).toResponse(any(Hotel.class));
        }
    }

    @Nested
    @DisplayName("createHotel")
    class CreateHotel {
        @Test
        @DisplayName("should create hotel when name and address combination is unique")
        void should_CreateHotel_When_NameAndAddressUnique() {
            given(hotelRepository.existsByNameAndAddress("Grand Hotel", "Red Square 1")).willReturn(false);
            given(hotelMapper.toEntity(testRequest)).willReturn(testHotel);
            given(hotelRepository.save(testHotel)).willReturn(testHotel);
            given(hotelMapper.toResponseWithoutRooms(testHotel)).willReturn(testResponse);

            HotelResponse result = hotelService.createHotel(testRequest);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Grand Hotel");
            verify(hotelRepository, times(1)).existsByNameAndAddress("Grand Hotel", "Red Square 1");
            verify(hotelRepository, times(1)).save(testHotel);
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when hotel name and address already exists")
        void should_ThrowDuplicateResourceException_When_NameAndAddressExists() {
            given(hotelRepository.existsByNameAndAddress("Grand Hotel", "Red Square 1")).willReturn(true);

            assertThatThrownBy(() -> hotelService.createHotel(testRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Grand Hotel");

            verify(hotelRepository, times(1)).existsByNameAndAddress("Grand Hotel", "Red Square 1");
            verify(hotelRepository, never()).save(any(Hotel.class));
        }
    }

    @Nested
    @DisplayName("updateHotel")
    class UpdateHotel {
        @Test
        @DisplayName("should update hotel when hotel exists and name/address unchanged")
        void should_UpdateHotel_When_HotelExistsAndNameUnchanged() {
            given(hotelRepository.findById(1L)).willReturn(Optional.of(testHotel));
            doNothing().when(hotelMapper).updateEntityFromRequest(testRequest, testHotel);
            given(hotelRepository.save(any(Hotel.class))).willReturn(testHotel);
            given(hotelMapper.toResponseWithoutRooms(any(Hotel.class))).willReturn(testResponse);

            HotelResponse result = hotelService.updateHotel(1L, testRequest);

            assertThat(result).isNotNull();
            verify(hotelRepository, times(1)).findById(1L);
            verify(hotelRepository, times(1)).save(any(Hotel.class));
            verify(hotelRepository, never()).existsByNameAndAddress(anyString(), anyString());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when hotel does not exist")
        void should_ThrowResourceNotFoundException_When_HotelDoesNotExist() {
            given(hotelRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> hotelService.updateHotel(999L, testRequest))
                .isInstanceOf(ResourceNotFoundException.class);

            verify(hotelRepository, never()).save(any(Hotel.class));
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when new name/address conflicts")
        void should_ThrowDuplicateResourceException_When_NameAddressConflicts() {
            HotelRequest updateRequest = new HotelRequest(
                "Different Hotel",
                "Different Address",
                "Moscow",
                "Russia",
                4
            );

            given(hotelRepository.findById(1L)).willReturn(Optional.of(testHotel));
            given(hotelRepository.existsByNameAndAddress("Different Hotel", "Different Address")).willReturn(true);

            assertThatThrownBy(() -> hotelService.updateHotel(1L, updateRequest))
                .isInstanceOf(DuplicateResourceException.class);

            verify(hotelRepository, never()).save(any(Hotel.class));
        }
    }

    @Nested
    @DisplayName("deleteHotel")
    class DeleteHotel {
        @Test
        @DisplayName("should delete hotel when hotel exists")
        void should_DeleteHotel_When_HotelExists() {
            given(hotelRepository.existsById(1L)).willReturn(true);
            doNothing().when(hotelRepository).deleteById(1L);

            hotelService.deleteHotel(1L);

            verify(hotelRepository, times(1)).existsById(1L);
            verify(hotelRepository, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when hotel does not exist")
        void should_ThrowResourceNotFoundException_When_HotelDoesNotExist() {
            given(hotelRepository.existsById(999L)).willReturn(false);

            assertThatThrownBy(() -> hotelService.deleteHotel(999L))
                .isInstanceOf(ResourceNotFoundException.class);

            verify(hotelRepository, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("getHotelsByCity")
    class GetHotelsByCity {
        @Test
        @DisplayName("should return hotels when city has hotels")
        void should_ReturnHotels_When_CityHasHotels() {
            List<Hotel> moscowHotels = List.of(testHotel);
            given(hotelRepository.findByCity("Moscow")).willReturn(moscowHotels);
            given(hotelMapper.toResponseWithoutRooms(testHotel)).willReturn(testResponse);

            List<HotelResponse> result = hotelService.getHotelsByCity("Moscow");

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getCity()).isEqualTo("Moscow");
        }

        @Test
        @DisplayName("should return empty list when city has no hotels")
        void should_ReturnEmptyList_When_CityHasNoHotels() {
            given(hotelRepository.findByCity("Unknown City")).willReturn(List.of());

            List<HotelResponse> result = hotelService.getHotelsByCity("Unknown City");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getHotelsByMinStarRating")
    class GetHotelsByMinStarRating {
        @Test
        @DisplayName("should return hotels with minimum star rating")
        void should_ReturnHotels_When_MinStarsProvided() {
            List<Hotel> fiveStarHotels = List.of(testHotel);
            given(hotelRepository.findByStarRatingGreaterThanEqual(5)).willReturn(fiveStarHotels);
            given(hotelMapper.toResponseWithoutRooms(testHotel)).willReturn(testResponse);

            List<HotelResponse> result = hotelService.getHotelsByMinStarRating(5);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getStarRating()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("searchHotelsByName")
    class SearchHotelsByName {
        @Test
        @DisplayName("should return matching hotels when search term matches")
        void should_ReturnHotels_When_NameMatches() {
            given(hotelRepository.findByNameContainingIgnoreCase("Grand")).willReturn(List.of(testHotel));
            given(hotelMapper.toResponseWithoutRooms(testHotel)).willReturn(testResponse);

            List<HotelResponse> result = hotelService.searchHotelsByName("Grand");

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getName()).contains("Grand");
        }
    }

    @Nested
    @DisplayName("existsById")
    class ExistsById {
        @Test
        @DisplayName("should return true when hotel exists")
        void should_ReturnTrue_When_HotelExists() {
            given(hotelRepository.existsById(1L)).willReturn(true);

            boolean result = hotelService.existsById(1L);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when hotel does not exist")
        void should_ReturnFalse_When_HotelDoesNotExist() {
            given(hotelRepository.existsById(999L)).willReturn(false);

            boolean result = hotelService.existsById(999L);

            assertThat(result).isFalse();
        }
    }
}
