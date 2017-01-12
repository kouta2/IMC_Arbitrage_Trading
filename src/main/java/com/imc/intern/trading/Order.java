package com.imc.intern.trading;

/**
 * Created by imc on 10/01/2017.
 */

public class Order
{
    // NAJ: I would rename Order to "MarketOrder" so its clear. Order is ambiguous
    Order(double p, int v)
    {
        price = p;
        volume = v;
    }

    // NAJ: ditto on toString and fields as in myOrder. I would also pull them into a datamodel folder/package.
    public String toString()
    {
        return "price is: " + price + " volume is: " + volume;
    }

    double price;
    int volume;
}
