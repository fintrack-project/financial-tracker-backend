package com.fintrack.controller;

import com.fintrack.constants.UserNotificationType;
import com.fintrack.model.UserNotificationPreference;
import com.fintrack.service.UserNotificationPreferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/user/notification-preference")
public class UserNotificationPreferenceController {

    private UserNotificationPreferenceService service;

    public UserNotificationPreferenceController(UserNotificationPreferenceService service) {
        this.service = service;
    }

    @GetMapping("/fetch")
    public ResponseEntity<List<Map<String, Object>>> getNotificationPreferences(@RequestParam String accountId) {
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

        return ResponseEntity.ok(response);
    }

    // Helper method to create a preference map
    private Map<String, Object> createPreferenceMap(String notificationType, boolean isEnabled) {
        Map<String, Object> map = new HashMap<>();
        map.put("notification_type", notificationType);
        map.put("is_enabled", isEnabled);
        return map;
    }

    @PostMapping("/update")
    public ResponseEntity<Void> updateNotificationPreference(@RequestBody Map<String, Object> request) {
        // Extract values from the request map
        String accountId = (String) request.get("accountId");
        String notificationType = (String) request.get("notificationType");
        boolean isEnabled = (boolean) request.get("isEnabled");

        // Call the service to update the preference
        service.updateNotificationPreference(UUID.fromString(accountId), UserNotificationType.valueOf(notificationType), isEnabled);

        return ResponseEntity.ok().build();
    }
}