package com.phoenix.phnx.network

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.phoenix.phnx.PhnxApplication
import com.phoenix.phnx.R
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class NetworkActivity : AppCompatActivity() {
    private val app by lazy { application as PhnxApplication }
    private val profileId by lazy { app.profileManager.activeProfile().id }

    private lateinit var modeGroup: RadioGroup
    private lateinit var proxyRadio: RadioButton
    private lateinit var proxyEnabled: SwitchCompat
    private lateinit var freeProxyFallback: SwitchCompat
    private lateinit var directFallback: SwitchCompat
    private lateinit var proxyType: Spinner
    private lateinit var host: EditText
    private lateinit var port: EditText
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var connectionState: TextView
    private lateinit var result: TextView
    private val connectionExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var loadedConfig: ProfileNetworkConfig? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.network)
        setContentView(buildLayout())
        loadConfig()
    }

    override fun onStart() {
        super.onStart()
        app.networkManager.observeConnection { state ->
            runOnUiThread { connectionState.text = "Network state: ${state.name.lowercase()}" }
        }
    }

    override fun onStop() {
        app.networkManager.stopObservingConnection()
        super.onStop()
    }

    override fun onDestroy() {
        connectionExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun buildLayout(): View {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(getColor(R.color.phnx_cream))
        }
        content.addView(TextView(this).apply {
            text = getString(R.string.network)
            textSize = 30f
            setTextColor(getColor(R.color.phnx_blue))
        })
        content.addView(TextView(this).apply {
            text = "Profile: ${app.profileManager.activeProfile().name}"
            textSize = 15f
            setTextColor(getColor(R.color.phnx_muted))
            setPadding(0, dp(8), 0, dp(16))
        })

        modeGroup = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        val directRadio = RadioButton(this).apply {
            id = View.generateViewId()
            text = getString(R.string.network_direct)
        }
        proxyRadio = RadioButton(this).apply {
            id = View.generateViewId()
            text = getString(R.string.network_proxy)
        }
        modeGroup.addView(directRadio, RadioGroup.LayoutParams(0, -2, 1f))
        modeGroup.addView(proxyRadio, RadioGroup.LayoutParams(0, -2, 1f))
        directRadio.isChecked = true
        modeGroup.setOnCheckedChangeListener { _, _ -> updateProxyFields() }
        content.addView(modeGroup)

        proxyEnabled = toggle(
            getString(R.string.proxy_enabled),
            getString(R.string.proxy_enabled_summary),
        )
        proxyEnabled.setOnCheckedChangeListener { _, _ -> updateProxyFields() }
        content.addView(proxyEnabled)

        freeProxyFallback = toggle(
            getString(R.string.proxy_free_fallback),
            getString(R.string.proxy_free_fallback_summary),
        )
        content.addView(freeProxyFallback)

        directFallback = toggle(
            getString(R.string.proxy_direct_fallback),
            getString(R.string.proxy_direct_fallback_summary),
        )
        content.addView(directFallback)
        content.addView(TextView(this).apply {
            text = getString(R.string.proxy_public_warning)
            textSize = 13f
            setTextColor(getColor(R.color.phnx_muted))
            setPadding(0, 0, 0, dp(12))
        })

        proxyType = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@NetworkActivity,
                android.R.layout.simple_spinner_item,
                ProxyType.values().map { it.name },
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        }
        content.addView(proxyType, LinearLayout.LayoutParams(-1, dp(48)))
        host = field(getString(R.string.proxy_host), InputType.TYPE_CLASS_TEXT)
        content.addView(host)
        port = field(getString(R.string.proxy_port), InputType.TYPE_CLASS_NUMBER)
        content.addView(port)
        username = field(getString(R.string.proxy_username), InputType.TYPE_CLASS_TEXT)
        content.addView(username)
        password = field(
            getString(R.string.proxy_password),
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
        )
        content.addView(password)

        connectionState = TextView(this).apply {
            textSize = 15f
            setTextColor(getColor(R.color.phnx_text))
            setPadding(0, dp(12), 0, dp(4))
        }
        content.addView(connectionState)
        result = TextView(this).apply {
            textSize = 14f
            setTextColor(getColor(R.color.phnx_muted))
            setPadding(0, dp(4), 0, dp(12))
        }
        content.addView(result)

        val actions = LinearLayout(this).apply {
            gravity = Gravity.END
            orientation = LinearLayout.HORIZONTAL
        }
        actions.addView(Button(this).apply {
            text = getString(R.string.test_connection)
            setOnClickListener { testConnection() }
        })
        actions.addView(Button(this).apply {
            text = getString(R.string.save_network)
            setOnClickListener { saveNetwork() }
        })
        content.addView(actions)
        updateProxyFields()
        return ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.phnx_cream))
            addView(content)
        }
    }

    private fun loadConfig() {
        val config = app.networkManager.getConfig(profileId)
        loadedConfig = config
        modeGroup.check(if (config.mode == NetworkMode.PROXY) proxyRadio.id else modeGroup.getChildAt(0).id)
        proxyEnabled.isChecked = config.mode == NetworkMode.PROXY && config.enabled
        freeProxyFallback.isChecked = config.fallbackToFreeProxy
        directFallback.isChecked = config.fallbackToDirect
        config.proxyType?.let { proxyType.setSelection(ProxyType.values().indexOf(it)) }
        host.setText(config.proxyHost)
        port.setText(config.proxyPort.takeIf { it > 0 }?.toString().orEmpty())
        username.setText(config.username)
        password.hint = if (config.credentialReference == null) {
            getString(R.string.proxy_password)
        } else {
            getString(R.string.proxy_password_saved)
        }
        connectionState.text = "Network state: ${app.networkManager.reportConnectionState().name.lowercase()}"
        result.text = "Mode: ${config.mode.name.lowercase()}"
        updateProxyFields()
    }

    private fun saveNetwork() {
        runCatching {
            val config = readConfig()
            app.networkManager.saveConfig(config)
            loadedConfig = config
            password.text.clear()
            password.hint = if (config.credentialReference == null) {
                getString(R.string.proxy_password)
            } else {
                getString(R.string.proxy_password_saved)
            }
            result.text = getString(R.string.proxy_fallback_loading)
            applyNetworkAsync(config, "Saved.")
        }.onFailure { error ->
            result.text = error.message ?: "Could not save network configuration."
        }
    }

    private fun testConnection() {
        runCatching {
            val config = readConfig()
            app.networkManager.saveConfig(config)
            loadedConfig = config
            result.text = "Testing connection..."
            connectionExecutor.execute {
                val fallbacks = if (config.fallbackToFreeProxy && config.enabled) {
                    app.networkManager.fetchFreeProxyFallbacks()
                } else {
                    emptyList()
                }
                val appliedConfig = config.copy(freeProxyFallbacks = fallbacks)
                app.networkManager.saveConfig(appliedConfig)
                val test = app.networkManager.testConfig(profileId)
                val apply = app.networkManager.applyConfig(profileId, WebViewNetworkAdapter())
                runOnUiThread {
                    result.text = "Test: ${test.state.name.lowercase()} ${test.message}\n" +
                        "Fallbacks: ${fallbacks.size}\nApply: ${apply.message}"
                }
            }
        }.onFailure { error ->
            result.text = error.message ?: "Could not test network configuration."
        }
    }

    private fun readConfig(): ProfileNetworkConfig {
        val existing = loadedConfig ?: app.networkManager.getConfig(profileId)
        val mode = if (proxyRadio.isChecked) NetworkMode.PROXY else NetworkMode.DIRECT
        val currentUsername = username.text.toString().trim()
        val typedPassword = password.text.toString()
        val existingReference = existing.credentialReference?.takeIf {
            mode == NetworkMode.PROXY && currentUsername.isNotBlank()
        }
        val provisionalReference = existingReference
            ?: typedPassword.takeIf { currentUsername.isNotBlank() && it.isNotBlank() }?.let { "pending" }
        val provisional = ProfileNetworkConfig(
            id = existing.id,
            profileId = profileId,
            mode = mode,
            proxyType = if (mode == NetworkMode.PROXY) ProxyType.values()[proxyType.selectedItemPosition] else null,
            proxyHost = if (mode == NetworkMode.PROXY) host.text.toString().trim() else "",
            proxyPort = if (mode == NetworkMode.PROXY) port.text.toString().toIntOrNull() ?: 0 else 0,
            username = if (mode == NetworkMode.PROXY) currentUsername else "",
            credentialReference = provisionalReference,
            enabled = mode == NetworkMode.DIRECT || proxyEnabled.isChecked,
            fallbackToFreeProxy = mode == NetworkMode.PROXY && freeProxyFallback.isChecked,
            fallbackToDirect = mode == NetworkMode.PROXY && directFallback.isChecked,
            freeProxyFallbacks = existing.freeProxyFallbacks,
        )
        val errors = NetworkConfigValidator.validate(provisional)
        require(errors.isEmpty()) { errors.joinToString(" ") }
        val reference = if (mode == NetworkMode.PROXY && currentUsername.isNotBlank() && typedPassword.isNotBlank()) {
            app.networkManager.saveProxyCredential(profileId, typedPassword)
        } else {
            existingReference
        }
        return provisional.copy(credentialReference = reference)
    }

    private fun updateProxyFields() {
        val enabled = proxyRadio.isChecked
        proxyEnabled.isEnabled = enabled
        val routeEnabled = enabled && proxyEnabled.isChecked
        freeProxyFallback.isEnabled = routeEnabled
        directFallback.isEnabled = routeEnabled
        proxyType.isEnabled = routeEnabled
        host.isEnabled = routeEnabled
        port.isEnabled = routeEnabled
        username.isEnabled = routeEnabled
        password.isEnabled = routeEnabled
    }

    private fun applyNetworkAsync(config: ProfileNetworkConfig, prefix: String) {
        connectionExecutor.execute {
            val fallbacks = if (config.fallbackToFreeProxy && config.enabled) {
                app.networkManager.fetchFreeProxyFallbacks()
            } else {
                emptyList()
            }
            val appliedConfig = config.copy(freeProxyFallbacks = fallbacks)
            app.networkManager.saveConfig(appliedConfig)
            loadedConfig = appliedConfig
            val apply = app.networkManager.applyConfig(profileId, WebViewNetworkAdapter())
            runOnUiThread {
                result.text = buildString {
                    append(prefix).append(' ').append(apply.message)
                    if (config.fallbackToFreeProxy) {
                        append('\n').append(
                            if (fallbacks.isEmpty()) getString(R.string.proxy_fallback_none)
                            else getString(R.string.proxy_fallback_count, fallbacks.size),
                        )
                    }
                }
            }
        }
    }

    private fun field(hintText: String, type: Int) = EditText(this).apply {
        hint = hintText
        inputType = type
        isSingleLine = true
        setPadding(0, dp(8), 0, dp(8))
    }

    private fun toggle(title: String, summary: String): SwitchCompat = SwitchCompat(this).apply {
        text = "$title\n$summary"
        textSize = 16f
        setTextColor(getColor(R.color.phnx_text))
        setPadding(0, dp(10), 0, dp(10))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
