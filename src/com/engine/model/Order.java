package com.engine.model;

import java.util.Comparator;
import java.util.Objects;

public class Order implements Comparable<Order>{
    private final String orderId;
    private final String symbol;
    private final OrderType type;
    private final double price;
    private int quantity;
    private final long timestamp;

    // Highest price first; if equal, earliest timestamp first
    public static final Comparator<Order> BUY_COMPARATOR = (o1, o2) -> {
        int priceCompare = Double.compare(o2.getPrice(), o1.getPrice());
        if(priceCompare != 0){
            return priceCompare;
        }
        return Long.compare(o1.getTimestamp(), o2.getTimestamp());
    };

    // lowest price first; if equal, earliest timestamp first
    public static final Comparator<Order> SELL_COMPARATOR = (o1, o2) -> {
        int priceCompare = Double.compare(o1.getPrice(), o2.getPrice());
        if(priceCompare != 0){
            return priceCompare;
        }
        return Long.compare(o1.getTimestamp(), o1.getTimestamp());
    };

    public Order(String orderId, String symbol, OrderType type, double price, int quantity, long timestamp) {
        this.orderId = orderId;
        this.symbol = symbol.toUpperCase().trim();
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = System.currentTimeMillis();
    }

    public String getOrderId() {
        return orderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public OrderType getType() {
        return type;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setQuantity(int quantity){
        this.quantity = quantity;
    }

    @Override 
    public int compareTo(Order other){
        // Default choronological ordering
        return Long.compare(this.timestamp, other.timestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return false;
        }
        if (!(o instanceof Order)) {
            return false;
        }
        Order order = (Order) o;
        return Objects.equals(orderId, order.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }

    @Override
    public String toString() {
        return String.format("Order[ID=%s, %s, %s, Price=%.2f, Qty=%d, Time=%d]",
                orderId, symbol, type, price, quantity, timestamp);
    }
}
