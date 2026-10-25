package com.airporttransfer.pricing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** In the "prod" profile, warns loudly if placeholder prices or rates are still in the database. */
@Component
@Profile("prod")
public class DemoDataWarning implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataWarning.class);

    private final TariffRepository tariffs;
    private final ExchangeRateRepository rates;

    public DemoDataWarning(TariffRepository tariffs, ExchangeRateRepository rates) {
        this.tariffs = tariffs;
        this.rates = rates;
    }

    @Override
    public void run(ApplicationArguments args) {
        long demoTariffs = tariffs.countByDemoTrue();
        long demoRates = rates.countByDemoTrue();
        if (demoTariffs > 0 || demoRates > 0) {
            log.warn("DEMO DATA IN PRODUCTION: {} placeholder tariffs and {} placeholder exchange rates are still "
                + "in the database. Load the real price grid and rates before accepting bookings.", demoTariffs, demoRates);
        }
    }
}
