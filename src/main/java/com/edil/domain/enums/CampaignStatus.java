package com.edil.domain.enums;

public enum CampaignStatus {
    PENDING, APPROVED, REJECTED, ENDED_BY_CREATOR, ENDED, BEING_PROCESSED, ENDED_BY_CREATOR_PROCESSED

    // the status flow of a campaign looks like this
    //PENDING->APPROVED//REJECTED->ENDED_BY_CREATOR->BEING_PROCESSED->ENDED_BY_CREATOR_PROCESSED->ENDED
}
