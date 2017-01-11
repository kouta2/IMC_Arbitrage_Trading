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
    private RemoteExchangeView rmt_exch;
    private Arbitrage arb;

    public HitterHandler(RemoteExchangeView r, Arbitrage a)// String order_book, StrategyHandler pattern)
    {
        rmt_exch = r;
        arb = a;
    }

    /*
    called every 10 seconds and when ever the book changes
    */
    public void handleRetailState(RetailState retailState)
    {
        System.out.println("book is: " + retailState.getBook().toString());
        System.out.println("bids " + retailState.getBids().toString());
        System.out.println("asks " + retailState.getAsks().toString() + "\n");
        Symbol s = retailState.getBook();
        if(s.toString().equals(arb.getTacoBook().toString()))
            arb.executeTaco(retailState);
        else if(s.toString().equals(arb.getBeefBook().toString()))
            arb.executeBeef(retailState);
        else
            arb.executeTortilla(retailState);
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
        arb.removeFromCurrentOrders(trade.getOrderId());
        // System.out.println("Executed a trade!");
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
