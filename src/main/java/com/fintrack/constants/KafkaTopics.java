package com.fintrack.constants;

public enum KafkaTopics {
    TRANSACTIONS_CONFIRMED("TRANSACTIONS_CONFIRMED"),
    PROCESS_TRANSACTIONS_TO_HOLDINGS("PROCESS_TRANSACTIONS_TO_HOLDINGS");

    private final String topicName;

    KafkaTopics(String topicName) {
        this.topicName = topicName;
    }

    public String getTopicName() {
        return topicName;
    }
}