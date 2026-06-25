package com.ajrpachon.chatapp.utils

import android.content.pm.PackageManager
import android.os.Build
import java.io.File

object RootDetector {

    fun isRooted(packageManager: PackageManager): Boolean =
        checkSuBinaries() || checkBuildTags() || checkRootPackageNames(packageManager)

    private fun checkSuBinaries(): Boolean {
        val paths = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/system/app/Superuser.apk", "/system/app/SuperSU.apk",
            "/data/local/xbin/su", "/data/local/bin/su", "/data/local/su",
        )
        return paths.any { File(it).exists() }
    }

    private fun checkBuildTags(): Boolean =
        Build.TAGS?.contains("test-keys") == true

    private fun checkRootPackageNames(pm: PackageManager): Boolean {
        val rootPackages = listOf(
            "com.noshufou.android.su",
            "com.thirdparty.superuser",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.topjohnwu.magisk",
            "io.github.lsposed.manager",
        )
        return rootPackages.any { pkg ->
            try { pm.getPackageInfo(pkg, 0); true } catch (_: Exception) { false }
        }
    }
}
