package mephi.hotelservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mephi.hotelservice.dto.HotelStatisticsResponse;
import mephi.hotelservice.dto.SystemStatisticsResponse;
import mephi.hotelservice.service.StatisticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "Hotel and room occupancy statistics API")
@SecurityRequirement(name = "bearerAuth")
public class StatisticsController {
    private final StatisticsService statisticsService;

    @GetMapping("/system")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get system-wide statistics",
        description = "Get overall statistics for all hotels and rooms (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public ResponseEntity<SystemStatisticsResponse> getSystemStatistics() {
        log.debug("REST request to get system-wide statistics");

        SystemStatisticsResponse statistics = statisticsService.getSystemStatistics();

        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/hotels")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get all hotels statistics",
        description = "Get statistics for all hotels sorted by total bookings (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public ResponseEntity<List<HotelStatisticsResponse>> getAllHotelStatistics() {
        log.debug("REST request to get all hotel statistics");

        List<HotelStatisticsResponse> statistics = statisticsService.getAllHotelStatistics();

        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/hotels/{hotelId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get hotel statistics",
        description = "Get detailed statistics for a specific hotel (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
        @ApiResponse(responseCode = "404", description = "Hotel not found")
    })
    public ResponseEntity<HotelStatisticsResponse> getHotelStatistics(
        @Parameter(description = "Hotel ID") @PathVariable Long hotelId
    ) {
        log.debug("REST request to get statistics for hotel: {}", hotelId);

        HotelStatisticsResponse statistics = statisticsService.getHotelStatistics(hotelId);

        return ResponseEntity.ok(statistics);
    }
}
