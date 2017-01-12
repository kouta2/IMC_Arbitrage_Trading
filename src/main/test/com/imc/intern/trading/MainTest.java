package com.imc.intern.trading;

import com.imc.intern.exchange.client.RemoteExchangeView;
import org.junit.Test;
import org.mockito.Mockito;

public class MainTest
{
    @Test
    public void testHandlerSubscribe()
    {
        RemoteExchangeView exchangeView = Mockito.mock(RemoteExchangeView.class);
        Main.add_handlers(exchangeView);
        Mockito.verify(exchangeView).subscribe(Mockito.any(), Mockito.any(HitterHandler.class));
    }

}