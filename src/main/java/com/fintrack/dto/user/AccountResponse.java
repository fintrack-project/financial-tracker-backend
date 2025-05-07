package com.fintrack.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AccountResponse {
    @JsonProperty("accountId")
    private String accountId;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
} 