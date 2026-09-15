package com.edil.dto.response;



public record GetPdfInfoResponse (
        String pdfName,
        Long pdfSize,
        boolean hasPdf
){
}
