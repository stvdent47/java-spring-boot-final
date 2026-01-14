package mephi.bookingservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mephi.bookingservice.dto.BookingRequest;
import mephi.bookingservice.dto.BookingResponse;
import mephi.bookingservice.dto.hotel.RoomResponse;
import mephi.bookingservice.entity.BookingStatus;
import mephi.bookingservice.service.BookingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking management API")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Create a booking", description = "Create a new room booking")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Booking created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or room not available"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "503", description = "Hotel Service unavailable")
    })
    public ResponseEntity<BookingResponse> createBooking(
        @Valid @RequestBody BookingRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Creating booking for user: {}", userDetails.getUsername());

        BookingResponse response = bookingService.createBooking(request, userDetails.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    @Operation(summary = "Get my bookings", description = "Get all bookings for the current user with pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bookings retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Page<BookingResponse>> getMyBookings(
        @AuthenticationPrincipal UserDetails userDetails,
        @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        log.debug(
            "Getting bookings for user: {} (page: {}, size: {})",
            userDetails.getUsername(),
            pageable.getPageNumber(),
            pageable.getPageSize()
        );

        Page<BookingResponse> bookings = bookingService.getBookingsByUserPaged(userDetails.getUsername(), pageable);

        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking by ID", description = "Get a specific booking by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Booking retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<BookingResponse> getBookingById(
        @Parameter(description = "Booking ID") @PathVariable Long id
    ) {
        log.debug("Getting booking: {}", id);

        BookingResponse response = bookingService.getBookingById(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/reference/{reference}")
    @Operation(summary = "Get booking by reference", description = "Get a specific booking by its reference number")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Booking retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<BookingResponse> getBookingByReference(
        @Parameter(description = "Booking reference") @PathVariable String reference
    ) {
        log.debug("Getting booking by reference: {}", reference);

        BookingResponse response = bookingService.getBookingByReference(reference);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a booking", description = "Cancel an existing booking")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Booking cancelled successfully"),
        @ApiResponse(responseCode = "400", description = "Cannot cancel booking in current status"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Not authorized to cancel this booking"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<BookingResponse> cancelBooking(
        @Parameter(description = "Booking ID") @PathVariable Long id,
        @Parameter(description = "Cancellation reason") @RequestParam(required = false) String reason,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Cancelling booking {} for user: {}", id, userDetails.getUsername());

        boolean isAdmin = userDetails.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        BookingResponse response = bookingService.cancelBooking(id, userDetails.getUsername(), reason, isAdmin);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all bookings (Admin)", description = "Get all bookings in the system with pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bookings retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<Page<BookingResponse>> getAllBookings(
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        log.debug("Getting all bookings (admin) - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<BookingResponse> bookings = bookingService.getAllBookingsPaged(pageable);

        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get bookings by status (Admin)", description = "Get all bookings with a specific status with pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bookings retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<Page<BookingResponse>> getBookingsByStatus(
        @Parameter(description = "Booking status") @PathVariable BookingStatus status,
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        log.debug("Getting bookings by status: {} (page: {}, size: {})", status, pageable.getPageNumber(), pageable.getPageSize());

        Page<BookingResponse> bookings = bookingService.getBookingsByStatusPaged(status, pageable);

        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/rooms/recommend")
    @Operation(summary = "Get recommended rooms", description = "Get room recommendations based on filters")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Recommendations retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "503", description = "Hotel Service unavailable")
    })
    public ResponseEntity<List<RoomResponse>> getRecommendedRooms(
        @Parameter(description = "Hotel ID") @RequestParam(required = false) Long hotelId,
        @Parameter(description = "Room type") @RequestParam(required = false) String roomType,
        @Parameter(description = "Guest count") @RequestParam(required = false) Integer guestCount
    ) {
        log.debug(
            "Getting room recommendations: hotelId={}, roomType={}, guestCount={}",
            hotelId,
            roomType,
            guestCount
        );

        List<RoomResponse> rooms = bookingService.getRecommendedRooms(hotelId, roomType, guestCount);

        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/rooms/{roomId}")
    @Operation(summary = "Get room details", description = "Get details of a specific room")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Room details retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "404", description = "Room not found"),
        @ApiResponse(responseCode = "503", description = "Hotel Service unavailable")
    })
    public ResponseEntity<RoomResponse> getRoomDetails(
        @Parameter(description = "Room ID") @PathVariable Long roomId
    ) {
        log.debug("Getting room details: {}", roomId);

        RoomResponse room = bookingService.getRoomDetails(roomId);

        return ResponseEntity.ok(room);
    }
}
