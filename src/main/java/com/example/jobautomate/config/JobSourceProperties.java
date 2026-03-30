package com.example.jobautomate.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jobs")
public class JobSourceProperties {

    private String provider = "adzuna";
    private boolean seedFallbackEnabled = true;
    private final Adzuna adzuna = new Adzuna();
    private final JSearch jsearch = new JSearch();
    private final Jooble jooble = new Jooble();
    private final Aggregation aggregation = new Aggregation();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isSeedFallbackEnabled() {
        return seedFallbackEnabled;
    }

    public void setSeedFallbackEnabled(boolean seedFallbackEnabled) {
        this.seedFallbackEnabled = seedFallbackEnabled;
    }

    public Adzuna getAdzuna() {
        return adzuna;
    }

    public JSearch getJsearch() {
        return jsearch;
    }

    public Jooble getJooble() {
        return jooble;
    }

    public Aggregation getAggregation() {
        return aggregation;
    }

    public static class Adzuna {

        private String baseUrl;
        private String appId;
        private String appKey;
        private int resultsPerPage = 20;
        private int pagesPerCountry = 1;
        private int maxDaysOld = 7;
        private List<String> searchCountries = List.of("in");

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getAppKey() {
            return appKey;
        }

        public void setAppKey(String appKey) {
            this.appKey = appKey;
        }

        public int getResultsPerPage() {
            return resultsPerPage;
        }

        public void setResultsPerPage(int resultsPerPage) {
            this.resultsPerPage = resultsPerPage;
        }

        public int getPagesPerCountry() {
            return pagesPerCountry;
        }

        public void setPagesPerCountry(int pagesPerCountry) {
            this.pagesPerCountry = pagesPerCountry;
        }

        public int getMaxDaysOld() {
            return maxDaysOld;
        }

        public void setMaxDaysOld(int maxDaysOld) {
            this.maxDaysOld = maxDaysOld;
        }

        public List<String> getSearchCountries() {
            return searchCountries;
        }

        public void setSearchCountries(List<String> searchCountries) {
            this.searchCountries = searchCountries;
        }
    }

    public static class JSearch {

        private String baseUrl = "https://jsearch.p.rapidapi.com";
        private String rapidApiKey;
        private String rapidApiHost = "jsearch.p.rapidapi.com";
        private int page = 1;
        private int numPages = 1;
        private String country = "in";
        private int timeoutMs = 10000;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getRapidApiKey() {
            return rapidApiKey;
        }

        public void setRapidApiKey(String rapidApiKey) {
            this.rapidApiKey = rapidApiKey;
        }

        public String getRapidApiHost() {
            return rapidApiHost;
        }

        public void setRapidApiHost(String rapidApiHost) {
            this.rapidApiHost = rapidApiHost;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getNumPages() {
            return numPages;
        }

        public void setNumPages(int numPages) {
            this.numPages = numPages;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    public static class Jooble {

        private String baseUrl = "https://jooble.org/api";
        private String apiKey;
        private int page = 1;
        private int resultsPerPage = 20;
        private String defaultLocation = "India";
        private int timeoutMs = 10000;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getResultsPerPage() {
            return resultsPerPage;
        }

        public void setResultsPerPage(int resultsPerPage) {
            this.resultsPerPage = resultsPerPage;
        }

        public String getDefaultLocation() {
            return defaultLocation;
        }

        public void setDefaultLocation(String defaultLocation) {
            this.defaultLocation = defaultLocation;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    public static class Aggregation {

        private double similarityThreshold = 0.6;
        private int topLimit = 30;
        private int providerConcurrency = 3;
        private double adzunaShare = 0.5;
        private double joobleShare = 0.3;
        private double jsearchShare = 0.2;

        public double getSimilarityThreshold() {
            return similarityThreshold;
        }

        public void setSimilarityThreshold(double similarityThreshold) {
            this.similarityThreshold = similarityThreshold;
        }

        public int getTopLimit() {
            return topLimit;
        }

        public void setTopLimit(int topLimit) {
            this.topLimit = topLimit;
        }

        public int getProviderConcurrency() {
            return providerConcurrency;
        }

        public void setProviderConcurrency(int providerConcurrency) {
            this.providerConcurrency = providerConcurrency;
        }

        public double getAdzunaShare() {
            return adzunaShare;
        }

        public void setAdzunaShare(double adzunaShare) {
            this.adzunaShare = adzunaShare;
        }

        public double getJoobleShare() {
            return joobleShare;
        }

        public void setJoobleShare(double joobleShare) {
            this.joobleShare = joobleShare;
        }

        public double getJsearchShare() {
            return jsearchShare;
        }

        public void setJsearchShare(double jsearchShare) {
            this.jsearchShare = jsearchShare;
        }
    }
}
