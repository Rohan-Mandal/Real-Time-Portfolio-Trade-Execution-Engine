package com.engine.service;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import com.engine.model.Order;
import com.engine.model.OrderType;
import com.engine.model.Trade;

public class OrderBook {
    private final String symbol;
    private final PriorityQueue<Order> buyOrders;
    private final PriorityQueue<Order> sellOrders;
    private final List<Trade> tradeHistory;
    private long tradeSequence = 0;

    public OrderBook(String symbol) {
        this.symbol = symbol;
        this.buyOrders = new PriorityQueue<>(Order.BUY_COMPARATOR);
        this.sellOrders = new PriorityQueue<>(Order.SELL_COMPARATOR);
        this.tradeHistory = new ArrayList<>();
    }

    public String getSymbol(){
        return symbol;
    }

    /**
     * Submits an order to the book and trigger the matching process.
     */
    public synchronized List<Trade> submitOrder(Order order){
        if (!order.getSymbol().equalsIgnoreCase(symbol)) {
            throw new IllegalArgumentException("Symbol mismathc: Order symbol " 
            + order.getSymbol() + " does not match book " + symbol);
        }
        if (order.getType() == OrderType.BUY) {
            buyOrders.add(order);
        } else{
            sellOrders.add(order);
        }
        return matchOrder();
    }

    /**
     * Core matching algorith: Crosses bids and ask based on price-time priority.
     */
    public List<Trade> matchOrder(){
        List<Trade> executeTrades = new ArrayList<>();

        while(!buyOrders.isEmpty() && !sellOrders.isEmpty()){
            Order bestBid = buyOrders.peek();
            Order bestAsk = sellOrders.peek();

            // Condition for a trade: Buyer si willing to pay at least the Seller's ask price
            if (bestBid.getPrice() >= bestAsk.getPrice()) {
                int matchedQty = Math.min(bestBid.getQuantity(), bestAsk.getQuantity());

                // Trade price is typically the resting order's price(the earlier order)
                double executionPrice = bestBid.getTimestamp() < bestAsk.getTimestamp() ? bestBid.getPrice() : bestAsk.getPrice();
                long timeStamp = bestBid.getTimestamp() < bestAsk.getTimestamp() ? bestBid.getTimestamp() : bestAsk.getTimestamp();
                tradeSequence++;
                String tradeId = "TRD-" + symbol + "-" + tradeSequence;

                Trade trade = new Trade(tradeId, bestBid.getOrderId(), bestAsk.getOrderId(), symbol, executionPrice, matchedQty, timeStamp);
                executeTrades.add(trade);
                tradeHistory.add(trade);

                // Adjust quantities
                bestBid.setQuantity(bestBid.getQuantity() - matchedQty);
                bestAsk.setQuantity(bestAsk.getQuantity() - matchedQty);

                // Evictfully-filled orders
                if (bestBid.getQuantity() == 0) {
                    buyOrders.poll();
                }
                if (bestAsk.getQuantity() == 0) {
                    sellOrders.poll();
                }
            } else{
                // highest bid is below lowest ask-> no match possible
                break;
            }
        }
        return executeTrades;
    }

    public synchronized int getBuyOrderCount(){
        return buyOrders.size();
    }

    public synchronized int getSellOrderCount(){
        return sellOrders.size();
    }

    public synchronized List<Trade> getTradeHistory(){
        return new ArrayList<>(tradeHistory);
    }
}
