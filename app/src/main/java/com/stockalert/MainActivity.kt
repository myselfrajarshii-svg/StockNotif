package com.stockalert

import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private val scope = MainScope()

    // UI refs
    private lateinit var switchEnabled : SwitchMaterial
    private lateinit var chipGroupStocks: ChipGroup
    private lateinit var chipGroupTimes : ChipGroup
    private lateinit var btnAddStock    : Button
    private lateinit var btnAddTime     : Button
    private lateinit var btnTestNow     : Button
    private lateinit var tvStatus       : TextView
    private lateinit var fabSave        : FloatingActionButton

    // Working lists (edited in-memory, saved on FAB press)
    private val stocks = mutableListOf<String>()
    private val times  = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        loadData()
        setupListeners()
        requestPermissions()
        NotificationHelper.createChannel(this)
        updateStatus()
    }

    private fun bindViews() {
        switchEnabled  = findViewById(R.id.switchEnabled)
        chipGroupStocks= findViewById(R.id.chipGroupStocks)
        chipGroupTimes = findViewById(R.id.chipGroupTimes)
        btnAddStock    = findViewById(R.id.btnAddStock)
        btnAddTime     = findViewById(R.id.btnAddTime)
        btnTestNow     = findViewById(R.id.btnTestNow)
        tvStatus       = findViewById(R.id.tvStatus)
        fabSave        = findViewById(R.id.fabSave)
    }

    private fun loadData() {
        stocks.clear(); stocks.addAll(PrefsManager.getStocks(this))
        times.clear();  times.addAll(PrefsManager.getTimes(this))
        switchEnabled.isChecked = PrefsManager.isEnabled(this)
        refreshChips()
    }

    private fun setupListeners() {
        switchEnabled.setOnCheckedChangeListener { _, checked ->
            PrefsManager.setEnabled(this, checked)
            if (checked) AlarmScheduler.scheduleAll(this)
            else         AlarmScheduler.cancelAll(this)
            updateStatus()
        }

        btnAddStock.setOnClickListener { showAddStockDialog() }
        btnAddTime.setOnClickListener  { showTimePicker() }

        fabSave.setOnClickListener {
            PrefsManager.setStocks(this, stocks)
            PrefsManager.setTimes(this,  times)
            AlarmScheduler.scheduleAll(this)
            updateStatus()
            Snackbar.make(fabSave, "✅ Saved & alarms scheduled", Snackbar.LENGTH_SHORT).show()
        }

        btnTestNow.setOnClickListener { testFetchNow() }
    }

    // ── Chips ────────────────────────────────────────────────

    private fun refreshChips() {
        chipGroupStocks.removeAllViews()
        stocks.forEach { sym -> chipGroupStocks.addView(makeChip(sym) { stocks.remove(sym); refreshChips() }) }

        chipGroupTimes.removeAllViews()
        times.forEach { t -> chipGroupTimes.addView(makeChip(t) { times.remove(t); refreshChips() }) }
    }

    private fun makeChip(label: String, onDelete: () -> Unit): Chip {
        return Chip(this).apply {
            text = label
            isCloseIconVisible = true
            setOnCloseIconClickListener { onDelete() }
        }
    }

    // ── Dialogs ──────────────────────────────────────────────

    private fun showAddStockDialog() {
        val et = EditText(this).apply {
            hint = "e.g. NIFTYBEES.NS"
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(this)
            .setTitle("Add Stock Symbol")
            .setMessage("Use Yahoo Finance format: SYMBOL.NS for NSE stocks.")
            .setView(et)
            .setPositiveButton("Add") { _, _ ->
                val sym = et.text.toString().trim().uppercase()
                if (sym.isNotEmpty() && !stocks.contains(sym)) {
                    stocks.add(sym)
                    refreshChips()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTimePicker() {
        TimePickerDialog(this, { _, hour, minute ->
            val t = "%02d:%02d".format(hour, minute)
            if (!times.contains(t)) {
                times.add(t)
                times.sort()
                refreshChips()
            }
        }, 10, 30, true).show()
    }

    // ── Test fetch ───────────────────────────────────────────

    private fun testFetchNow() {
        tvStatus.text = "⏳ Fetching prices..."
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                StockFetcher.fetchPrices(stocks)
            }
            val lines = stocks.map { sym ->
                StockFetcher.formatLine(sym, result[sym.trim().uppercase()])
            }
            NotificationHelper.postPriceAlert(
                context   = this@MainActivity,
                timeLabel = "Test",
                lines     = lines
            )
            tvStatus.text = "✅ Test notification sent!"
        }
    }

    // ── Status ───────────────────────────────────────────────

    private fun updateStatus() {
        if (!PrefsManager.isEnabled(this)) {
            tvStatus.text = "⏸ Alerts are disabled"
            return
        }
        val timeList = PrefsManager.getTimes(this)
        tvStatus.text = if (timeList.isEmpty()) "⚠️ No alert times set — tap Save"
                        else "✅ Active — alerts at: ${timeList.joinToString(", ")} IST"
    }

    // ── Permissions ──────────────────────────────────────────

    private fun requestPermissions() {
        // POST_NOTIFICATIONS (Android 13+)
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        // SCHEDULE_EXACT_ALARM (Android 12+)
        if (Build.VERSION.SDK_INT >= 31) {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                AlertDialog.Builder(this)
                    .setTitle("Permission Required")
                    .setMessage("To send alerts at exact times, please enable 'Alarms & Reminders' for Stock Alert.")
                    .setPositiveButton("Open Settings") { _, _ ->
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:$packageName")))
                    }
                    .setNegativeButton("Later", null)
                    .show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
