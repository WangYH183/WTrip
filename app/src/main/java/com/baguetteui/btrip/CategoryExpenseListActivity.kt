package com.baguetteui.btrip

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baguetteui.btrip.data.TripRepository
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class CategoryExpenseListActivity : AppCompatActivity() {

    private lateinit var repo: TripRepository

    private var tripId: Long = -1L
    private lateinit var mainType: MainType

    private val items: MutableList<Expense> = mutableListOf()
    private lateinit var adapter: ExpenseTileAdapter

    private lateinit var tvEmpty: TextView

    private val detailLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) { /* Flow 自动刷新 */ }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_expense_list)

        repo = TripRepository(this)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        val tvHeader = findViewById<TextView>(R.id.tvHeader)
        val tvSum = findViewById<TextView>(R.id.tvSum)
        val rv = findViewById<RecyclerView>(R.id.rvTiles)
        tvEmpty = findViewById(R.id.tvEmpty)

        topAppBar.setNavigationOnClickListener { finish() }
        topAppBar.title = ""
        topAppBar.inflateMenu(R.menu.menu_category_expense_list)
        topAppBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_add -> {
                    openCreateExpenseWithDefaultType()
                    true
                }
                else -> false
            }
        }

        tripId = intent.getLongExtra(EXTRA_TRIP_ID, -1L)
        mainType = parseMainTypeExtra(intent.getStringExtra(EXTRA_MAIN_TYPE))
        tvHeader.text = mainType.displayName

        adapter = ExpenseTileAdapter(
            items = items,
            onClick = { expense ->
                val i = Intent(this, ExpenseDetailActivity::class.java).apply {
                    putExtra("trip_id", tripId)
                    putExtra("expense_id", expense.id)
                    putExtra("expense_obj", expense)
                }
                detailLauncher.launch(i)
            },
            onLongPress = { expense, anchor ->
                showItemMenu(expense, anchor)
            }
        )

        rv.layoutManager = GridLayoutManager(this, 2)
        rv.adapter = adapter

        lifecycleScope.launch {
            repo.observeExpenses(tripId).collect { list ->
                val filtered = list.filter { it.mainType == mainType }
                items.clear()
                items.addAll(filtered)
                adapter.submit()

                val sum = filtered.sumOf { it.amountCny }
                tvSum.text = "合计：${formatCny(sum)}"

                tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
                rv.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun showItemMenu(expense: Expense, anchor: View) {
        val menu = PopupMenu(this, anchor)
        menu.menuInflater.inflate(R.menu.menu_expense_item, menu.menu)
        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete -> {
                    ConfirmDialogs.confirmDelete(this, "记录", expense.title) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            ImageCompressor.deleteIfLocalFile(expense.photoUri)
                            repo.deleteExpense(expense.id)
                        }
                    }
                    true
                }
                else -> false
            }
        }
        menu.show()
    }

    private fun openCreateExpenseWithDefaultType() {
        val i = Intent(this, ExpenseDetailActivity::class.java).apply {
            putExtra("trip_id", tripId)
            putExtra("expense_id", -1L)
            putExtra(EXTRA_DEFAULT_MAIN_TYPE, mainType.name)
        }
        detailLauncher.launch(i)
    }

    private fun parseMainTypeExtra(value: String?): MainType {
        return runCatching { MainType.valueOf(value ?: "") }.getOrDefault(MainType.EXPERIENCE)
    }

    private fun formatCny(amount: Int): String {
        val nf = NumberFormat.getNumberInstance(Locale.CHINA)
        return "¥${nf.format(amount)}"
    }

    companion object {
        const val EXTRA_TRIP_ID = "extra_trip_id"
        const val EXTRA_MAIN_TYPE = "extra_main_type"
        const val EXTRA_DEFAULT_MAIN_TYPE = "extra_default_main_type"
    }
}