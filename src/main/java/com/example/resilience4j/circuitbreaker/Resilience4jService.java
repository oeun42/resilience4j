package com.example.resilience4j.circuitbreaker;


import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

import java.util.Random;
@Service
public class Resilience4jService {


    @CircuitBreaker(name = "circuitBreakerTest", fallbackMethod = "circuitBreakerTestFallback")
    public String circuitBreakerTest() throws Exception {
        test();
        return "hello world";
    }

    public String test(){
        throw new RuntimeException("failed");

    }

    private String circuitBreakerTestFallback(RuntimeException t){
        System.out.println(t.getMessage());
        return "call failed";
    }

    private String circuitBreakerTestFallback(CallNotPermittedException t){
        return "circuit open";
    }


    public String bulkheadTest() throws InterruptedException {
        Thread.sleep(10000);

        return "hello world";
    }
}
