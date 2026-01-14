package mephi.bookingservice.dto.hotel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {
    private Long id;
    private Long hotelId;
    private String hotelName;
    private String roomNumber;
    private String roomType;
    private BigDecimal pricePerNight;
    private Integer maxOccupancy;
    private Boolean available;
    private Integer timesBooked;
}
