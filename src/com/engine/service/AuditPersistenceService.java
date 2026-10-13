package com.engine.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.engine.model.Trade;

public class AuditPersistenceService {
    private static final String CSV_HEADER = "TRADE_ID,BUY_ORDER_ID,SELL_ORDER_ID,SYMBOL,PRICE,QUANTITY,TIMESTAMP";
    private final Path storagePath;

    public AuditPersistenceService(String filePathStr) {
        this.storagePath = Paths.get(filePathStr);
    }

    /**
     * Appends a batch of trades to the disk file using Java NIO BufferedWrite.
     * Uses try-with-resources for automatic resource management.
     */
    public synchronized void appendTrades(List<Trade> trades) throws IOException {
        if (trades == null || trades.isEmpty()) {
            return;
        }
        boolean fileExists = Files.exists(storagePath);

        // Ensure parent directories exist if configured with subpaths
        if (storagePath.getParent() != null && !Files.exists(storagePath.getParent())) {
            Files.createDirectories(storagePath.getParent());
        }
        try (BufferedWriter writer = Files.newBufferedWriter(storagePath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            // Write CSV header if the file is brand new
            if (!fileExists || Files.size(storagePath) == 0) {
                writer.write(CSV_HEADER);
                writer.newLine();
            }

            for (Trade t : trades) {
                String record = String.format("%s,%s,%s,%s,%.2f,%d,%d",
                        t.getTradeId(),
                        t.getBuyOrderId(),
                        t.getSellOrderId(),
                        t.getSymbol(),
                        t.getPrice(),
                        t.getQuantity(),
                        t.getTimestamp());
                writer.write(record);
                writer.newLine();
            }
            writer.flush();
        }
    }

    /**
     * Recovers trades from disk back into memory using Files.lines() stream
     * processing.
     */
    public List<Trade> recoveryTrades() throws IOException {
        if (!Files.exists(storagePath)) {
            return new ArrayList<>();
        }
        try (var stream = Files.lines(storagePath, StandardCharsets.UTF_8)) {
            return stream
                    .filter(line -> !line.trim().isEmpty())
                    .filter(line -> !line.startsWith("TRADE_ID")) // skip CSV header
                    .map(this::parseTradeFromCsv)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Parse a single CSV line into an immutable Trade record.
     */
    private Trade parseTradeFromCsv(String line) {
        String[] token = line.split(",");
        if (token.length < 7) {
            throw new IllegalArgumentException("Corrupt log line: " + line);
        }

        String tradeId = token[0].trim();
        String buyId = token[1].trim();
        String SellId = token[2].trim();
        String symbol = token[3].trim();
        double price = Double.parseDouble(token[4].trim());
        int qty = Integer.parseInt(token[5].trim());

        return new Trade(tradeId, buyId, SellId, symbol, price, qty, qty);
    }

    public Path getStoragePath() {
        return storagePath;
    }
}
