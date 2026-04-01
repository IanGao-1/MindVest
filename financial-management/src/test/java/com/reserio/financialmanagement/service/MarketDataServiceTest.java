package com.reserio.financialmanagement.service;

import com.reserio.financialmanagement.dto.YahooQuoteDTO;
import com.reserio.financialmanagement.repository.AssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarketDataServiceTest {

    @Mock
    private AssetRepository assetRepository;

    private MarketDataService marketDataService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        marketDataService = Mockito.spy(new MarketDataService());
    }

    @Test
    void testGetMarketPrice() {
        YahooQuoteDTO quoteDTO = new YahooQuoteDTO();
        quoteDTO.setPrice(BigDecimal.valueOf(180.25));
        Mockito.doReturn(quoteDTO).when(marketDataService).getYahooQuote("AAPL");

        double price = marketDataService.getMarketPrice("AAPL");

        assertEquals(180.25, price);
    }
}
