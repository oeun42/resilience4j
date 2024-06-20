package com.example.resilience4j.circuitbreaker;


import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

import java.util.Random;
@Service
public class Resilience4jService {


    @CircuitBreaker(name = "circuitBreakerFailure", fallbackMethod = "circuitBreakerTestFallback")
    public String circuitBreakerFailure() throws Exception {
        failApiCall();
        return "hello world";
    }

    @CircuitBreaker(name = "circuitBreakerSlowCall", fallbackMethod = "circuitBreakerTestFallback")
    public String circuitBreakerSlowCall() throws InterruptedException {
        slowApiCall();
        return "hello world";
    }

    public void failApiCall(){
        throw new RuntimeException("failed");
    }

    public void slowApiCall() throws InterruptedException {
        Thread.sleep(2000);
    }

    private String circuitBreakerTestFallback(RuntimeException t){
        System.out.println(t.getMessage());
        return "call failed";
    }

    private String circuitBreakerTestFallback(CallNotPermittedException t){
        return "circuit open";
    }

    @Bulkhead(name="bulkheadTest", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "bulkheadTestFallback")
    public String bulkhead() throws InterruptedException {
        Thread.sleep(10000);

        return "hello world";
    }

    private String bulkheadTestFallback(RuntimeException t){
        System.out.println(t.getMessage());
        return "Max Concurrent Calls";
    }
}
