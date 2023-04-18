package com.mf.circuit.breaker;

import lombok.val;

public class DelayedRemoteService implements RemoteService{

    private final long serverStartTime;

    private final int delay;

    public DelayedRemoteService(long serverStartTime, int delay) {
        this.serverStartTime = serverStartTime;
        this.delay = delay;
    }

    public DelayedRemoteService(){
        this.delay = 20;
        this.serverStartTime = System.nanoTime();
    }

    @Override
    public String call() throws RemoteServiceException {
        val currentTime = System.nanoTime();
        if ((currentTime - serverStartTime) * 1.0 / (1000*1000*1000) < delay) {
            throw new RemoteServiceException("Delayed service is down");
        }
        return "Delayed service is working";
    }
}
