package com.example.resilience4j.circuitbreaker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Resilience4jController {

    @Autowired
    private Resilience4jService resilience4jService;

    @GetMapping("/circuit-breaker")
    public String circuitBreakerTest() throws Exception {
        return resilience4jService.circuitBreakerTest();
    }
}
