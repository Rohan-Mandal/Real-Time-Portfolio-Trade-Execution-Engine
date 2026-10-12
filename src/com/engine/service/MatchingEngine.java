package com.engine.service;

import com.engine.model.Order;
import com.engine.model.Trade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class MatchingEngine implements Runnable {
    private final BlockingQueue<Order> inboundQueue;
    private final Map<String, OrderBook> booksBySymbol;
    private final List<Trade> globalTradeLedger;
    private final AtomicBoolean isRunning;
    private Thread consumerThread;

    public MatchingEngine(int queueCapacity) {
        this.inboundQueue = new ArrayBlockingQueue<>(queueCapacity);
        this.booksBySymbol = new ConcurrentHashMap<>();
        this.globalTradeLedger = Collections.synchronizedList(new ArrayList<>());
        this.isRunning = new AtomicBoolean(false);
    }

    /**
     * Non-blocking or blocking enqueue called by Producer threads.
     * Uses put() to naturally apply backpressure if queue is full.
     */
    public void submitOrder(Order order) throws InterruptedException {
        if (!isRunning.get() && order != Order.POISON_PILL) {
            throw new IllegalStateException("MatchingEngine is stopped. Cannot accept new orders.");
        }
        inboundQueue.put(order);
    }

    /**
     * Starts the matching engine background consumer thread.
     */
    public synchronized void start() {
        if (isRunning.compareAndSet(false, true)) {
            consumerThread = new Thread(this, "matchingEngine-Consumer");
            consumerThread.start();
        }
    }

    /**
     * Consumer execution loop: continuouslydrains the queue and matches.
     */

    @Override
    public void run() {
        System.out.println("[" + Thread.currentThread().getName() + "] engine processing loop started.");
        while (true) {
            try {
                Order order = inboundQueue.take();
                // Poison pill check for graceful shutdown
                if (order == Order.POISON_PILL) {
                    System.out.println(
                            "[" + Thread.currentThread().getName() + "] received shutdown sentinel. Exiting...");
                    break;
                }
                processOrder(order);
            } catch (InterruptedException e) {
                // Thread interrupted during take()
                System.out.println("[" + Thread.currentThread().getName() + "] Thread interrupted.");
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("[" + Thread.currentThread().getName() + "] engine consumer topped cleanly.00");
    }

    private void processOrder(Order order) {
        // Retrieve or atomically instantiate and OrderBook for the given ticker symbol
        OrderBook book = booksBySymbol.computeIfAbsent(order.getSymbol(), OrderBook::new);
        List<Trade> trades = book.submitOrder(order);
        if (!trades.isEmpty()) {
            globalTradeLedger.addAll(trades);
            for (Trade t : trades) {
                System.out.println("   >>>  [EXECUTION]  " + t);
            }
        }
    }

    /**
     * Initiates orderly shutdown: stops ingestion, injects poison pill, and waits
     * for consumer to finish.`
     */
    public synchronized void stop() {
        if (isRunning.compareAndSet(true, false)) {
            try {
                // Signal termination via poison pill
                inboundQueue.put(Order.POISON_PILL);
                if (consumerThread != null) {
                    consumerThread.join();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public OrderBook getOrderBook(String symbol) {
        return booksBySymbol.get(symbol.toUpperCase().trim());
    }

    public List<Trade> getGlobalTradeLedger() {
        synchronized (globalTradeLedger) {
            return new ArrayList<>(globalTradeLedger);
        }
    }

    public int getPendingQueueSize() {
        return inboundQueue.size();
    }
}
