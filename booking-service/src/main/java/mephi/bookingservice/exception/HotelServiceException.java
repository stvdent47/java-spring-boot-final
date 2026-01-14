package mephi.bookingservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class HotelServiceException extends RuntimeException {
    private final int statusCode;

    public HotelServiceException(String message) {
        super(message);
        this.statusCode = 503;
    }

    public HotelServiceException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public HotelServiceException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 503;
    }
}
