package com.phoenix.phnx.system

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build

enum class DefaultBrowserState {
    DEFAULT,
    NOT_DEFAULT,
    UNKNOWN,
    UNAVAILABLE,
}

class DefaultBrowserManager(private val context: Context) {
    fun state(): DefaultBrowserState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return DefaultBrowserState.UNKNOWN
        val roleManager = context.getSystemService(RoleManager::class.java)
            ?: return DefaultBrowserState.UNAVAILABLE
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER)) return DefaultBrowserState.UNAVAILABLE
        return if (roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)) {
            DefaultBrowserState.DEFAULT
        } else {
            DefaultBrowserState.NOT_DEFAULT
        }
    }

    fun requestRoleIntent(): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val roleManager = context.getSystemService(RoleManager::class.java) ?: return null
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER)) return null
        return roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
    }
}
