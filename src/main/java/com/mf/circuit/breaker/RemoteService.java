package com.mf.circuit.breaker;

public interface RemoteService {
    String call() throws RemoteServiceException;
}
