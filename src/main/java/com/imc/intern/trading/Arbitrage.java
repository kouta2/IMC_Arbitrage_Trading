package com.imc.intern.trading;

import com.google.common.util.concurrent.RateLimiter;
import com.imc.intern.exchange.client.RemoteExchangeView;
import com.imc.intern.exchange.datamodel.Side;
import com.imc.intern.exchange.datamodel.api.OrderType;
import com.imc.intern.exchange.datamodel.api.OwnTrade;
import com.imc.intern.exchange.datamodel.api.RetailState;
import com.imc.intern.exchange.datamodel.api.Symbol;
import com.imc.intern.exchange.views.ExchangeView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.jvm.hotspot.debugger.cdbg.Sym;
import sun.reflect.generics.tree.Tree;

import java.util.*;

/**
 * Created by imc on 11/01/2017.
 */
public class Arbitrage
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Arbitrage.class);
    private RateLimiter ioc_limiter =  RateLimiter.create(1);

    private BookHandler taco;
    private BookHandler beef;
    private BookHandler tortilla;

    private double offset = 0;

    private ExchangeView rmt_exch;
    private OrderPlacer order_placer;

    private HashMap<Long, MyOrder> my_orders = new HashMap<>(); // maps my orders to the actual order object
    private HashMap<Double, Integer> my_bids = new HashMap<>(); // maps my bid prices to the total volume of my bids at that price
    private HashMap<Double, Integer> my_asks = new HashMap<>(); // maps my ask prices to the total volume of my asks at that price
    private HashSet<OwnTrade> transactions = new HashSet<>(); // keeps track of my transaction history

    private int num_taco_trades = 0;
    private int num_beef_trades = 0;
    private int num_tort_trades = 0;

    private int actual_num_taco_trades = 0;
    private int actual_num_beef_trades = 0;
    private int actual_num_tort_trades = 0;

    Arbitrage (BookHandler ta, BookHandler b, BookHandler to, ExchangeView r)
    {
        taco = ta;
        beef = b;
        tortilla = to;
        rmt_exch = r;
        order_placer = new OrderPlacer(rmt_exch);
    }

    /*
    checks if you should sell or buy tacos and if so, you do it
     */
    public void executeOrders()
    {
        // LOGGER.info("taco book: " + taco.getAsks().toString());
        // LOGGER.info("beef book: " + beef.toString());
        // LOGGER.info("tortilla book: " + tortilla.toString());
        executeSellTacoOrders();
        executeBuyTacoOrders();

    }

    // NAJ: there is quite a bit duplicate code between sellTaco and buyTaco, I'd work on a generic method for both
    /*
    checks if you should sell tacos and if you should, you create the order
     */
    public void executeSellTacoOrders()
    {
        TreeMap<Double,Integer> curr = taco.getBids();
        if(curr.size() == 0)
        {
            //LOGGER.info("in if statement");
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
        // LOGGER.info("sell");
        Double tortilla_best_ask_price = curr.firstKey();
        Integer tortilla_best_ask_volume = 0;
        if(tortilla_best_ask_price != null)
            tortilla_best_ask_volume = curr.get(tortilla_best_ask_price);

        if(taco_best_bid_price != null && beef_best_ask_price != null && tortilla_best_ask_price != null)
        {
            // LOGGER.info("taco bid: " + taco_best_bid_price + " should be > beef ask + tortilla ask " + (beef_best_ask_price + tortilla_best_ask_price));
            sellTacoBuyOthers(taco_best_bid_price, taco_best_bid_volume, beef_best_ask_price, beef_best_ask_volume, tortilla_best_ask_price, tortilla_best_ask_volume);
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

        // LOGGER.info("buy");
        if(taco_best_ask_price != null && beef_best_bid_price != null && tortilla_best_bid_price != null)
        {
            // LOGGER.info("taco ask: " + taco_best_ask_price + " should be < beef bid + tortilla bid " + (beef_best_bid_price + tortilla_best_bid_price));
            buyTacoSellOthers(taco_best_ask_price, taco_best_ask_volume, beef_best_bid_price, beef_best_bid_volume, tortilla_best_bid_price, tortilla_best_bid_volume);
        }

    }

    /*
    places sell orders on tacos
     */
    private void sellTacoBuyOthers(double taco_best_bid_price, int taco_best_bid_volume, double beef_best_ask_price, int beef_best_ask_volume, double tortilla_best_ask_price, int tortilla_best_ask_volume)
    {
        if(taco_best_bid_price - offset > beef_best_ask_price + tortilla_best_ask_price)
        {
            int min_volume = calculateVolume(taco_best_bid_volume, beef_best_ask_volume, tortilla_best_ask_volume);
            if(min_volume <= 0)
                return;
            order_placer.placeTheOrder(Symbol.of(taco.getBookName()), taco_best_bid_price, min_volume, OrderType.IMMEDIATE_OR_CANCEL, Side.SELL);
            order_placer.placeTheOrder(Symbol.of(beef.getBookName()), beef_best_ask_price, min_volume, OrderType.IMMEDIATE_OR_CANCEL, Side.BUY);
            order_placer.placeTheOrder(Symbol.of(tortilla.getBookName()), tortilla_best_ask_price, min_volume, OrderType.IMMEDIATE_OR_CANCEL, Side.BUY);
            num_taco_trades -= min_volume;
            num_beef_trades += min_volume;
            num_tort_trades += min_volume;
            ioc_limiter.acquire();
            // ioc_limiter.
            LOGGER.info("SOLD!!! because I can sell taco at " + taco_best_bid_price + " and buy back beef and tortilla at " + (beef_best_ask_price + tortilla_best_ask_price));
        }
    }

    /*
    places buy orders on tacos
     */
    private void buyTacoSellOthers(double taco_best_ask_price, int taco_best_ask_volume, double beef_best_bid_price, int beef_best_bid_volume, double tortilla_best_bid_price, int tortilla_best_bid_volume)
    {
        if(taco_best_ask_price + offset < beef_best_bid_price + tortilla_best_bid_price)
        {
            int min_volume = calculateVolume(taco_best_ask_volume, beef_best_bid_volume, tortilla_best_bid_volume);
            if(min_volume <= 0)
                return;
            order_placer.placeTheOrder(Symbol.of(taco.getBookName()), taco_best_ask_price, min_volume, OrderType.IMMEDIATE_OR_CANCEL, Side.BUY);
            order_placer.placeTheOrder(Symbol.of(beef.getBookName()), beef_best_bid_price, min_volume, OrderType.IMMEDIATE_OR_CANCEL, Side.SELL);
            order_placer.placeTheOrder(Symbol.of(tortilla.getBookName()), tortilla_best_bid_price, min_volume, OrderType.IMMEDIATE_OR_CANCEL, Side.SELL);
            num_taco_trades += min_volume;
            num_beef_trades -= min_volume;
            num_tort_trades -= min_volume;
            ioc_limiter.acquire();
            LOGGER.info("BOUGHT!!! I can buy taco at " + taco_best_ask_price + " and sell back beef and tortilla at " + (beef_best_bid_price + tortilla_best_bid_price));
        }
    }

    public void update_actual_pos(OwnTrade trade)
    {
        String book = trade.getBook().toString();
        Side s = trade.getSide();
        int volume = trade.getVolume();
        if(book.equals(taco.getBookName()))
            actual_num_taco_trades = s == Side.BUY ? actual_num_taco_trades + volume : actual_num_taco_trades - volume;
        else if(book.equals(beef.getBookName()))
            actual_num_beef_trades = s == Side.BUY ? actual_num_beef_trades + volume : actual_num_beef_trades - volume;
        else
            actual_num_tort_trades = s == Side.BUY ? actual_num_tort_trades + volume : actual_num_tort_trades - volume;
    }

    private int calculateVolume(int taco_v, int beef_v, int tort_v)
    {
        return Math.min(10, Math.min(taco_v, Math.min(beef_v, tort_v)));
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

    // NAJ: I would test this method to make sure its doing what you think its doing
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

    public void GoodTilCancelOrder(Symbol s, Side side, double p, int v)
    {
        order_placer.placeTheOrder(s, p, v, OrderType.GOOD_TIL_CANCEL, side);
    }

    public void placeGTCOrdersWhereNeeded(OwnTrade trade)
    {
        String book = trade.getBook().toString();
        OrderType o = OrderType.GOOD_TIL_CANCEL;
        if(book.equals(taco.getBookName()) && num_taco_trades != actual_num_taco_trades)
        {
            Symbol s = getTacoBookSymbol();
            if(actual_num_taco_trades < num_taco_trades)
            {
                order_placer.placeTheOrder(s, getPrice(taco, Side.SELL), num_taco_trades - actual_num_taco_trades, o, Side.BUY);
            }
            else
            {
                order_placer.placeTheOrder(s, getPrice(taco, Side.BUY), actual_num_taco_trades - num_taco_trades, o, Side.SELL);
            }
            actual_num_taco_trades = num_taco_trades;
        }
        else if(book.equals(beef.getBookName()) && num_beef_trades != actual_num_beef_trades)
        {
            Symbol s = getBeefBookSymbol();
            if(actual_num_beef_trades < num_beef_trades)
            {
                order_placer.placeTheOrder(s, getPrice(beef, Side.SELL), num_beef_trades - actual_num_beef_trades, o, Side.BUY);
            }
            else
            {
                order_placer.placeTheOrder(s, getPrice(beef, Side.BUY), actual_num_beef_trades - num_beef_trades, o, Side.SELL);
            }
            actual_num_beef_trades = num_beef_trades;
        }
        else if(book.equals(tortilla.getBookName()) && num_tort_trades != actual_num_tort_trades)
        {
            Symbol s = getTortillaBookSymbol();
            if(actual_num_tort_trades < num_tort_trades)
            {
                order_placer.placeTheOrder(s, getPrice(tortilla, Side.SELL), num_tort_trades - actual_num_tort_trades, o, Side.BUY);
            }
            else
            {
                order_placer.placeTheOrder(s, getPrice(tortilla, Side.BUY), actual_num_tort_trades - num_tort_trades, o, Side.SELL);
            }
            actual_num_tort_trades = num_tort_trades;
        }
    }

    private double getPrice(BookHandler b, Side side)
    {
        if(side == Side.BUY)
        {
            return b.getBids().firstKey() + .05;
        }
        else
        {
            return b.getAsks().firstKey() - .05;
        }
    }
}