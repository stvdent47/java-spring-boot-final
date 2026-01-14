package mephi.hotelservice.mapper;

import mephi.hotelservice.dto.HotelRequest;
import mephi.hotelservice.dto.HotelResponse;
import mephi.hotelservice.entity.Hotel;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {RoomMapper.class})
public interface HotelMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rooms", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Hotel toEntity(HotelRequest request);

    @Named("withRooms")
    @Mapping(target = "totalRooms", expression = "java(hotel.getRooms() != null ? hotel.getRooms().size() : 0)")
    @Mapping(target = "availableRooms", expression = "java(countAvailableRooms(hotel))")
    @Mapping(target = "rooms", source = "rooms")
    HotelResponse toResponse(Hotel hotel);

    @Named("withoutRooms")
    @Mapping(target = "totalRooms", expression = "java(hotel.getRooms() != null ? hotel.getRooms().size() : 0)")
    @Mapping(target = "availableRooms", expression = "java(countAvailableRooms(hotel))")
    @Mapping(target = "rooms", ignore = true)
    HotelResponse toResponseWithoutRooms(Hotel hotel);

    @IterableMapping(qualifiedByName = "withoutRooms")
    List<HotelResponse> toResponseList(List<Hotel> hotels);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rooms", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(HotelRequest request, @MappingTarget Hotel hotel);

    default int countAvailableRooms(Hotel hotel) {
        if (hotel.getRooms() == null) {
            return 0;
        }

        return (int) hotel.getRooms().stream()
            .filter(room -> Boolean.TRUE.equals(room.getAvailable()))
            .count();
    }
}
