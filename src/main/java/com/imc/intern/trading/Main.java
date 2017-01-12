package com.imc.intern.trading;

import com.imc.intern.exchange.client.ExchangeClient;
import com.imc.intern.exchange.client.RemoteExchangeView;
import com.imc.intern.exchange.datamodel.Side;
import com.imc.intern.exchange.datamodel.api.*;
import com.imc.intern.exchange.datamodel.api.Error;
import com.imc.intern.exchange.datamodel.jms.ExposureUpdate;
import com.imc.intern.exchange.views.ExchangeView;
import com.sun.tools.javac.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.print.Book;
import java.lang.reflect.Array;
import java.util.*;

public class Main
{
    // private static final String EXCHANGE_URL = "tcp://wintern.imc.com:61616";
    private static final String EXCHANGE_URL = "tcp://54.227.125.23:61616";
    private static final String USERNAME = "akouta";
    private static final String PASSWORD = "meant trip meat wear";
    private static final String BOOK = "AKO1";

    // private static final String TACO = "AKO.TACO";
    // private static final String BEEF = "AKO.BEEF";
    // private static final String TORTILLA = "AKO.TORT";

    private static final String TACO = "TACO";
    private static final String BEEF = "BEEF";
    private static final String TORTILLA = "TORT";

    private final int book_size = 3;
    private final double target = 20;
    private final double fees = .1;

    private int count = 0;

    private final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    /*
    Sets up my handlers
     */
    public static void add_handlers(ExchangeView rmt_exch)
    {
        HitterHandler handler = new HitterHandler(rmt_exch, TACO, BEEF, TORTILLA);
        rmt_exch.subscribe(Symbol.of(TACO), handler);
        rmt_exch.subscribe(Symbol.of(BEEF), handler);
        rmt_exch.subscribe(Symbol.of(TORTILLA), handler);
    }

    public static void main(String[] args) throws Exception
    {
        ExchangeClient client = ExchangeClient.create(EXCHANGE_URL, Account.of(USERNAME), PASSWORD);
        ExchangeView rmt_exch = client.getExchangeView();

        client.start();
        add_handlers(rmt_exch);
        // client.stop();
    }
}
