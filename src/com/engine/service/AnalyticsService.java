package com.engine.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.engine.model.Trade;

public class AnalyticsService {
    private final List<Trade> trades;

    public AnalyticsService(List<Trade> trades) {
        this.trades = trades;
    }

    /**
     * Map & Reduce: Calculates total dollar volume executed across all trades.
     */
    public double calculateTotalTurnover() {
        return trades.stream()
                .mapToDouble(t -> t.getPrice() * t.getQuantity())
                .sum();
    }

    /**
     * Filters & Collect: Returns trades where dollar value exceeds the threshold.
     */
    public List<Trade> findHighValueTrades(double threshold) {
        return trades.stream()
                .filter(t -> (t.getPrice() * t.getQuantity()) >= threshold)
                .collect(Collectors.toList());
    }

    /**
     * GroupingBy: Aggregates total shares traded per ticker symbol.
     */
    public Map<String, Integer> getVolumeBySymbol() {
        return trades.stream().collect(Collectors.groupingBy(Trade::getSymbol, Collectors
                .summingInt(Trade::getQuantity)));
    }

    /**
     * GroupingBy & Average: Computes Volume weighted Average Price (VWAP) per
     * symbol.
     */
    public Map<String, Double> getAveragePriceBySymbol() {
        return trades.stream()
                .collect(Collectors.groupingBy(
                        Trade::getSymbol,
                        Collectors.averagingDouble(Trade::getPrice)));
    }

    /**
     * Optional & Max: Identifies the single trade with the highest unit price.
     */
    public Optional<Trade> getHighestPriceTrade() {
        return trades.stream()
                .max(Comparator.comparingDouble(Trade::getPrice));
    }

    /**
     * optional & Max: identifies the ticket symbol that experienced the highest
     * volume.
     */
    public Optional<Map.Entry<String, Integer>> getMostTradeSymbol() {
        return getVolumeBySymbol().entrySet().stream()
                .max(Map.Entry.comparingByValue());
    }
}
