package com.baguetteui.wtrip

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.baguetteui.wtrip.data.TripRepository
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class TripDetailActivity : AppCompatActivity() {

    private lateinit var repo: TripRepository
    private var tripId: Long = -1L
    private var tripTitle: String = ""
    private var tripDays: Int = 0

    private lateinit var pieChart: PieChart
    private lateinit var tvTotalAmount: TextView

    private lateinit var tvSumTransport: TextView
    private lateinit var tvSumFood: TextView
    private lateinit var tvSumHotel: TextView
    private lateinit var tvSumExperience: TextView

    private var changed: Boolean = false

    private val detailLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) changed = true
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_detail)

        repo = TripRepository(this)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        val btnAddExpense = findViewById<MaterialButton>(R.id.btnAddExpense)

        val tvTripTitle = findViewById<TextView>(R.id.tvTripTitle)
        val tvTripDays = findViewById<TextView>(R.id.tvTripDays)

        pieChart = findViewById(R.id.pieChart)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)

        tvSumTransport = findViewById(R.id.tvSumTransport)
        tvSumFood = findViewById(R.id.tvSumFood)
        tvSumHotel = findViewById(R.id.tvSumHotel)
        tvSumExperience = findViewById(R.id.tvSumExperience)

        val cardTransport = findViewById<MaterialCardView>(R.id.cardTransport)
        val cardFood = findViewById<MaterialCardView>(R.id.cardFood)
        val cardHotel = findViewById<MaterialCardView>(R.id.cardHotel)
        val cardExperience = findViewById<MaterialCardView>(R.id.cardExperience)

        topAppBar.setNavigationOnClickListener { finish() }

        tripId = intent.getLongExtra("trip_id", -1L)
        tripTitle = intent.getStringExtra("title").orEmpty()
        tripDays = intent.getIntExtra("days", 0)

        tvTripTitle.text = tripTitle
        tvTripDays.text = "${tripDays}天"

        // ✅ 最优：新增记录时先选分类，再进入详情页（减少用户操作）
        btnAddExpense.setOnClickListener { showPickMainTypeAndCreate() }

        cardTransport.setOnClickListener { openCategoryList(MainType.TRANSPORT) }
        cardFood.setOnClickListener { openCategoryList(MainType.FOOD) }
        cardHotel.setOnClickListener { openCategoryList(MainType.HOTEL) }
        cardExperience.setOnClickListener { openCategoryList(MainType.EXPERIENCE) }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) { repo.getTrip(tripId) }
        }

        initPieChart()

        lifecycleScope.launch {
            repo.observeMainTypeTotals(tripId).collect { (map, total) ->
                tvSumTransport.text = formatCny(map[MainType.TRANSPORT] ?: 0)
                tvSumFood.text = formatCny(map[MainType.FOOD] ?: 0)
                tvSumHotel.text = formatCny(map[MainType.HOTEL] ?: 0)
                tvSumExperience.text = formatCny(map[MainType.EXPERIENCE] ?: 0)
                tvTotalAmount.text = "总计：${formatCny(total)}"
            }
        }

        lifecycleScope.launch {
            repo.observePaymentMethodTotals(tripId).collect { map ->
                updatePieChart(map)
            }
        }
    }

    override fun finish() {
        if (changed) setResult(Activity.RESULT_OK)
        super.finish()
    }

    private fun showPickMainTypeAndCreate() {
        val types = MainType.entries.toTypedArray()
        val labels = types.map { it.displayName }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("选择分类")
            .setItems(labels) { _, which ->
                openExpenseDetailForCreate(types[which])
            }
            .show()
    }

    private fun openExpenseDetailForCreate(type: MainType) {
        val i = Intent(this, ExpenseDetailActivity::class.java).apply {
            putExtra("trip_id", tripId)
            putExtra("expense_id", -1L)
            putExtra(CategoryExpenseListActivity.EXTRA_DEFAULT_MAIN_TYPE, type.name)
        }
        detailLauncher.launch(i)
    }

    private fun openCategoryList(type: MainType) {
        val i = Intent(this, CategoryExpenseListActivity::class.java).apply {
            putExtra(CategoryExpenseListActivity.EXTRA_TRIP_ID, tripId)
            putExtra(CategoryExpenseListActivity.EXTRA_MAIN_TYPE, type.name)
        }
        startActivity(i)
    }

    private fun updatePieChart(byPay: Map<PaymentMethod, Int>) {
        val entries = mutableListOf<PieEntry>()
        for (pay in PaymentMethod.entries) {
            val amount = byPay[pay] ?: 0
            if (amount > 0) entries.add(PieEntry(amount.toFloat(), pay.displayName))
        }

        val ds = PieDataSet(entries, "").apply {
            colors = listOf(
                ContextCompat.getColor(this@TripDetailActivity, android.R.color.holo_blue_bright),
                ContextCompat.getColor(this@TripDetailActivity, android.R.color.holo_green_light),
                ContextCompat.getColor(this@TripDetailActivity, android.R.color.holo_orange_light),
                ContextCompat.getColor(this@TripDetailActivity, android.R.color.holo_red_light),
                ContextCompat.getColor(this@TripDetailActivity, android.R.color.holo_purple),
            )
            valueTextColor = ContextCompat.getColor(this@TripDetailActivity, android.R.color.black)
            valueTextSize = 14f
            sliceSpace = 4f
            selectionShift = 8f
        }

        pieChart.data = PieData(ds)
        pieChart.centerText = ""
        pieChart.setEntryLabelColor(ContextCompat.getColor(this, android.R.color.black))
        pieChart.setEntryLabelTextSize(14f)
        pieChart.description.isEnabled = false
        pieChart.setDrawEntryLabels(true)
        pieChart.holeRadius = 32f
        pieChart.transparentCircleRadius = 38f
        pieChart.invalidate()
    }

    private fun initPieChart() {
        pieChart.setUsePercentValues(false)
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(android.graphics.Color.TRANSPARENT)
        pieChart.setDrawEntryLabels(true)
        pieChart.setNoDataText("暂无记录")
        pieChart.setNoDataTextColor(android.graphics.Color.GRAY)
        pieChart.legend.apply {
            verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            orientation = Legend.LegendOrientation.HORIZONTAL
            isWordWrapEnabled = true
            textSize = 14f
            form = Legend.LegendForm.CIRCLE
            formSize = 12f
            xEntrySpace = 12f
        }
    }

    private fun formatCny(amount: Int): String {
        val nf = NumberFormat.getNumberInstance(Locale.CHINA)
        return "¥${nf.format(amount)}"
    }
}