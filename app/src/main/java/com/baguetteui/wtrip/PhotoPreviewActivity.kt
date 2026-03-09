package com.baguetteui.wtrip

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class PhotoPreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_preview)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        val iv = findViewById<ImageView>(R.id.ivPhoto)

        topAppBar.setNavigationOnClickListener { finish() }

        val uriStr = intent.getStringExtra(EXTRA_URI)
        val uri = uriStr?.let { runCatching { Uri.parse(it) }.getOrNull() }

        if (uri != null) {
            iv.setImageURI(uri)
        }
    }

    companion object {
        const val EXTRA_URI = "extra_uri"
    }
}