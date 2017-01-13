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

    private double price;

    @Override
    public String toString()
    {
        return "Order{" +
                "price=" + price +
                ", volume=" + volume +
                '}';
    }

    private int volume;

    public double getPrice()
    {
        return price;
    }

    public int getVolume()
    {
        return volume;
    }

    public void setPrice(double price)
    {

        this.price = price;
    }

    public void setVolume(int volume)
    {
        this.volume = volume;
    }
}
