package com.mf.circuit.breaker;

import lombok.val;

import java.util.concurrent.atomic.AtomicInteger;

public class DefaultCircuitBreaker implements CircuitBreaker{

    /**
     * 调用超时时间
     */
    private final long timeout;
    /**
     * 熔断器尝试自动恢复的时间
     */
    private final long retryTimePeriod;
    /**
     * 远程服务
     */
    private final RemoteService service;
    /**
     * 调用最后失败时间
     */
    long lastFailureTime;
    /**
     * 最后失败返回信息
     */
    private String lastFailureResponse;
    /**
     * 失敗次數
     */
    private final AtomicInteger failureCount = new AtomicInteger(0);
    /**
     * 失败上限阈值
     */
    private final int failureThreshold;

    /**
     * 系统状态
     */
    private State state;
    private static final long FUTURE_TIME = 1000L * 1000 * 1000 * 1000;

    /**
     * Constructor to create an instance of Circuit Breaker.
     *
     * @param timeout          Timeout for the API request. Not necessary for this simple example
     * @param failureThreshold Number of failures we receive from the depended on service before
     *                         changing state to 'OPEN'
     * @param retryTimePeriod  Time, in nanoseconds, period after which a new request is made to
     *                         remote service for status check.
     */
    DefaultCircuitBreaker(RemoteService serviceToCall, long timeout, int failureThreshold,
                          long retryTimePeriod) {
        this.service = serviceToCall;
        // We start in a closed state hoping that everything is fine
        this.state = State.CLOSED;
        this.failureThreshold = failureThreshold;
        // Timeout for the API request.
        // Used to break the calls made to remote resource if it exceeds the limit
        this.timeout = timeout;
        this.retryTimePeriod = retryTimePeriod;
        //An absurd amount of time in future which basically indicates the last failure never happened
        this.lastFailureTime = System.nanoTime() + FUTURE_TIME;
        this.failureCount.set(0);
    }

    /**
     * 调用成功，重置之前失败的状态，系统处于 不熔断 阶段
     */
    @Override
    public void recordSuccess() {
        this.failureCount.set(0);
        this.lastFailureTime = System.nanoTime() + FUTURE_TIME;
        this.state = State.CLOSED;
    }

    @Override
    public void recordFailure(String response) {
        this.failureCount.incrementAndGet();
        this.lastFailureTime = System.nanoTime();
        this.lastFailureResponse = response;
    }

    @Override
    public String getState() {
        evaluateState();
        return state.name();
    }

    @Override
    public void setState(State state) {
        this.state = state;
        switch (state) {
            case OPEN:{

                this.failureCount.set(this.failureThreshold);
                this.lastFailureTime = System.nanoTime();
            }
            break;
            case HALF_OPEN:{
                this.failureCount.set(this.failureThreshold);
                this.lastFailureTime = System.nanoTime() - retryTimePeriod;
            }
            break;
            default:
                this.failureCount.set(0);
                this.lastFailureTime = System.nanoTime() + FUTURE_TIME;
        }
    }

    @Override
    public String attemptRequest() throws RemoteServiceException {
        evaluateState();
        // 系统处于 熔断 阶段， 就直接返回上一次 错误返回，不进行链路调用
        if (state == State.OPEN) {
            return lastFailureResponse;
        } else {
            try {
                long start = System.nanoTime();
                // 执行链路调用
                val response = service.call();
                // 判断是否超时
                if (System.nanoTime() - start > timeout) {
                    this.failureCount.incrementAndGet();
                }
                // 记录成功
                recordSuccess();
                return response;
            } catch (RemoteServiceException ex) {
                // 记录失败
                recordFailure(ex.getMessage());
                throw ex;
            }
        }
    }


    protected void evaluateState(){
        // 首先判断 失败次数 是否大于 失败上限阈值
        if (failureCount.get() >= failureThreshold) {
            //触发熔断条件

            // 是否满足触发 自动恢复的条件
            if ((System.nanoTime() - lastFailureTime) > retryTimePeriod) {
                // 满足 系统处于 半开 状态
                state = State.HALF_OPEN;
            } else {
                // 不满足 系统处于 熔断 状态
                state = State.OPEN;
            }
        } else {
            // 没有触发 熔断 条件
            state = State.CLOSED;
        }
    }
}
