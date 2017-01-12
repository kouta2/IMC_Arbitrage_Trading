package com.imc.intern.trading;

import com.imc.intern.exchange.client.RemoteExchangeView;
import com.imc.intern.exchange.datamodel.Side;
import com.imc.intern.exchange.datamodel.api.OrderType;
import com.imc.intern.exchange.datamodel.api.OwnTrade;
import com.imc.intern.exchange.datamodel.api.RetailState;
import com.imc.intern.exchange.datamodel.api.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.jvm.hotspot.debugger.cdbg.Sym;

import java.util.*;

/**
 * Created by imc on 11/01/2017.
 */
public class Arbitrage
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Arbitrage.class);
    
    private BookHandler taco;
    private BookHandler beef;
    private BookHandler tortilla;

    private double offset = 0;

    private RemoteExchangeView rmt_exch;

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

    /*
    checks if you should sell or buy tacos and if so, you do it
     */
    public void executeOrders()
    {
        // System.out.println("taco book: " + taco.getAsks().toString());
        // System.out.println("beef book: " + beef.toString());
        // System.out.println("tortilla book: " + tortilla.toString());

        executeSellTacoOrders();
        executeBuyTacoOrders();

    }

    /*
    checks if you should sell tacos and if you should, you create the order
     */
    public void executeSellTacoOrders()
    {
        TreeMap<Double,Integer> curr = taco.getBids();
        if(curr.size() == 0)
        {
            // System.out.println("in if statement");
            return;
        }
        Double taco_best_bid_price = curr.firstKey();
        Integer taco_best_bid_volume = 0;
        if(taco_best_bid_price != null)
            taco_best_bid_volume = curr.get(taco_best_bid_price);

        curr = beef.getAsks();
        if(curr.size() == 0)
            return;
        Double beef_best_ask_price = curr.firstKey();
        Integer beef_best_ask_volume = 0;
        if(beef_best_ask_price != null)
            beef_best_ask_volume = curr.get(beef_best_ask_price);

        curr = tortilla.getAsks();
        if(curr.size() == 0)
            return;
        System.out.println("sell");
        Double tortilla_best_ask_price = curr.firstKey();
        Integer tortilla_best_ask_volume = 0;
        if(tortilla_best_ask_price != null)
            tortilla_best_ask_volume = curr.get(tortilla_best_ask_price);

        if(taco_best_bid_price != null && beef_best_ask_price != null && tortilla_best_ask_price != null)
        {
            LOGGER.info("taco bid: " + taco_best_bid_price + " should be > beef ask + tortilla ask " + (beef_best_ask_price + tortilla_best_ask_price));
            placeSellOrdersOnTaco(taco_best_bid_price, taco_best_bid_volume, beef_best_ask_price, beef_best_ask_volume, tortilla_best_ask_price, tortilla_best_ask_volume);
        }
    }

    /*
    checks if you should buy tacos and if you should, you create the order
     */
    public void executeBuyTacoOrders()
    {
        TreeMap<Double, Integer> curr = taco.getAsks();
        if(curr.size() == 0)
            return;
        Double taco_best_ask_price = curr.firstKey();
        Integer taco_best_ask_volume = 0;
        if(taco_best_ask_price != null)
            taco_best_ask_volume = curr.get(taco_best_ask_price);

        curr = beef.getBids();
        if(curr.size() == 0)
            return;
        Double beef_best_bid_price = curr.firstKey();
        Integer beef_best_bid_volume = 0;
        if(beef_best_bid_price != null)
            beef_best_bid_volume = curr.get(beef_best_bid_price);

        curr = tortilla.getBids();
        if(curr.size() == 0)
            return;
        Double tortilla_best_bid_price = curr.firstKey();
        Integer tortilla_best_bid_volume = 0;
        if(tortilla_best_bid_price != null)
            tortilla_best_bid_volume = curr.get(tortilla_best_bid_price);

        System.out.println("buy");
        if(taco_best_ask_price != null && beef_best_bid_price != null && tortilla_best_bid_price != null)
        {
            LOGGER.info("taco ask: " + taco_best_ask_price + " should be < beef bid + tortilla bid " + (beef_best_bid_price + tortilla_best_bid_price));
            placeBuyOrdersOnTaco(taco_best_ask_price, taco_best_ask_volume, beef_best_bid_price, beef_best_bid_volume, tortilla_best_bid_price, tortilla_best_bid_volume);
        }

    }

    /*
    places sell orders on tacos
     */
    public void placeSellOrdersOnTaco(double taco_best_bid_price, int taco_best_bid_volume, double beef_best_ask_price, int beef_best_ask_volume, double tortilla_best_ask_price, int tortilla_best_ask_volume)
    {
        if(taco_best_bid_price - offset > beef_best_ask_price + tortilla_best_ask_price)
        {
            int min_volume = Math.min(taco_best_bid_volume, Math.min(beef_best_ask_volume, tortilla_best_ask_volume));
            if(min_volume <= 0)
                return;
            long order_id = rmt_exch.createOrder(Symbol.of(taco.getBookName()), taco_best_bid_price, min_volume, OrderType.IMMEDIATE_OR_CANCEL, Side.SELL);
            my_orders.put(order_id, new MyOrder(order_id, taco_best_bid_price, min_volume, OrderType.IMMEDIATE_OR_CANCEL, Side.SELL));
            if(my_asks.containsKey(taco_best_bid_price))
                my_asks.put(taco_best_bid_price, my_asks.get(taco_best_bid_price) + min_volume);
            else
                my_asks.put(taco_best_bid_price, min_volume);
            LOGGER.info("SOLD!!! because I can sell taco at " + taco_best_bid_price + " and buy back beef and tortilla at " + (beef_best_ask_price + tortilla_best_ask_price));
        }
    }

    /*
    places buy orders on tacos
     */
    public void placeBuyOrdersOnTaco(double taco_best_ask_price, int taco_best_ask_volume, double beef_best_bid_price, int beef_best_bid_volume, double tortilla_best_bid_price, int tortilla_best_bid_volume)
    {
        if(taco_best_ask_price + offset < beef_best_bid_price + tortilla_best_bid_price)
        {
            int min_volume = Math.min(taco_best_ask_volume, Math.min(beef_best_bid_volume, tortilla_best_bid_volume));
            if(min_volume <= 0)
                return;
            long order_id = rmt_exch.createOrder(Symbol.of(taco.getBookName()), taco_best_ask_price, min_volume, OrderType.IMMEDIATE_OR_CANCEL, Side.BUY);
            my_orders.put(order_id, new MyOrder(order_id, taco_best_ask_price, min_volume, OrderType.IMMEDIATE_OR_CANCEL, Side.BUY));
            if(my_bids.containsKey(taco_best_ask_price))
                my_bids.put(taco_best_ask_price, my_bids.get(taco_best_ask_price) + min_volume);
            else
                my_bids.put(taco_best_ask_price, min_volume);
            LOGGER.info("BOUGHT!!! I can buy taco at " + taco_best_ask_price + " and sell back beef and tortilla at " + (beef_best_bid_price + tortilla_best_bid_price));
        }
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

    public void handleMyOrders(OwnTrade trade)
    {
        long order_id = trade.getOrderId();
        int trade_volume = trade.getVolume();
        MyOrder o = my_orders.get(order_id);
        if(o.volume == trade_volume)
            removeFromCurrentOrders(order_id);
        else
        {
            o.volume -= trade_volume;
            my_orders.put(order_id, o);
            if(trade.getSide() == Side.BUY)
            {
                double price = trade.getPrice();
                my_bids.put(price, my_bids.get(price) - trade_volume);
            }
        }
    }

    public Symbol getTacoBookSymbol()
    {
        return Symbol.of(taco.getBookName());
    }

    public Symbol getBeefBookSymbol()
    {
        return Symbol.of(beef.getBookName());
    }

    public Symbol getTortillaBookSymbol()
    {
        return Symbol.of(tortilla.getBookName());
    }

    public BookHandler getTacoBook()
    {
        return taco;
    }

    public BookHandler getBeefBook()
    {
        return beef;
    }

    public BookHandler getTortillaBook()
    {
        return tortilla;
    }
}