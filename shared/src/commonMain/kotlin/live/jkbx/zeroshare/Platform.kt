package live.jkbx.zeroshare

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform