package com.baguetteui.wtrip

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.baguetteui.wtrip.data.TripRepository
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class ExpenseDetailActivity : AppCompatActivity() {

    private lateinit var repo: TripRepository

    private var tripId: Long = -1L
    private var expenseId: Long = -1L

    private var current: Expense? = null

    private lateinit var appBar: AppBarLayout
    private lateinit var topAppBar: MaterialToolbar

    private lateinit var tilTitle: TextInputLayout
    private lateinit var etTitle: TextInputEditText

    private lateinit var tilAmount: TextInputLayout
    private lateinit var etAmount: TextInputEditText

    private lateinit var tilPay: TextInputLayout
    private lateinit var etPay: MaterialAutoCompleteTextView

    private lateinit var tilMainType: TextInputLayout
    private lateinit var etMainType: MaterialAutoCompleteTextView

    private lateinit var tilReservation: TextInputLayout
    private lateinit var etReservation: TextInputEditText

    private lateinit var tilReview: TextInputLayout
    private lateinit var etReview: TextInputEditText

    private lateinit var tilEventTime: TextInputLayout
    private lateinit var etEventTime: TextInputEditText

    private lateinit var groupTransport: android.view.View
    private lateinit var tilTransportSubType: TextInputLayout
    private lateinit var etTransportSubType: MaterialAutoCompleteTextView
    private lateinit var tilRoute: TextInputLayout
    private lateinit var etRoute: TextInputEditText
    private lateinit var tilTransportPeriod: TextInputLayout
    private lateinit var etTransportPeriod: TextInputEditText
    private lateinit var tilArriveAtVenue: TextInputLayout
    private lateinit var etArriveAtVenue: TextInputEditText
    private lateinit var tilDuration: TextInputLayout
    private lateinit var etDuration: TextInputEditText

    private lateinit var tilSubType: TextInputLayout
    private lateinit var etSubType: MaterialAutoCompleteTextView

    private lateinit var ivPhoto: ImageView
    private lateinit var btnPickPhoto: android.view.View
    private lateinit var btnRemovePhoto: android.view.View

    private var photoUri: String? = null

    private var eventTimeMillis: Long? = null
    private var startTimeMillis: Long? = null
    private var endTimeMillis: Long? = null

    private lateinit var subTypeAdapter: ArrayAdapter<String>
    private val foodSubTypes = listOf("早餐", "午餐", "晚餐", "宵夜")
    private val experienceSubTypes = listOf("门票", "演出", "项目", "其他")
    private val durationRegex = Regex("""^\d{2}:[0-5]\d$""")

    private val pickPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        val saved = copyImageToFilesDir(uri) ?: run {
            Toast.makeText(this, "选择图片失败", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        ImageCompressor.deleteIfLocalFile(photoUri)
        photoUri = saved.toString()
        renderPhoto()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_detail)

        repo = TripRepository(this)

        tripId = intent.getLongExtra("trip_id", -1L)
        expenseId = intent.getLongExtra("expense_id", -1L)

        bindViews()
        setupDropdowns()
        setupClearIcons()

        // ✅ Apply status bar inset to AppBarLayout (keeps toolbar title/icons visible)
        Insets.applyTopInsetPadding(appBar)

        // ✅ Ensure menu is present (some configurations may not inflate app:menu reliably)
        if (topAppBar.menu.size() == 0) {
            topAppBar.inflateMenu(R.menu.menu_expense_detail)
        }

        topAppBar.setNavigationOnClickListener { finish() }
        topAppBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_save -> {
                    save()
                    true
                }
                R.id.action_delete -> {
                    delete()
                    true
                }
                else -> false
            }
        }

        // ✅ Make dropdowns reliably open on click
        etMainType.setOnClickListener { etMainType.showDropDown() }
        etPay.setOnClickListener { etPay.showDropDown() }
        etSubType.setOnClickListener { etSubType.showDropDown() }
        etTransportSubType.setOnClickListener { etTransportSubType.showDropDown() }

        current = intent.getSerializableExtraCompat("expense_obj")
        applyExpenseToUi(current)

        btnPickPhoto.setOnClickListener { pickPhotoLauncher.launch("image/*") }
        btnRemovePhoto.setOnClickListener {
            ImageCompressor.deleteIfLocalFile(photoUri)
            photoUri = null
            renderPhoto()
        }

        ivPhoto.setOnClickListener {
            val uri = photoUri ?: return@setOnClickListener
            startActivity(Intent(this, PhotoPreviewActivity::class.java).apply {
                putExtra(PhotoPreviewActivity.EXTRA_URI, uri)
            })
        }

        etEventTime.setOnClickListener {
            pickDateTime(eventTimeMillis, "选择时间") { millis ->
                eventTimeMillis = millis
                etEventTime.setText(formatDateTime(millis))
            }
        }

        etTransportPeriod.setOnClickListener { pickTransportPeriod() }

        etMainType.setOnItemClickListener { _, _, _, _ ->
            val mt = parseMainType(etMainType.text?.toString().orEmpty()) ?: MainType.EXPERIENCE
            applyMainTypeUi(mt, clearSubTypeValue = true)
        }

        etTransportSubType.setOnItemClickListener { _, _, _, _ ->
            etArriveAtVenue.setText(calcArriveAtVenuePreview().orEmpty())
        }
    }

    private fun setupClearIcons() {
        tilEventTime.endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
        tilEventTime.setEndIconOnClickListener {
            eventTimeMillis = null
            etEventTime.setText("")
        }

        tilTransportPeriod.endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
        tilTransportPeriod.setEndIconOnClickListener {
            startTimeMillis = null
            endTimeMillis = null
            etTransportPeriod.setText("")
            etArriveAtVenue.setText("")
        }
    }

    private fun bindViews() {
        appBar = findViewById(R.id.appBar)
        topAppBar = findViewById(R.id.topAppBar)

        tilTitle = findViewById(R.id.tilTitle)
        etTitle = findViewById(R.id.etTitle)

        tilAmount = findViewById(R.id.tilAmount)
        etAmount = findViewById(R.id.etAmount)

        tilPay = findViewById(R.id.tilPay)
        etPay = findViewById(R.id.etPay)

        tilMainType = findViewById(R.id.tilMainType)
        etMainType = findViewById(R.id.etMainType)

        tilReservation = findViewById(R.id.tilReservation)
        etReservation = findViewById(R.id.etReservation)

        tilReview = findViewById(R.id.tilReview)
        etReview = findViewById(R.id.etReview)

        tilEventTime = findViewById(R.id.tilEventTime)
        etEventTime = findViewById(R.id.etEventTime)

        tilSubType = findViewById(R.id.tilSubType)
        etSubType = findViewById(R.id.etSubType)

        groupTransport = findViewById(R.id.groupTransport)
        tilTransportSubType = findViewById(R.id.tilTransportSubType)
        etTransportSubType = findViewById(R.id.etTransportSubType)

        tilRoute = findViewById(R.id.tilRoute)
        etRoute = findViewById(R.id.etRoute)

        tilTransportPeriod = findViewById(R.id.tilTransportPeriod)
        etTransportPeriod = findViewById(R.id.etTransportPeriod)

        tilArriveAtVenue = findViewById(R.id.tilArriveAtVenue)
        etArriveAtVenue = findViewById(R.id.etArriveAtVenue)

        tilDuration = findViewById(R.id.tilDuration)
        etDuration = findViewById(R.id.etDuration)

        ivPhoto = findViewById(R.id.ivPhoto)
        btnPickPhoto = findViewById(R.id.btnPickPhoto)
        btnRemovePhoto = findViewById(R.id.btnRemovePhoto)
    }

    private fun setupDropdowns() {
        etMainType.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, MainType.entries.map { it.displayName })
        )
        etPay.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, PaymentMethod.entries.map { it.displayName })
        )

        subTypeAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        etSubType.setAdapter(subTypeAdapter)

        etTransportSubType.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, listOf("飞机", "铁路", "巴士", "打车", "轮渡", "骑行"))
        )
    }

    private fun applyMainTypeUi(mainType: MainType, clearSubTypeValue: Boolean) {
        val isTransport = mainType == MainType.TRANSPORT
        groupTransport.isVisible = isTransport
        tilTransportPeriod.isEndIconVisible = isTransport

        when (mainType) {
            MainType.TRANSPORT -> {
                tilSubType.isVisible = false
                if (clearSubTypeValue) etSubType.setText("", false)
                etArriveAtVenue.setText(calcArriveAtVenuePreview().orEmpty())
            }
            MainType.FOOD -> {
                tilSubType.isVisible = true
                setSubTypeOptions(foodSubTypes)
                if (clearSubTypeValue) etSubType.setText("", false)
                clearTransportFields()
            }
            MainType.EXPERIENCE -> {
                tilSubType.isVisible = true
                setSubTypeOptions(experienceSubTypes)
                if (clearSubTypeValue) etSubType.setText("", false)
                clearTransportFields()
            }
            MainType.HOTEL -> {
                tilSubType.isVisible = false
                if (clearSubTypeValue) etSubType.setText("", false)
                clearTransportFields()
            }
        }
    }

    private fun setSubTypeOptions(options: List<String>) {
        subTypeAdapter.clear()
        subTypeAdapter.addAll(options)
        subTypeAdapter.notifyDataSetChanged()
    }

    private fun clearTransportFields() {
        startTimeMillis = null
        endTimeMillis = null
        etTransportPeriod.setText("")
        etArriveAtVenue.setText("")
        etTransportSubType.setText("", false)
        etRoute.setText("")
        etDuration.setText("")
        tilDuration.error = null
    }

    private fun applyExpenseToUi(expense: Expense?) {
        if (expense == null) {
            topAppBar.title = "新增记录"
            photoUri = null
            renderPhoto()

            val defaultMainTypeName = intent.getStringExtra(CategoryExpenseListActivity.EXTRA_DEFAULT_MAIN_TYPE)
            val defaultMainType = runCatching { MainType.valueOf(defaultMainTypeName ?: "") }.getOrNull()
            val initMainType = defaultMainType ?: MainType.EXPERIENCE

            etMainType.setText(initMainType.displayName, false)
            etPay.setText(PaymentMethod.CASH.displayName, false)

            eventTimeMillis = null
            startTimeMillis = null
            endTimeMillis = null
            etEventTime.setText("")
            etTransportPeriod.setText("")
            etArriveAtVenue.setText("")

            applyMainTypeUi(initMainType, clearSubTypeValue = true)
            return
        }

        topAppBar.title = "记录详情"
        etTitle.setText(expense.title)
        etAmount.setText(expense.amountCny.toString())
        etMainType.setText(expense.mainType.displayName, false)
        etPay.setText(expense.paymentMethod.displayName, false)

        etReservation.setText(expense.reservationPlatform.orEmpty())
        etReview.setText(expense.shortReview.orEmpty())

        photoUri = expense.photoUri
        renderPhoto()

        eventTimeMillis = expense.eventTimeMillis
        startTimeMillis = expense.startTimeMillis
        endTimeMillis = expense.endTimeMillis

        etEventTime.setText(eventTimeMillis?.let { formatDateTime(it) }.orEmpty())
        etTransportPeriod.setText(buildTransportPeriodText(startTimeMillis, endTimeMillis))

        applyMainTypeUi(expense.mainType, clearSubTypeValue = false)

        when (expense.mainType) {
            MainType.TRANSPORT -> {
                etTransportSubType.setText(expense.subType.orEmpty(), false)
                etRoute.setText(expense.route.orEmpty())
                etDuration.setText(expense.durationHHmm.orEmpty())
                etArriveAtVenue.setText(calcArriveAtVenue(expense).orEmpty())
            }
            MainType.FOOD, MainType.EXPERIENCE -> etSubType.setText(expense.subType.orEmpty(), false)
            MainType.HOTEL -> etSubType.setText("", false)
        }
    }

    private fun renderPhoto() {
        val uri = photoUri?.let { Uri.parse(it) }
        ivPhoto.isVisible = uri != null
        btnRemovePhoto.isVisible = uri != null
        if (uri != null) ivPhoto.setImageURI(uri)
    }

    private fun save() {
        tilTitle.error = null
        tilAmount.error = null
        tilPay.error = null
        tilDuration.error = null

        val title = etTitle.text?.toString()?.trim().orEmpty()
        val amount = etAmount.text?.toString()?.trim()?.toIntOrNull()
        val pay = parsePaymentMethod(etPay.text?.toString().orEmpty())
        val mainType = parseMainType(etMainType.text?.toString().orEmpty()) ?: MainType.EXPERIENCE

        var ok = true
        if (title.isBlank()) {
            tilTitle.error = "请输入消费内容"
            ok = false
        }
        if (amount == null || amount <= 0) {
            tilAmount.error = "请输入大于 0 的整数"
            ok = false
        }
        if (pay == null) {
            tilPay.error = "请选择支付方式"
            ok = false
        }

        val isTransport = mainType == MainType.TRANSPORT
        val durationInput = etDuration.text?.toString()?.trim().orEmpty()
        if (isTransport && durationInput.isNotBlank() && !durationRegex.matches(durationInput)) {
            tilDuration.error = "时长格式需为 HH:mm（例如 05:40）"
            ok = false
        }
        if (!ok) return

        if (isTransport && startTimeMillis != null && endTimeMillis != null && endTimeMillis!! < startTimeMillis!!) {
            Toast.makeText(this, "到达时间不能早于出发时间", Toast.LENGTH_SHORT).show()
            return
        }

        val reservation = etReservation.text?.toString()?.trim().orEmpty().ifBlank { null }
        val review = etReview.text?.toString()?.trim().orEmpty().ifBlank { null }?.take(20)

        val dateEpochDay = inferDateEpochDay(mainType, eventTimeMillis, startTimeMillis)
        val finalStart = if (isTransport) startTimeMillis else null
        val finalEnd = if (isTransport) endTimeMillis else null

        val subtype: String? = when (mainType) {
            MainType.TRANSPORT -> etTransportSubType.text?.toString()?.trim().orEmpty().ifBlank { null }
            MainType.FOOD -> etSubType.text?.toString()?.trim().orEmpty().ifBlank { null }
            MainType.EXPERIENCE -> etSubType.text?.toString()?.trim().orEmpty().ifBlank { null }
            MainType.HOTEL -> null
        }

        val updated = (current ?: Expense(
            id = 0,
            tripId = tripId,
            title = title,
            amountCny = amount!!,
            mainType = mainType,
            paymentMethod = pay!!,
            dateEpochDay = dateEpochDay
        )).copy(
            title = title,
            amountCny = amount!!,
            paymentMethod = pay!!,
            mainType = mainType,
            reservationPlatform = reservation,
            shortReview = review,
            photoUri = photoUri,
            eventTimeMillis = eventTimeMillis,
            startTimeMillis = finalStart,
            endTimeMillis = finalEnd,
            subType = subtype,
            route = if (isTransport) etRoute.text?.toString()?.trim().orEmpty().ifBlank { null } else null,
            durationHHmm = if (isTransport) durationInput.ifBlank { null } else null,
            dateEpochDay = dateEpochDay
        )

        lifecycleScope.launch(Dispatchers.IO) {
            repo.upsertExpense(updated)
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun delete() {
        val e = current ?: run { finish(); return }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("删除记录")
            .setMessage("确定删除「${e.title}」吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    ImageCompressor.deleteIfLocalFile(e.photoUri)
                    repo.deleteExpense(e.id)
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
            .show()
    }

    private fun copyImageToFilesDir(input: Uri): Uri? {
        return runCatching {
            val dir = File(filesDir, "wtrip_images").apply { mkdirs() }
            val outFile = File(dir, "img_${UUID.randomUUID()}.jpg")

            contentResolver.openInputStream(input).use { ins ->
                requireNotNull(ins)
                FileOutputStream(outFile).use { fos ->
                    ins.copyTo(fos)
                }
            }
            Uri.fromFile(outFile)
        }.getOrNull()
    }

    private fun parseMainType(input: String): MainType? =
        MainType.entries.firstOrNull { it.displayName == input } ?: runCatching { MainType.valueOf(input) }.getOrNull()

    private fun parsePaymentMethod(input: String): PaymentMethod? =
        PaymentMethod.entries.firstOrNull { it.displayName == input } ?: runCatching { PaymentMethod.valueOf(input) }.getOrNull()

    private fun inferDateEpochDay(mainType: MainType, eventMillis: Long?, transportStartMillis: Long?): Long {
        val zone = ZoneId.systemDefault()
        val millis = when {
            eventMillis != null -> eventMillis
            mainType == MainType.TRANSPORT && transportStartMillis != null -> transportStartMillis
            else -> null
        }
        return if (millis != null) {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone).toLocalDate().toEpochDay()
        } else LocalDate.now().toEpochDay()
    }

    private fun formatDateTime(epochMillis: Long): String {
        val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
        return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    }

    private fun buildTransportPeriodText(start: Long?, end: Long?): String {
        if (start == null || end == null) return ""
        val s = formatDateTime(start)
        val e = formatDateTime(end).substringAfter(" ")
        return "$s-$e"
    }

    private fun pickTransportPeriod() {
        pickDateTime(startTimeMillis, "选择出发时间") { startMillis ->
            startTimeMillis = startMillis
            pickDateTime(endTimeMillis, "选择到达时间") { endMillis ->
                endTimeMillis = endMillis
                etTransportPeriod.setText(buildTransportPeriodText(startTimeMillis, endTimeMillis))
                etArriveAtVenue.setText(calcArriveAtVenuePreview().orEmpty())
            }
        }
    }

    private fun calcArriveAtVenue(expense: Expense): String? {
        val start = expense.startTimeMillis ?: return null
        val sub = expense.subType ?: return null
        val minusMinutes = if (sub.contains("飞机")) 90 else 20
        val arriveMillis = start - minusMinutes * 60_000L
        val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(arriveMillis), ZoneId.systemDefault())
        return dt.format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    private fun calcArriveAtVenuePreview(): String? {
        val start = startTimeMillis ?: return null
        val sub = etTransportSubType.text?.toString()?.trim().orEmpty()
        if (sub.isBlank()) return null
        val minusMinutes = if (sub.contains("飞机")) 90 else 20
        val arriveMillis = start - minusMinutes * 60_000L
        val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(arriveMillis), ZoneId.systemDefault())
        return dt.format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    private fun pickDateTime(initialMillis: Long?, title: String, onPicked: (Long) -> Unit) {
        val zone = ZoneId.systemDefault()
        val init = initialMillis?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), zone) } ?: LocalDateTime.now()

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        val dt = LocalDateTime.of(year, month + 1, dayOfMonth, hourOfDay, minute)
                        onPicked(dt.atZone(zone).toInstant().toEpochMilli())
                    },
                    init.hour,
                    init.minute,
                    true
                ).apply { setTitle(title) }.show()
            },
            init.year,
            init.monthValue - 1,
            init.dayOfMonth
        ).apply { setTitle(title) }.show()
    }
}

private inline fun <reified T> Intent.getSerializableExtraCompat(name: String): T? {
    @Suppress("DEPRECATION")
    return getSerializableExtra(name) as? T
}