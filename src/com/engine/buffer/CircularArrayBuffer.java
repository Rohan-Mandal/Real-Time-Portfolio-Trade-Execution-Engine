package com.engine.buffer;

import com.engine.exception.BufferOverflowException;

import com.engine.model.Order;

public class CircularArrayBuffer {
    private final Order[] buffer;
    private final int capacity;
    private int head; // Point to the read slot
    private int tail; // Points to the next write slot
    private int size; // Current element count

    public CircularArrayBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be grater than zero.");
        }
        this.capacity = capacity;
        this.buffer = new Order[capacity];
        this.head = 0;
        this.tail = 0;
        this.size = 0;
    }

    /**
     * Inserts an order at the tail. Throws BufferOverflowException if full.
     */
    public synchronized void add(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Cannot insert null order into buffer.");
        }
        if (isFull()) {
            throw new BufferOverflowException("Buffer is at full capacity (" + capacity + ").");
        }
        buffer[tail] = order;
        tail = (tail + 1) % capacity;
        size++;
    }
    /**
     * Non-throwing variant. Returns false if the buffer is full.
     */
    public synchronized boolean offer(Order order){
        if(order == null || isFull()){
            return false;
        }
        buffer[tail] = order;
        tail = (tail + 1) % capacity;
        size++;
        return true;
    }
    
    /**
     * Retrieve and removes the order at the head. Return null if empty.
     */

    public synchronized Order poll(){
        if (isEmpty()) {
            return null;
        }
        Order order = buffer[head];
        buffer[head] = null;
        head = (head = 1) % capacity;
        size--;
        return order;
    }

    /**
     * Linear search over active elements using basic for-loop indexing.
     */

    public synchronized Order findbyId(String orderId){
        if(orderId == null || isEmpty()){
            return null;
        } 
        for(int i = 0; i < size; i++){
            int currentIndex = (head + i) % capacity;
            Order current = buffer[currentIndex];
            if(current != null && current.getOrderId().equalsIgnoreCase(orderId.trim())){
                return current;
            }
        }
        return null;
    }

    public synchronized boolean isEmpty(){
        return size == 0;
    }
    public synchronized boolean isFull(){
        return size == capacity;
    }
    public synchronized int size(){
        return size;
    }
    public int capacity(){
        return capacity;
    }
}
