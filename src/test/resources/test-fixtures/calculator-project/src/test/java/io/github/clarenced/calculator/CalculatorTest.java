package io.github.clarenced.calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Calculator Unit Tests")
class CalculatorTest {

    private Calculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new Calculator();
    }

    @Test
    @DisplayName("Adding two positive numbers")
    void testAdd() {
        assertEquals(5, calculator.add(2, 3));
    }

    @Test
    @DisplayName("Adding a positive and a negative number")
    void testAddNegative() {
        assertEquals(-1, calculator.add(2, -3));
    }

    @Test
    @DisplayName("Subtracting two numbers")
    void testSubtract() {
        assertEquals(1, calculator.subtract(3, 2));
    }

    @Test
    @DisplayName("Subtracting resulting in a negative number")
    void testSubtractNegativeResult() {
        assertEquals(-1, calculator.subtract(2, 3));
    }

    @Test
    @DisplayName("Multiplying two positive numbers")
    void testMultiply() {
        assertEquals(6, calculator.multiply(2, 3));
    }

    @Test
    @DisplayName("Multiplying by zero")
    void testMultiplyByZero() {
        assertEquals(0, calculator.multiply(5, 0));
    }

    @Test
    @DisplayName("Dividing two numbers")
    void testDivide() {
        assertEquals(2.5, calculator.divide(5, 2));
    }

    @Test
    @DisplayName("Division by zero throws ArithmeticException")
    void testDivideByZero() {
        ArithmeticException exception = assertThrows(
                ArithmeticException.class,
                () -> calculator.divide(10, 0)
        );
        assertEquals("Division by zero is not allowed", exception.getMessage());
    }
}