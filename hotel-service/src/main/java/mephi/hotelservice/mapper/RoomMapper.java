package mephi.hotelservice.mapper;

import mephi.hotelservice.dto.RoomRequest;
import mephi.hotelservice.dto.RoomResponse;
import mephi.hotelservice.entity.Room;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoomMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "timesBooked", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "available", defaultValue = "true")
    Room toEntity(RoomRequest request);

    @Mapping(target = "hotelId", source = "hotel.id")
    @Mapping(target = "hotelName", source = "hotel.name")
    RoomResponse toResponse(Room room);

    List<RoomResponse> toResponseList(List<Room> rooms);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "timesBooked", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(RoomRequest request, @MappingTarget Room room);
}
