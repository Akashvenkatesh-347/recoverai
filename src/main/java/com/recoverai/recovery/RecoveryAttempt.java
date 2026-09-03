package com.recoverai.recovery;

import com.recoverai.payment.Payment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "recovery_attempts")
@Getter
@Setter
@NoArgsConstructor
public class RecoveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendedAction action;

    @Column(nullable = false)
    private int retryCountBefore;

    @Column(nullable = false)
    private LocalDateTime attemptedAt;

    @Column(nullable = false, length = 500)
    private String result;

    @PrePersist
    protected void onCreate() {
        if (attemptedAt == null) {
            attemptedAt = LocalDateTime.now();
        }
    }
}