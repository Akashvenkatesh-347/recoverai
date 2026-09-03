package com.recoverai.recovery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecoveryAttemptRepository
        extends JpaRepository<RecoveryAttempt, Long> {

    List<RecoveryAttempt> findByPaymentIdOrderByAttemptedAtDesc(Long paymentId);
}