package com.edil.exception;

public class CampaignNotFoundException extends RuntimeException {
    public CampaignNotFoundException(String message) {
        super("the campaign with the id " + message + " was not found");
    }
}
