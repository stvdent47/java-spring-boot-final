package mephi.hotelservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelStatisticsResponse {
    private Long hotelId;
    private String hotelName;
    private Integer totalRooms;
    private Integer availableRooms;
    private Integer occupiedRooms;
    private BigDecimal occupancyRate;
    private Integer totalBookings;
    private BigDecimal averageTimesBooked;
    private List<RoomStatistics> roomStatistics;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoomStatistics {
        private Long roomId;
        private String roomNumber;
        private String roomType;
        private Boolean available;
        private Integer timesBooked;
        private BigDecimal pricePerNight;
    }
}
