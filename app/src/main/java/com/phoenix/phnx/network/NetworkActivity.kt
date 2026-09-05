package com.phoenix.phnx.network

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
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
import com.phoenix.phnx.chromium.network.ChromiumProxyAdapter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class NetworkActivity : AppCompatActivity() {
    private val app by lazy { application as PhnxApplication }
    private val profileId by lazy { app.profileManager.activeProfile().id }
    private val operationExecutor: ExecutorService = Executors.newFixedThreadPool(2)

    private lateinit var modeGroup: RadioGroup
    private lateinit var directRadio: RadioButton
    private lateinit var myProxyRadio: RadioButton
    private lateinit var freeProxyRadio: RadioButton
    private lateinit var directFallback: SwitchCompat
    private lateinit var myProxyPanel: LinearLayout
    private lateinit var freeProxyPanel: LinearLayout
    private lateinit var proxyType: Spinner
    private lateinit var host: EditText
    private lateinit var port: EditText
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var protocolFilter: Spinner
    private lateinit var sortFilter: Spinner
    private lateinit var countryFilter: EditText
    private lateinit var healthyOnly: SwitchCompat
    private lateinit var httpsOnly: SwitchCompat
    private lateinit var activeProxyStatus: TextView
    private lateinit var networkState: TextView
    private lateinit var result: TextView
    private lateinit var failureActions: LinearLayout
    private lateinit var freeProxyList: LinearLayout

    private var loadedConfig: ProfileNetworkConfig? = null
    private var discoveredProxies: List<ProxyEndpoint> = emptyList()
    private var lastFailedProxy: ProxyEndpoint? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.network)
        setContentView(buildLayout())
        loadConfig()
    }

    override fun onStart() {
        super.onStart()
        app.networkManager.observeConnection { state ->
            runOnUiThread { networkState.text = "Network state: ${state.name.lowercase()}" }
        }
    }

    override fun onStop() {
        app.networkManager.stopObservingConnection()
        super.onStop()
    }

    override fun onDestroy() {
        operationExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun buildLayout(): View {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(28))
            setBackgroundColor(getColor(R.color.phnx_cream))
        }
        content.addView(header(getString(R.string.network)))
        content.addView(TextView(this).apply {
            text = "Profile: ${app.profileManager.activeProfile().name}"
            textSize = 15f
            setTextColor(getColor(R.color.phnx_muted))
            setPadding(0, dp(8), 0, dp(16))
        })
        content.addView(warning(getString(R.string.proxy_public_warning)))

        modeGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
            setPadding(0, dp(12), 0, dp(8))
        }
        directRadio = modeRadio(getString(R.string.network_direct))
        myProxyRadio = modeRadio(getString(R.string.network_my_proxy))
        freeProxyRadio = modeRadio(getString(R.string.network_free_public_proxy))
        modeGroup.addView(directRadio)
        modeGroup.addView(myProxyRadio)
        modeGroup.addView(freeProxyRadio)
        modeGroup.setOnCheckedChangeListener { _, _ -> updateModeVisibility() }
        content.addView(modeGroup)

        directFallback = toggle(
            "Allow direct fallback",
            "Off by default. If enabled, PHNX may expose the normal IP after every proxy route fails.",
        )
        content.addView(directFallback)

        myProxyPanel = buildMyProxyPanel()
        freeProxyPanel = buildFreeProxyPanel()
        content.addView(myProxyPanel)
        content.addView(freeProxyPanel)

        networkState = TextView(this).apply {
            textSize = 15f
            setTextColor(getColor(R.color.phnx_text))
            setPadding(0, dp(16), 0, dp(4))
        }
        content.addView(networkState)
        result = TextView(this).apply {
            textSize = 14f
            setTextColor(getColor(R.color.phnx_muted))
            setPadding(0, dp(4), 0, dp(10))
        }
        content.addView(result)
        failureActions = buildFailureActions()
        content.addView(failureActions)

        updateModeVisibility()
        return ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.phnx_cream))
            addView(content)
        }
    }

    private fun buildMyProxyPanel(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(sectionTitle("MY PROXY"))
        proxyType = Spinner(this@NetworkActivity).apply {
            adapter = ArrayAdapter(
                this@NetworkActivity,
                android.R.layout.simple_spinner_item,
                ProxyType.values().map { it.name },
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        }
        addView(proxyType, LinearLayout.LayoutParams(-1, dp(48)))
        host = field(getString(R.string.proxy_host), InputType.TYPE_CLASS_TEXT)
        port = field(getString(R.string.proxy_port), InputType.TYPE_CLASS_NUMBER)
        username = field(getString(R.string.proxy_username), InputType.TYPE_CLASS_TEXT)
        password = field(
            getString(R.string.proxy_password),
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
        )
        addView(host)
        addView(port)
        addView(username)
        addView(password)
        val actions = LinearLayout(this@NetworkActivity).apply { gravity = Gravity.END }
        actions.addView(Button(this@NetworkActivity).apply {
            text = getString(R.string.test_connection)
            setOnClickListener { testMyProxy() }
        })
        actions.addView(Button(this@NetworkActivity).apply {
            text = getString(R.string.save_proxy)
            setOnClickListener { saveMyProxy() }
        })
        addView(actions)
    }

    private fun buildFreeProxyPanel(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(sectionTitle("FREE PUBLIC PROXY"))
        activeProxyStatus = TextView(this@NetworkActivity).apply {
            textSize = 15f
            setTextColor(getColor(R.color.phnx_text))
            setPadding(0, 0, 0, dp(12))
        }
        addView(activeProxyStatus)

        val actions = LinearLayout(this@NetworkActivity).apply { gravity = Gravity.END }
        actions.addView(Button(this@NetworkActivity).apply {
            text = getString(R.string.fetch_proxies)
            setOnClickListener { fetchProxies(forceRefresh = false) }
        })
        actions.addView(Button(this@NetworkActivity).apply {
            text = getString(R.string.refresh_proxies)
            setOnClickListener { fetchProxies(forceRefresh = true) }
        })
        actions.addView(Button(this@NetworkActivity).apply {
            text = getString(R.string.auto_select_best)
            setOnClickListener { autoSelectBest() }
        })
        addView(actions)

        protocolFilter = filterSpinner(listOf("All protocols") + ProxyType.values().map { it.name })
        sortFilter = filterSpinner(listOf("Health", "Latency", "Country", "Protocol"))
        addView(protocolFilter, LinearLayout.LayoutParams(-1, dp(48)))
        addView(sortFilter, LinearLayout.LayoutParams(-1, dp(48)))
        countryFilter = field("Country code (optional)", InputType.TYPE_CLASS_TEXT)
        addView(countryFilter)
        healthyOnly = toggle(getString(R.string.healthy_only), "Hide failed and unknown routes.")
        httpsOnly = toggle(getString(R.string.https_only), "Only show routes reported to support HTTPS.")
        addView(healthyOnly)
        addView(httpsOnly)
        freeProxyList = LinearLayout(this@NetworkActivity).apply { orientation = LinearLayout.VERTICAL }
        addView(freeProxyList)

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = renderProxyList()
            override fun afterTextChanged(s: Editable?) = Unit
        }
        countryFilter.addTextChangedListener(watcher)
        protocolFilter.onItemSelectedListener = redrawListener()
        sortFilter.onItemSelectedListener = redrawListener()
        healthyOnly.setOnCheckedChangeListener { _, _ -> renderProxyList() }
        httpsOnly.setOnCheckedChangeListener { _, _ -> renderProxyList() }
    }

    private fun buildFailureActions(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.END
        visibility = View.GONE
        addView(Button(this@NetworkActivity).apply {
            text = "Retry"
            setOnClickListener { lastFailedProxy?.let(::useProxy) ?: testMyProxy() }
        })
        addView(Button(this@NetworkActivity).apply {
            text = "Choose another proxy"
            setOnClickListener {
                freeProxyRadio.isChecked = true
                freeProxyPanel.visibility = View.VISIBLE
                result.text = "Choose a different route, then tap Use proxy."
            }
        })
        addView(Button(this@NetworkActivity).apply {
            text = "Switch to Direct"
            setOnClickListener { useDirectConnection() }
        })
    }

    private fun loadConfig() {
        val config = app.networkManager.getConfig(profileId)
        loadedConfig = config
        discoveredProxies = config.freeProxyFallbacks
        val mode = when {
            config.mode == NetworkMode.FREE_PUBLIC_PROXY -> NetworkMode.FREE_PUBLIC_PROXY
            config.mode == NetworkMode.PROXY && config.proxyHost.isBlank() && config.fallbackToFreeProxy -> NetworkMode.FREE_PUBLIC_PROXY
            config.mode.usesProxy() -> NetworkMode.MY_PROXY
            else -> NetworkMode.DIRECT
        }
        modeGroup.check(radioFor(mode).id)
        directFallback.isChecked = config.fallbackToDirect
        config.proxyType?.let { type -> ProxyType.values().indexOf(type).takeIf { it >= 0 }?.let(proxyType::setSelection) }
        host.setText(config.proxyHost)
        port.setText(config.proxyPort.takeIf { it > 0 }?.toString().orEmpty())
        username.setText(config.username)
        password.hint = if (config.credentialReference == null) getString(R.string.proxy_password)
        else getString(R.string.proxy_password_saved)
        networkState.text = "Network state: ${app.networkManager.reportConnectionState().name.lowercase()}"
        result.text = "Active mode: ${modeLabel(mode)}"
        updateActiveStatus(config)
        renderProxyList()
    }

    private fun saveMyProxy() {
        runCatching { readMyProxyConfig() }.onSuccess { config ->
            saveAndApply(config, "My proxy saved.")
        }.onFailure { showFailure(it.message ?: "Could not save proxy configuration.") }
    }

    private fun testMyProxy() {
        runCatching { readMyProxyConfig() }.onSuccess { config ->
            app.networkManager.saveConfig(config)
            loadedConfig = config
            password.text.clear()
            result.text = "Testing proxy connection..."
        operationExecutor.execute {
            val outcome = runCatching { app.networkManager.testConfig(profileId) }
            runOnUiThread {
                outcome.onSuccess { test ->
                    if (test.state == ConnectionTestState.SUCCESS) {
                        result.text = "My proxy: Healthy ${test.latencyMs ?: "?"} ms. ${test.message}"
                        hideFailureActions()
                    } else {
                        lastFailedProxy = null
                        showFailure("Proxy connection failed. ${test.message}")
                    }
                }.onFailure { error ->
                    showFailure("Proxy test failed. ${error.message ?: error.javaClass.simpleName}")
                }
            }
        }
        }.onFailure { showFailure(it.message ?: "Could not test proxy configuration.") }
    }

    private fun useDirectConnection() {
        val existing = loadedConfig ?: app.networkManager.getConfig(profileId)
        val config = ProfileNetworkConfig(
            id = existing.id,
            profileId = profileId,
            mode = NetworkMode.DIRECT,
            enabled = true,
            freeProxyFallbacks = discoveredProxies,
        )
        saveAndApply(config, "Direct connection selected.")
    }

    private fun fetchProxies(forceRefresh: Boolean) {
        freeProxyRadio.isChecked = true
        result.text = if (forceRefresh) "Refreshing public proxies and checking health..." else "Fetching public proxies and checking health..."
        operationExecutor.execute {
            val outcome = runCatching {
                app.networkManager.fetchPublicProxies(profileId, forceRefresh = forceRefresh)
            }
            runOnUiThread {
                outcome.onSuccess { proxies ->
                    discoveredProxies = proxies
                    loadedConfig = app.networkManager.getConfig(profileId)
                    renderProxyList()
                    loadedConfig?.let(::updateActiveStatus)
                    hideFailureActions()
                    result.text = if (proxies.isEmpty()) {
                        "No public proxy endpoints were returned."
                    } else {
                        "Checked ${proxies.size} public proxy endpoint(s)."
                    }
                }.onFailure { error ->
                    showFailure("Could not fetch public proxies. ${error.message ?: error.javaClass.simpleName}")
                }
            }
        }
    }

    private fun autoSelectBest() {
        val best = app.networkManager.selectBestProxy(filteredProxies())
        if (best == null) {
            result.text = "No healthy proxy is available. Fetch or refresh the list first."
        } else {
            useProxy(best)
        }
    }

    private fun useProxy(endpoint: ProxyEndpoint) {
        lastFailedProxy = endpoint
        val existing = loadedConfig ?: app.networkManager.getConfig(profileId)
        val config = ProfileNetworkConfig(
            id = existing.id,
            profileId = profileId,
            mode = NetworkMode.FREE_PUBLIC_PROXY,
            proxyType = endpoint.type,
            proxyHost = endpoint.host,
            proxyPort = endpoint.port,
            enabled = true,
            fallbackToDirect = directFallback.isChecked,
            freeProxyFallbacks = discoveredProxies,
        )
        app.networkManager.saveConfig(config)
        loadedConfig = config
        result.text = "Checking ${endpoint.host}:${endpoint.port} before applying..."
        operationExecutor.execute {
            val outcome = runCatching {
                val test = app.networkManager.testProxy(profileId, endpoint)
                val apply = if (test.state == ConnectionTestState.SUCCESS) {
                    app.networkManager.applyConfig(profileId, ChromiumProxyAdapter())
                } else {
                    null
                }
                test to apply
            }
            runOnUiThread {
                outcome.onSuccess { (test, apply) ->
                    if (test.state == ConnectionTestState.SUCCESS && apply?.status == NetworkApplyStatus.APPLIED) {
                        result.text = "Free proxy active: ${endpoint.host}:${endpoint.port} (${test.latencyMs ?: "?"} ms)."
                        hideFailureActions()
                        updateActiveStatus(config.copy(freeProxyFallbacks = discoveredProxies))
                    } else {
                        showFailure("Proxy connection failed. ${test.message}\n${apply?.message.orEmpty()}".trim())
                    }
                }.onFailure { error ->
                    showFailure("Proxy connection failed. ${error.message ?: error.javaClass.simpleName}")
                }
            }
        }
    }

    private fun saveAndApply(config: ProfileNetworkConfig, successPrefix: String) {
        app.networkManager.saveConfig(config)
        loadedConfig = config
        password.text.clear()
        operationExecutor.execute {
            val outcome = runCatching { app.networkManager.applyConfig(profileId, ChromiumProxyAdapter()) }
            runOnUiThread {
                outcome.onSuccess { apply ->
                    if (apply.status == NetworkApplyStatus.APPLIED) {
                        result.text = "$successPrefix ${apply.message}"
                        hideFailureActions()
                        updateActiveStatus(config)
                    } else {
                        showFailure("Proxy connection failed. ${apply.message}")
                    }
                }.onFailure { error ->
                    showFailure("Could not apply network configuration. ${error.message ?: error.javaClass.simpleName}")
                }
            }
        }
    }

    private fun readMyProxyConfig(): ProfileNetworkConfig {
        val existing = loadedConfig ?: app.networkManager.getConfig(profileId)
        val currentUsername = username.text.toString().trim()
        val typedPassword = password.text.toString()
        val existingReference = existing.credentialReference?.takeIf { currentUsername.isNotBlank() }
        val provisional = ProfileNetworkConfig(
            id = existing.id,
            profileId = profileId,
            mode = NetworkMode.MY_PROXY,
            proxyType = ProxyType.values()[proxyType.selectedItemPosition],
            proxyHost = host.text.toString().trim(),
            proxyPort = port.text.toString().toIntOrNull() ?: 0,
            username = currentUsername,
            credentialReference = existingReference,
            enabled = true,
            fallbackToDirect = directFallback.isChecked,
            freeProxyFallbacks = discoveredProxies,
        )
        require(NetworkConfigValidator.validate(provisional.copy(
            credentialReference = existingReference ?: typedPassword.takeIf { it.isNotBlank() }?.let { "pending" },
        )).isEmpty()) { NetworkConfigValidator.validate(provisional.copy(
            credentialReference = existingReference ?: typedPassword.takeIf { it.isNotBlank() }?.let { "pending" },
        )).joinToString(" ") }
        val reference = if (currentUsername.isNotBlank() && typedPassword.isNotBlank()) {
            app.networkManager.saveProxyCredential(profileId, typedPassword)
        } else {
            existingReference
        }
        return provisional.copy(credentialReference = reference)
    }

    private fun updateModeVisibility() {
        val mode = selectedMode()
        myProxyPanel.visibility = if (mode == NetworkMode.MY_PROXY) View.VISIBLE else View.GONE
        freeProxyPanel.visibility = if (mode == NetworkMode.FREE_PUBLIC_PROXY) View.VISIBLE else View.GONE
        directFallback.isEnabled = mode != NetworkMode.DIRECT
        if (mode == NetworkMode.DIRECT) hideFailureActions()
    }

    private fun updateActiveStatus(config: ProfileNetworkConfig) {
        if (!config.mode.usesProxy()) {
            activeProxyStatus.text = "DIRECT\nNo proxy is active."
            return
        }
        val endpoint = config.freeProxyFallbacks.firstOrNull { it.type == config.proxyType && it.host == config.proxyHost && it.port == config.proxyPort }
        val health = endpoint?.let { "${healthDot(it.health)} ${it.health.name.lowercase()}" } ?: "* unknown"
        val latency = endpoint?.latencyMs?.let { "\nLatency: ${it}ms" }.orEmpty()
        val country = endpoint?.countryCode?.takeIf { it.isNotBlank() }?.let { "\nCountry: $it" }.orEmpty()
        val checked = endpoint?.lastCheckedAt?.let { "\nLast checked: ${checkedAge(it)}" }.orEmpty()
        activeProxyStatus.text = "${modeLabel(config.mode)}\n${config.proxyType?.name ?: "No proxy selected"} ${config.proxyHost}:${config.proxyPort}\n$health$latency$country$checked"
    }

    private fun renderProxyList() {
        if (!::freeProxyList.isInitialized) return
        freeProxyList.removeAllViews()
        filteredProxies().forEach { endpoint -> freeProxyList.addView(proxyCard(endpoint)) }
        if (freeProxyList.childCount == 0) {
            freeProxyList.addView(text("No proxies match the current filters. Tap Fetch proxies to discover routes.", 14f))
        }
    }

    private fun filteredProxies(): List<ProxyEndpoint> {
        val protocol = protocolFilter.selectedItem?.toString().orEmpty()
        val country = countryFilter.text.toString().trim().uppercase()
        val filtered = discoveredProxies.filter { endpoint ->
            (protocol == "All protocols" || endpoint.type.name == protocol) &&
                (country.isBlank() || endpoint.countryCode.contains(country)) &&
                (!healthyOnly.isChecked || endpoint.health == ProxyHealthStatus.HEALTHY) &&
                (!httpsOnly.isChecked || endpoint.httpsSupported)
        }
        return when (sortFilter.selectedItem?.toString()) {
            "Latency" -> filtered.sortedBy { it.latencyMs ?: it.reportedLatencyMs ?: Int.MAX_VALUE }
            "Country" -> filtered.sortedBy { it.countryCode }
            "Protocol" -> filtered.sortedBy { it.type.name }
            else -> filtered.sortedWith(compareBy<ProxyEndpoint> { healthRank(it.health) }.thenBy { it.latencyMs ?: Int.MAX_VALUE })
        }
    }

    private fun proxyCard(endpoint: ProxyEndpoint): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(12), dp(14), dp(12))
        setBackgroundColor(getColor(R.color.phnx_navy))
        addView(text(endpoint.type.name, 17f))
        addView(text("${endpoint.host}:${endpoint.port}", 15f))
        addView(text("${endpoint.countryCode.ifBlank { "--" }}  ${healthDot(endpoint.health)} ${endpoint.health.name.lowercase()}  ${endpoint.latencyMs?.let { "$it ms" } ?: "latency unknown"}", 14f))
        addView(text("Last checked: ${endpoint.lastCheckedAt?.let(::checkedAge) ?: "not checked"}", 13f))
        if (endpoint.source.isNotBlank()) addView(text("Source: ${endpoint.source}", 13f))
        if (endpoint.anonymity.isNotBlank() || endpoint.httpsSupported) {
            addView(text("${endpoint.anonymity.ifBlank { "Anonymity unknown" }}${if (endpoint.httpsSupported) "  HTTPS supported" else ""}", 13f))
        }
        addView(Button(this@NetworkActivity).apply {
            text = getString(R.string.use_proxy)
            setOnClickListener { useProxy(endpoint) }
        })
    }

    private fun showFailure(message: String) {
        result.text = message
        failureActions.visibility = View.VISIBLE
    }

    private fun hideFailureActions() {
        if (::failureActions.isInitialized) failureActions.visibility = View.GONE
    }

    private fun selectedMode(): NetworkMode = when (modeGroup.checkedRadioButtonId) {
        myProxyRadio.id -> NetworkMode.MY_PROXY
        freeProxyRadio.id -> NetworkMode.FREE_PUBLIC_PROXY
        else -> NetworkMode.DIRECT
    }

    private fun radioFor(mode: NetworkMode): RadioButton = when (mode) {
        NetworkMode.MY_PROXY, NetworkMode.PROXY -> myProxyRadio
        NetworkMode.FREE_PUBLIC_PROXY -> freeProxyRadio
        NetworkMode.DIRECT -> directRadio
    }

    private fun modeLabel(mode: NetworkMode): String = when (mode) {
        NetworkMode.DIRECT -> "DIRECT"
        NetworkMode.MY_PROXY, NetworkMode.PROXY -> "MY PROXY"
        NetworkMode.FREE_PUBLIC_PROXY -> "FREE PUBLIC PROXY"
    }

    private fun healthRank(status: ProxyHealthStatus): Int = when (status) {
        ProxyHealthStatus.HEALTHY -> 0
        ProxyHealthStatus.SLOW -> 1
        ProxyHealthStatus.CHECKING -> 2
        ProxyHealthStatus.UNKNOWN -> 3
        ProxyHealthStatus.FAILED -> 4
    }

    private fun healthDot(status: ProxyHealthStatus): String = when (status) {
        ProxyHealthStatus.HEALTHY -> "●"
        ProxyHealthStatus.SLOW -> "●"
        ProxyHealthStatus.CHECKING -> "◌"
        ProxyHealthStatus.UNKNOWN -> "○"
        ProxyHealthStatus.FAILED -> "×"
    }

    private fun checkedAge(timestamp: Long): String {
        val seconds = ((System.currentTimeMillis() - timestamp).coerceAtLeast(0L) / 1000L)
        return when {
            seconds < 60 -> "$seconds sec ago"
            seconds < 3600 -> "${seconds / 60} min ago"
            else -> "${seconds / 3600} hr ago"
        }
    }

    private fun header(value: String) = TextView(this).apply {
        text = value
        textSize = 30f
        setTextColor(getColor(R.color.phnx_blue))
    }

    private fun sectionTitle(value: String) = TextView(this).apply {
        text = value
        textSize = 18f
        setTextColor(getColor(R.color.phnx_blue))
        setPadding(0, dp(20), 0, dp(8))
    }

    private fun warning(value: String) = TextView(this).apply {
        text = value
        textSize = 13f
        setTextColor(getColor(R.color.phnx_error))
        setPadding(0, dp(8), 0, dp(12))
    }

    private fun text(value: String, size: Float = 14f) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(getColor(R.color.phnx_text))
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun modeRadio(label: String) = RadioButton(this).apply {
        id = View.generateViewId()
        text = label
        textSize = 16f
        setTextColor(getColor(R.color.phnx_text))
    }

    private fun filterSpinner(values: List<String>) = Spinner(this).apply {
        adapter = ArrayAdapter(
            this@NetworkActivity,
            android.R.layout.simple_spinner_item,
            values,
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    }

    private fun redrawListener() = object : android.widget.AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) = renderProxyList()
    }

    private fun field(hintText: String, type: Int) = EditText(this).apply {
        hint = hintText
        inputType = type
        isSingleLine = true
        setPadding(0, dp(8), 0, dp(8))
    }

    private fun toggle(title: String, summary: String): SwitchCompat = SwitchCompat(this).apply {
        text = "$title\n$summary"
        textSize = 15f
        setTextColor(getColor(R.color.phnx_text))
        setPadding(0, dp(8), 0, dp(8))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
