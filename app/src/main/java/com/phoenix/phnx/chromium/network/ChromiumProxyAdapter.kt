package com.phoenix.phnx.chromium.network

import com.phoenix.phnx.network.ChromiumNetworkAdapter
import com.phoenix.phnx.network.ProfileNetworkConfig
import com.phoenix.phnx.network.WebViewNetworkAdapter
import com.phoenix.phnx.network.NetworkApplyResult

class ChromiumProxyAdapter : ChromiumNetworkAdapter {
    private val delegate = WebViewNetworkAdapter()

    override fun apply(config: ProfileNetworkConfig): NetworkApplyResult = delegate.apply(config)
}
