package com.example.jobautomate.service;

import java.util.Map;
import org.springframework.util.StringUtils;

public class CountryMapper {
  private static final Map<String, String> NAME_TO_CODE = Map.ofEntries(
    // Major countries
    Map.entry("india", "in"),
    Map.entry("united states", "us"),
    Map.entry("usa", "us"),
    Map.entry("america", "us"),
    Map.entry("united kingdom", "gb"),
    Map.entry("uk", "gb"),
    Map.entry("britain", "gb"),
    Map.entry("canada", "ca"),
    Map.entry("australia", "au"),
    Map.entry("germany", "de"),
    Map.entry("france", "fr"),
    Map.entry("singapore", "sg"),
    Map.entry("ireland", "ie"),
    Map.entry("netherlands", "nl"),
    Map.entry("spain", "es"),
    Map.entry("italy", "it"),
    Map.entry("sweden", "se"),
    Map.entry("switzerland", "ch"),
    Map.entry("new zealand", "nz"),
    Map.entry("south africa", "za"),

    // Asia
    Map.entry("china", "cn"),
    Map.entry("japan", "jp"),
    Map.entry("south korea", "kr"),
    Map.entry("indonesia", "id"),
    Map.entry("malaysia", "my"),
    Map.entry("philippines", "ph"),
    Map.entry("thailand", "th"),
    Map.entry("vietnam", "vn"),
    Map.entry("bangladesh", "bd"),
    Map.entry("pakistan", "pk"),
    Map.entry("sri lanka", "lk"),
    Map.entry("uae", "ae"),
    Map.entry("united arab emirates", "ae"),
    Map.entry("saudi arabia", "sa"),
    Map.entry("qatar", "qa"),
    Map.entry("kuwait", "kw"),

    // Europe
    Map.entry("norway", "no"),
    Map.entry("denmark", "dk"),
    Map.entry("finland", "fi"),
    Map.entry("poland", "pl"),
    Map.entry("austria", "at"),
    Map.entry("belgium", "be"),
    Map.entry("portugal", "pt"),
    Map.entry("greece", "gr"),
    Map.entry("czech republic", "cz"),
    Map.entry("hungary", "hu"),
    Map.entry("romania", "ro"),
    Map.entry("bulgaria", "bg"),
    Map.entry("slovakia", "sk"),
    Map.entry("slovenia", "si"),
    Map.entry("croatia", "hr"),
    Map.entry("estonia", "ee"),
    Map.entry("latvia", "lv"),
    Map.entry("lithuania", "lt"),

    // Americas
    Map.entry("mexico", "mx"),
    Map.entry("brazil", "br"),
    Map.entry("argentina", "ar"),
    Map.entry("chile", "cl"),
    Map.entry("colombia", "co"),
    Map.entry("peru", "pe"),

    // Africa
    Map.entry("egypt", "eg"),
    Map.entry("nigeria", "ng"),
    Map.entry("kenya", "ke"),
    Map.entry("morocco", "ma"),
    Map.entry("ghana", "gh"),
    Map.entry("ethiopia", "et"),
    Map.entry("tanzania", "tz"),
    Map.entry("uganda", "ug"),
    Map.entry("algeria", "dz"),   // 🔥 important for your case
    Map.entry("tunisia", "tn"),
    Map.entry("zimbabwe", "zw")
);

    public static String toCountryCode(String countryName) {
        if (!StringUtils.hasText(countryName)) {
            return "in"; // default
        }
        String normalized = normalize(countryName);
        return NAME_TO_CODE.getOrDefault(normalized, normalized); // fallback to normalized lowercase
    }

    private static String normalize(String name) {
        if (!StringUtils.hasText(name)) return "";
        return name.trim().toLowerCase().replaceAll("[^a-z ]", "").replaceAll("\\s+", " ");
    }
}
