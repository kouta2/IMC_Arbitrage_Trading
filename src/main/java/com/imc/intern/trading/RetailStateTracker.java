package com.imc.intern.trading;

import com.imc.intern.exchange.datamodel.api.RetailState;

import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by imc on 10/01/2017.
 */
// MWANG: I suggest you name this class something else. "BookHandler" is pretty vague, so I had to read through the code
// before I figured out what it was supposed to be doing. If you call it "RetailStateTracker" or "BookDepthManager" or
// something like that then it is clearer.
public class BookHandler
{
    private final int book_size = 3;
    private String BOOK;
    private TreeMap<Double, Integer> bids = new TreeMap<>(Collections.reverseOrder());
    private TreeMap<Double, Integer> asks = new TreeMap<>(); // NAJ: probably want to sort in reverse order here, can pass comparator

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

    String getBookName()
    {
        return BOOK;
    }

    public void update_book(RetailState rtl_state)
    {

        List<RetailState.Level> curr_bids = rtl_state.getBids();
        update_book_tree(curr_bids, true);
        List<RetailState.Level> curr_asks = rtl_state.getAsks();
        update_book_tree(curr_asks, false);
    }

    // NAJ: "update_book_tree" is more clear here
    public void update_book_tree(List<RetailState.Level> book, boolean bids_book)
    {
        TreeMap<Double, Integer> temp = bids_book == true ? bids : asks;
        for(RetailState.Level l : book)
        {
            // NAJ: price should be Double
            double price = l.getPrice();
            Integer volume = temp.get(l.getPrice());
            if(volume == null)
            {
                temp.put(price, l.getVolume());
                if(temp.size() == book_size)
                    temp.remove(temp.lastKey());
            }
            else
            {
                // MWANG: I suspect you don't want to be adding the new volume. Retail state updates give you
                // the new volume at that level.
                temp.put(price, temp.get(price) + volume);
            }
        }
    }

    public String toString()
    {
        return "BIDS:\n" + bids.toString() + "\nASKS:\n" + asks.toString();
    }

}
