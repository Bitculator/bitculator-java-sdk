import com.bitculator.ApiException;
import com.bitculator.Bitculator;
import com.bitculator.Page;
import com.bitculator.Params;
import com.bitculator.Quota;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Run with: BITCULATOR_API_KEY=... java -cp <bitculator jar + jackson jars>:examples Quickstart
 * (against local dev, add BITCULATOR_BASE_URL=http://localhost)
 *
 * Lives outside src/main on purpose — Maven ignores it; see "Run the example" in the README.
 */
public class Quickstart {

    public static void main(String[] args) {
        String baseUrl = System.getenv().getOrDefault("BITCULATOR_BASE_URL", "https://bitculator.com");
        Bitculator client = Bitculator.builder(System.getenv("BITCULATOR_API_KEY"))
                .baseUrl(baseUrl)
                .build();

        try {
            client.meta().ping();

            // Single resource — the { "data": ... } envelope is unwrapped for you.
            JsonNode bitcoin = client.coins().get("bitcoin", Params.of());
            // "63520.780763913" — a decimal string, never rounded; read with asText(),
            // parse with BigDecimal if you need math. Never asDouble().
            System.out.println("BTC price: " + bitcoin.get("price").asText());

            // Top 5 coins by marketcap.
            Page top = client.coins().list(Params.of().set("per_page", 5).set("sort", "-marketcap"));
            for (JsonNode coin : top.data()) {
                System.out.println("#" + coin.get("rank").asInt() + " "
                        + coin.get("symbol").asText() + " " + coin.get("price").asText());
            }

            // Auto-paginate the first ~40 gainers (iterating a Page walks every page).
            int count = 0;
            for (JsonNode coin : client.coins().gainers(Params.of().set("per_page", 20))) {
                System.out.println("gainer: " + coin.get("symbol").asText() + " " + coin.path("change_24h").asText());
                if (++count >= 40) {
                    break;
                }
            }

            Quota quota = client.quota();
            System.out.println("quota: " + quota.remaining() + "/" + quota.limit());
        } catch (ApiException e) {
            System.out.println("API " + e.status() + ": " + e.code() + " " + e.getMessage());
        }
    }
}
