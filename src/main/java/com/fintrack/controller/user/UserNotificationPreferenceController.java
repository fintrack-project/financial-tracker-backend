package com.fintrack.controller.user;

import com.fintrack.common.ApiResponse;
import com.fintrack.common.ResponseWrapper;
import com.fintrack.constants.user.UserNotificationType;
import com.fintrack.model.user.UserNotificationPreference;
import com.fintrack.service.user.UserNotificationPreferenceService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@RestController
@RequestMapping(value = "/api/user/notification-preference", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserNotificationPreferenceController {

    private static final Logger logger = LoggerFactory.getLogger(UserNotificationPreferenceController.class);
    private final UserNotificationPreferenceService service;

    public UserNotificationPreferenceController(UserNotificationPreferenceService service) {
        this.service = service;
    }

    @GetMapping(
        value = "/fetch",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getNotificationPreferences(@RequestParam String accountId) {
        try {
            List<UserNotificationPreference> preferences = service.getNotificationPreferencesByAccountId(UUID.fromString(accountId));

            // Initialize default preferences
            List<Map<String, Object>> response = new ArrayList<>();
            response.add(createPreferenceMap("EMAIL", false));
            response.add(createPreferenceMap("SMS", false));
            response.add(createPreferenceMap("PUSH", false));

            // Update the default preferences based on the database values
            for (UserNotificationPreference pref : preferences) {
                for (Map<String, Object> res : response) {
                    if (res.get("notification_type").toString().equalsIgnoreCase(pref.getNotificationType().getTypeName())) {
                        res.put("is_enabled", pref.isEnabled());
                    }
                }
            }

            return ResponseWrapper.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseWrapper.badRequest("Invalid accountId format");
        } catch (Exception e) {
            logger.error("Error fetching notification preferences: ", e);
            return ResponseWrapper.internalServerError("Failed to fetch notification preferences");
        }
    }

    // Helper method to create a preference map
    private Map<String, Object> createPreferenceMap(String notificationType, boolean isEnabled) {
        Map<String, Object> map = new HashMap<>();
        map.put("notification_type", notificationType);
        map.put("is_enabled", isEnabled);
        return map;
    }

    @PostMapping(
        value = "/update",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<Void>> updateNotificationPreference(@RequestBody Map<String, Object> request) {
        try {
            // Extract values from the request map
            String accountId = (String) request.get("accountId");
            String notificationType = (String) request.get("notificationType");
            Boolean isEnabled = (Boolean) request.get("isEnabled");

            if (accountId == null || notificationType == null || isEnabled == null) {
                return ResponseWrapper.badRequest("Missing required fields: accountId, notificationType, or isEnabled");
            }

            // Call the service to update the preference
            service.updateNotificationPreference(
                UUID.fromString(accountId), 
                UserNotificationType.valueOf(notificationType), 
                isEnabled
            );

            return ResponseWrapper.ok(null, "Notification preference updated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseWrapper.badRequest("Invalid accountId or notification type format");
        } catch (Exception e) {
            logger.error("Error updating notification preference: ", e);
            return ResponseWrapper.internalServerError("Failed to update notification preference");
        }
    }
}