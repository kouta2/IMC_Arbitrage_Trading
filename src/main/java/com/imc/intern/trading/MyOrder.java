package com.imc.intern.trading;

import com.imc.intern.exchange.datamodel.Side;
import com.imc.intern.exchange.datamodel.api.OrderType;

/**
 * Created by imc on 10/01/2017.
 */
public class MyOrder extends MarketOrder
{
    public MyOrder(long id, double p, int v, OrderType t, Side s)
    {
        super(p, v);
        order_id = id;
        type = t;
        side = s;
    }

    @Override
    public String toString()
    {
        return "MyOrder{" + super.toString() +
                "order_id=" + order_id +
                ", type=" + type +
                ", side=" + side +
                '}';
    }


    private long order_id;
    private OrderType type;
    private Side side;

    long getOrderId()
    {
        return order_id;
    }

    OrderType getType()
    {
        return type;
    }

    Side getSide()
    {
        return side;
    }

    public void setOrder_id(long order_id)
    {
        this.order_id = order_id;
    }

    public void setType(OrderType type)
    {
        this.type = type;
    }

    public void setSide(Side side)
    {
        this.side = side;
    }
}
