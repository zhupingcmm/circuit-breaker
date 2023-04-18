package com.mf.circuit.breaker;

import lombok.val;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AppTest {

    private static final int STARTUP_DELAY = 4;

    private static final int FAILURE_THRESHOLD = 1;

    private static final int RETRY_PERIOD = 2;

    private MonitoringService monitoringService;

    private CircuitBreaker delayedServiceCircuitBreaker;

    private CircuitBreaker quickServiceCircuitBreaker;


    @Before
    public void setUpCircuitBreaker(){
        val delayedService = new DelayedRemoteService(System.nanoTime(), STARTUP_DELAY);
        delayedServiceCircuitBreaker = new DefaultCircuitBreaker(delayedService, 3000, FAILURE_THRESHOLD, RETRY_PERIOD * 1000 * 1000 * 1000);
        val quickService = new QuickRemoteService();
        //Set the circuit Breaker parameters
        quickServiceCircuitBreaker = new DefaultCircuitBreaker(quickService, 3000, FAILURE_THRESHOLD,
                RETRY_PERIOD * 1000 * 1000 * 1000);


        monitoringService = new MonitoringService(delayedServiceCircuitBreaker,
                quickServiceCircuitBreaker);
    }

    @Test
    public void testFailure_OpenStateTransition(){


        //Calling delayed service, which will be unhealthy till 4 seconds
        assertEquals("Delayed service is down", monitoringService.delayedServiceResponse());
        //As failure threshold is "1", the circuit breaker is changed to OPEN
        assertEquals("OPEN", delayedServiceCircuitBreaker.getState());
        //As circuit state is OPEN, we expect a quick fallback response from circuit breaker.
        assertEquals("Delayed service is down", monitoringService.delayedServiceResponse());

        //Meanwhile, the quick service is responding and the circuit state is CLOSED
        assertEquals("Quick Service is working", monitoringService.quickServiceResposne());
        assertEquals("CLOSED", quickServiceCircuitBreaker.getState());
    }

}
