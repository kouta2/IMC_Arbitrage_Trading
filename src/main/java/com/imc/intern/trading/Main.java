package com.imc.intern.trading;

import com.imc.intern.exchange.client.ExchangeClient;
import com.imc.intern.exchange.client.RemoteExchangeView;
import com.imc.intern.exchange.datamodel.Side;
import com.imc.intern.exchange.datamodel.api.*;
import com.imc.intern.exchange.datamodel.api.Error;
import com.imc.intern.exchange.datamodel.jms.ExposureUpdate;
import com.sun.tools.javac.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.print.Book;
import java.lang.reflect.Array;
import java.util.*;

public class Main
{
    private static final String EXCHANGE_URL = "tcp://wintern.imc.com:61616";
    private static final String USERNAME = "akouta";
    private static final String PASSWORD = "meant trip meat wear";
    private static final String BOOK = "AKO1";

    private static final int book_size = 3;
    private static final double target = 20;
    private static final double fees = .1;

    private static int count = 0;

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);


    /*
    Sets up my handlers
     */
    public static void add_handlers(RemoteExchangeView rmt_exch) // , HashMap<Long, MyOrder> my_orders, HashMap<Double, Integer> my_bids, HashMap<Double, Integer> my_asks, HashSet<OwnTrade> transactions, ArrayList<Order> bids, ArrayList<Order> asks)
    {
        rmt_exch.subscribe(Symbol.of(BOOK), new HitterHandler(rmt_exch));
    }

    public static void main(String[] args) throws Exception
    {
        ExchangeClient client = ExchangeClient.create(EXCHANGE_URL, Account.of(USERNAME), PASSWORD);
        RemoteExchangeView rmt_exch = client.getExchangeView();

        add_handlers(rmt_exch); // , order_ids, my_bids, my_asks, transactions, bids, asks);

        int price = 130;
        int volume = 52;
        client.start();
        // client.stop();
    }


}
