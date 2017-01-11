package com.imc.intern.trading;

import com.imc.intern.exchange.client.RemoteExchangeView;
import com.imc.intern.exchange.datamodel.Side;
import com.imc.intern.exchange.datamodel.api.OrderType;
import com.imc.intern.exchange.datamodel.api.OwnTrade;
import com.imc.intern.exchange.datamodel.api.RetailState;
import com.imc.intern.exchange.datamodel.api.Symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by imc on 10/01/2017.
 */
public class StrategyHandler
{
    private final double target = 20;
    private final double fees = .1;
    private double position_offset = 0;
    private int position = 0;
    private String BOOK;

    private HashMap<Long, MyOrder> my_orders = new HashMap<>(); // maps my orders to the actual order object
    private HashMap<Double, Integer> my_bids = new HashMap<>(); // maps my bid prices to the total volume of my bids at that price
    private HashMap<Double, Integer> my_asks = new HashMap<>(); // maps my ask prices to the total volume of my asks at that price
    private HashSet<OwnTrade> transactions = new HashSet<>(); // keeps track of my transaction history

    HashMap<Long, MyOrder> getMyOrders()
    {
        return my_orders;
    }

    HashMap<Double, Integer> getMyBids()
    {
        return my_bids;
    }

    HashMap<Double, Integer> getMyAsks()
    {
        return my_asks;
    }

    HashSet<OwnTrade> getTransactions()
    {
        return transactions;
    }

    private static int count = 0;

    StrategyHandler()
    {
        BOOK = "";
    }

    StrategyHandler(String order_book)
    {
        BOOK = order_book;
    }

    public double calculateMarketPrice(ArrayList<Order> bids, ArrayList<Order> asks)
    {
        double bid_price = 0;
        int total_volume = 0;
        for(Order o : bids)
        {
            if(o.price != -1)
            {
                bid_price += o.price * o.volume;
                total_volume = o.volume;
            }
        }

        bid_price /= total_volume;
        double ask_price = 0;
        total_volume = 0;
        for(Order o : asks)
        {
            if(o.price != -1)
            {
                ask_price += o.price * o.volume;
                total_volume += o.volume;
            }
        }
        ask_price /= total_volume;
        return (bid_price + ask_price)/2;
    }

    /*
    Looks for opportunities based on the new RetailState and the state of the book and places orders on them
    */
    //cproctor: You can clean up some of the unused parameters. The name on this is a bit misleading as well. You don't need to pass in as parameters things that are fields, my_orders for example
    //cproctor: Consider breaking up this method as well. It's quite hard to see what the intent is. Pulling out some well named methods can help here!
    public void place_orders(RemoteExchangeView rmt_exch, BookHandler book, RetailState rtl_state)
    {
        List<RetailState.Level> rtl_bids = rtl_state.getBids();
        placeOrdersOnNewBids(rmt_exch, rtl_bids, book);

        List<RetailState.Level> rtl_asks = rtl_state.getAsks();
        placeOrdersOnNewAsks(rmt_exch, rtl_asks, book);
    }

    private void placeOrdersOnNewAsks(RemoteExchangeView rmt_exch, List<RetailState.Level> rtl_asks, BookHandler book)
    {
        for (RetailState.Level o : rtl_asks)
        {
            double price = o.getPrice();
            int volume = o.getVolume();
            if (volume > 0 && price < target - fees - position_offset) // this order is valued higher than what I think its worth and is on the ask side
            {
                long order_id = rmt_exch.createOrder(Symbol.of(BOOK), price, volume, OrderType.IMMEDIATE_OR_CANCEL, Side.BUY);
                // count++; // this is just to limit me to 2 trades for now
                my_orders.put(order_id, new MyOrder(order_id, price, volume, OrderType.IMMEDIATE_OR_CANCEL, Side.BUY));
                my_bids.put(price, volume);
                position--;
                // position_offset = position/10d;
                System.out.println("BOUGHT!! Order price is " + price + " which is less than " + (target - fees - position_offset));
            }
        }
    }

    private void placeOrdersOnNewBids(RemoteExchangeView rmt_exch, List<RetailState.Level> rtl_bids, BookHandler book)
    {
        for (RetailState.Level o : rtl_bids)
        {
            double price = o.getPrice();
            int volume = o.getVolume();
            if (volume > 0 && price > target + fees + position_offset) // this order is valued higer than what I think its worth and is on the bid side
            {
                // count++; // this is just to limit me to 2 trades for now
                long order_id = rmt_exch.createOrder(Symbol.of(BOOK), price, volume, OrderType.IMMEDIATE_OR_CANCEL, Side.SELL);
                // count++;
                my_orders.put(order_id, new MyOrder(order_id, price, volume, OrderType.IMMEDIATE_OR_CANCEL, Side.SELL));
                my_asks.put(price, volume);
                position++;
                // position_offset = position/10d;
                System.out.println("SOLD!!! Order price is " + price + " which is greater than " + (target + fees + position_offset));
            }
        }
    }

    void removeFromCurrentOrders(Side side, double price, int volume, long orderId)
    {
        if(side == Side.BUY)
            my_bids.put(price, my_bids.get(price) - volume);
        else
            my_asks.put(price, my_asks.get(price) - volume);
        my_orders.remove(orderId);
    }
}
