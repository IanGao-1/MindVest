package com.reserio.financialmanagement.service;

import com.reserio.financialmanagement.repository.AssetRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class MarketDataServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private MarketDataService marketDataService;

    @Test
    void testGetMarketPrice() {
        double price = marketDataService.getMarketPrice("AAPL");
        assertEquals(180.25, price);
    }
}