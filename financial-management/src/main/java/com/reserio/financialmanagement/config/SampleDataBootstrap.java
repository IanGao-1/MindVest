package com.reserio.financialmanagement.config;

import com.reserio.financialmanagement.service.SampleDataService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SampleDataBootstrap implements ApplicationRunner {
    private final SampleDataService sampleDataService;

    public SampleDataBootstrap(SampleDataService sampleDataService) {
        this.sampleDataService = sampleDataService;
    }

    @Override
    public void run(ApplicationArguments args) {
        sampleDataService.resetUnsupportedOrEmptyData();
    }
}
