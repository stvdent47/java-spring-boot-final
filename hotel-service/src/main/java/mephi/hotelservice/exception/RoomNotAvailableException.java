package mephi.hotelservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.CONFLICT)
public class RoomNotAvailableException extends RuntimeException {
    private final Long roomId;
    private final String requestId;

    public RoomNotAvailableException(Long roomId, String message) {
        super(message);
        this.roomId = roomId;
        this.requestId = null;
    }

    public RoomNotAvailableException(Long roomId, String requestId, String message) {
        super(message);
        this.roomId = roomId;
        this.requestId = requestId;
    }
}
