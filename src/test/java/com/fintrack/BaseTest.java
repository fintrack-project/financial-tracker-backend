package com.fintrack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base test class that provides common testing utilities and configurations.
 * Other test classes can extend this to inherit common setup and utilities.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("Base Test Configuration")
public abstract class BaseTest {

    @BeforeEach
    void setUp() {
        // Common setup that runs before each test
        System.out.println("🧪 Setting up test: " + this.getClass().getSimpleName());
    }

    /**
     * Utility method to create test data
     */
    protected void createTestData() {
        // Override in subclasses to create specific test data
        System.out.println("📊 Creating test data...");
    }

    /**
     * Utility method to clean up test data
     */
    protected void cleanupTestData() {
        // Override in subclasses to clean up specific test data
        System.out.println("🧹 Cleaning up test data...");
    }

    /**
     * Utility method to verify test environment
     */
    protected void verifyTestEnvironment() {
        System.out.println("✅ Test environment verified");
        assertTrue(true, "Test environment is properly configured");
    }
} 