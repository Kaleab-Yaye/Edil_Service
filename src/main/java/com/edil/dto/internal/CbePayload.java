package com.edil.dto.internal;


// if the mapping doens happen we ahve to add the ignore parremter here
public record CbePayload(
        String id,
        String debitAccountNo,
        String debitAmount,
        String debitValueDate,
        String creditAccountHolder,
        String debitAccountHolder,
        String status

) {
}
