package com.imc.intern.trading;

import com.imc.intern.exchange.datamodel.api.RetailState;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by imc on 10/01/2017.
 */
public class BookHandler
{
    private final int book_size = 3;
    private String BOOK;

    BookHandler(String order_book)
    {
        BOOK = order_book;
    }

    public void udpate_book(ArrayList<Order> bids, ArrayList<Order> asks, List<RetailState.Level> curr_bids, List<RetailState.Level> curr_asks)
    {
        // bids
        for (int j = 0; j < Math.min(book_size, curr_bids.size()); j++)
        {
            RetailState.Level order_in_book = curr_bids.get(j);
            // bids.set(i, new Order(order_in_book.getPrice(), order_in_book.getVolume()));
            for (int i = 0; i < book_size; i++)
            {
                Order old_bid = bids.get(i);
                double price_in_book = order_in_book.getPrice();
                int volume_in_book = order_in_book.getVolume();
                if (price_in_book > old_bid.price) // new order
                {
                    bids.add(i, new Order(price_in_book, volume_in_book));
                    bids.remove(book_size); // remove the last thing in the book
                    break;
                } else if (old_bid.price == order_in_book.getPrice()) // volume needs to be updated
                {
                    if (order_in_book.getVolume() != 0) // order was only partially hit
                    {
                        old_bid.volume = order_in_book.getVolume();
                        bids.set(i, old_bid);
                    } else // all of the order has been hit
                    {
                        bids.remove(i);
                        bids.add(new Order(-1, 0));
                    }
                    break;
                } else if (old_bid.price == -1) // don't have 3 orders in the book yet
                {
                    old_bid.price = price_in_book;
                    old_bid.volume = volume_in_book;
                    bids.set(i, old_bid);
                    break;
                }
            }

        }
        for (int j = 0; j < Math.min(book_size, curr_asks.size()); j++)
        {
            RetailState.Level order_in_book = curr_asks.get(j);
            for (int i = 0; i < book_size; i++)
            {
                Order old_ask = asks.get(i);
                double price_in_book = order_in_book.getPrice();
                int volume_in_book = order_in_book.getVolume();
                if (price_in_book < old_ask.price) // new order
                {
                    asks.add(i, new Order(price_in_book, volume_in_book));
                    asks.remove(book_size); // remove the last thing in the book
                    break;
                } else if (old_ask.price == order_in_book.getPrice()) // volume needs to be updated
                {
                    if (order_in_book.getVolume() != 0) // order was only partially hit
                    {
                        old_ask.volume = order_in_book.getVolume();
                        asks.set(i, old_ask);
                    } else // all of the order has been hit
                    {
                        asks.remove(i);
                        asks.add(new Order(-1, 0));
                    }
                    break;
                } else if (old_ask.price == -1) // don't have 3 orders in the book yet
                {
                    old_ask.price = price_in_book;
                    old_ask.volume = volume_in_book;
                    asks.set(i, old_ask);
                    break;
                }
            }
        }
    }

    public void print_book(ArrayList<Order> bids, ArrayList<Order> asks)
    {
        System.out.println("BIDS:\n" + bids.toString());
        System.out.println("ASKS:\n" + asks.toString());
        System.out.println("\n");
    }
}