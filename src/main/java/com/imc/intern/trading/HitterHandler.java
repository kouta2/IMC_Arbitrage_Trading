package com.imc.intern.trading;

import com.imc.intern.exchange.client.RemoteExchangeView;
import com.imc.intern.exchange.datamodel.Side;
import com.imc.intern.exchange.datamodel.api.*;
import com.imc.intern.exchange.datamodel.jms.ExposureUpdate;

import java.util.*;

/**
 * Created by imc on 10/01/2017.
 */
public class HitterHandler implements OrderBookHandler
{
    private RemoteExchangeView rmt_exch;
    private Arbitrage arb;
    HashMap<Symbol, BookHandler> symbol_to_book;


    public HitterHandler(RemoteExchangeView r, String taco, String beef, String tortilla)// String order_book, StrategyHandler pattern)
    {
        rmt_exch = r;
        arb = new Arbitrage(new BookHandler(taco), new BookHandler(beef), new BookHandler(tortilla), rmt_exch);
        symbol_to_book = new HashMap<>();
        symbol_to_book.put(Symbol.of(taco), arb.getTacoBook());
        symbol_to_book.put(Symbol.of(beef), arb.getBeefBook());
        symbol_to_book.put(Symbol.of(tortilla), arb.getTortillaBook());
    }

    /*
    called every 10 seconds and when ever the book changes
    */
    public void handleRetailState(RetailState retailState)
    {
        Symbol s = retailState.getBook();
//        System.out.println("book is: " + s.toString());
//        System.out.println("bids " + retailState.getBids().toString());
//        System.out.println("asks " + retailState.getAsks().toString() + "\n");

        symbol_to_book.get(s).update_book(retailState);
        arb.executeOrders();
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
        // arb.removeFromCurrentOrders(trade.getOrderId());
        // System.out.println("Executed a trade!");
        arb.handleMyOrders(trade);
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
        // remove a order that was accidentally added when making an order
        arb.removeFromCurrentOrders(error.getRequestId());
    }
}
