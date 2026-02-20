package com.jonecx.ibex.ui.permission

import android.os.Build
import android.os.Environment
import javax.inject.Inject

interface PermissionChecker {
    fun hasStoragePermission(): Boolean
}

class RealPermissionChecker @Inject constructor() : PermissionChecker {
    override fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }
}
