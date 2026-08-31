package com.edil.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "receipts")
public class Receipt {
    @Id
    @Getter @Setter
    private String id;

    @Column(name = "reference_number")
    @Getter @Setter
    private  String referenceNumber;

}
