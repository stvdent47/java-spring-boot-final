package mephi.hotelservice.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mephi.hotelservice.entity.RoomType;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomRequest {
    @NotNull(message = "Hotel ID is required")
    private Long hotelId;

    @NotBlank(message = "Room number is required")
    @Size(max = 20, message = "Room number must not exceed 20 characters")
    private String roomNumber;

    @NotNull(message = "Room type is required")
    private RoomType roomType;

    @NotNull(message = "Price per night is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price format is invalid")
    private BigDecimal pricePerNight;

    @NotNull(message = "Max occupancy is required")
    @Min(value = 1, message = "Max occupancy must be at least 1")
    @Max(value = 20, message = "Max occupancy must not exceed 20")
    private Integer maxOccupancy;

    private Boolean available;
}
