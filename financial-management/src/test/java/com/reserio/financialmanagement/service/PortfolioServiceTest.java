package com.reserio.financialmanagement.service;

import com.reserio.financialmanagement.dto.PortfolioDTO;
import com.reserio.financialmanagement.model.Portfolio;
import com.reserio.financialmanagement.repository.PortfolioRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class PortfolioServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @InjectMocks
    private PortfolioService portfolioService;

    @Test
    void testGetPortfolioById() {
        Portfolio portfolio = new Portfolio();
        portfolio.setId(1L);
        portfolio.setName("My Portfolio");
        portfolio.setDescription("My investment portfolio");

        Mockito.when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio));

        PortfolioDTO portfolioDTO = portfolioService.getPortfolioById(1L);

        assertNotNull(portfolioDTO);
        assertEquals("My Portfolio", portfolioDTO.getName());
        assertEquals("My investment portfolio", portfolioDTO.getDescription());
    }
}