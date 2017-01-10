package com.imc.intern.trading;

import com.imc.intern.exchange.client.RemoteExchangeView;
import com.imc.intern.exchange.datamodel.Side;
import com.imc.intern.exchange.datamodel.api.OrderType;
import com.imc.intern.exchange.datamodel.api.RetailState;
import com.imc.intern.exchange.datamodel.api.Symbol;

import java.util.ArrayList;
import java.util.HashMap;
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

    private static int count = 0;

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
    Looks for opportunities based on the new RetailState
    */
    public void create_opportunities(RemoteExchangeView rmt_exch, ArrayList<Order> bids, ArrayList<Order> asks, RetailState rtl_state, HashMap<Long, MyOrder> my_orders, HashMap<Double, Integer> my_bids, HashMap<Double, Integer> my_asks)
    {
        List<RetailState.Level> rtl_bids = rtl_state.getBids();
        for (RetailState.Level o : rtl_bids)
        {
            double price = o.getPrice();
            int volume = o.getVolume();
            if (volume > 0 && price > target + fees + position_offset) // this order is valued higer than what I think its worth and is on the bid side
            {
                // count++; // this is just to limit me to 2 trades for now
                long order_id = rmt_exch.createOrder(Symbol.of(BOOK), price, volume, OrderType.IMMEDIATE_OR_CANCEL, Side.SELL);
                if (order_id > -1)
                {
                    // count++;
                    my_orders.put(order_id, new MyOrder(order_id, price, volume, OrderType.GOOD_TIL_CANCEL.IMMEDIATE_OR_CANCEL, Side.SELL));
                    my_asks.put(price, volume);
                    position++;
                    // position_offset = position/10d;
                    System.out.println("SOLD!!! Order price is " + price + " which is greater than " + (target + fees + position_offset));
                }
            }
        }
        List<RetailState.Level> rtl_asks = rtl_state.getAsks();
        for (RetailState.Level o : rtl_asks)
        {
            double price = o.getPrice();
            int volume = o.getVolume();
            if (volume > 0 && price < target - fees - position_offset) // this order is valued higher than what I think its worth and is on the ask side
            {
                long order_id = rmt_exch.createOrder(Symbol.of(BOOK), price, volume, OrderType.IMMEDIATE_OR_CANCEL, Side.BUY);
                if (order_id > -1)
                {
                    // count++; // this is just to limit me to 2 trades for now
                    my_orders.put(order_id, new MyOrder(order_id, price, volume, OrderType.IMMEDIATE_OR_CANCEL, Side.BUY));
                    my_bids.put(price, volume);
                    position--;
                    // position_offset = position/10d;
                    System.out.println("BOUGHT!! Order price is " + price + " which is less than " + (target - fees - position_offset));
                }
            }
        }
    }
}
