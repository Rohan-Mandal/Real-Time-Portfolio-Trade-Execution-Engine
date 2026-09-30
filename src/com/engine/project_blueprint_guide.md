# PROJECT SPECIFICATION & TECHNICAL BLUEPRINT
## Pure Java Real-Time Portfolio & Trade Execution Engine

---

# 1. Executive Summary & Objective

The objective of this project is to build a high-performance, in-memory **Order Matching and Portfolio Analytics Engine** using **Pure Java (Standard Edition)** without external dependencies, libraries, or frameworks (such as Spring, Lombok, or Guava).

The system is deliberately structured into sequential architectural tiers. Building it will systematically test and solidify your knowledge of Java:
1. **Core Language Fundamentals:** Memory indexing with primitive arrays, string manipulation, procedural loops, custom checked/unchecked exceptions.
2. **Object-Oriented Design & Collections:** Encapsulation, immutability, `Comparable`/`Comparator`, `PriorityQueue`, `HashMap`, `TreeMap`.
3. **Multithreading & Concurrency:** Thread life-cycles, `BlockingQueue`, Producer-Consumer architecture, atomic primitives (`AtomicLong`), reentrant locking, race condition mitigation.
4. **Modern Java 8 Paradigms:** Functional interfaces, method references, lambda expressions, `Optional<T>`, declarative pipeline processing via the `Stream` API (`filter`, `map`, `reduce`, `groupingBy`).
5. **Persistence & I/O:** Java NIO, try-with-resources, disk serialization, and audit logging.

---

# 2. High-Level Architecture (HLD)

### 2.1 System Architecture Diagram

```
                     ┌───────────────────────────────────────┐
                     │          Input Data Stream            │
                     │  (Console CLI / Batch CSV File Feed)   │
                     └───────────────────┬───────────────────┘
                                         │
                                         ▼
                     ┌───────────────────────────────────────┐
                     │        Order Ingestion Workers        │
                     │  - StringParserUtil (String splits)   │
                     │  - Validation (Custom Exceptions)     │
                     │  - Fixed Array Ring Buffer (Raw Array)│
                     └───────────────────┬───────────────────┘
                                         │
                                         ▼
                     ┌───────────────────────────────────────┐
                     │   Thread-Safe Ingestion Dispatcher    │
                     │   (BlockingQueue / Producer-Consumer) │
                     └───────────────────┬───────────────────┘
                                         │
                                         ▼
                     ┌───────────────────────────────────────┐
                     │         Order Matching Engine         │
                     │  - Symbol-specific Order Books        │
                     │  - Price-Time Priority (PriorityQueue)│
                     │  - Trade Execution & ID Generation    │
                     └───────────────────┬───────────────────┘
                                         │
                                         ▼
                     ┌───────────────────────────────────────┐
                     │       Portfolio & Analytics Hub       │
                     │  - Java 8 Streams (PnL, Top Movers)   │
                     │  - Functional Listeners (Lambdas)     │
                     │  - File Audit Persistence (Java NIO)  │
                     └───────────────────────────────────────┘
```

### 2.2 Component Responsibilities

1. **Ingestion & Validation Engine:** Reads pipe-delimited raw strings (`"ORD101|AAPL|BUY|150.25|50"`). Performs validation checks using basic control flow (`if`/`else`, `switch`) and populates a custom circular array buffer.
2. **Matching Engine (Order Book):** Maintains bid and ask sides for each symbol. Matches orders when $\text{Bid Price} \ge \text{Ask Price}$. Manages execution quantities, partial fills, and trade emission.
3. **Execution Pipeline (Concurrency):** Decouples order ingestion from matching using concurrent workers. Producer threads ingest orders concurrently while a consumer dispatch thread routes orders to the order book under strict concurrency locks.
4. **Portfolio & Analytics Hub:** Uses Java 8 Streams and lambdas to calculate metrics (e.g., net portfolio turnover, asset allocation percentages, high-volume traders) over trade streams in real time.
5. **Audit Logger:** Persists trade confirmations to disk through buffered channels using Java NIO.

---

# 3. Java Concept-to-Component Matrix

| Module | Java Fundamentals Practiced |
| :--- | :--- |
| **Domain Models** | Primitive types (`double`, `int`, `long`), references, `enum`, `equals()`, `hashCode()`, `toString()` |
| **Array Buffer** | 1D fixed-size arrays (`T[]`), pointer arithmetic (`(tail + 1) % size`), standard `for`/`while` loops |
| **Parsing & Exceptions** | `String`, `StringBuilder`, `String.split()`, checked (`Exception`) vs unchecked (`RuntimeException`) |
| **Order Book** | `Comparable<T>`, `Comparator<T>`, `PriorityQueue`, `HashMap`, `TreeMap`, `Collections` |
| **Matching & Concurrency**| `Thread`, `Runnable`, `BlockingQueue`, `ReentrantLock`, `AtomicLong`, `volatile` |
| **Analytics Engine** | `Stream<T>`, `.filter()`, `.mapToDouble()`, `.collect(Collectors.groupingBy())`, `Optional<T>`, Lambdas |
| **Persistence** | `java.nio.file.Files`, `Paths`, `StandardOpenOption`, `BufferedReader`, try-with-resources |

---

# 4. Project Directory Structure

```text
trade-engine/
└── src/
    └── com/
        └── engine/
            ├── Main.java
            ├── model/
            │   ├── Order.java
            │   ├── Trade.java
            │   ├── OrderType.java             (BUY, SELL)
            │   └── OrderStatus.java           (NEW, PARTIAL, FILLED, REJECTED)
            ├── buffer/
            │   └── CircularArrayBuffer.java   (Raw array ring buffer)
            ├── exception/
            │   ├── InvalidOrderException.java (Checked)
            │   └── EngineBusyException.java   (Unchecked)
            ├── service/
            │   ├── OrderBook.java             (PriorityQueue matching)
            │   ├── MatchingEngine.java        (Multi-book manager)
            │   ├── PortfolioService.java      (Stream analytics)
            │   └── AuditLogger.java           (NIO persistence)
            └── util/
                └── StringParserUtil.java      (String/Loop validation)
```

---

# 5. Step-by-Step Implementation Roadmap

---

## Phase 1: Core Fundamentals (Primitives, Arrays, Strings, Exceptions)

### Step 1.1: Domain Enums & Custom Exceptions
Create baseline enums and establish checked vs. unchecked exception handling.

* **File:** `model/OrderType.java`
```java
package com.engine.model;

public enum OrderType {
    BUY, SELL
}
```

* **File:** `model/OrderStatus.java`
```java
package com.engine.model;

public enum OrderStatus {
    NEW, PARTIALLY_FILLED, FILLED, REJECTED, CANCELLED
}
```

* **File:** `exception/InvalidOrderException.java`
```java
package com.engine.exception;

// Checked exception: callers are forced to handle malformed input
public class InvalidOrderException extends Exception {
    public InvalidOrderException(String message) {
        super(message);
    }
}
```

* **File:** `exception/EngineBusyException.java`
```java
package com.engine.exception;

// Unchecked exception: indicates system-level operational overload
public class EngineBusyException extends RuntimeException {
    public EngineBusyException(String message) {
        super(message);
    }
}
```

---

### Step 1.2: Raw Array-Based Circular Buffer
Implement a custom bounded queue using only raw arrays and modulo indexing to master pointer tracking without utility libraries.

* **File:** `buffer/CircularArrayBuffer.java`
```java
package com.engine.buffer;

import com.engine.model.Order;

public class CircularArrayBuffer {
    private final Order[] buffer;
    private final int capacity;
    private int head = 0;
    private int tail = 0;
    private int size = 0;

    public CircularArrayBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.buffer = new Order[capacity];
    }

    public synchronized boolean offer(Order order) {
        if (size == capacity) {
            return false; // Buffer full
        }
        buffer[tail] = order;
        tail = (tail + 1) % capacity;
        size++;
        return true;
    }

    public synchronized Order poll() {
        if (size == 0) {
            return null; // Buffer empty
        }
        Order item = buffer[head];
        buffer[head] = null; // Clean reference for GC
        head = (head + 1) % capacity;
        size--;
        return item;
    }

    public synchronized int size() {
        return size;
    }

    public synchronized boolean isEmpty() {
        return size == 0;
    }

    // Demonstrates manual loop indexing and basic string matching
    public synchronized Order findByIdLinear(String orderId) {
        for (int i = 0; i < size; i++) {
            int index = (head + i) % capacity;
            if (buffer[index] != null && buffer[index].getOrderId().equals(orderId)) {
                return buffer[index];
            }
        }
        return null;
    }
}
```

---

### Step 1.3: String Parsing & Manual Validation
Validate incoming raw messages using basic string scanning, condition checking, and manual conversions.

* **File:** `util/StringParserUtil.java`
```java
package com.engine.util;

import com.engine.exception.InvalidOrderException;
import com.engine.model.Order;
import com.engine.model.OrderType;

public class StringParserUtil {

    // Expected format: "ORDER_ID|SYMBOL|TYPE|PRICE|QUANTITY"
    // Example: "ORD101|AAPL|BUY|150.25|50"
    public static Order parseOrderString(String rawLine) throws InvalidOrderException {
        if (rawLine == null || rawLine.trim().isEmpty()) {
            throw new InvalidOrderException("Input line cannot be null or empty");
        }

        String[] tokens = rawLine.split("\\|");
        if (tokens.length != 5) {
            throw new InvalidOrderException("Malformed input. Expected 5 tokens, received: " + tokens.length);
        }

        String orderId = tokens[0].trim();
        String symbol = tokens[1].trim().toUpperCase();
        String typeStr = tokens[2].trim().toUpperCase();
        String priceStr = tokens[3].trim();
        String qtyStr = tokens[4].trim();

        if (orderId.isEmpty() || symbol.isEmpty()) {
            throw new InvalidOrderException("Order ID and Symbol cannot be blank");
        }

        OrderType type;
        try {
            type = OrderType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            throw new InvalidOrderException("Invalid order type: " + typeStr + ". Must be BUY or SELL.");
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price <= 0.0) {
                throw new InvalidOrderException("Price must be strictly positive: " + price);
            }
        } catch (NumberFormatException e) {
            throw new InvalidOrderException("Invalid numeric price: " + priceStr);
        }

        int quantity;
        try {
            quantity = Integer.parseInt(qtyStr);
            if (quantity <= 0) {
                throw new InvalidOrderException("Quantity must be greater than zero: " + quantity);
            }
        } catch (NumberFormatException e) {
            throw new InvalidOrderException("Invalid integer quantity: " + qtyStr);
        }

        return new Order(orderId, symbol, type, price, quantity);
    }
}
```

---

## Phase 2: Object-Oriented Design & Java Collections Framework

### Step 2.1: Domain Models with `Comparable`
Implement complete domain entities. Ensure correct implementations of `.equals()`, `.hashCode()`, and `.compareTo()`.

* **File:** `model/Order.java`
```java
package com.engine.model;

import java.util.Objects;

public class Order implements Comparable<Order> {
    private final String orderId;
    private final String symbol;
    private final OrderType type;
    private final double price;
    private int quantity;
    private final int originalQuantity;
    private OrderStatus status;
    private final long timestamp;

    public Order(String orderId, String symbol, OrderType type, double price, int quantity) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.originalQuantity = quantity;
        this.status = OrderStatus.NEW;
        this.timestamp = System.nanoTime();
    }

    public String getOrderId() { return orderId; }
    public String getSymbol() { return symbol; }
    public OrderType getType() { return type; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public int getOriginalQuantity() { return originalQuantity; }
    public OrderStatus getStatus() { return status; }
    public long getTimestamp() { return timestamp; }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    // Natural ordering: Price-time priority for BUY orders (highest price first).
    // Note: OrderBook overrides this for SELL orders using custom comparators.
    @Override
    public int compareTo(Order other) {
        int priceComparison = Double.compare(other.price, this.price);
        if (priceComparison != 0) {
            return priceComparison;
        }
        return Long.compare(this.timestamp, other.timestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order)) return false;
        Order order = (Order) o;
        return Objects.equals(orderId, order.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s %s %d@%.2f (%s)", 
            orderId, symbol, type, quantity, price, status);
    }
}
```

* **File:** `model/Trade.java`
```java
package com.engine.model;

import java.time.Instant;

public class Trade {
    private final String tradeId;
    private final String buyOrderId;
    private final String sellOrderId;
    private final String symbol;
    private final double executionPrice;
    private final int quantity;
    private final Instant executedAt;

    public Trade(String tradeId, String buyOrderId, String sellOrderId, 
                 String symbol, double executionPrice, int quantity) {
        this.tradeId = tradeId;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.symbol = symbol;
        this.executionPrice = executionPrice;
        this.quantity = quantity;
        this.executedAt = Instant.now();
    }

    public String getTradeId() { return tradeId; }
    public String getBuyOrderId() { return buyOrderId; }
    public String getSellOrderId() { return sellOrderId; }
    public String getSymbol() { return symbol; }
    public double getExecutionPrice() { return executionPrice; }
    public int getQuantity() { return quantity; }
    public Instant getExecutedAt() { return executedAt; }

    @Override
    public String toString() {
        return String.format("TRADE: #%s %s Qty:%d @ $%.2f [BuyRef:%s, SellRef:%s]",
            tradeId, symbol, quantity, executionPrice, buyOrderId, sellOrderId);
    }
}
```

---

### Step 2.2: Order Book Implementation
Implement matching logic using `PriorityQueue`, `HashMap`, and `Comparator`.

* **File:** `service/OrderBook.java`
```java
package com.engine.service;

import com.engine.model.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class OrderBook {
    private final String symbol;
    private final PriorityQueue<Order> buyOrders;
    private final PriorityQueue<Order> sellOrders;
    private final Map<String, Order> orderIndex;
    private final AtomicLong tradeIdSequence = new AtomicLong(1000);

    public OrderBook(String symbol) {
        this.symbol = symbol;
        // Buy Queue: Descending order by price (highest bid first)
        this.buyOrders = new PriorityQueue<>((o1, o2) -> {
            int comp = Double.compare(o2.getPrice(), o1.getPrice());
            return comp != 0 ? comp : Long.compare(o1.getTimestamp(), o2.getTimestamp());
        });

        // Sell Queue: Ascending order by price (lowest ask first)
        this.sellOrders = new PriorityQueue<>((o1, o2) -> {
            int comp = Double.compare(o1.getPrice(), o2.getPrice());
            return comp != 0 ? comp : Long.compare(o1.getTimestamp(), o2.getTimestamp());
        });

        this.orderIndex = new HashMap<>();
    }

    public synchronized List<Trade> processOrder(Order order) {
        orderIndex.put(order.getOrderId(), order);
        List<Trade> trades = new ArrayList<>();

        if (order.getType() == OrderType.BUY) {
            buyOrders.add(order);
        } else {
            sellOrders.add(order);
        }

        executeMatches(trades);
        return trades;
    }

    private void executeMatches(List<Trade> trades) {
        while (!buyOrders.isEmpty() && !sellOrders.isEmpty()) {
            Order topBuy = buyOrders.peek();
            Order topSell = sellOrders.peek();

            // Match condition: Bid price must meet or beat Ask price
            if (topBuy.getPrice() >= topSell.getPrice()) {
                int matchedQty = Math.min(topBuy.getQuantity(), topSell.getQuantity());
                // Execution price follows the earlier resting order (topSell in standard books)
                double executionPrice = topSell.getPrice();

                String tradeId = "TRD-" + tradeIdSequence.incrementAndGet();
                Trade trade = new Trade(tradeId, topBuy.getOrderId(), topSell.getOrderId(), 
                                        symbol, executionPrice, matchedQty);
                trades.add(trade);

                topBuy.setQuantity(topBuy.getQuantity() - matchedQty);
                topSell.setQuantity(topSell.getQuantity() - matchedQty);

                if (topBuy.getQuantity() == 0) {
                    topBuy.setStatus(OrderStatus.FILLED);
                    buyOrders.poll();
                } else {
                    topBuy.setStatus(OrderStatus.PARTIALLY_FILLED);
                }

                if (topSell.getQuantity() == 0) {
                    topSell.setStatus(OrderStatus.FILLED);
                    sellOrders.poll();
                } else {
                    topSell.setStatus(OrderStatus.PARTIALLY_FILLED);
                }
            } else {
                break; // No spread overlap; order book is settled
            }
        }
    }

    public synchronized Optional<Order> getOrderById(String orderId) {
        return Optional.ofNullable(orderIndex.get(orderId));
    }
}
```

---

## Phase 3: Multithreading & Concurrency

Decouple order ingestion from the matching engine using `BlockingQueue`, worker threads, and graceful shutdown handling.

* **File:** `service/MatchingEngine.java`
```java
package com.engine.service;

import com.engine.model.Order;
import com.engine.model.Trade;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class MatchingEngine {
    private final BlockingQueue<Order> inboundQueue = new ArrayBlockingQueue<>(1024);
    private final Map<String, OrderBook> books = new ConcurrentHashMap<>();
    private final List<Consumer<Trade>> tradeListeners = new CopyOnWriteArrayList<>();
    private final ExecutorService consumerExecutor = Executors.newSingleThreadExecutor();
    private volatile boolean running = true;

    public MatchingEngine() {
        startDispatcher();
    }

    public void registerTradeListener(Consumer<Trade> listener) {
        tradeListeners.add(listener);
    }

    public boolean submitOrder(Order order) {
        if (!running) {
            return false;
        }
        return inboundQueue.offer(order);
    }

    private void startDispatcher() {
        consumerExecutor.submit(() -> {
            while (running || !inboundQueue.isEmpty()) {
                try {
                    Order order = inboundQueue.poll(200, TimeUnit.MILLISECONDS);
                    if (order != null) {
                        OrderBook book = books.computeIfAbsent(order.getSymbol(), OrderBook::new);
                        List<Trade> executedTrades = book.processOrder(order);
                        
                        // Notify listeners using Java 8 functional consumers
                        for (Trade trade : executedTrades) {
                            for (Consumer<Trade> listener : tradeListeners) {
                                listener.accept(trade);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    public void shutdown() {
        this.running = false;
        consumerExecutor.shutdown();
        try {
            if (!consumerExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                consumerExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            consumerExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

---

## Phase 4: Java 8 Features (Streams, Lambdas, Optional)

Implement analytical reports using declarative Java 8 stream pipelines.

* **File:** `service/PortfolioService.java`
```java
package com.engine.service;

import com.engine.model.Trade;

import java.util.*;
import java.util.stream.Collectors;

public class PortfolioService {
    private final List<Trade> tradeHistory = new ArrayList<>();

    public synchronized void recordTrade(Trade trade) {
        tradeHistory.add(trade);
    }

    // 1. Filter: Extract all trades for a designated ticker
    public synchronized List<Trade> getTradesBySymbol(String symbol) {
        return tradeHistory.stream()
                .filter(t -> t.getSymbol().equalsIgnoreCase(symbol))
                .collect(Collectors.toList());
    }

    // 2. Map & Reduce: Compute overall dollar turnover
    public synchronized double calculateTotalTurnover() {
        return tradeHistory.stream()
                .mapToDouble(t -> t.getExecutionPrice() * t.getQuantity())
                .sum();
    }

    // 3. GroupingBy: Categorize trades by ticker symbol
    public synchronized Map<String, List<Trade>> getTradesGroupedBySymbol() {
        return tradeHistory.stream()
                .collect(Collectors.groupingBy(Trade::getSymbol));
    }

    // 4. Volume aggregation by symbol
    public synchronized Map<String, Integer> getTotalVolumePerSymbol() {
        return tradeHistory.stream()
                .collect(Collectors.groupingBy(
                        Trade::getSymbol,
                        Collectors.summingInt(Trade::getQuantity)
                ));
    }

    // 5. Optional & Max: Find the single highest value transaction
    public synchronized Optional<Trade> findLargestTrade() {
        return tradeHistory.stream()
                .max(Comparator.comparingDouble(t -> t.getExecutionPrice() * t.getQuantity()));
    }
}
```

---

## Phase 5: Persistence & I/O (NIO & File Handling)

Implement trade audit logging using modern Java NIO utilities and auto-closing resources.

* **File:** `service/AuditLogger.java`
```java
package com.engine.service;

import com.engine.model.Trade;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class AuditLogger {
    private final Path logFilePath;

    public AuditLogger(String fileName) {
        this.logFilePath = Paths.get(fileName);
        initFile();
    }

    private void initFile() {
        try {
            if (!Files.exists(logFilePath)) {
                Files.createFile(logFilePath);
            }
        } catch (IOException e) {
            System.err.println("Failed to initialize audit file: " + e.getMessage());
        }
    }

    public synchronized void logTrade(Trade trade) {
        String logEntry = String.format("%s | %s | %s | %d | %.2f | %s%n",
                trade.getExecutedAt(),
                trade.getTradeId(),
                trade.getSymbol(),
                trade.getQuantity(),
                trade.getExecutionPrice(),
                trade.getBuyOrderId() + "<->" + trade.getSellOrderId()
        );

        // Try-with-resources handles closing file handles reliably
        try (BufferedWriter writer = Files.newBufferedWriter(
                logFilePath,
                StandardOpenOption.CREATE, 
                StandardOpenOption.APPEND)) {
            writer.write(logEntry);
        } catch (IOException e) {
            System.err.println("Failed to append audit record: " + e.getMessage());
        }
    }
}
```

---

## Phase 6: Orchestration (`Main.java`)

Bring every subsystem together in an end-to-end operational pipeline.

* **File:** `Main.java`
```java
package com.engine;

import com.engine.buffer.CircularArrayBuffer;
import com.engine.exception.InvalidOrderException;
import com.engine.model.Order;
import com.engine.service.AuditLogger;
import com.engine.service.MatchingEngine;
import com.engine.service.PortfolioService;
import com.engine.util.StringParserUtil;

import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   INITIALIZING PURE JAVA TRADING & PORTFOLIO ENGINE");
        System.out.println("==================================================");

        // 1. Initialize Core Services
        MatchingEngine engine = new MatchingEngine();
        PortfolioService portfolio = new PortfolioService();
        AuditLogger logger = new AuditLogger("trades_audit.log");

        // 2. Attach Listeners via Lambdas (Java 8)
        engine.registerTradeListener(portfolio::recordTrade);
        engine.registerTradeListener(logger::logTrade);
        engine.registerTradeListener(trade -> 
            System.out.println(">>> EXECUTION DISPATCHED: " + trade)
        );

        // 3. Test Raw Array Circular Buffer (Phase 1 Validation)
        System.out.println("\n--- Step 1: Testing Fixed Array Circular Buffer ---");
        CircularArrayBuffer buffer = new CircularArrayBuffer(3);
        Order sampleOrder1 = new Order("O-01", "AAPL", com.engine.model.OrderType.BUY, 150.0, 10);
        Order sampleOrder2 = new Order("O-02", "MSFT", com.engine.model.OrderType.BUY, 300.0, 5);
        buffer.offer(sampleOrder1);
        buffer.offer(sampleOrder2);
        System.out.println("Buffer size after 2 offers: " + buffer.size());
        System.out.println("Polled from buffer: " + buffer.poll());
        System.out.println("Buffer size after poll: " + buffer.size());

        // 4. Ingest and Parse Test Data (Loops, Strings, Exceptions)
        System.out.println("\n--- Step 2: Ingesting Raw String Feeds ---");
        List<String> rawFeeds = Arrays.asList(
            "ORD101|AAPL|SELL|180.50|100",
            "ORD102|AAPL|BUY|182.00|40",   // Crosses with ORD101 -> Match 40 @ 180.50
            "ORD103|GOOG|BUY|2800.00|10",
            "ORD104|GOOG|SELL|2790.00|5",   // Crosses with ORD103 -> Match 5 @ 2790.00
            "ORD105|AAPL|BUY|181.00|60",   // Crosses with remaining 60 of ORD101 -> Match 60 @ 180.50
            "MALFORMED_RECORD_SKIPPED"
        );

        for (String raw : rawFeeds) {
            try {
                Order parsed = StringParserUtil.parseOrderString(raw);
                boolean queued = engine.submitOrder(parsed);
                if (queued) {
                    System.out.println("Successfully submitted: " + parsed.getOrderId());
                }
            } catch (InvalidOrderException e) {
                System.err.println("Rejected line: '" + raw + "' Reason: " + e.getMessage());
            }
        }

        // Wait briefly for asynchronous matching consumer to settle
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}

        // 5. Generate Portfolio Analytics via Java 8 Streams
        System.out.println("\n==================================================");
        System.out.println("            PORTFOLIO ANALYTICS (JAVA 8)");
        System.out.println("==================================================");

        System.out.printf("Total Turnover: $%.2f%n", portfolio.calculateTotalTurnover());

        System.out.println("\nVolume Per Symbol:");
        portfolio.getTotalVolumePerSymbol().forEach((sym, vol) -> 
            System.out.println(" - " + sym + ": " + vol + " units")
        );

        System.out.println("\nTrades Grouped By Symbol:");
        portfolio.getTradesGroupedBySymbol().forEach((sym, list) -> 
            System.out.println(" - " + sym + " has " + list.size() + " completed trade(s)")
        );

        portfolio.findLargestTrade().ifPresent(t -> 
            System.out.println("\nLargest Single Trade: " + t)
        );

        // 6. Graceful Shutdown
        System.out.println("\nTerminating engine dispatcher...");
        engine.shutdown();
        System.out.println("Engine shutdown complete. Check 'trades_audit.log' for persisted logs.");
    }
}
```

---

# 6. Step-by-Step Build & Verification Guide

Follow these steps directly in your terminal or IDE:

### 1. Compile All Sources
Navigate to the root directory containing `src` and compile:
```bash
javac -d bin $(find src -name "*.java")
```
*(On Windows Command Prompt: `dir /s /b src\*.java > sources.txt && javac -d bin @sources.txt`)*

### 2. Run the Application
Execute the compiled classes from `bin`:
```bash
java -cp bin com.engine.Main
```

### 3. Verify Output
1. **Console Matches:** Confirm trades match correctly (e.g., `ORD102` buying `AAPL` from `ORD101` at `180.50`).
2. **Exception Handling:** Confirm that `"MALFORMED_RECORD_SKIPPED"` triggers an `InvalidOrderException` cleanly without crashing the pipeline.
3. **Stream Aggregation:** Confirm total volume calculations match the sum of executed trade quantities.
4. **Disk I/O:** Open `trades_audit.log` in your working directory and verify that entries were appended with valid timestamps.