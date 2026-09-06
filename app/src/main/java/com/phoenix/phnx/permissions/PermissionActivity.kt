package com.phoenix.phnx.permissions

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.phoenix.phnx.PhnxApplication
import com.phoenix.phnx.R

class PermissionActivity : AppCompatActivity() {
    private val app by lazy { application as PhnxApplication }
    private val profileId by lazy { app.profileManager.activeProfile().id }
    private lateinit var permissionList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.site_permissions)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(getColor(R.color.phnx_cream))
        }
        content.addView(TextView(this).apply {
            text = getString(R.string.site_permissions)
            textSize = 30f
            setTextColor(getColor(R.color.phnx_blue))
        })
        content.addView(TextView(this).apply {
            text = getString(R.string.site_permissions_summary)
            textSize = 14f
            setTextColor(getColor(R.color.phnx_muted))
            setPadding(0, dp(8), 0, dp(16))
        })
        content.addView(TextView(this).apply {
            text = getString(R.string.site_notifications_unsupported)
            textSize = 13f
            setTextColor(getColor(R.color.phnx_muted))
            setPadding(0, 0, 0, dp(12))
        })
        content.addView(Button(this).apply {
            text = getString(R.string.open_app_settings)
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            }
        })
        content.addView(Button(this).apply {
            text = getString(R.string.reset_all_site_permissions)
            setOnClickListener { confirmResetAll() }
        })
        permissionList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(permissionList)

        setContentView(ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.phnx_cream))
            addView(content)
        })
    }

    override fun onResume() {
        super.onResume()
        if (::permissionList.isInitialized) refreshList()
    }

    private fun refreshList() {
        permissionList.removeAllViews()
        val permissions = app.permissionManager.getForProfile(profileId)
        if (permissions.isEmpty()) {
            permissionList.addView(TextView(this).apply {
                text = getString(R.string.no_site_permissions)
                textSize = 16f
                setTextColor(getColor(R.color.phnx_muted))
                setPadding(0, dp(18), 0, 0)
            })
            return
        }
        permissions.groupBy { it.origin }.forEach { (origin, decisions) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(16), 0, dp(16))
            }
            val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            header.addView(TextView(this@PermissionActivity).apply {
                text = origin
                textSize = 17f
                setTextColor(getColor(R.color.phnx_text))
            }, LinearLayout.LayoutParams(0, -2, 1f))
            header.addView(Button(this@PermissionActivity).apply {
                text = getString(R.string.reset)
                setOnClickListener {
                    app.permissionManager.clearOrigin(profileId, origin)
                    refreshList()
                }
            })
            row.addView(header)
            decisions.forEach { permission ->
                row.addView(TextView(this).apply {
                    text = "${permission.type.name}: ${permission.decision.name}"
                    textSize = 14f
                    setTextColor(getColor(R.color.phnx_muted))
                    setPadding(dp(4), dp(4), 0, 0)
                })
            }
            permissionList.addView(row)
        }
    }

    private fun confirmResetAll() {
        AlertDialog.Builder(this)
            .setTitle(R.string.reset_all_site_permissions)
            .setMessage(R.string.reset_all_site_permissions_warning)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.reset) { _, _ ->
                app.permissionManager.clearProfile(profileId)
                refreshList()
            }
            .show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
