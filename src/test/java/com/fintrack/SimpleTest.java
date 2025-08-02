package com.fintrack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test class to verify that the testing framework is working correctly.
 * This is the first test we'll create to ensure our setup is working.
 */
@DisplayName("Simple Test Suite")
public class SimpleTest {

    @Test
    @DisplayName("Should pass a basic assertion")
    void shouldPassBasicAssertion() {
        // Arrange
        String expected = "Hello, FinTrack!";
        String actual = "Hello, FinTrack!";
        
        // Act & Assert
        assertEquals(expected, actual, "Basic string comparison should pass");
    }

    @Test
    @DisplayName("Should perform basic math operations")
    void shouldPerformBasicMath() {
        // Arrange
        int a = 5;
        int b = 3;
        
        // Act
        int sum = a + b;
        int product = a * b;
        
        // Assert
        assertEquals(8, sum, "5 + 3 should equal 8");
        assertEquals(15, product, "5 * 3 should equal 15");
        assertTrue(sum > 0, "Sum should be positive");
        assertTrue(product > sum, "Product should be greater than sum");
    }

    @Test
    @DisplayName("Should handle null checks")
    void shouldHandleNullChecks() {
        // Arrange
        String nullString = null;
        String nonNullString = "FinTrack";
        
        // Act & Assert
        assertNull(nullString, "nullString should be null");
        assertNotNull(nonNullString, "nonNullString should not be null");
        assertTrue(nonNullString.length() > 0, "nonNullString should have length > 0");
    }

    @Test
    @DisplayName("Should verify test framework is working")
    void shouldVerifyTestFrameworkIsWorking() {
        // This test verifies that our testing framework is properly set up
        assertTrue(true, "If you see this, the test framework is working!");
        System.out.println("✅ FinTrack Backend Unit Testing Framework is working correctly!");
    }
} 