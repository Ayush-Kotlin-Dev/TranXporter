package com.ayush.tranxporter.driver.di

import org.koin.dsl.module
val driverModule = module {
    includes(driverViewModel)
}