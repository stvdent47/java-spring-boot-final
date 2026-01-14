package mephi.hotelservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityResponse {
    private Long roomId;
    private Long hotelId;
    private String requestId;
    private boolean confirmed;
    private String message;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalPrice;
    private Integer nights;
}
