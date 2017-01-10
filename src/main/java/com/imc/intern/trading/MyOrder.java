package com.imc.intern.trading;

import com.imc.intern.exchange.datamodel.Side;
import com.imc.intern.exchange.datamodel.api.OrderType;

/**
 * Created by imc on 10/01/2017.
 */
public class MyOrder
{
    MyOrder(long id, double p, int v, OrderType t, Side s)
    {
        order_id = id;
        price = p;
        volume = v;
        type = t;
        side = s;
    }

    public String toString()
    {
        return "order id is: " + order_id + " price is: " + price + " volume is: " + volume + "OrderType is: " + type.toString() + " Side is: " + side.toString();
    }

    long order_id;
    double price;
    int volume;
    OrderType type;
    Side side;
}
