package com.stockalert

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Fetches real-time stock prices from Yahoo Finance.
 * No API key required — uses the public quote endpoint.
 *
 * NSE symbols use the ".NS" suffix on Yahoo Finance:
 *   NIFTYBEES → NIFTYBEES.NS
 *   BANKBEES  → BANKBEES.NS
 */
object StockFetcher {

    data class StockPrice(
        val symbol: String,
        val price: Double,
        val change: Double,        // absolute change
        val changePct: Double,     // percentage change
        val currency: String = "INR"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Fetches prices for a list of symbols.
     * Returns a map of symbol → StockPrice (or null if fetch failed).
     * This is a BLOCKING call — run on a background thread.
     */
    fun fetchPrices(symbols: List<String>): Map<String, StockPrice?> {
        val result = mutableMapOf<String, StockPrice?>()

        if (symbols.isEmpty()) return result

        // Yahoo Finance v7 quote endpoint — free, no key needed
        val joined = symbols.joinToString("%2C") { it.trim().uppercase() }
        val url = "https://query1.finance.yahoo.com/v7/finance/quote?symbols=$joined&fields=regularMarketPrice,regularMarketChange,regularMarketChangePercent,currency"

        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return symbols.associateWith { null }

            val json    = JSONObject(body)
            val quoteResponse = json.getJSONObject("quoteResponse")
            val resultArr    = quoteResponse.getJSONArray("result")

            for (i in 0 until resultArr.length()) {
                val q      = resultArr.getJSONObject(i)
                val sym    = q.optString("symbol", "")
                val price  = q.optDouble("regularMarketPrice", Double.NaN)
                val change = q.optDouble("regularMarketChange", 0.0)
                val pct    = q.optDouble("regularMarketChangePercent", 0.0)
                val curr   = q.optString("currency", "INR")

                if (sym.isNotEmpty() && !price.isNaN()) {
                    result[sym] = StockPrice(sym, price, change, pct, curr)
                } else {
                    result[sym] = null
                }
            }

            // Mark any symbols that weren't in the response as null
            symbols.forEach { s ->
                val key = s.trim().uppercase()
                if (!result.containsKey(key)) result[key] = null
            }

            result
        } catch (e: Exception) {
            symbols.associateWith { null }
        }
    }

    /** Formats a price result into a readable notification line */
    fun formatLine(symbol: String, price: StockPrice?): String {
        return if (price == null) {
            "• $symbol — unavailable"
        } else {
            val sign   = if (price.change >= 0) "+" else ""
            val pctFmt = "%.2f".format(price.changePct)
            val priceFmt = "%.2f".format(price.price)
            "• ${symbol.removeSuffix(".NS")}  ₹$priceFmt  ($sign${sign}${pctFmt}%)"
        }
    }
}
