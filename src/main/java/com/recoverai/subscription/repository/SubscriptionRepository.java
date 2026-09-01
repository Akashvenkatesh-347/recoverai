package com.recoverai.subscription.repository;

import com.recoverai.subscription.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
}