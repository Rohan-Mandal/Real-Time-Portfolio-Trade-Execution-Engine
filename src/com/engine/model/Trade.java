package com.engine.model;

import java.util.Objects;

public class Trade {
    private final String tradeId;
    private final String buyOrderId;
    private final String sellOrderId;
    private final String symbol;
    private final double price;
    private final int quantity;
    private final long timestamp;
    
    public Trade(String tradeId, String buyOrderId, String sellOrderId, String symbol, double price, int quantity,
            long timestamp) {
        this.tradeId = tradeId;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = timestamp;
    }

    public String getTradeId() {
        return tradeId;
    }

    public String getBuyOrderId() {
        return buyOrderId;
    }

    public String getSellOrderId() {
        return sellOrderId;
    }

    public String getSymbol() {
        return symbol;
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
    public boolean equals(Object o){
        if (this == o) {
            return true;
        }
        if(!(o instanceof Trade)){
            return false;
        }
        Trade trade = (Trade) o;
        return Objects.equals(tradeId, trade.tradeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tradeId);
    }

    @Override 
    public String toString(){
        return String.format("Trade[ID=%s, %s, Price=%.2f, Qty=%d, Buy=%s, Sell=%s]",
                tradeId, symbol, price, quantity, buyOrderId, sellOrderId);
    }
}
