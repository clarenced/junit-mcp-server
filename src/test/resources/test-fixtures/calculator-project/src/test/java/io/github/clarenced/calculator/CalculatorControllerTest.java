package io.github.clarenced.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CalculatorController.class)
@DisplayName("Calculator REST API Tests")
class CalculatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Calculator calculator;

    @Test
    @DisplayName("GET /calculator/add returns correct sum")
    void testAdd() throws Exception {
        when(calculator.add(3, 5)).thenReturn(8);

        mockMvc.perform(get("/calculator/add").param("a", "3").param("b", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("add"))
                .andExpect(jsonPath("$.result").value(8));
    }

    @Test
    @DisplayName("GET /calculator/subtract returns correct difference")
    void testSubtract() throws Exception {
        when(calculator.subtract(10, 4)).thenReturn(6);

        mockMvc.perform(get("/calculator/subtract").param("a", "10").param("b", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("subtract"))
                .andExpect(jsonPath("$.result").value(6));
    }

    @Test
    @DisplayName("GET /calculator/divide returns correct quotient")
    void testDivide() throws Exception {
        when(calculator.divide(10.0, 4.0)).thenReturn(2.5);

        mockMvc.perform(get("/calculator/divide").param("a", "10").param("b", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("divide"))
                .andExpect(jsonPath("$.result").value(2.5));
    }

    @Test
    @DisplayName("GET /calculator/divide by zero returns 400 Bad Request")
    void testDivideByZero() throws Exception {
        when(calculator.divide(10.0, 0.0))
                .thenThrow(new ArithmeticException("Division by zero is not allowed"));

        mockMvc.perform(get("/calculator/divide").param("a", "10").param("b", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Division by zero is not allowed"));
    }
}