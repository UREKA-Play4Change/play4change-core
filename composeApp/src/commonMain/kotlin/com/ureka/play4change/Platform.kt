package com.ureka.play4change

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform