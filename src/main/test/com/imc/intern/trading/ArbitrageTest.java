package com.imc.intern.trading;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 * Created by imc on 12/01/2017.
 */
public class ArbitrageTest
{

    @Test
    public void testPlaceSellOrders()
    {
        Arbitrage a = Mockito.mock(Arbitrage.class);
        a.executeSellTacoOrders();
        // Mockito.verify();



    }
}