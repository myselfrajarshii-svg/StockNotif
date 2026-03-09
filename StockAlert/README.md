# 📈 Stock Alert — Android App

Get free push notifications with live NSE stock prices at exact IST times (e.g. 10:30 AM and 3:00 PM), every market day — no TradingView premium needed.

---

## How It Works

| Component | What it does |
|---|---|
| `AlarmScheduler` | Schedules exact daily alarms in IST using `AlarmManager` |
| `AlarmReceiver` | Fires at alarm time, checks it's a weekday, fetches prices |
| `StockFetcher` | Calls Yahoo Finance public API — free, no API key needed |
| `NotificationHelper` | Posts a rich push notification with all prices + % change |
| `BootReceiver` | Re-schedules alarms after device reboot |
| `MainActivity` | UI to configure stocks, times, and test alerts |

---

## Build Instructions (Android Studio)

### Requirements
- Android Studio Hedgehog (2023.1) or newer
- JDK 17+
- Android device or emulator running Android 8.0+ (API 26+)

### Steps
1. Open Android Studio → **File → Open** → select the `StockAlert` folder
2. Wait for Gradle sync to complete
3. Connect your Android phone via USB (enable Developer Mode + USB Debugging)
4. Click **Run ▶** (Shift+F10)
5. The app installs and opens on your phone

### First-time setup on your phone
1. Open the app
2. Grant **Notifications** permission when prompted
3. Grant **Alarms & Reminders** permission (Settings will open automatically)
4. Tap **"Test Alert Now"** to verify prices are fetching
5. Tap the **Save (✓) FAB** — alarms are now scheduled!

---

## Adding / Changing Stocks

Stocks use **Yahoo Finance symbol format**:

| NSE Stock | Yahoo Symbol |
|---|---|
| NIFTYBEES | `NIFTYBEES.NS` |
| BANKBEES | `BANKBEES.NS` |
| JUNIORBEES | `JUNIORBEES.NS` |
| GOLDBEES | `GOLDBEES.NS` |
| RELIANCE | `RELIANCE.NS` |
| TCS | `TCS.NS` |
| HDFCBANK | `HDFCBANK.NS` |
| INFY | `INFY.NS` |

---

## Notification Sample

```
📈 Stock Alert — 10:30 AM IST

• NIFTYBEES   ₹245.30  (+1.2%)
• BANKBEES    ₹412.50  (-0.5%)
• JUNIORBEES  ₹712.80  (+0.8%)
• GOLDBEES    ₹58.40   (+0.1%)
• SETFNIF50   ₹185.20  (+1.1%)
```

---

## Important Notes

- **Market days only**: Alerts are skipped on Saturdays and Sundays automatically
- **Public holidays**: Yahoo Finance returns the last traded price on holidays — the app will still notify, but the price won't change
- **Battery optimisation**: If alerts stop firing, go to Settings → Battery → find "Stock Alert" → set to "Unrestricted" or "No restrictions"
- **Android 12+**: You must grant the "Alarms & Reminders" exact alarm permission for precise scheduling
- **No API key needed**: Yahoo Finance's public endpoint is used — completely free

---

## Data Source

Prices are fetched from:
```
https://query1.finance.yahoo.com/v7/finance/quote?symbols=NIFTYBEES.NS,...
```
This is the same data Yahoo Finance shows on finance.yahoo.com — 15-minute delayed for NSE during market hours, real-time for some brokers.
