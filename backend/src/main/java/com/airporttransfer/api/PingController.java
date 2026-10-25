package com.airporttransfer.api;

import com.airporttransfer.domain.Currency;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PingController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        List<String> currencies = Arrays.stream(Currency.values()).map(Enum::name).toList();
        return Map.of("status", "ok", "currencies", currencies, "baseCurrency", Currency.TND.name());
    }
}
