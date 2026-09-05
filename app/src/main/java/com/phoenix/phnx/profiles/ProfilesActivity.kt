package com.phoenix.phnx.profiles

import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.phoenix.phnx.PhnxApplication
import com.phoenix.phnx.R
import com.phoenix.phnx.identity.IdentityActivity

class ProfilesActivity : AppCompatActivity() {
    private val profileManager by lazy { (application as PhnxApplication).profileManager }
    private lateinit var profileList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.profiles)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(getColor(R.color.phnx_cream))
        }
        content.addView(TextView(this).apply {
            text = getString(R.string.profiles)
            textSize = 30f
            setTextColor(getColor(R.color.phnx_blue))
        })
        content.addView(TextView(this).apply {
            text = getString(R.string.profiles_isolation_note)
            textSize = 14f
            setTextColor(getColor(R.color.phnx_muted))
            setPadding(0, dp(8), 0, dp(16))
        })
        profileList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(profileList)
        content.addView(Button(this).apply {
            text = getString(R.string.new_profile)
            setOnClickListener { showCreateDialog() }
        })

        setContentView(ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.phnx_cream))
            addView(content)
        })
        refreshProfiles()
    }

    private fun refreshProfiles() {
        profileList.removeAllViews()
        val activeId = profileManager.activeProfile().id
        profileManager.getAllProfiles().forEach { profile ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(12), 0, dp(12))
                contentDescription = profile.name
            }
            row.addView(TextView(this).apply {
                text = if (profile.id == activeId) {
                    getString(R.string.profile_active_name, profile.name)
                } else {
                    profile.name
                }
                textSize = 18f
                setTextColor(getColor(R.color.phnx_text))
            })
            row.addView(TextView(this).apply {
                text = getString(R.string.profile_status, profile.status.name.lowercase())
                textSize = 14f
                setTextColor(getColor(R.color.phnx_muted))
                setPadding(0, dp(4), 0, dp(8))
            })
            val actions = LinearLayout(this).apply { gravity = Gravity.END }
            actions.addView(Button(this@ProfilesActivity).apply {
                text = getString(R.string.identity)
                setOnClickListener {
                    startActivity(Intent(this@ProfilesActivity, IdentityActivity::class.java).apply {
                        putExtra("profile_id", profile.id)
                    })
                }
            })
            actions.addView(Button(this).apply {
                text = getString(R.string.rename)
                setOnClickListener { showRenameDialog(profile) }
            })
            if (profile.id != activeId) {
                actions.addView(Button(this@ProfilesActivity).apply {
                    text = getString(R.string.switch_profile)
                    setOnClickListener { switchProfile(profile) }
                })
                actions.addView(Button(this@ProfilesActivity).apply {
                    text = getString(R.string.delete)
                    setOnClickListener { confirmDelete(profile) }
                })
            }
            row.addView(actions)
            profileList.addView(row)
        }
    }

    private fun switchProfile(profile: ProfileEntity) {
        profileManager.switchProfile(profile.id) ?: return
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) {
            Toast.makeText(this, R.string.profile_switch_failed, Toast.LENGTH_SHORT).show()
            return
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(launchIntent)
        finishAffinity()
        Process.killProcess(Process.myPid())
    }

    private fun showCreateDialog() {
        val input = profileInput()
        AlertDialog.Builder(this)
            .setTitle(R.string.new_profile)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.create) { _, _ ->
                profileManager.createProfile(input.text.toString())
                refreshProfiles()
            }
            .show()
    }

    private fun showRenameDialog(profile: ProfileEntity) {
        val input = profileInput().apply { setText(profile.name) }
        AlertDialog.Builder(this)
            .setTitle(R.string.rename_profile)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                if (profileManager.renameProfile(profile.id, input.text.toString()) == null) {
                    Toast.makeText(this, R.string.profile_name_required, Toast.LENGTH_SHORT).show()
                }
                refreshProfiles()
            }
            .show()
    }

    private fun confirmDelete(profile: ProfileEntity) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_profile)
            .setMessage(getString(R.string.delete_profile_warning, profile.name))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                if (profileManager.deleteProfile(profile.id)) {
                    (application as PhnxApplication).networkManager.clearConfig(profile.id)
                    (application as PhnxApplication).deviceProfileManager.clearProfileConfiguration(profile.id)
                }
                refreshProfiles()
            }
            .show()
    }

    private fun profileInput(): EditText = EditText(this).apply {
        hint = getString(R.string.profile_name)
        isSingleLine = true
        setPadding(dp(20), dp(8), dp(20), dp(8))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
