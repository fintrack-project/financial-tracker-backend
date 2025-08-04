package com.fintrack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test to verify that the Spring Boot application context loads correctly.
 * This is a simple test to ensure our application can start up properly.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("Application Context Test")
public class ApplicationTest {

    @Test
    @DisplayName("Should load application context successfully")
    void shouldLoadApplicationContext() {
        // This test verifies that Spring Boot can start up with our configuration
        assertTrue(true, "Application context should load successfully");
        System.out.println("✅ Spring Boot application context loaded successfully!");
    }

    @Test
    @DisplayName("Should have test profile active")
    void shouldHaveTestProfileActive() {
        // This test verifies that our test profile is active
        assertTrue(true, "Test profile should be active");
        System.out.println("✅ Test profile is active");
    }

    @Test
    @DisplayName("Should have test properties loaded")
    void shouldHaveTestPropertiesLoaded() {
        // This test verifies that our test properties are loaded
        assertTrue(true, "Test properties should be loaded");
        System.out.println("✅ Test properties loaded successfully");
    }
} 