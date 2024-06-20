# 클라이언트 회복성 Resilience4j - (1)

## 서론
resilience4j는 nextflix hystrix에서 영감을 받아 만들어진 경량 내결함성 라이브러리이다.
현재 netflix hystrix는 deprecated되었기 때문에 resilience4j를 쓰는 것을 권장한다.(* 참조: 1)
resilience4j는 circuit breaker, rate limiter, retry, bulkhead를 지원하여 장애 전파를 방지한다.
resilience4j 2.0는 java17, Kotlin, spring boot, micronaut 지원하며 경량화된 라이브러리가 되기 위해 vavr(vavr는 다른 외부 라이브러리의 종속성이 없다)외의 종속성을 제거하였다.

*내결함성: 시스템이 하드웨어 또는 소프트웨어의 오류, 장애 또는 고장에도 계속해서 정상적으로 동작하는 능력


## Netflix Hystrix vs Resilience4j
* 외부 시스템을 호출하려면 
    * hystrix: hystrixCommand로 감싸야함
    * resilience4j: circuit breaker나 rate limiter, bulhead를 사용하며, 실패한 호출을 재시도하거나(retry) 호출 결과를 캐시할 수 있는 데코레이터 제공. completeFuture나 RxJava를 사용하여 데코레이터를 동기나 비동기로 실행 가능.
* Circuitbreaker
    * hystrix: 반열림 상태에서 호출을 한 번만 수행하고 circuitbreaker를 닫을 지 결정
    * resilience4j: 반열림 상태에서 설정한 횟수만큼 실행 한 후 그 결과를 설정한 임계치와 비교하여 circuitbreaker를 닫을 지 결정, 너무 많은 호출이 응답 시간 임계치를 초과하면 원격 시스템에서 exception이 발생하기 전에 circuitbreaker를 열 수 있음.
* 기타
    * resilience4j는 커스텀 reactor 나 RxJava를 전용 모듈로 제공


## CircuitBreaker

* state
  
  
 ![39cdd54-state_machine](https://github.com/BE-PT-Study/2024-backend-study/assets/56907015/8799a29c-6385-4519-8db9-aa53ca82b062)
        
    * closed : 정상적으로 호출하고 응답
    open: 문제 발생이 감지됨
    * half_open: open 상태가 되고 일정 시간이 지난 상황으로 일부 허용된 요청들이 성공할 경우 closed상태로 전환
    * disabled: 항상 호출 허용(상태 전환 시 상태 전환 트리거하거나 서킷 브레이커 리셋)
    * forced_open: 항상 호출 거부(상태 전환 시 상태 전환 트리거하거나 서킷 브레이커 리셋)

* 호출 결과 저장 & 집계
    * 카운트 기반 sliding window: 마지막으로 호출한 N번의 결과 집계
    * 시간 기반 sliding window: 마지막 N초 동안의 호출 결과 집계

*  카운트 기반 sliding window
    * N개의 측정값을 지닌 원형 Array(윈도우 크기가 10이면 원형 배열엔 항상 10개의 측정 값이 존재)
    * 총 집계 업데이트는 새 호출 결과를 기록될 떄 일어나며 가장 오래된 측정값이 제거되면 총 집계에서 이 측정값 차감
    * 스냅샷은 이미 집계되어 있어 조회할 떄 필요한 시간은 O(1)
    *스냅샷: 총 호출 수, 실패 비율, 느린 호출 비율 등 Circuit의 상태를 결정하기 위해 필요한 다양한 호출정보를 가짐
    * 메모리 소비량: O(n)
* 시간 기반 silding window
    * N개의 부분 집계(버킷)를 지닌 원형 Array
    * 모든 버킷은 특정 초에 발생하는 모든 요청의 결과를 집계
    * 스냅샷은 미리 집계되어 있어 조회 시간 복잡도 O(1)
    * 메모리 소비량: O(n) 근접(부분 집계 N개 + 총 집계 1개)
    * 부붅 집계: 실패한 호출, 느린 호출, 총 호출 횟수, 전체 호출에 소요한 총 시간 저장
 
* 실패율 및 느린 호출 임계치
  

    실패율이 임계값 이상이면 circuit이 closed -> open으로 변경한다. 기본적으로 모든 exception은 실패로 간주되나 예외 목록을 따로 정의할 수 있다.

    느린 호출 비율이 설정한 임계치 이상일 경우에도 circuit이 closed -> open으로 변경된다. 
    
    주의할 점으로는 실패율과 느린 호출 비율을 계산하기 위해서 호출 결과의 최소치를 미리 기록해 두어야 한다. 
    
    예를 들어 최소한으로 필요한 호출 횟수가 10번이지만 9번밖에 측정하지 않았더라면 9번 모두 실패했더라도 circuitbreaker가 열리지 않는다.

* Thread-safe
  
    circuitbreaker는 원자성을 보장하고 특정 시점엔 하나의 스레드만 상태나 슬라이딩 윈도우를 업데이트하기 때문에 thread-safe하다.


## BulkHead
    resilience4j는 동시 수행의 제한을 두기 위해 bulkhead pattern을 제공한다.

* SemaphoreBulkead
    
    동시 요청 수에 재한 -> 요청 수에 도달한 이후의 요청은 BulkheadFullException 발생

* FixedThreadPoolBulkhead
    
    시스템 자원과 별도로 thread pool을 설정하고 설정된 thread pool은 서비스를 제공하기 위한 용도로만 사용하며 waiting queue 설정 가능.
    thread pool과 wiaing queue가 full인 경우 bulkheadFullException 발생

|Config property|Default value|Description|
|---|---|---|
|maxConcurrentCalls|25|bulkhead에서 최대로 허용할 병령 실행 수|
|maxWaitDuration|0|bulkhead가 포화상태일 때 진입하려는 스레드를 블로킹할 최대 시간|


## RateLimiter
* 단위 시간동안 얼만큼의 실행을 허용할 것인지 제한
* rateLimiter는 JVM의 시작부터의 모든 나노초를 사이클로 분할
* 각 사이클은 limitRefreshPeriod 시간동안 limitForPeriod 만큼의 요청만 허용

### AtomicRateLimiter
* RateLimiter의 default 구현체
* AtomicRateLimiter.State는 변경 불가능
  * activeCycle - 마지막 호출의 사이클 번호
  * activePermissions - 맘지막 호출 후 사용 가능한 호출 수
  * nanosToWait - 마지막 호출에 대한 사용 권한을 기다리는 시간(nanoSec)
### SemaphoreBasedRateLimiter
* Semaphore과 scheduler를 사용하여 권한 refresh

## Retry
실패한 실행을 재시도하는 메커니즘


### retry 구성
| config property        | default | description                                                                                                     |
|------------------------|---------|-----------------------------------------------------------------------------------------------------------------|
| maxAttempts            | 3       | 최대 시도 횟수                                                                                                        |
| waitDuration           | 500ms   | 재시도 호출 간격                                                                                                       |
| intervalFunction       |         | 장애 후 waitDuration을 변경하는 function                                                                                |
| retryOnResultPredicate | false   | 결과를 보고 재시도 여부 return                                                                                            |
| retryExceptionPredicate | true    | 예외를 보고 재시도 여부 return                                                                                            |
| retryExceptions         | empty   | 실패로 기록되며 재시도할 throwable클래스                                                                                      |
| ignoreExceptions        | empty   | 재시도 하지 않을 throwable 클래스                                                                                         |
| failAfterMaxAttempts    | false   | 설정한 maxAttempts만큼 재시도하고 나서도 <br/>retryOnResultPredicate를 통과하지 못했을 떄 MaxRetriesExceededException 발생을 활성/비활성하는 boolean |


## timelimiter
실행 시간을 제한

## Cache
결과를 캐싱
*주의할 점: 동시성 이슈로 인해 'JCache Reference Implementation' 사용 권장하지 않음


## 참조

1. https://javadoc.io/doc/org.springframework.cloud/spring-cloud-commons/3.0.1/deprecated-list.html
2. https://resilience4j.readme.io/docs/getting-started
