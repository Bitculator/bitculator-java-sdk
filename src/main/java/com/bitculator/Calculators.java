package com.bitculator;

import com.fasterxml.jackson.databind.JsonNode;

/** Financial calculators. */
public final class Calculators extends Resource {

    Calculators(HttpClient http) {
        super(http);
    }

    /** Dollar-cost-averaging calculator. Params: slug, amount, interval, start, end, series. */
    public JsonNode dca(Params params) {
        return http.get("/api/v1/calculators/dca", params);
    }

    /** Profit / loss calculator. Params: slug, amount, buy_date, sell_date, buy_fee, sell_fee. */
    public JsonNode profitLoss(Params params) {
        return http.get("/api/v1/calculators/profit-loss", params);
    }

    /** Compound interest calculator. Params: principal, rate, duration, duration_unit,
     * compound_frequency, contribution, contribution_frequency. {@code rate} is the annual
     * nominal rate in %; each compounding period earns {@code rate} divided by the periods per year. */
    public JsonNode compoundInterest(Params params) {
        return http.get("/api/v1/calculators/compound-interest", params);
    }

    /** Loan-vs-sell calculator. Params: slug, crypto_amount, needed_cash, term_months,
     * interest_rate, ltv, expected_growth, tax_rate. */
    public JsonNode loan(Params params) {
        return http.get("/api/v1/calculators/loan", params);
    }

    /** Staking rewards calculator. Params: amount, period, period_unit, apy, compound_frequency, commission. */
    public JsonNode staking(Params params) {
        return http.get("/api/v1/calculators/staking", params);
    }
}
