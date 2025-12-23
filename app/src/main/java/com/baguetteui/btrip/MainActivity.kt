package com.baguetteui.btrip

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baguetteui.btrip.data.TripRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var repo: TripRepository

    private val trips = mutableListOf<Trip>()
    private val expenseSummaryCache: MutableMap<Long, Pair<Int, Int?>> = mutableMapOf()

    private lateinit var adapter: TripAdapter

    private val createTripLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val data = result.data ?: return@registerForActivityResult

            val id = data.getLongExtra("trip_id", -1L)
            val title = data.getStringExtra("trip_title").orEmpty()
            val days = data.getIntExtra("trip_days", 0)
            if (id <= 0L || title.isBlank() || days <= 0) return@registerForActivityResult

            val newTrip = Trip(id = id, title = title, days = days)
            lifecycleScope.launch(Dispatchers.IO) { repo.upsertTrip(newTrip) }
        }

    private val detailLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _: ActivityResult ->
            // Flow 自动更新，无需手动刷新
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repo = TripRepository(this)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        val btnAddTrip = findViewById<MaterialButton>(R.id.btnAddTrip)
        val rvTrips = findViewById<RecyclerView>(R.id.rvTrips)

        topAppBar.setNavigationOnClickListener { }

        adapter = TripAdapter(
            items = trips,
            getSummary = { trip -> expenseSummaryCache[trip.id] ?: (0 to null) },
            onClick = { trip ->
                val i = Intent(this, TripDetailActivity::class.java).apply {
                    putExtra("trip_id", trip.id)
                    putExtra("title", trip.title)
                    putExtra("days", trip.days)
                }
                detailLauncher.launch(i)
            },
            onLongPress = { trip, anchor ->
                showTripMenu(trip, anchor)
            }
        )

        rvTrips.layoutManager = LinearLayoutManager(this)
        rvTrips.adapter = adapter

        btnAddTrip.setOnClickListener {
            createTripLauncher.launch(Intent(this, CreateTripActivity::class.java))
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) { seedIfNeededOnce() }

            launch {
                repo.observeTrips().collect { list ->
                    trips.clear()
                    trips.addAll(list)
                    adapter.submit()
                }
            }

            launch {
                repo.observeTripSummaries().collect { summaries ->
                    expenseSummaryCache.clear()
                    expenseSummaryCache.putAll(summaries)
                    adapter.submit()
                }
            }
        }

        // ✅ 临时：把 crash.log 打到 Logcat（放在 onCreate 最后）
        CrashLogger.read(this)?.let { android.util.Log.e("CRASH_LOG", it) }
    }

    private fun showTripMenu(trip: Trip, anchor: View) {
        val menu = PopupMenu(this, anchor)
        menu.menu.add(0, 1, 0, "编辑行程")
        menu.menu.add(0, 2, 1, "删除行程")
        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    showEditTripDialog(trip)
                    true
                }
                2 -> {
                    confirmDeleteTrip(trip)
                    true
                }
                else -> false
            }
        }
        menu.show()
    }

    private fun showEditTripDialog(trip: Trip) {
        val view = layoutInflater.inflate(R.layout.dialog_two_fields, null, false)
        val til1 = view.findViewById<TextInputLayout>(R.id.tilField1)
        val til2 = view.findViewById<TextInputLayout>(R.id.tilField2)
        val et1 = view.findViewById<TextInputEditText>(R.id.etField1)
        val et2 = view.findViewById<TextInputEditText>(R.id.etField2)

        til1.hint = "目的地"
        til2.hint = "天数"
        et1.setText(trip.title)
        et2.setText(trip.days.toString())
        et2.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        val dialog = AlertDialog.Builder(this)
            .setTitle("编辑行程")
            .setView(view)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                til1.error = null
                til2.error = null

                val title = et1.text?.toString()?.trim().orEmpty()
                val days = et2.text?.toString()?.trim()?.toIntOrNull()

                var ok = true
                if (title.isBlank()) {
                    til1.error = "请输入目的地"
                    ok = false
                }
                if (days == null || days <= 0) {
                    til2.error = "请输入大于 0 的整数"
                    ok = false
                }
                if (!ok) return@setOnClickListener

                val updated = trip.copy(title = title, days = days!!)

                lifecycleScope.launch(Dispatchers.IO) {
                    repo.upsertTrip(updated)
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun confirmDeleteTrip(trip: Trip) {
        AlertDialog.Builder(this)
            .setTitle("删除行程")
            .setMessage("确定删除「${trip.title}」吗？该行程下的所有记录也会一起删除。")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    repo.deleteTrip(trip.id)
                }
            }
            .show()
    }

    private suspend fun seedIfNeededOnce() {
        val prefs = getSharedPreferences("btrip_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("seed_done", false)) return

        val existing = repo.getTripsOnce()
        if (existing.isNotEmpty()) {
            prefs.edit().putBoolean("seed_done", true).apply()
            return
        }

        val tripId = repo.createTrip(title = "武汉 · 东京", days = 4)

        repo.createExpense(
            tripId = tripId,
            title = "春秋航空（武汉-东京）",
            amountCny = 2000,
            mainType = MainType.TRANSPORT,
            paymentMethod = PaymentMethod.ALIPAY
        )

        repo.createExpense(
            tripId = tripId,
            title = "pho粉",
            amountCny = 2000,
            mainType = MainType.FOOD,
            paymentMethod = PaymentMethod.WECHAT
        )

        repo.createExpense(
            tripId = tripId,
            title = "新宿XX酒店",
            amountCny = 2000,
            mainType = MainType.HOTEL,
            paymentMethod = PaymentMethod.VISA
        )

        repo.createExpense(
            tripId = tripId,
            title = "东京塔门票",
            amountCny = 1500,
            mainType = MainType.EXPERIENCE,
            paymentMethod = PaymentMethod.CASH,
            subType = "TICKET"
        )

        repo.createExpense(
            tripId = tripId,
            title = "夜间游船项目",
            amountCny = 500,
            mainType = MainType.EXPERIENCE,
            paymentMethod = PaymentMethod.OTHER,
            subType = "OTHER"
        )

        prefs.edit().putBoolean("seed_done", true).apply()
    }
}