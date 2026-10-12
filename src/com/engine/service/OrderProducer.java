package com.engine.service;

import java.util.List;

import com.engine.model.Order;
import com.engine.util.StringParserUtil;

public class OrderProducer implements Runnable{
    private final String producerID;
    private final MatchingEngine engine;
    private final List<String> rawOrderLines;

    public OrderProducer(String producerId, MatchingEngine engine, List<String> rawOrderLines){
        this.producerID = producerId;
        this.engine = engine;
        this.rawOrderLines = rawOrderLines;
    }

    @Override 
    public void run(){
        System.out.println("[" + Thread.currentThread().getName() + "] Producer " + producerID + " started.");

        for (String line : rawOrderLines) {
            try {
                Order order = StringParserUtil.parseOrder(line);
                engine.submitOrder(order);
                // Simulate realistic networ/arrival jitter (10-30)ms
                Thread.sleep(15);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("[" + producerID + "] Ingestion interrupted.");
                break;
            } catch(Exception e){
                System.err.println("[" + producerID + "] Skipped invalid order string: " + e.getMessage());
            }
        }
        System.out.println("[" + Thread.currentThread().getName() + "] Producer " + producerID + " finished batch.");
    }
}
