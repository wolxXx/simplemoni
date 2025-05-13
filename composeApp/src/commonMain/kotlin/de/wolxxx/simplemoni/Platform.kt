package de.wolxxx.simplemoni

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform