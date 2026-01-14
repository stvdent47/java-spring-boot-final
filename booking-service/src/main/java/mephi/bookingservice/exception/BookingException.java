package mephi.bookingservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BookingException extends RuntimeException {
    private final String bookingReference;
    private final String errorCode;

    public BookingException(String message) {
        super(message);
        this.bookingReference = null;
        this.errorCode = null;
    }

    public BookingException(String message, String bookingReference) {
        super(message);
        this.bookingReference = bookingReference;
        this.errorCode = null;
    }

    public BookingException(String message, String bookingReference, String errorCode) {
        super(message);
        this.bookingReference = bookingReference;
        this.errorCode = errorCode;
    }
}
