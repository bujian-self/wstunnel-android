package cn.bujian.wstunnel.client

import android.content.Context

object Prefs {
    private const val FILE = "prefs"
    private lateinit var sp: android.content.SharedPreferences

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    var serverUrl: String
        get() = sp.getString("server_url", "").orEmpty()
        set(v) = sp.edit().putString("server_url", v).apply()

    var sni: String
        get() = sp.getString("sni", "").orEmpty()
        set(v) = sp.edit().putString("sni", v).apply()

    var socksPort: Int
        get() = sp.getInt("socks_port", 8888)
        set(v) = sp.edit().putInt("socks_port", v).apply()

    var poolSize: Int
        get() = sp.getInt("pool_size", 4)
        set(v) = sp.edit().putInt("pool_size", v).apply()

    var username: String
        get() = sp.getString("username", "").orEmpty()
        set(v) = sp.edit().putString("username", v).apply()

    var password: String
        get() = sp.getString("password", "").orEmpty()
        set(v) = sp.edit().putString("password", v).apply()
}
