package mephi.hotelservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mephi.hotelservice.dto.HotelRequest;
import mephi.hotelservice.dto.HotelResponse;
import mephi.hotelservice.service.HotelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
@Tag(name = "Hotels", description = "Hotel management API")
public class HotelController {
    private final HotelService hotelService;

    @GetMapping
    @Operation(summary = "Get all hotels", description = "Retrieve a list of all hotels")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved hotels list")
    public ResponseEntity<List<HotelResponse>> getAllHotels() {
        log.debug("REST request to get all hotels");

        List<HotelResponse> hotels = hotelService.getAllHotels();

        return ResponseEntity.ok(hotels);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get hotel by ID", description = "Retrieve a specific hotel by its ID, including rooms")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved hotel"),
        @ApiResponse(responseCode = "404", description = "Hotel not found")
    })
    public ResponseEntity<HotelResponse> getHotelById(
        @Parameter(description = "Hotel ID") @PathVariable Long id
    ) {
        log.debug("REST request to get hotel: {}", id);

        HotelResponse hotel = hotelService.getHotelById(id);

        return ResponseEntity.ok(hotel);
    }

    @GetMapping("/search")
    @Operation(summary = "Search hotels by name", description = "Search hotels by partial name match (case-insensitive)")
    public ResponseEntity<List<HotelResponse>> searchHotels(
        @Parameter(description = "Hotel name to search for") @RequestParam String name
    ) {
        log.debug("REST request to search hotels by name: {}", name);

        List<HotelResponse> hotels = hotelService.searchHotelsByName(name);

        return ResponseEntity.ok(hotels);
    }

    @GetMapping("/city/{city}")
    @Operation(summary = "Get hotels by city", description = "Retrieve all hotels in a specific city")
    public ResponseEntity<List<HotelResponse>> getHotelsByCity(
        @Parameter(description = "City name") @PathVariable String city
    ) {
        log.debug("REST request to get hotels in city: {}", city);

        List<HotelResponse> hotels = hotelService.getHotelsByCity(city);

        return ResponseEntity.ok(hotels);
    }

    @GetMapping("/country/{country}")
    @Operation(summary = "Get hotels by country", description = "Retrieve all hotels in a specific country")
    public ResponseEntity<List<HotelResponse>> getHotelsByCountry(
        @Parameter(description = "Country name") @PathVariable String country
    ) {
        log.debug("REST request to get hotels in country: {}", country);

        List<HotelResponse> hotels = hotelService.getHotelsByCountry(country);

        return ResponseEntity.ok(hotels);
    }

    @GetMapping("/stars/{minStars}")
    @Operation(summary = "Get hotels by minimum star rating", description = "Retrieve hotels with at least the specified star rating")
    public ResponseEntity<List<HotelResponse>> getHotelsByMinStarRating(
        @Parameter(description = "Minimum star rating (1-5)") @PathVariable Integer minStars
    ) {
        log.debug("REST request to get hotels with minimum {} stars", minStars);

        List<HotelResponse> hotels = hotelService.getHotelsByMinStarRating(minStars);

        return ResponseEntity.ok(hotels);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Create a new hotel", description = "Create a new hotel (Admin only)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Hotel created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
        @ApiResponse(responseCode = "409", description = "Hotel already exists")
    })
    public ResponseEntity<HotelResponse> createHotel(
        @Valid @RequestBody HotelRequest request
    ) {
        log.info("REST request to create hotel: {}", request.getName());

        HotelResponse createdHotel = hotelService.createHotel(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdHotel);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Update a hotel", description = "Update an existing hotel (Admin only)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Hotel updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
        @ApiResponse(responseCode = "404", description = "Hotel not found"),
        @ApiResponse(responseCode = "409", description = "Duplicate hotel name/address")
    })
    public ResponseEntity<HotelResponse> updateHotel(
        @Parameter(description = "Hotel ID") @PathVariable Long id,
        @Valid @RequestBody HotelRequest request
    ) {
        log.info("REST request to update hotel: {}", id);

        HotelResponse updatedHotel = hotelService.updateHotel(id, request);

        return ResponseEntity.ok(updatedHotel);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Delete a hotel", description = "Delete a hotel and all its rooms (Admin only)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Hotel deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
        @ApiResponse(responseCode = "404", description = "Hotel not found")
    })
    public ResponseEntity<Void> deleteHotel(
        @Parameter(description = "Hotel ID") @PathVariable Long id
    ) {
        log.info("REST request to delete hotel: {}", id);

        hotelService.deleteHotel(id);

        return ResponseEntity.noContent().build();
    }
}
