package com.airporttransfer.pricing;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRepository extends JpaRepository<CurrencyDefinition, String> {

    List<CurrencyDefinition> findAllByOrderByCodeAsc();

    List<CurrencyDefinition> findAllByActiveTrueOrderByCodeAsc();

    Optional<CurrencyDefinition> findByCodeAndActiveTrue(String code);
}
