package com.imc.intern.trading;

import com.imc.intern.exchange.client.RemoteExchangeView;
import com.imc.intern.exchange.datamodel.Side;
import com.imc.intern.exchange.datamodel.api.*;
import com.imc.intern.exchange.datamodel.jms.ExposureUpdate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by imc on 10/01/2017.
 */
public class HitterHandler implements OrderBookHandler
{
    private BookHandler book;
    private StrategyHandler strat;

    HashMap<Long, MyOrder> my_orders = new HashMap<>(); // maps my orders to the actual order object
    HashMap<Double, Integer> my_bids = new HashMap<>(); // maps my bid prices to the total volume of my bids at that price
    HashMap<Double, Integer> my_asks = new HashMap<>(); // maps my ask prices to the total volume of my asks at that price
    HashSet<OwnTrade> transactions = new HashSet<>(); // keeps track of my transaction history
    ArrayList<Order> bids = new ArrayList<>(); // keeps track of the bids in the book
    ArrayList<Order> asks = new ArrayList<>(); // keeps track of the asks in the book

    RemoteExchangeView rmt_exch;

    public HitterHandler(RemoteExchangeView r, String order_book)
    {
        //cproctor: I don't think that you need to initialize this, looking more closely at the rest of your code
        for(int i = 0; i < 3; i++)
        {
            bids.add(new Order(-1, 0));
            asks.add(new Order(-1, 0));
        }
        rmt_exch = r;
        book = new BookHandler(order_book);
        strat = new StrategyHandler(order_book);
    }

    /*
    called every 10 seconds and when ever the book changes
    */
    public void handleRetailState(RetailState retailState)
    {
        List<RetailState.Level> curr_bids = retailState.getBids();
        List<RetailState.Level> curr_asks = retailState.getAsks();

        strat.create_opportunities(rmt_exch, bids, asks, retailState, my_orders, my_bids, my_asks); // bids, asks);

        book.udpate_book(bids, asks, curr_bids, curr_asks);

        // book.print_book(bids, asks);
    }

    /*
    called everytime something is changed with any of my exposures
     */
    public void handleExposures(ExposureUpdate exposures)
    {
        // update volume of order
    }

    /*
    called everytime something happened to my own trades
     */
    public void handleOwnTrade(OwnTrade trade)
    {
        System.out.println("Executed a trade!");
        // transactions.add(trade);
        my_orders.remove(trade.getOrderId());
        if (trade.getSide() == Side.BUY)
        {
            my_bids.put(trade.getPrice(), my_bids.get(trade.getPrice()) - trade.getVolume());
        } else // SELL
        {
            my_asks.put(trade.getPrice(), my_asks.get(trade.getPrice()) - trade.getVolume());
        }
    }

    /*
    called everytime there is a transaction
     */
    public void handleTrade(Trade trade)
    {

    }

    /*
    called everytime there is an error
     */
    public void handleError(com.imc.intern.exchange.datamodel.api.Error error)
    {

    }
}
