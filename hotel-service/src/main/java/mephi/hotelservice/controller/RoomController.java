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
import mephi.hotelservice.dto.AvailabilityRequest;
import mephi.hotelservice.dto.AvailabilityResponse;
import mephi.hotelservice.dto.RoomRequest;
import mephi.hotelservice.dto.RoomResponse;
import mephi.hotelservice.entity.RoomType;
import mephi.hotelservice.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
@Tag(name = "Rooms", description = "Room management and availability API")
public class RoomController {
    private final RoomService roomService;

    @GetMapping
    @Operation(summary = "Get all rooms", description = "Retrieve a list of all rooms")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved rooms list")
    public ResponseEntity<List<RoomResponse>> getAllRooms() {
        log.debug("REST request to get all rooms");

        List<RoomResponse> rooms = roomService.getAllRooms();

        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get room by ID", description = "Retrieve a specific room by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved room"),
        @ApiResponse(responseCode = "404", description = "Room not found")
    })
    public ResponseEntity<RoomResponse> getRoomById(
        @Parameter(description = "Room ID") @PathVariable Long id
    ) {
        log.debug("REST request to get room: {}", id);

        RoomResponse room = roomService.getRoomById(id);

        return ResponseEntity.ok(room);
    }

    @GetMapping("/hotel/{hotelId}")
    @Operation(summary = "Get rooms by hotel", description = "Retrieve all rooms for a specific hotel")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved rooms"),
        @ApiResponse(responseCode = "404", description = "Hotel not found")
    })
    public ResponseEntity<List<RoomResponse>> getRoomsByHotelId(
        @Parameter(description = "Hotel ID") @PathVariable Long hotelId
    ) {
        log.debug("REST request to get rooms for hotel: {}", hotelId);

        List<RoomResponse> rooms = roomService.getRoomsByHotelId(hotelId);

        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/available")
    @Operation(summary = "Get available rooms", description = "Retrieve all available rooms, optionally filtered")
    public ResponseEntity<List<RoomResponse>> getAvailableRooms(
        @Parameter(description = "Hotel ID filter") @RequestParam(required = false) Long hotelId,
        @Parameter(description = "Room type filter") @RequestParam(required = false) RoomType roomType,
        @Parameter(description = "Minimum guest capacity") @RequestParam(required = false) Integer guestCount,
        @Parameter(description = "Maximum price per night") @RequestParam(required = false) BigDecimal maxPrice
    ) {
        log.debug(
            "REST request to get available rooms: hotelId={}, roomType={}, guestCount={}, maxPrice={}",
            hotelId,
            roomType,
            guestCount,
            maxPrice
        );

        List<RoomResponse> rooms;

        if (hotelId != null && roomType != null) {
            rooms = roomService.getAvailableRoomsByHotelIdAndType(hotelId, roomType);
        }
        else if (hotelId != null) {
            rooms = roomService.getAvailableRoomsByHotelId(hotelId);
        }
        else if (roomType != null) {
            rooms = roomService.getAvailableRoomsByType(roomType);
        }
        else if (guestCount != null) {
            rooms = roomService.getAvailableRoomsByCapacity(guestCount);
        }
        else if (maxPrice != null) {
            rooms = roomService.getAvailableRoomsByMaxPrice(maxPrice);
        }
        else {
            rooms = roomService.getAvailableRooms();
        }

        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/recommend")
    @Operation(
        summary = "Get recommended rooms",
        description = "Get available rooms ordered by booking frequency (load balancing). Rooms with fewer bookings are recommended first."
    )
    public ResponseEntity<List<RoomResponse>> getRecommendedRooms(
        @Parameter(description = "Hotel ID filter") @RequestParam(required = false) Long hotelId,
        @Parameter(description = "Room type filter") @RequestParam(required = false) RoomType roomType,
        @Parameter(description = "Minimum guest capacity") @RequestParam(required = false) Integer guestCount
    ) {
        log.debug(
            "REST request to get recommended rooms: hotelId={}, roomType={}, guestCount={}",
            hotelId,
            roomType,
            guestCount
        );

        List<RoomResponse> rooms = roomService.getRecommendedRooms(hotelId, roomType, guestCount);

        return ResponseEntity.ok(rooms);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Create a new room", description = "Create a new room in a hotel (Admin only)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Room created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
        @ApiResponse(responseCode = "404", description = "Hotel not found"),
        @ApiResponse(responseCode = "409", description = "Room number already exists in hotel")
    })
    public ResponseEntity<RoomResponse> createRoom(
        @Valid @RequestBody RoomRequest request
    ) {
        log.info("REST request to create room: {} in hotel {}", request.getRoomNumber(), request.getHotelId());

        RoomResponse createdRoom = roomService.createRoom(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdRoom);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Update a room", description = "Update an existing room (Admin only)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Room updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
        @ApiResponse(responseCode = "404", description = "Room or Hotel not found"),
        @ApiResponse(responseCode = "409", description = "Duplicate room number")
    })
    public ResponseEntity<RoomResponse> updateRoom(
        @Parameter(description = "Room ID") @PathVariable Long id,
        @Valid @RequestBody RoomRequest request
    ) {
        log.info("REST request to update room: {}", id);

        RoomResponse updatedRoom = roomService.updateRoom(id, request);

        return ResponseEntity.ok(updatedRoom);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Delete a room", description = "Delete a room (Admin only)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Room deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
        @ApiResponse(responseCode = "404", description = "Room not found")
    })
    public ResponseEntity<Void> deleteRoom(
        @Parameter(description = "Room ID") @PathVariable Long id
    ) {
        log.info("REST request to delete room: {}", id);

        roomService.deleteRoom(id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/confirm-availability")
    @Operation(
        summary = "Confirm room availability (Internal)",
        description = "Internal API for Booking Service to confirm room availability as part of the saga pattern",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Availability check completed"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Room not found")
    })
    public ResponseEntity<AvailabilityResponse> confirmAvailability(
        @Parameter(description = "Room ID") @PathVariable Long id,
        @Valid @RequestBody AvailabilityRequest request
    ) {
        log.info("Internal API: Confirm availability for room {} with requestId: {}", id, request.getRequestId());

        AvailabilityResponse response = roomService.confirmAvailability(id, request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/release")
    @Operation(
        summary = "Release room (Internal)",
        description = "Internal API for Booking Service to release a room (compensation action in saga pattern)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Room released successfully"),
        @ApiResponse(responseCode = "404", description = "Room not found")
    })
    public ResponseEntity<AvailabilityResponse> releaseRoom(
        @Parameter(description = "Room ID") @PathVariable Long id,
        @Parameter(description = "Original request ID") @RequestParam String requestId
    ) {
        log.info("Internal API: Release room {} for requestId: {}", id, requestId);

        AvailabilityResponse response = roomService.releaseRoom(id, requestId);

        return ResponseEntity.ok(response);
    }
}
