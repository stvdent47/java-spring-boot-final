package mephi.bookingservice.dto.hotel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityRequest {
    private String requestId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer guestCount;
}
