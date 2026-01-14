package mephi.bookingservice.client;

import lombok.extern.slf4j.Slf4j;
import mephi.bookingservice.dto.hotel.AvailabilityRequest;
import mephi.bookingservice.dto.hotel.AvailabilityResponse;
import mephi.bookingservice.dto.hotel.RoomResponse;
import mephi.bookingservice.exception.HotelServiceException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class HotelServiceClientFallbackFactory implements FallbackFactory<HotelServiceClient> {
    @Override
    public HotelServiceClient create(Throwable cause) {
        log.error("Hotel Service fallback triggered", cause);

        return new HotelServiceClient() {
            @Override
            public RoomResponse getRoomById(Long id) {
                throw new HotelServiceException("Hotel Service is unavailable. Cannot retrieve room details.", cause);
            }

            @Override
            public List<RoomResponse> getAvailableRooms() {
                throw new HotelServiceException("Hotel Service is unavailable. Cannot retrieve available rooms.", cause);
            }

            @Override
            public List<RoomResponse> getAvailableRoomsByHotel(Long hotelId) {
                throw new HotelServiceException("Hotel Service is unavailable. Cannot retrieve available rooms for hotel.", cause);
            }

            @Override
            public List<RoomResponse> getRecommendedRooms(Long hotelId, String roomType, Integer guestCount) {
                throw new HotelServiceException("Hotel Service is unavailable. Cannot retrieve room recommendations.", cause);
            }

            @Override
            public AvailabilityResponse confirmAvailability(Long roomId, AvailabilityRequest request) {
                log.error("Failed to confirm availability for room {}: {}", roomId, cause.getMessage());

                return AvailabilityResponse.builder()
                    .roomId(roomId)
                    .requestId(request.getRequestId())
                    .confirmed(false)
                    .message("Hotel Service is unavailable. Please try again later.")
                    .build();
            }

            @Override
            public void releaseRoom(Long roomId, String requestId) {
                log.error("Failed to release room {}: {}", roomId, cause.getMessage());

                AvailabilityResponse.builder()
                    .roomId(roomId)
                    .requestId(requestId)
                    .confirmed(false)
                    .message("Hotel Service is unavailable. Room release may be pending.")
                    .build();
            }
        };
    }
}
