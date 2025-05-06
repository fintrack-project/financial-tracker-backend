package com.fintrack.constants.user;

public enum UserNotificationType {

    EMAIL("EMAIL"),
    SMS("SMS"),
    PUSH("PUSH"),
    NONE("NONE");

    private final String typeName;

    UserNotificationType(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }
}