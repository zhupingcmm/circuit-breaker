package com.mf.circuit.breaker;

public class MonitoringService {

    private final CircuitBreaker delayedService;

    private final CircuitBreaker quickService;

    public MonitoringService(CircuitBreaker delayedService, CircuitBreaker quickService) {
        this.delayedService = delayedService;
        this.quickService = quickService;
    }

    public String localResourceReponse() {
        return "local service is working";
    }

    public String delayedServiceResponse() {
        try {
            return delayedService.attemptRequest();
        } catch (RemoteServiceException e) {
            return e.getMessage();
        }
    }

    public String quickServiceResposne(){
        try {
            return quickService.attemptRequest();
        } catch (RemoteServiceException e) {
           return e.getMessage();
        }
    }
}
