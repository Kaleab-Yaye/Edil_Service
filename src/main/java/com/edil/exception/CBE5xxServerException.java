package com.edil.exception;

public class CBE5xxServerException extends RuntimeException {

    public CBE5xxServerException(String message) {
        super("CBE GAVE 500 FAMILY STATUS: "+message);
    }
}
