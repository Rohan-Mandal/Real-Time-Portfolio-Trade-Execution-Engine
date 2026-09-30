package com.engine;

import com.engine.buffer.CircularArrayBuffer;
import com.engine.exception.InvalidOrderException;
import  com.engine.exception.BufferOverflowException;
import com.engine.model.Order;
import com.engine.util.StringParserUtil;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== 1. Testing StringparseUtil ===");
        String[] testInputs = {
                "ORD001|AAPL|BUY|182.50|50",
                "ORD002|MSFT|SELL|415.20|20",
                "ORD003|TSLA|HOLD|200.00|10", // Invalid type
                "ORD004|GOOGL|BUY|-10.00|15", // Negative price
                "ORD005|AMZN|SELL|175.00|0", // Zero quantity
                "MALFORMED_LINE_DATA" // Missing fields
        };
        for (String input : testInputs) {
            try {
                Order parsed = StringParserUtil.parseOrder(input);
                System.out.println("[SUCCESS] Parsed: " + parsed);
            } catch (InvalidOrderException e) {
                System.out.println("[REJECTED] Input: \"" + input + "\" -> Reason: " + e.getMessage());
            }
        }

        System.out.println("\n=== 2. Testing CircularArrayBuffer ===");
        CircularArrayBuffer buffer = new CircularArrayBuffer(3);

        try {
            Order o1 = StringParserUtil.parseOrder("ORD001|AAPL|BUY|180.00|10");
            Order o2 = StringParserUtil.parseOrder("ORD002|AAPL|SELL|181.00|10");
            Order o3 = StringParserUtil.parseOrder("ORD003|MSFT|BUY|410.00|5");
            Order o4 = StringParserUtil.parseOrder("ORD004|NVDA|BUY|900.00|2");

            buffer.add(o1);
            buffer.add(o2);
            buffer.add(o3);
            System.out.println("Buffer size after 3 additions: " + buffer.size() + "/" + buffer.capacity());

            // Search by ID
            Order found = buffer.findbyId("ORD002");
            System.out.println("Search 'ORD002': " + (found != null ? found : "Not found"));

            // Polling head element to free up a slot
            Order polled = buffer.poll();
            System.out.println("Polled from buffer: " + polled.getOrderId());

            // Adding after wrap-around
            buffer.add(o4);
            System.out.println("Added ORD004 successfully. Current size: " + buffer.size());

            // Testing overflow exception
            System.out.println("Attempting overflow insertion...");
            Order o5 = StringParserUtil.parseOrder("ORD005|META|BUY|500.00|8");
            buffer.add(o5); // This will throw BufferOverflowException

        } catch (InvalidOrderException e) {
            System.err.println("Validation failed: " + e.getMessage());
        } catch (BufferOverflowException e) {
            System.out.println("[CAUGHT EXPECTED OVERFLOW] " + e.getMessage());
        }

        System.out.println("\nPhase 1 verification complete.");

    }
}