package com.imc.intern.trading;

import com.imc.intern.exchange.datamodel.Side;
import com.imc.intern.exchange.datamodel.api.OrderType;

/**
 * Created by imc on 10/01/2017.
 */
// NAJ: MyOrder should extend Order as it shares a few fields/getters
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

    // NAJ: intelliJ can auto-generate toString for you, I'd give it a shot.
    public String toString()
    {
        return "order id is: " + order_id + " price is: " + price + " volume is: " + volume + "OrderType is: " + type.toString() + " Side is: " + side.toString();
    }

    // NAJ: typically fields of a class are private and protected from interaction. I would privatise these and use intelliJ
    // NAJ: to autogenerate setters/getters when you need them.
    long order_id;
    double price;
    int volume;
    OrderType type;
    Side side;
}
