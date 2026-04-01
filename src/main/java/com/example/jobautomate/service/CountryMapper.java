package com.example.jobautomate.service;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CountryMapper {

    private static final Map<String, String> NAME_TO_CODE = new HashMap<>();

    static {
        for (String iso : Locale.getISOCountries()) {
            Locale locale = new Locale("", iso);
            String countryName = locale.getDisplayCountry(Locale.ENGLISH).toLowerCase();

            NAME_TO_CODE.put(countryName, iso.toLowerCase());
        }

        // 🔥 Add common aliases manually
        NAME_TO_CODE.put("usa", "us");
        NAME_TO_CODE.put("america", "us");
        NAME_TO_CODE.put("uk", "gb");
        NAME_TO_CODE.put("uae", "ae");
    }

    public static String getCode(String country) {
        if (country == null) return "us";

        return NAME_TO_CODE.getOrDefault(country.toLowerCase(), "us");
    }
}