package com.edil.repository;


import com.edil.domain.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, String> {

    boolean existsReceiptById(String uniqueV2Key);

}
