package mephi.hotelservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelResponse {
    private Long id;
    private String name;
    private String address;
    private String city;
    private String country;
    private Integer starRating;
    private Integer totalRooms;
    private Integer availableRooms;
    private List<RoomResponse> rooms;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
