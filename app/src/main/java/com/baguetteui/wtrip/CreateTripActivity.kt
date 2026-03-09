package com.baguetteui.wtrip

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.baguetteui.wtrip.data.TripRepository
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateTripActivity : AppCompatActivity() {

    private lateinit var repo: TripRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = TripRepository(this)

        val view = layoutInflater.inflate(R.layout.dialog_two_fields, null, false)
        val til1 = view.findViewById<TextInputLayout>(R.id.tilField1)
        val til2 = view.findViewById<TextInputLayout>(R.id.tilField2)
        val et1 = view.findViewById<TextInputEditText>(R.id.etField1)
        val et2 = view.findViewById<TextInputEditText>(R.id.etField2)

        til1.hint = "目的地"
        til2.hint = "天数"
        et2.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        val dialog = AlertDialog.Builder(this)
            .setTitle("创建行程")
            .setView(view)
            .setNegativeButton("取消") { _, _ ->
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
            .setPositiveButton("创建", null)
            .setCancelable(false)
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

                lifecycleScope.launch {
                    val newId = withContext(Dispatchers.IO) {
                        repo.createTrip(title = title, days = days!!)
                    }

                    val data = Intent().apply {
                        putExtra("trip_id", newId)
                        putExtra("trip_title", title)
                        putExtra("trip_days", days)
                    }
                    setResult(Activity.RESULT_OK, data)
                    finish()
                }
            }
        }

        dialog.show()
    }
}