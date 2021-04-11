package com.byte96.crop_image.utils.exception;

public class LoadFileException extends RuntimeException {

    public LoadFileException(String message) {
        super(message);
    }

    public LoadFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
