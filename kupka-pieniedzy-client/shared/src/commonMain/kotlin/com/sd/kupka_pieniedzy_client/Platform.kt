package com.sd.kupka_pieniedzy_client

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
