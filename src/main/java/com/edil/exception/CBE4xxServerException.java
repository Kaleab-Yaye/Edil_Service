package com.edil.exception;

public class CBE4xxServerException extends RuntimeException
{
    public CBE4xxServerException(String message) {
        super(
                "CBE SERVER RESPONDED WITH 400 FAMILY STATUS CODE OF " + message);
    }
}
