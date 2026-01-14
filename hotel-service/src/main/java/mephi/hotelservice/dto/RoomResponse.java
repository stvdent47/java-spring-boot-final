package mephi.hotelservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mephi.hotelservice.entity.RoomType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {
    private Long id;
    private Long hotelId;
    private String hotelName;
    private String roomNumber;
    private RoomType roomType;
    private BigDecimal pricePerNight;
    private Integer maxOccupancy;
    private Boolean available;
    private Integer timesBooked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
