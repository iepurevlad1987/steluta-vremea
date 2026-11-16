package ro.iepur.agenda

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

/**
 * Ecranul aplicatiei: acces la calendar, si cum arata widget-ul. Orice schimbare se vede
 * imediat pe ecranul de start. Construit din cod, fara layout XML - sunt cinci controale.
 */
class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var grant: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val p = Prefs.of(this)
        val dp = resources.displayMetrics.density
        fun Int.px() = (this * dp).toInt()

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22.px(), 20.px(), 22.px(), 28.px())
        }
        fun title(text: String, size: Float = 13f, top: Int = 22) = TextView(this).apply {
            this.text = text
            textSize = size
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, top.px(), 0, 6.px())
        }.also { col.addView(it) }

        title(getString(R.string.app_name), 28f, 8)
        TextView(this).apply {
            text = getString(R.string.intro)
            alpha = 0.8f
        }.also { col.addView(it) }

        // Acces la calendar
        title(getString(R.string.s_access))
        status = TextView(this).also { col.addView(it) }
        grant = Button(this).apply {
            setOnClickListener {
                if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CALENDAR) ||
                    !p.getBoolean("asked", false)
                ) {
                    p.edit().putBoolean("asked", true).apply()
                    requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR), 1)
                } else {
                    // Refuzat de doua ori: Android nu mai arata dialogul, doar Setarile.
                    startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")),
                    )
                }
            }
        }.also { col.addView(it) }

        // Transparenta
        title(getString(R.string.s_opacity))
        val opLabel = TextView(this).also { col.addView(it) }
        SeekBar(this).apply {
            max = 100
            progress = Prefs.opacity(this@MainActivity)
            opLabel.text = getString(R.string.opacity_value, progress)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, v: Int, user: Boolean) {
                    opLabel.text = getString(R.string.opacity_value, v)
                }
                override fun onStartTrackingTouch(s: SeekBar) {}
                override fun onStopTrackingTouch(s: SeekBar) {
                    save { putInt(Prefs.OPACITY, s.progress) }
                }
            })
        }.also { col.addView(it) }

        // Ton
        title(getString(R.string.s_tone))
        RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            val dark = RadioButton(context).apply { id = View.generateViewId(); text = getString(R.string.tone_dark) }
            val light = RadioButton(context).apply { id = View.generateViewId(); text = getString(R.string.tone_light) }
            addView(dark); addView(light)
            check(if (Prefs.light(this@MainActivity)) light.id else dark.id)
            setOnCheckedChangeListener { _, id -> save { putBoolean(Prefs.LIGHT, id == light.id) } }
        }.also { col.addView(it) }

        Switch(this).apply {
            text = getString(R.string.s_edge)
            isChecked = Prefs.edge(this@MainActivity)
            setPadding(0, 12.px(), 0, 0)
            setOnCheckedChangeListener { _, on -> save { putBoolean(Prefs.EDGE, on) } }
        }.also { col.addView(it) }

        // Cate zile
        title(getString(R.string.s_days))
        RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            val current = Prefs.days(this@MainActivity)
            listOf(3, 7, 14, 30).forEach { n ->
                val b = RadioButton(context).apply {
                    id = View.generateViewId()
                    text = resources.getQuantityString(R.plurals.days, n, n)
                    tag = n
                }
                addView(b)
                if (n == current) check(b.id)
            }
            setOnCheckedChangeListener { g, id -> save { putInt(Prefs.DAYS, g.findViewById<View>(id).tag as Int) } }
        }.also { col.addView(it) }

        // Pune widget-ul
        title(getString(R.string.s_widget))
        TextView(this).apply { text = getString(R.string.how_to_add); alpha = 0.8f }.also { col.addView(it) }
        val manager = AppWidgetManager.getInstance(this)
        if (manager.isRequestPinAppWidgetSupported) {
            Button(this).apply {
                text = getString(R.string.pin)
                setOnClickListener {
                    manager.requestPinAppWidget(ComponentName(this@MainActivity, AgendaWidget::class.java), null, null)
                }
            }.also { col.addView(it) }
        }

        val scroll = ScrollView(this).apply { addView(col) }
        // Android 15+ deseneaza aplicatia pana sub bara de stare; impingem continutul in jos.
        scroll.setOnApplyWindowInsetsListener { v, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        AgendaWidget.redraw(this)
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        refreshStatus()
        AgendaWidget.redraw(this)
    }

    private fun refreshStatus() {
        val ok = CalendarRepo.hasPermission(this)
        status.text = getString(if (ok) R.string.access_ok else R.string.access_missing)
        grant.text = getString(R.string.grant)
        grant.visibility = if (ok) View.GONE else View.VISIBLE
        status.gravity = Gravity.START
    }

    private fun save(block: android.content.SharedPreferences.Editor.() -> Unit) {
        Prefs.of(this).edit().apply(block).commit()
        AgendaWidget.redraw(this)
    }
}
