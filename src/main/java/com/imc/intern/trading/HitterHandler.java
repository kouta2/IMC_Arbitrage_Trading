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
    private RemoteExchangeView rmt_exch;

    public HitterHandler(RemoteExchangeView r, String order_book)
    {
        //cproctor: I don't think that you need to initialize this, looking more closely at the rest of your code
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

        strat.create_opportunities(rmt_exch, book, retailState); // bids, asks);

        book.udpate_book(retailState);

        // book.print_book();
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
        strat.getMyOrders().remove(trade.getOrderId());
        if (trade.getSide() == Side.BUY)
        {
            strat.removeFromCurrentOrders(true, trade.getPrice(), trade.getVolume());
        }
        else // SELL
        {
            strat.removeFromCurrentOrders(false, trade.getPrice(), trade.getVolume());
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
