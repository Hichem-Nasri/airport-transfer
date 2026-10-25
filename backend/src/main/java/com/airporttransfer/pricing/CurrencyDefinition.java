package com.airporttransfer.pricing;

import jakarta.persistence.*;

@Entity
@Table(name = "currencies")
public class CurrencyDefinition {

    @Id
    @Column(name = "code", length = 3)
    private String code;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false, length = 8)
    private String symbol;

    @Column(nullable = false)
    private int decimals;

    @Column(nullable = false)
    private boolean active;

    public CurrencyDefinition() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getDecimals() {
        return decimals;
    }

    public void setDecimals(int decimals) {
        this.decimals = decimals;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
