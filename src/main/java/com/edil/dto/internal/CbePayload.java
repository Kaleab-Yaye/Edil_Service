package com.edil.dto.internal;


import java.time.Instant;

// if the mapping doens happen we ahve to add the ignore parremter here
public record CbePayload(
        String id,
        String debitAccountNo,
        String debitAmount,
        String debitValueDate,
        String creditAccountNo,
        String creditAccountHolder,
        String debitAccountHolder,
        Instant dateTimes,
        String status,
        String v2Key





)
{

    public CbePayload withv2Key(String v2Key){
        return new CbePayload(this.id, this.debitAccountNo, this.debitAmount, this.debitValueDate, this.creditAccountNo, this.creditAccountHolder,
        this.debitAccountHolder, this.dateTimes, this.status, v2Key);
    }
}
