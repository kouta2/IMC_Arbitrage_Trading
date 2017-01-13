package com.imc.intern.trading;

import com.imc.intern.exchange.client.RemoteExchangeView;
import com.imc.intern.exchange.datamodel.Side;
import com.imc.intern.exchange.datamodel.api.*;
import com.imc.intern.exchange.datamodel.jms.ExposureUpdate;
import com.imc.intern.exchange.views.ExchangeView;
import org.apache.commons.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by imc on 10/01/2017.
 */
public class HitterHandler implements OrderBookHandler
{
    private ExchangeView rmt_exch;
    private Arbitrage arb;
    private HashMap<Symbol, RetailStateTracker> symbol_to_book;
    private static final Logger LOGGER = LoggerFactory.getLogger(Arbitrage.class);

    private int num_trades;

    // private num_taco_trades;

    public HitterHandler(ExchangeView r, String taco, String beef, String tortilla)
    {
        rmt_exch = r;
        arb = new Arbitrage(new RetailStateTracker(taco), new RetailStateTracker(beef), new RetailStateTracker(tortilla), rmt_exch);
        symbol_to_book = new HashMap<>();
        symbol_to_book.put(Symbol.of(taco), arb.getTacoBook());
        symbol_to_book.put(Symbol.of(beef), arb.getBeefBook());
        symbol_to_book.put(Symbol.of(tortilla), arb.getTortillaBook());
    }

    /*
    called on connection and whenever there are updates
    */
    public void handleRetailState(RetailState retailState)
    {
        Symbol s = retailState.getBook();
//        LOGGER.info("book is: " + s.toString());
//        LOGGER.info("bids " + retailState.getBids().toString());
//        LOGGER.info("asks " + retailState.getAsks().toString() + "\n");

        symbol_to_book.get(s).update_book(retailState);
        arb.executeOrders();
    }

    /*
    called everytime something is changed with any of my exposures
     */
    public void handleExposures(ExposureUpdate exposures)
    {
        // update volume of order
        LOGGER.info(exposures.toString());
    }

    /*
    called everytime something happened to my own trades
     */
    public void handleOwnTrade(OwnTrade trade)
    {

        // arb.removeFromCurrentOrders(trade.getOrderId());
        LOGGER.info(trade.toString());
        // LOGGER.info("Executed a trade!");
        // arb.handleMyOrders(trade);

        // arb.update_actual_pos(trade);
        // arb.placeGTCOrdersWhereNeeded(trade);

        num_trades++;
        LOGGER.info("" + num_trades);
    }

    /*
    called everytime there is a transaction
     */
    public void handleTrade(Trade trade)
    {
        // LOGGER.info(trade.toString());
    }

    /*
    called everytime there is an error
     */
    public void handleError(com.imc.intern.exchange.datamodel.api.Error error)
    {
        // remove a order that was accidentally added when making an order
        // arb.removeFromCurrentOrders(error.getRequestId());
        LOGGER.info(error.toString());

    }
}
