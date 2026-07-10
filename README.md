# Bitculator Data API — Java SDK

[bitculator.com](https://bitculator.com) · [API documentation](https://bitculator.com/en/documentation/api/v1) · [Get an API key](https://bitculator.com/user/developer/api)
[Maven Central — `com.bitculator:bitculator`](https://central.sonatype.com/artifact/com.bitculator/bitculator) · [GitHub](https://github.com/Bitculator/bitculator-java-sdk)

The official Java SDK for the [Bitculator Data API](https://bitculator.com).
Uses the JDK's `java.net.http` client + Jackson; Java 17+.

> Keep your API key server-side. It is a Bearer token with the `data-api` ability
> and is not meant for client-side embedding.

## Install

```xml
<dependency>
  <groupId>com.bitculator</groupId>
  <artifactId>bitculator</artifactId>
  <version>1.0.1</version>
</dependency>
```

## Quick start

```java
import com.bitculator.Bitculator;
import com.bitculator.Params;
import com.fasterxml.jackson.databind.JsonNode;

Bitculator client = new Bitculator(System.getenv("BITCULATOR_API_KEY"));

// Single resource — the { "data": ... } envelope is unwrapped; rows are JsonNodes.
JsonNode bitcoin = client.coins().get("bitcoin", Params.of());
System.out.println(bitcoin.get("price").asText()); // "63520.780763913"

// A paginated list.
var page = client.coins().list(Params.of().set("per_page", 50).set("sort", "-marketcap"));
System.out.println(page.meta().total() + " " + page.data().size());
```

## Run the example

`examples/Quickstart.java` is a runnable end-to-end tour. It lives outside
`src/main`, so Maven ignores it — compile and run it directly with the built
jar + Jackson on the classpath:

```bash
mvn -q package
CP="target/bitculator-1.0.1.jar:$(ls ~/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.17.3/jackson-databind-2.17.3.jar ~/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.17.3/jackson-core-2.17.3.jar ~/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.17.3/jackson-annotations-2.17.3.jar | tr '\n' ':')"
javac -cp "$CP" examples/Quickstart.java
BITCULATOR_API_KEY=... java -cp "$CP:examples" Quickstart
```

## Prices are decimal strings

Prices, rates, and supplies come back as **strings** (e.g. `"63520.780763913"`)
to preserve full precision. Read them with `.asText()` (and parse with
`BigDecimal` if you need math) — never `.asDouble()`, which loses precision.

## Typed models

Rows are Jackson `JsonNode`s. Convert to your own class with the exposed mapper:

```java
record Coin(String slug, String symbol, String price) {}
Coin btc = client.mapper().treeToValue(client.coins().get("bitcoin", Params.of()), Coin.class);
```

## Pagination

Paginated methods return a `Page`. Read one page, or iterate to walk them all:

```java
// One page at a time.
Page page = client.exchanges().list(Params.of().set("per_page", 100));
while (page != null) {
    for (JsonNode exchange : page.data()) System.out.println(exchange.get("name").asText());
    page = page.nextPage();
}

// Or auto-paginate across every page (a Page is Iterable).
for (JsonNode coin : client.coins().list(Params.of().set("sort", "-marketcap"))) {
    System.out.println(coin.get("symbol").asText());
}
```

## Errors

Everything throws unchecked exceptions extending `BitculatorException`. HTTP
failures throw `ApiException` carrying the `{ error: { code, message, details } }`
envelope; branch on the `isXxx()` predicates.

```java
try {
    client.coins().list(Params.of().set("per_page", 9999)); // over the plan cap -> 422
} catch (ApiException e) {
    if (e.isValidation()) System.out.println(e.code() + " " + e.details());
    else if (e.isRateLimited()) System.out.println("retry after " + e.retryAfter());
    else System.out.println(e.status() + " " + e.getMessage());
}
```

`isAuth` (401) · `isPermission` (403) · `isNotFound` (404) · `isValidation` (422) ·
`isRateLimited` (429) · `isServer` (5xx). Timeouts throw `TimeoutException`;
network failures throw `ConnectionException`.

## Quota

Every response carries `X-Quota-*` headers, surfaced on the client after each call:

```java
client.coins().list(Params.of());
Quota q = client.quota(); // { limit, remaining, reset, raw }
```

## Configuration

```java
Bitculator client = Bitculator.builder("your-api-key")
    .baseUrl("https://bitculator.com")        // default
    .timeout(java.time.Duration.ofSeconds(30)) // default
    .maxRetries(2)                            // auto-retry 429/5xx/network; 0 to disable
    .httpClient(myJdkHttpClient)              // optional: bring your own java.net.http.HttpClient
                                              // (proxies, custom SSL contexts, executors)
    .build();
```

## Resources

`coins` · `prices` · `markets` · `exchanges` · `wallets` · `globalMarket` ·
`sentiment` · `indicators` · `liquidations` · `conversion` · `calculators` ·
`editorial` · `alarms` · `webhooks` · `meta`

Full endpoint reference: <https://bitculator.com/en/documentation/api/v1>.

## License

MIT
