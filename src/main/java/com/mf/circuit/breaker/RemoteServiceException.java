package com.mf.circuit.breaker;

public class RemoteServiceException extends Exception{

    public RemoteServiceException(String message){
        super(message);
    }
}
