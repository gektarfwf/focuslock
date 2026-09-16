package com.focuslock.app

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppPickerActivity : AppCompatActivity() {

    private val selected = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_picker)

        selected += Prefs.blocked(this)
        selected -= packageName

        val rv = findViewById<RecyclerView>(R.id.rv)
        rv.layoutManager = LinearLayoutManager(this)

        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.packageName != packageName }
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }

        rv.adapter = Adapter(apps, pm, selected)

        findViewById<android.view.View>(R.id.btnSave).setOnClickListener {
            Prefs.setBlocked(this, selected)
            finish()
        }
    }

    private class Adapter(
        val items: List<ApplicationInfo>,
        val pm: PackageManager,
        val selected: MutableSet<String>
    ) : RecyclerView.Adapter<Adapter.VH>() {

        class VH(v: android.view.View) : RecyclerView.ViewHolder(v) {
            val icon: ImageView = v.findViewById(R.id.icon)
            val name: TextView = v.findViewById(R.id.name)
            val check: CheckBox = v.findViewById(R.id.check)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
            return VH(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: VH, position: Int) {
            val ai = items[position]
            h.icon.setImageDrawable(pm.getApplicationIcon(ai))
            h.name.text = pm.getApplicationLabel(ai)
            h.check.setOnCheckedChangeListener(null)
            h.check.isChecked = ai.packageName in selected
            h.check.setOnCheckedChangeListener { _, checked ->
                if (checked) selected += ai.packageName else selected -= ai.packageName
            }
            h.itemView.setOnClickListener {
                h.check.isChecked = !h.check.isChecked
            }
        }
    }
}