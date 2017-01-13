package com.imc.intern.trading;

import com.google.common.util.concurrent.RateLimiter;
import com.imc.intern.exchange.datamodel.Side;
import com.imc.intern.exchange.datamodel.api.OrderType;
import com.imc.intern.exchange.datamodel.api.OwnTrade;
import com.imc.intern.exchange.datamodel.api.RetailState;
import com.imc.intern.exchange.datamodel.api.Symbol;
import com.imc.intern.exchange.views.ExchangeView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.tree.Tree;

import java.util.*;

/**
 * Created by imc on 11/01/2017.
 */
// MWANG: THis looks mostly good, but as Naj mentioned has a lot of duplication and unused code - it makes it difficult
// to tell if you have any bugs. Pulling out and reusing methods will help improve clarity, which makes it easier for
// us to to read but also for you to tell if the code is correct.
public class Arbitrage
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Arbitrage.class);
    private RateLimiter ioc_limiter =  RateLimiter.create(1);

    private RetailStateTracker taco;
    private RetailStateTracker beef;
    private RetailStateTracker tortilla;

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

    private int num_orders_allowed = 21;

    private long time_of_last_trade = 0;

    private ArrayList<Long> taco_orders;
    private ArrayList<Long> beef_orders;
    private ArrayList<Long> tort_orders;

    private long wait_time = 11000;

    Arbitrage (RetailStateTracker ta, RetailStateTracker b, RetailStateTracker to, ExchangeView r)
    {
        taco = ta;
        beef = b;
        tortilla = to;
        rmt_exch = r;
        order_placer = new OrderPlacer(rmt_exch);
        ioc_limiter.setRate(1.0/35.0);
        // ioc_limiter.s;
    }

    /*
    checks if you should sell or buy tacos and if so, you do it
     */
    public void executeOrders()
    {
        // LOGGER.info("taco book: " + taco.getAsks().toString());
        // LOGGER.info("beef book: " + beef.toString());
        // LOGGER.info("tortilla book: " + tortilla.toString());
        long curr_time = System.currentTimeMillis();
        if(curr_time - time_of_last_trade < wait_time)
        {
            return;
        }
        // if(executeSellTacoOrders() == true)
        //     return;
        // executeBuyTacoOrders();
        /*
        if(executeOrdersHelper(taco.getBids(), beef.getAsks(), tortilla.getAsks(), Side.SELL) == true)
            return;
        executeOrdersHelper(taco.getAsks(), beef.getBids(), tortilla.getBids(), Side.BUY);
        */
        if(executeOrdersHelper(taco.getAsks(), beef.getBids(), tortilla.getBids(), Side.BUY) == true)
            return;
        executeOrdersHelper(taco.getBids(), beef.getAsks(), tortilla.getAsks(), Side.SELL);

    }

    public void placeGTCOrdersWhereNeeded(OwnTrade trade)
    {
        String book = trade.getBook().toString();
        OrderType o = OrderType.GOOD_TIL_CANCEL;
        int volume = 0;
        if(book.equals(taco.getBookName()) && num_taco_trades != actual_num_taco_trades)
        {
            volume = Math.min(num_orders_allowed, Math.abs(num_taco_trades - actual_num_taco_trades));
            LOGGER.info("making a GTC taco order with at a price of " + trade.getPrice() + " and a volume of " + volume);
            Symbol s = getTacoBookSymbol();
            if(actual_num_taco_trades < num_taco_trades)
            {
                order_placer.placeTheOrder(s, trade.getPrice() + .05, volume, o, Side.BUY);
                actual_num_taco_trades += volume;
            }
            else
            {
                order_placer.placeTheOrder(s, trade.getPrice() - .05, volume, o, Side.SELL);
                actual_num_taco_trades -= volume;
            }
            // actual_num_taco_trades = num_taco_trades;
        }
        else if(book.equals(beef.getBookName()) && num_beef_trades != actual_num_beef_trades)
        {
            volume = Math.min(num_orders_allowed, Math.abs(num_beef_trades - actual_num_beef_trades));
            LOGGER.info("making a GTC beef order with at a price of " + trade.getPrice() + " and a volume of " + volume);
            Symbol s = getBeefBookSymbol();
            if(actual_num_beef_trades < num_beef_trades)
            {
                order_placer.placeTheOrder(s, trade.getPrice() + .05, volume, o, Side.BUY);
                actual_num_beef_trades += volume;
            }
            else
            {
                order_placer.placeTheOrder(s, trade.getPrice() - .05, volume, o, Side.SELL);
                actual_num_beef_trades -= volume;
            }
            // actual_num_beef_trades = num_beef_trades;
        }
        else if(book.equals(tortilla.getBookName()) && num_tort_trades != actual_num_tort_trades)
        {
            volume = Math.min(num_orders_allowed, Math.abs(num_tort_trades - actual_num_tort_trades));
            Symbol s = getTortillaBookSymbol();
            LOGGER.info("making a GTC tortilla order with at a price of " + trade.getPrice() + " and a volume of " + volume);
            if(actual_num_tort_trades < num_tort_trades)
            {
                order_placer.placeTheOrder(s, trade.getPrice() + .05, volume, o, Side.BUY);
                actual_num_tort_trades += volume;
            }
            else
            {
                order_placer.placeTheOrder(s, trade.getPrice() - .05, volume, o, Side.SELL);
                actual_num_tort_trades -= volume;
            }
            // actual_num_tort_trades = num_tort_trades;
        }
    }

    public boolean executeOrdersHelper(TreeMap<Double, Integer> taco_book, TreeMap<Double, Integer> beef_book, TreeMap<Double, Integer> tort_book, Side side)
    {
        if(taco_book.size() == 0 || beef_book.size() == 0 || tort_book.size() == 0)
            return false;
        Double taco_p = taco_book.firstKey();
        Integer taco_v = taco_book.get(taco_p);
        Double beef_p = beef_book.firstKey();
        Integer beef_v = beef_book.get(beef_p);
        Double tort_p = tort_book.firstKey();
        Integer tort_v = tort_book.get(tort_p);
        // LOGGER.info(taco_p + " " + taco_v + " " + beef_p + " " + beef_v + " " + tort_p + " " + tort_v);
        if(side == Side.BUY)
        {
            return buyTacoSellOthers(taco_p, taco_v, beef_p, beef_v, tort_p, tort_v);
        }
        else
        {
            return sellTacoBuyOthers(taco_p, taco_v, beef_p, beef_v, tort_p, tort_v);
        }
    }

    // NAJ: there is quite a bit duplicate code between sellTaco and buyTaco, I'd work on a generic method for both
    /*
    checks if you should sell tacos and if you should, you create the order
     */
    public boolean executeSellTacoOrders()
    {
        TreeMap<Double,Integer> curr = taco.getBids();
        if(curr.size() == 0)
        {
            //LOGGER.info("in if statement");
            return false;
        }
        Double taco_best_bid_price = curr.firstKey();
        Integer taco_best_bid_volume = 0;
        if(taco_best_bid_price != null)
            taco_best_bid_volume = curr.get(taco_best_bid_price);

        curr = beef.getAsks();
        if(curr.size() == 0)
            return false;
        Double beef_best_ask_price = curr.firstKey();
        Integer beef_best_ask_volume = 0;
        if(beef_best_ask_price != null)
            beef_best_ask_volume = curr.get(beef_best_ask_price);

        curr = tortilla.getAsks();
        if(curr.size() == 0)
            return false;
        // LOGGER.info("sell");
        Double tortilla_best_ask_price = curr.firstKey();
        Integer tortilla_best_ask_volume = 0;
        if(tortilla_best_ask_price != null)
            tortilla_best_ask_volume = curr.get(tortilla_best_ask_price);

        if(taco_best_bid_price != null && beef_best_ask_price != null && tortilla_best_ask_price != null)
        {
            // LOGGER.info("taco bid: " + taco_best_bid_price + " should be > beef ask + tortilla ask " + (beef_best_ask_price + tortilla_best_ask_price));
            return sellTacoBuyOthers(taco_best_bid_price, taco_best_bid_volume, beef_best_ask_price, beef_best_ask_volume, tortilla_best_ask_price, tortilla_best_ask_volume);
        }
        return false;
    }

    /*
    checks if you should buy tacos and if you should, you create the order
     */
    public boolean executeBuyTacoOrders()
    {
        TreeMap<Double, Integer> curr = taco.getAsks();
        if(curr.size() == 0)
            return false;
        Double taco_best_ask_price = curr.firstKey();
        Integer taco_best_ask_volume = 0;
        if(taco_best_ask_price != null)
            taco_best_ask_volume = curr.get(taco_best_ask_price);

        curr = beef.getBids();
        if(curr.size() == 0)
            return false;
        Double beef_best_bid_price = curr.firstKey();
        Integer beef_best_bid_volume = 0;
        if(beef_best_bid_price != null)
            beef_best_bid_volume = curr.get(beef_best_bid_price);

        curr = tortilla.getBids();
        if(curr.size() == 0)
            return false;
        Double tortilla_best_bid_price = curr.firstKey();
        Integer tortilla_best_bid_volume = 0;
        if(tortilla_best_bid_price != null)
            tortilla_best_bid_volume = curr.get(tortilla_best_bid_price);

        // LOGGER.info("buy");
        if(taco_best_ask_price != null && beef_best_bid_price != null && tortilla_best_bid_price != null)
        {
            // LOGGER.info("taco ask: " + taco_best_ask_price + " should be < beef bid + tortilla bid " + (beef_best_bid_price + tortilla_best_bid_price));
            return buyTacoSellOthers(taco_best_ask_price, taco_best_ask_volume, beef_best_bid_price, beef_best_bid_volume, tortilla_best_bid_price, tortilla_best_bid_volume);
        }
        return false;
    }

    /*
    places sell orders on tacos
     */
    private boolean sellTacoBuyOthers(double taco_best_bid_price, int taco_best_bid_volume, double beef_best_ask_price, int beef_best_ask_volume, double tortilla_best_ask_price, int tortilla_best_ask_volume)
    {
        if(taco_best_bid_price - offset > beef_best_ask_price + tortilla_best_ask_price)
        {
            int min_volume = calculateVolume(taco_best_bid_volume, beef_best_ask_volume, tortilla_best_ask_volume);
            if(min_volume <= 0)
                return false;
            order_placer.placeTheOrder(Symbol.of(taco.getBookName()), taco_best_bid_price, min_volume, OrderType.IMMEDIATE_OR_CANCEL, Side.SELL);
            order_placer.placeTheOrder(Symbol.of(beef.getBookName()), beef_best_ask_price, min_volume, OrderType.IMMEDIATE_OR_CANCEL, Side.BUY);
            order_placer.placeTheOrder(Symbol.of(tortilla.getBookName()), tortilla_best_ask_price, min_volume, OrderType.IMMEDIATE_OR_CANCEL, Side.BUY);

            num_taco_trades -= min_volume;
            num_beef_trades += min_volume;
            num_tort_trades += min_volume;
            time_of_last_trade = System.currentTimeMillis();
            // ioc_limiter.acquire();
            LOGGER.info("SOLD!!! because I can sell taco at " + taco_best_bid_price + " and buy back beef and tortilla at " + (beef_best_ask_price + tortilla_best_ask_price));
            return true;
        }
        return false;
    }

    /*
    places buy orders on tacos
     */
    private boolean buyTacoSellOthers(double taco_best_ask_price, int taco_best_ask_volume, double beef_best_bid_price, int beef_best_bid_volume, double tortilla_best_bid_price, int tortilla_best_bid_volume)
    {
        if(taco_best_ask_price + offset < beef_best_bid_price + tortilla_best_bid_price)
        {
            int min_volume = calculateVolume(taco_best_ask_volume, beef_best_bid_volume, tortilla_best_bid_volume);
            if(min_volume <= 0)
                return false;
            order_placer.placeTheOrder(Symbol.of(taco.getBookName()), taco_best_ask_price, min_volume, OrderType.IMMEDIATE_OR_CANCEL, Side.BUY);
            order_placer.placeTheOrder(Symbol.of(beef.getBookName()), beef_best_bid_price, min_volume, OrderType.IMMEDIATE_OR_CANCEL, Side.SELL);
            order_placer.placeTheOrder(Symbol.of(tortilla.getBookName()), tortilla_best_bid_price, min_volume, OrderType.IMMEDIATE_OR_CANCEL, Side.SELL);
            num_taco_trades += min_volume;
            num_beef_trades -= min_volume;
            num_tort_trades -= min_volume;
            time_of_last_trade = System.currentTimeMillis();
            // ioc_limiter.acquire();

            LOGGER.info("BOUGHT!!! I can buy taco at " + taco_best_ask_price + " and sell back beef and tortilla at " + (beef_best_bid_price + tortilla_best_bid_price));
            return true;
        }
        return false;
    }

    public void update_actual_pos(OwnTrade trade)
    {
        String book = trade.getBook().toString();
        Side s = trade.getSide();
        int volume = Math.min(num_orders_allowed, trade.getVolume());
        if(book.equals(taco.getBookName()))
            actual_num_taco_trades = s == Side.BUY ? actual_num_taco_trades + volume : actual_num_taco_trades - volume;
        else if(book.equals(beef.getBookName()))
            actual_num_beef_trades = s == Side.BUY ? actual_num_beef_trades + volume : actual_num_beef_trades - volume;
        else
            actual_num_tort_trades = s == Side.BUY ? actual_num_tort_trades + volume : actual_num_tort_trades - volume;
    }

    private int calculateVolume(int taco_v, int beef_v, int tort_v)
    {
        return Math.min(num_orders_allowed, Math.min(taco_v, Math.min(beef_v, tort_v)));
    }

    void removeFromCurrentOrders(long orderId)
    {
        MyOrder o = my_orders.get(orderId);
        if(o.getSide() == Side.BUY)
            my_bids.put(o.getPrice(), my_bids.get(o.getPrice()) - o.getVolume());
        else
            my_asks.put(o.getPrice(), my_asks.get(o.getPrice()) - o.getVolume());
        my_orders.remove(orderId);
    }

    // NAJ: I would test this method to make sure its doing what you think its doing
    public void handleMyOrders(OwnTrade trade)
    {
        long order_id = trade.getOrderId();
        int trade_volume = trade.getVolume();
        MyOrder o = my_orders.get(order_id);
        if(o.getVolume() == trade_volume)
            removeFromCurrentOrders(order_id);
        else
        {
            o.setVolume(o.getVolume() - trade_volume);
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

    public RetailStateTracker getTacoBook()
    {
        return taco;
    }

    public RetailStateTracker getBeefBook()
    {
        return beef;
    }

    public RetailStateTracker getTortillaBook()
    {
        return tortilla;
    }

    public void GoodTilCancelOrder(Symbol s, Side side, double p, int v)
    {
        order_placer.placeTheOrder(s, p, v, OrderType.GOOD_TIL_CANCEL, side);
    }
}
