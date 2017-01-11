package com.imc.intern.trading;

import com.imc.intern.exchange.datamodel.api.RetailState;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by imc on 10/01/2017.
 */
public class BookHandler
{
    private final int book_size = 3;
    private String BOOK;
    // private ArrayList<Order> bids = new ArrayList<>(); // keeps track of the bids in the book
    // private ArrayList<Order> asks = new ArrayList<>(); // keeps track of the asks in the book
    private TreeMap<Double, Integer> bids = new TreeMap<>();
    private TreeMap<Double, Integer> asks = new TreeMap<>();

    BookHandler(String order_book)
    {
        BOOK = order_book;
    }

    TreeMap<Double, Integer> getBids()
    {
        return bids;
    }

    TreeMap<Double, Integer> getAsks()
    {
        return asks;
    }

    int getBookSize()
    {
        return book_size;
    }

    String getBookName()
    {
        return BOOK;
    }

    public void update_book(RetailState rtl_state)
    {

        List<RetailState.Level> curr_bids = rtl_state.getBids();
        // update_book_helper(curr_bids, true);
        List<RetailState.Level> curr_asks = rtl_state.getAsks();
        // update_book_helper(curr_asks, false);

        for(RetailState.Level l : curr_bids)
        {
            double price = l.getPrice();
            Integer volume = bids.get(l.getPrice());
            if(volume == null)
            {
                bids.put(price, l.getVolume());
                if(bids.size() == 4)
                    bids.remove(bids.lastKey());
            }
            else
            {
                bids.put(price, bids.get(price) + volume);
            }
        }

        for(RetailState.Level l : curr_asks)
        {
            double price = l.getPrice();
            Integer volume = asks.get(l.getPrice());
            if(volume == null)
            {
                asks.put(price, l.getVolume());
                if(asks.size() == 4)
                    asks.remove(asks.lastKey());
            }
            else
            {
                asks.put(price, asks.get(price) + volume);
            }
        }
        System.out.println("bid book size is " + bids.size() + " and ask book size is " + asks.size());

    }

    public void update_book_helper(List<RetailState.Level> book, boolean bids_book)
    {
        TreeMap<Double, Integer> temp = bids_book ? bids : asks;
        for(RetailState.Level l : book)
        {
            double price = l .getPrice();
            Integer volume = temp.get(l.getPrice());
            if(volume == null)
            {
                temp.put(price, l.getVolume());
                if(temp.size() == 3)
                    temp.remove(temp.lastKey());
            }
            else
            {
                temp.put(price, temp.get(price) + volume);
            }
        }
    }

    public void print_book()
    {
        System.out.println("BIDS:\n" + bids.toString());
        System.out.println("ASKS:\n" + asks.toString());
        System.out.println("\n");
    }

}