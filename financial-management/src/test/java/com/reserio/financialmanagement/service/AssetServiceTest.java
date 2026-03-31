package com.reserio.financialmanagement.service;

import com.reserio.financialmanagement.dto.AssetDTO;
import com.reserio.financialmanagement.model.Asset;
import com.reserio.financialmanagement.repository.AssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private AssetService assetService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAssetById() {
        Asset asset = new Asset();
        asset.setId(1L);
        asset.setTicker("AAPL");
        asset.setName("Apple Inc.");
        asset.setType("stock");
        asset.setQuantity(10.0);
        asset.setAvgCost(150.0);
        asset.setCurrentPrice(180.25);

        Mockito.when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

        AssetDTO assetDTO = assetService.getAssetById(1L);

        assertNotNull(assetDTO);
        assertEquals("AAPL", assetDTO.getTicker());
        assertEquals("Apple Inc.", assetDTO.getName());
    }
}
