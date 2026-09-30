package com.engine.model;

import java.util.Objects;

public class Order {
    private final String orderId;
    private final String symbol;
    private final OrderType type;
    private final double price;
    private int quantity;
    private final long timestamp;

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
