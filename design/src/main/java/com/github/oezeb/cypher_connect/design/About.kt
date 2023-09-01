package com.github.oezeb.cypher_connect.design;

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat

class About : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.about)

         val html = resources.openRawResource(R.raw.about).bufferedReader().use { it.readText() }
        findViewById<TextView>(R.id.about_text).apply {
            movementMethod = LinkMovementMethod.getInstance()
            text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }

        findViewById<TextView>(R.id.title).apply { text = getString(R.string.about) }
    }

    fun back(view: View) = finish()
}