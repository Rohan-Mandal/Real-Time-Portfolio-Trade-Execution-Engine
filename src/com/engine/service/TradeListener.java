package com.engine.service;

import com.engine.model.Trade;

@FunctionalInterface
public interface TradeListener {
    void onTradeExecuted(Trade trade);
}
