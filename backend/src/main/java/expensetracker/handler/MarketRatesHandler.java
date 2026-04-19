package expensetracker.handler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler for fetching live gold and silver market rates in INR.
 * Uses GoldAPI.io (free tier: 100 requests/month) with in-memory caching.
 * Falls back to MetalpriceAPI if primary fails.
 *
 * Cache TTL: 5 minutes (rates don't change every second).
 */
public class MarketRatesHandler {
    private static final Logger log = LoggerFactory.getLogger(MarketRatesHandler.class);
    private static final Gson gson = new Gson();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Cache
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes
    private static volatile Map<String, Object> cachedRates = null;
    private static volatile long cacheTimestamp = 0;

    private final String goldApiKey;
    private final String metalPriceApiKey;

    public MarketRatesHandler() {
        this.goldApiKey = System.getenv().getOrDefault("GOLD_API_KEY", "");
        this.metalPriceApiKey = System.getenv().getOrDefault("METAL_PRICE_API_KEY", "");

        if (goldApiKey.isEmpty() && metalPriceApiKey.isEmpty()) {
            log.warn("No GOLD_API_KEY or METAL_PRICE_API_KEY set. Market rates will use fallback data.");
        }
    }

    /**
     * GET /api/market-rates
     * Returns live gold and silver rates in INR.
     */
    public void getRates(Context ctx) {
        try {
            // Check cache first
            if (cachedRates != null && (System.currentTimeMillis() - cacheTimestamp) < CACHE_TTL_MS) {
                ctx.json(cachedRates);
                return;
            }

            Map<String, Object> rates = null;

            // Try GoldAPI.io first
            if (!goldApiKey.isEmpty()) {
                rates = fetchFromGoldApi();
            }

            // Try MetalpriceAPI as fallback
            if (rates == null && !metalPriceApiKey.isEmpty()) {
                rates = fetchFromMetalPriceApi();
            }

            // Use fallback static rates if all APIs fail
            if (rates == null) {
                rates = getFallbackRates();
            }

            // Cache the result
            cachedRates = rates;
            cacheTimestamp = System.currentTimeMillis();

            ctx.json(rates);
        } catch (Exception e) {
            log.error("Error fetching market rates: {}", e.getMessage());
            ctx.json(getFallbackRates());
        }
    }

    /**
     * Fetch from GoldAPI.io
     * Free tier: 100 requests/month
     * Endpoint: GET https://www.goldapi.io/api/XAU/INR
     */
    private Map<String, Object> fetchFromGoldApi() {
        try {
            // Fetch gold price
            HttpRequest goldReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.goldapi.io/api/XAU/INR"))
                    .header("x-access-token", goldApiKey)
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> goldResp = httpClient.send(goldReq, HttpResponse.BodyHandlers.ofString());

            if (goldResp.statusCode() != 200) {
                log.warn("GoldAPI returned status {} for gold", goldResp.statusCode());
                return null;
            }

            JsonObject goldJson = JsonParser.parseString(goldResp.body()).getAsJsonObject();
            double goldPricePerOz = goldJson.get("price").getAsDouble();
            double goldPricePerGram = goldJson.get("price_gram_24k").getAsDouble();
            double goldPrice22k = goldJson.has("price_gram_22k") ? goldJson.get("price_gram_22k").getAsDouble() : goldPricePerGram * 0.9167;
            double goldPrice18k = goldJson.has("price_gram_18k") ? goldJson.get("price_gram_18k").getAsDouble() : goldPricePerGram * 0.75;

            // Fetch silver price
            HttpRequest silverReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.goldapi.io/api/XAG/INR"))
                    .header("x-access-token", goldApiKey)
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> silverResp = httpClient.send(silverReq, HttpResponse.BodyHandlers.ofString());

            double silverPricePerGram = 0;
            double silverPricePerKg = 0;
            if (silverResp.statusCode() == 200) {
                JsonObject silverJson = JsonParser.parseString(silverResp.body()).getAsJsonObject();
                silverPricePerGram = silverJson.has("price_gram_24k") ? silverJson.get("price_gram_24k").getAsDouble() : silverJson.get("price").getAsDouble() / 31.1035;
                silverPricePerKg = silverPricePerGram * 1000;
            }

            return buildRatesResponse(goldPricePerGram, goldPrice22k, goldPrice18k,
                    silverPricePerGram, silverPricePerKg, "GoldAPI.io", false);

        } catch (Exception e) {
            log.error("GoldAPI fetch failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fetch from MetalpriceAPI
     * Free tier: 100 requests/month
     */
    private Map<String, Object> fetchFromMetalPriceApi() {
        try {
            String url = "https://api.metalpriceapi.com/v1/latest?api_key=" + metalPriceApiKey + "&base=INR&currencies=XAU,XAG";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                log.warn("MetalpriceAPI returned status {}", resp.statusCode());
                return null;
            }

            JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
            JsonObject rates = json.getAsJsonObject("rates");

            // MetalpriceAPI returns INR per troy oz (inverted)
            double goldInrPerOz = 1.0 / rates.get("XAU").getAsDouble();
            double silverInrPerOz = 1.0 / rates.get("XAG").getAsDouble();

            double gold24kPerGram = goldInrPerOz / 31.1035;
            double gold22kPerGram = gold24kPerGram * 0.9167;
            double gold18kPerGram = gold24kPerGram * 0.75;
            double silverPerGram = silverInrPerOz / 31.1035;
            double silverPerKg = silverPerGram * 1000;

            return buildRatesResponse(gold24kPerGram, gold22kPerGram, gold18kPerGram,
                    silverPerGram, silverPerKg, "MetalpriceAPI", false);

        } catch (Exception e) {
            log.error("MetalpriceAPI fetch failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fallback rates based on approximate current Indian market prices (March 2026).
     * Reference: goodreturns.in/gold-rates
     */
    private Map<String, Object> getFallbackRates() {
        return buildRatesResponse(
                14456.00,  // Gold 24K per gram
                13250.00,  // Gold 22K per gram
                11050.00,  // Gold 18K per gram
                235.00,    // Silver per gram
                235000.00, // Silver per kg
                "Offline (cached)",
                true
        );
    }

    private Map<String, Object> buildRatesResponse(double gold24k, double gold22k, double gold18k,
                                                    double silverPerGram, double silverPerKg,
                                                    String source, boolean isFallback) {
        Map<String, Object> response = new ConcurrentHashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("currency", "INR");
        response.put("source", source);
        response.put("isFallback", isFallback);

        Map<String, Object> gold = new ConcurrentHashMap<>();
        gold.put("gold24kPerGram", Math.round(gold24k * 100.0) / 100.0);
        gold.put("gold22kPerGram", Math.round(gold22k * 100.0) / 100.0);
        gold.put("gold18kPerGram", Math.round(gold18k * 100.0) / 100.0);
        gold.put("gold24kPer10Gram", Math.round(gold24k * 10 * 100.0) / 100.0);
        gold.put("gold22kPer10Gram", Math.round(gold22k * 10 * 100.0) / 100.0);
        response.put("gold", gold);

        Map<String, Object> silver = new ConcurrentHashMap<>();
        silver.put("silverPerGram", Math.round(silverPerGram * 100.0) / 100.0);
        silver.put("silverPerKg", Math.round(silverPerKg * 100.0) / 100.0);
        silver.put("silverPer100Gram", Math.round(silverPerGram * 100 * 100.0) / 100.0);
        response.put("silver", silver);

        return response;
    }
}
