package com.ktroute.app.exception;

import org.springframework.http.HttpStatus;

public class BookingException extends AppException {
    public BookingException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
