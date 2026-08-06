package com.jonecx.ibex.di

import com.jonecx.ibex.ui.permission.PermissionChecker
import com.jonecx.ibex.ui.permission.RealPermissionChecker
import org.koin.dsl.module

val permissionModule = module {
    single<PermissionChecker> { RealPermissionChecker() }
}
