package com.airporttransfer.catalog;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "airports")
public class Airport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "iata_code", nullable = false, unique = true, length = 3)
    private String iataCode;

    @Column(name = "name_fr", nullable = false, length = 150)
    private String nameFr;

    @Column(name = "name_en", nullable = false, length = 150)
    private String nameEn;

    @Column(name = "name_ar", nullable = false, length = 150)
    private String nameAr;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    /** ISO 3166-1 alpha-2 country of the airport (TN, FR). Decides which destinations a customer can pick. */
    @Column(nullable = false, length = 2)
    private String country = "TN";

    /** IANA time zone of the airport: pickup times are entered and shown in it. */
    @Column(name = "time_zone", nullable = false, length = 40)
    private String timeZone = "Africa/Tunis";

    /** Comma-separated terminal names (for instance "Terminal 1,Terminal 2"); empty when the airport has no choice. */
    @Column(length = 200)
    private String terminals;

    @Column(nullable = false)
    private boolean active;

    public Airport() {
    }

    public Long getId() {
        return id;
    }

    public String getIataCode() {
        return iataCode;
    }

    public void setIataCode(String iataCode) {
        this.iataCode = iataCode;
    }

    public String getNameFr() {
        return nameFr;
    }

    public void setNameFr(String nameFr) {
        this.nameFr = nameFr;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public String getNameAr() {
        return nameAr;
    }

    public void setNameAr(String nameAr) {
        this.nameAr = nameAr;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getTerminals() {
        return terminals;
    }

    public void setTerminals(String terminals) {
        this.terminals = terminals;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
