package com.mf.circuit.breaker;

public interface CircuitBreaker {

    void recordSuccess();

    void recordFailure(String response);

    String getState();

    void setState(State state);

    String attemptRequest() throws RemoteServiceException;
}
