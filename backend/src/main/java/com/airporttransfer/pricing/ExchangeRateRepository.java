package com.airporttransfer.pricing;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    /** The rate that applies now: the most recent one. */
    Optional<ExchangeRate> findFirstByCurrencyCodeOrderByCreatedAtDescIdDesc(String currencyCode);

    List<ExchangeRate> findAllByOrderByCreatedAtDescIdDesc();

    List<ExchangeRate> findByCurrencyCodeOrderByCreatedAtDescIdDesc(String currencyCode);

    long countByDemoTrue();
}
