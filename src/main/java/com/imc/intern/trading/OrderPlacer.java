package com.imc.intern.trading;

import com.imc.intern.exchange.datamodel.Side;
import com.imc.intern.exchange.datamodel.api.OrderType;
import com.imc.intern.exchange.datamodel.api.Symbol;
import com.imc.intern.exchange.views.ExchangeView;

/**
 * Created by imc on 12/01/2017.
 */
public class OrderPlacer
{
    private ExchangeView rmt_exch;

    OrderPlacer(ExchangeView r)
    {
        rmt_exch = r;
    }

    public long placeTheOrder(Symbol s, double p, int v, OrderType o, Side si)
    {
        return rmt_exch.createOrder(s, p, v, o, si);
        /*
        my_orders.put(order_id, new MyOrder(order_id, p, v, o, si));
        if(si == Side.BUY && my_bids.containsKey(p))
        {
            my_bids.put(p, my_bids.get(p) + v);
        }
        else if(si == Side.SELL && my_asks.containsKey(p))
        {
            my_asks.put(p, my_asks.get(p) + v)
        }
        */
    }
}
