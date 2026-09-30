package com.engine.util;

import com.engine.exception.InvalidOrderException;
import com.engine.model.Order;
import com.engine.model.OrderType;

public class StringParserUtil {
    private static final String DELIMITER_REGEX = "\\|";
    private static final int EXPECTED_FIELD_COUNT = 5;

    public StringParserUtil() {
        // Prevent instantiation of utility class
    }

    /**
     * Expected format: ORDER_ID|SYMBOL|TYPE|PRICE|QUANTITY
     * Example: "ORD101|AAPL|BUY|150.50|10"
     */
    public static Order parseOrder(String rawLine) throws InvalidOrderException {
        if (rawLine == null || rawLine.trim().isEmpty()) {
            throw new InvalidOrderException("Input line cannot be null or empty.");
        }

        String[] tokens = rawLine.split(DELIMITER_REGEX);
        int tokenLength = tokens.length;
        if (tokenLength != EXPECTED_FIELD_COUNT) {
            throw new InvalidOrderException(
                    String.format("Invalid token count : Expected %d fields, got %d from input: '%s'",
                            EXPECTED_FIELD_COUNT, tokenLength, rawLine));
        }
        
        String orderId = tokens[0].trim();
        String symbol = tokens[1].trim();
        String typeStr = tokens[2].trim();
        String priceStr = tokens[3].trim();
        String qtyStr = tokens[4].trim();

        // validate OrderID
        if(orderId.isEmpty()){
            throw new InvalidOrderException("Order ID cannot be blank.");
        }
        // Validate symbol
        if(symbol.isEmpty()){
            throw new InvalidOrderException("Stock symbol cannot be blank.");
        }
        // Validate order Type
        OrderType type;
        try{
            type = OrderType.fromString(typeStr);
        } catch(IllegalArgumentException e){
            throw new InvalidOrderException("Invalid orde type:" + typeStr + "'. Expected BUY or SELL");
        }

        // validate price
        double price;
        try{
            price = Double.parseDouble(priceStr);
            if(price <= 0.0){
                throw new InvalidOrderException("Price must be strictly positive. Found: " + priceStr);
            }
        } catch(NumberFormatException e){
            throw new InvalidOrderException("Price is not a valid number: '" + priceStr + "'");
        }

        // Validate Quantity
        int quantity;
        try{
            quantity = Integer.parseInt(qtyStr);
            if (quantity <= 0) {
                throw new InvalidOrderException("Quantity must be strictly positive. Found: '" + quantity + "'");
            }
        } catch(NumberFormatException e){
            throw new InvalidOrderException("Quantity is not a valid integer: '" + qtyStr + "'");
        }

        return new Order(orderId, symbol, type, price, quantity, System.currentTimeMillis());
    }
}
