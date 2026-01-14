package mephi.bookingservice.client;

import mephi.bookingservice.dto.hotel.AvailabilityRequest;
import mephi.bookingservice.dto.hotel.AvailabilityResponse;
import mephi.bookingservice.dto.hotel.RoomResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
    name = "hotel-service",
    fallbackFactory = HotelServiceClientFallbackFactory.class
)
public interface HotelServiceClient {
    @GetMapping("/rooms/{id}")
    RoomResponse getRoomById(@PathVariable("id") Long id);

    @GetMapping("/rooms/available")
    List<RoomResponse> getAvailableRooms();

    @GetMapping("/rooms/available")
    List<RoomResponse> getAvailableRoomsByHotel(@RequestParam("hotelId") Long hotelId);

    @GetMapping("/rooms/recommend")
    List<RoomResponse> getRecommendedRooms(
        @RequestParam(value = "hotelId", required = false) Long hotelId,
        @RequestParam(value = "roomType", required = false) String roomType,
        @RequestParam(value = "guestCount", required = false) Integer guestCount
    );

    @PostMapping("/rooms/{id}/confirm-availability")
    AvailabilityResponse confirmAvailability(
        @PathVariable("id") Long roomId,
        @RequestBody AvailabilityRequest request
    );

    @PostMapping("/rooms/{id}/release")
    void releaseRoom(
        @PathVariable("id") Long roomId,
        @RequestParam("requestId") String requestId
    );
}
