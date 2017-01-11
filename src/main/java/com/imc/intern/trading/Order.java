package com.imc.intern.trading;

/**
 * Created by imc on 10/01/2017.
 */

public class Order
{
    Order(double p, int v)
    {
        price = p;
        volume = v;
    }

    public String toString()
    {
        return "price is: " + price + " volume is: " + volume;
    }

    double price;
    int volume;
}
