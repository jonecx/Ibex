package com.jonecx.ibex.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AppStorageUtilsTest {

    private val context = RuntimeEnvironment.getApplication() as Context

    private fun installApp(pkg: String, system: Boolean, sizeBytes: Int) {
        val apk = File.createTempFile(pkg, ".apk").apply { writeBytes(ByteArray(sizeBytes)) }
        val info = ApplicationInfo().apply {
            packageName = pkg
            sourceDir = apk.absolutePath
            flags = if (system) ApplicationInfo.FLAG_SYSTEM else 0
        }
        val packageInfo = PackageInfo().apply {
            packageName = pkg
            applicationInfo = info
        }
        shadowOf(context.packageManager).installPackage(packageInfo)
    }

    @Test
    fun `userInstalledApps_systemAppPresent_excludesSystemApp`() {
        installApp("com.example.user", system = false, sizeBytes = 10)
        installApp("com.example.system", system = true, sizeBytes = 10)

        val packages = AppStorageUtils.userInstalledApps(context).map { it.packageName }

        assertTrue(packages.contains("com.example.user"))
        assertFalse(packages.contains("com.example.system"))
    }

    @Test
    fun `appStats_mixedApps_sumsUserApkSizesOnly`() {
        val before = AppStorageUtils.appStats(context)
        installApp("com.example.a", system = false, sizeBytes = 1000)
        installApp("com.example.b", system = false, sizeBytes = 500)
        installApp("com.example.sys", system = true, sizeBytes = 999_999)

        val after = AppStorageUtils.appStats(context)

        assertEquals(before.count + 2, after.count)
        assertEquals(before.sizeBytes + 1500L, after.sizeBytes)
    }
}
