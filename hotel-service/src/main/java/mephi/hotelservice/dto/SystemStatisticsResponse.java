package mephi.hotelservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemStatisticsResponse {
    private Integer totalHotels;
    private Integer totalRooms;
    private Integer availableRooms;
    private Integer occupiedRooms;
    private BigDecimal overallOccupancyRate;
    private Integer totalBookings;
    private Map<String, Integer> roomsByType;
    private Map<String, BigDecimal> occupancyByType;
    private HotelStatisticsResponse mostBookedHotel;
    private HotelStatisticsResponse leastBookedHotel;
}
