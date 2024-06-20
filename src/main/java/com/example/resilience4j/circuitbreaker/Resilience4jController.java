package com.example.resilience4j.circuitbreaker;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Resilience4jController {

    @Autowired
    private Resilience4jService resilience4jService;

    @GetMapping("/circuit-breaker/failure-rate")
    public String circuitBreakerFailure() throws Exception {
        return resilience4jService.circuitBreakerFailure();
    }

    @GetMapping("/circuit-breaker/slow-call-rate")
    public String circuitBreakerSlowCall() throws Exception {
        return resilience4jService.circuitBreakerSlowCall();
    }

    @GetMapping("/bulkhead")
    public String bulkhead() throws InterruptedException {
        return resilience4jService.bulkhead();
    }

    @GetMapping("/rate-limiter")
    public String rateLimiter(){
        return resilience4jService.rateLimiter();
    }
}
