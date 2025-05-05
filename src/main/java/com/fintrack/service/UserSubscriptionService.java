package com.fintrack.service;

import com.fintrack.model.UserSubscription;
import com.fintrack.repository.UserSubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserSubscriptionService {

    private final UserSubscriptionRepository userSubscriptionRepository;

    @Autowired
    public UserSubscriptionService(UserSubscriptionRepository userSubscriptionRepository) {
        this.userSubscriptionRepository = userSubscriptionRepository;
    }

    public Optional<UserSubscription> getSubscriptionByAccountId(UUID accountId) {
        return userSubscriptionRepository.findByAccountId(accountId);
    }

    public UserSubscription createOrUpdateSubscription(UserSubscription subscription) {
        return userSubscriptionRepository.save(subscription);
    }

    public void deleteSubscription(Long id) {
        userSubscriptionRepository.deleteById(id);
    }
}