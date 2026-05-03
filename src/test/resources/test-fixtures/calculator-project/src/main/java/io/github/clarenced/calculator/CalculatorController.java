package io.github.clarenced.calculator;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/calculator")
public class CalculatorController {

    private final Calculator calculator;

    public CalculatorController(Calculator calculator) {
        this.calculator = calculator;
    }

    @GetMapping("/add")
    public ResponseEntity<Map<String, Object>> add(
            @RequestParam("a") int a,
            @RequestParam("b") int b) {
        int result = calculator.add(a, b);
        return ResponseEntity.ok(Map.of(
                "operation", "add",
                "a", a,
                "b", b,
                "result", result
        ));
    }

    @GetMapping("/subtract")
    public ResponseEntity<Map<String, Object>> subtract(
            @RequestParam("a") int a,
            @RequestParam("b") int b) {
        int result = calculator.subtract(a, b);
        return ResponseEntity.ok(Map.of(
                "operation", "subtract",
                "a", a,
                "b", b,
                "result", result
        ));
    }

    @GetMapping("/divide")
    public ResponseEntity<Map<String, Object>> divide(
            @RequestParam("a") double a,
            @RequestParam("b") double b) {
        try {
            double result = calculator.divide(a, b);
            return ResponseEntity.ok(Map.of(
                    "operation", "divide",
                    "a", a,
                    "b", b,
                    "result", result
            ));
        } catch (ArithmeticException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "operation", "divide",
                    "a", a,
                    "b", b,
                    "error", e.getMessage()
            ));
        }
    }
}