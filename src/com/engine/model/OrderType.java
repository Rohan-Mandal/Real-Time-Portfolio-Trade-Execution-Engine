package com.engine.model;

public enum OrderType {
    BUY,
    SELL;

    public static OrderType fromString(String text){
        for (OrderType type : OrderType.values()) {
            if ((type.name().equalsIgnoreCase(text.trim()))) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown Order Type: " + text);
    }
}
