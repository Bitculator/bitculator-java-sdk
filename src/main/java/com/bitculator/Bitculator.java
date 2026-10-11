package com.bitculator;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;

/**
 * The Bitculator Data API client.
 *
 * <pre>{@code
 * Bitculator client = new Bitculator("your-api-key");
 * JsonNode bitcoin = client.coins().get("bitcoin", Params.of());
 * }</pre>
 *
 * Each API group is a method on the client: {@code client.coins()},
 * {@code client.prices()}, {@code client.exchanges()}, and so on.
 */
public final class Bitculator {

    public static final String VERSION = "1.1.0";

    private final HttpClient http;
    private final Coins coins;
    private final Prices prices;
    private final Markets markets;
    private final Exchanges exchanges;
    private final Wallets wallets;
    private final GlobalMarket globalMarket;
    private final Sentiment sentiment;
    private final Indicators indicators;
    private final Liquidations liquidations;
    private final Conversion conversion;
    private final Calculators calculators;
    private final Editorial editorial;
    private final Alarms alarms;
    private final Webhooks webhooks;
    private final Meta meta;

    /** Create a client with default configuration. */
    public Bitculator(String apiKey) {
        this(builder(apiKey));
    }

    private Bitculator(Builder b) {
        this.http = new HttpClient(b.apiKey, b.baseUrl, b.timeout, b.maxRetries, b.userAgent, b.httpClient);
        this.coins = new Coins(http);
        this.prices = new Prices(http);
        this.markets = new Markets(http);
        this.exchanges = new Exchanges(http);
        this.wallets = new Wallets(http);
        this.globalMarket = new GlobalMarket(http);
        this.sentiment = new Sentiment(http);
        this.indicators = new Indicators(http);
        this.liquidations = new Liquidations(http);
        this.conversion = new Conversion(http);
        this.calculators = new Calculators(http);
        this.editorial = new Editorial(http);
        this.alarms = new Alarms(http);
        this.webhooks = new Webhooks(http);
        this.meta = new Meta(http);
    }

    /** Start configuring a client. */
    public static Builder builder(String apiKey) {
        return new Builder(apiKey);
    }

    public Coins coins() {
        return coins;
    }

    public Prices prices() {
        return prices;
    }

    public Markets markets() {
        return markets;
    }

    public Exchanges exchanges() {
        return exchanges;
    }

    public Wallets wallets() {
        return wallets;
    }

    public GlobalMarket globalMarket() {
        return globalMarket;
    }

    public Sentiment sentiment() {
        return sentiment;
    }

    public Indicators indicators() {
        return indicators;
    }

    public Liquidations liquidations() {
        return liquidations;
    }

    public Conversion conversion() {
        return conversion;
    }

    public Calculators calculators() {
        return calculators;
    }

    public Editorial editorial() {
        return editorial;
    }

    public Alarms alarms() {
        return alarms;
    }

    public Webhooks webhooks() {
        return webhooks;
    }

    public Meta meta() {
        return meta;
    }

    /** Quota (X-Quota-*) from the most recent response, or null before the first call. */
    public Quota quota() {
        return http.quota();
    }

    /** The Jackson mapper — handy for converting a {@code JsonNode} into your own type. */
    public ObjectMapper mapper() {
        return http.mapper();
    }

    /** Builder for {@link Bitculator}. */
    public static final class Builder {
        private final String apiKey;
        private String baseUrl = "https://bitculator.com";
        private Duration timeout = Duration.ofSeconds(30);
        private int maxRetries = 2;
        private String userAgent = "bitculator-java/" + VERSION;
        private java.net.http.HttpClient httpClient;

        private Builder(String apiKey) {
            if (apiKey == null || apiKey.isEmpty()) {
                throw new BitculatorException("A Data API key is required: new Bitculator(apiKey)");
            }
            this.apiKey = apiKey;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl.replaceAll("/+$", "");
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        /**
         * Bring your own {@link java.net.http.HttpClient} — for proxies, custom SSL
         * contexts, or executors. When unset, the SDK builds a default one using
         * the configured timeout as its connect timeout.
         */
        public Builder httpClient(java.net.http.HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Bitculator build() {
            return new Bitculator(this);
        }
    }
}
