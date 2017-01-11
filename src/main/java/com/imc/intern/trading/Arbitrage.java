package com.imc.intern.trading;

import com.imc.intern.exchange.client.RemoteExchangeView;
import com.imc.intern.exchange.datamodel.Side;
import com.imc.intern.exchange.datamodel.api.OrderType;
import com.imc.intern.exchange.datamodel.api.OwnTrade;
import com.imc.intern.exchange.datamodel.api.RetailState;
import com.imc.intern.exchange.datamodel.api.Symbol;
import sun.jvm.hotspot.debugger.cdbg.Sym;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by imc on 11/01/2017.
 */
public class Arbitrage
{
    private BookHandler taco;
    private BookHandler beef;
    private BookHandler tortilla;

    private double offset = 0;
    // private StrategyHandler strat;

    private RemoteExchangeView rmt_exch;
    // private TacoStrategy taco_strat;
    // private BeefStrategy beef_strat;
    // private TortillaStrategy tortilla_strat;

    private HashMap<Long, MyOrder> my_orders = new HashMap<>(); // maps my orders to the actual order object
    private HashMap<Double, Integer> my_bids = new HashMap<>(); // maps my bid prices to the total volume of my bids at that price
    private HashMap<Double, Integer> my_asks = new HashMap<>(); // maps my ask prices to the total volume of my asks at that price
    private HashSet<OwnTrade> transactions = new HashSet<>(); // keeps track of my transaction history

    Arbitrage (BookHandler ta, BookHandler b, BookHandler to, RemoteExchangeView r)
    {
        taco = ta;
        beef = b;
        tortilla = to;
        rmt_exch = r;
    }

    public void executeOrders()
    {
        Order curr = taco.getBids().get(0);
        double taco_best_bid_price = curr.price;
        int taco_best_bid_volume = curr.volume;

        curr = taco.getAsks().get(0);
        double taco_best_ask_price = curr.price;
        int taco_best_ask_volume = curr.volume;

        curr = beef.getBids().get(0);
        double beef_best_bid_price = curr.price;
        int beef_best_bid_volume = curr.volume;

        curr = beef.getAsks().get(0);
        double beef_best_ask_price = curr.price;
        int beef_best_ask_volume = curr.volume;

        curr = taco.getBids().get(0);
        double tortilla_best_bid_price = curr.price;
        int tortilla_best_bid_volume = curr.volume;

        curr = tortilla.getAsks().get(0);
        double tortilla_best_ask_price = curr.price;
        int tortilla_best_ask_volume = curr.volume;

        System.out.println("taco bid: " + taco_best_bid_price + " should be > beef ask + tortilla ask " + (beef_best_ask_price + tortilla_best_ask_price));

        System.out.println("taco ask: " + taco_best_ask_price + " should be < beef bid + tortilla bid " + (beef_best_bid_price + tortilla_best_bid_price));

        placeSellOrdersOnTaco(taco_best_bid_price, taco_best_bid_volume, beef_best_ask_price, beef_best_ask_volume, tortilla_best_ask_price, tortilla_best_ask_volume);

        placeBuyOrdersOnTaco(taco_best_ask_price, taco_best_ask_volume, beef_best_bid_price, beef_best_bid_volume, tortilla_best_bid_price, tortilla_best_bid_volume);
    }

    public void placeSellOrdersOnTaco(double taco_best_bid_price, int taco_best_bid_volume, double beef_best_ask_price, int beef_best_ask_volume, double tortilla_best_ask_price, int tortilla_best_ask_volume)
    {
        if(taco_best_bid_price > -1 && beef_best_ask_price > -1 && tortilla_best_ask_price > -1)
        {
            if(taco_best_bid_price - offset > beef_best_ask_price + tortilla_best_ask_price)
            {
                int min_volume = Math.min(taco_best_bid_volume, Math.min(beef_best_ask_volume, tortilla_best_ask_volume));
                long order_id = rmt_exch.createOrder(Symbol.of(taco.getBookName()), taco_best_bid_price, min_volume, OrderType.IMMEDIATE_OR_CANCEL, Side.SELL);
                my_orders.put(order_id, new MyOrder(order_id, taco_best_bid_price, min_volume, OrderType.IMMEDIATE_OR_CANCEL, Side.SELL));
                my_bids.put(taco_best_bid_price, min_volume);
                System.out.println("SOLD!!! because I can sell taco at " + taco_best_bid_price + " and buy back beef and tortilla at " + (beef_best_ask_price + tortilla_best_ask_price));
            }
        }
    }

    public void placeBuyOrdersOnTaco(double taco_best_ask_price, int taco_best_ask_volume, double beef_best_bid_price, int beef_best_bid_volume, double tortilla_best_bid_price, int tortilla_best_bid_volume)
    {
        if(taco_best_ask_price > -1 && beef_best_bid_price > -1 && tortilla_best_bid_price > -1)
        {
            if(taco_best_ask_price + offset < beef_best_bid_price + tortilla_best_bid_price)
            {
                int min_volume = Math.min(taco_best_ask_volume, Math.min(beef_best_bid_volume, tortilla_best_bid_volume));
                long order_id = rmt_exch.createOrder(Symbol.of(taco.getBookName()), taco_best_ask_price, min_volume, OrderType.IMMEDIATE_OR_CANCEL, Side.BUY);
                my_orders.put(order_id, new MyOrder(order_id, taco_best_ask_price, min_volume, OrderType.IMMEDIATE_OR_CANCEL, Side.BUY));
                my_bids.put(taco_best_ask_price, min_volume);
                System.out.println("BOUGHT!!! I can buy taco at " + taco_best_ask_price + " and sell back beef and tortilla at " + (beef_best_bid_price + tortilla_best_bid_price));
            }
        }

    }

    public void executeTaco(RetailState rtl_state)
    {
        taco.udpate_book(rtl_state);
        executeOrders();
    }

    public void executeBeef(RetailState rtl_state)
    {
        beef.udpate_book(rtl_state);
        executeOrders();
    }

    public void executeTortilla(RetailState rtl_state)
    {
        tortilla.udpate_book(rtl_state);
        executeOrders();
    }

    void removeFromCurrentOrders(long orderId)
    {
        MyOrder o = my_orders.get(orderId);
        if(o.side == Side.BUY)
            my_bids.put(o.price, my_bids.get(o.price) - o.volume);
        else
            my_asks.put(o.price, my_asks.get(o.price) - o.volume);
        my_orders.remove(orderId);
    }

    public Symbol getTacoBook()
    {
        return Symbol.of(taco.getBookName());
    }

    public Symbol getBeefBook()
    {
        return Symbol.of(beef.getBookName());
    }

    public Symbol getTortillaBook()
    {
        return Symbol.of(tortilla.getBookName());
    }

}