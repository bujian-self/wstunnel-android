package cn.bujian.wstunnel

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val FILE = "config"

    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        if (!::sp.isInitialized) {
            sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        }
    }

    var serverUrl: String
        get() = sp.getString("server_url", "").orEmpty()
        set(value) = sp.edit().putString("server_url", value).apply()

    var sni: String
        get() = sp.getString("sni", "").orEmpty()
        set(value) = sp.edit().putString("sni", value).apply()

    var socksPort: Int
        get() = sp.getInt("socks_port", 8888)
        set(value) = sp.edit().putInt("socks_port", value).apply()

    var dns: String
        get() = sp.getString("dns", "198.18.0.2").orEmpty()
        set(value) = sp.edit().putString("dns", value).apply()

    var poolSize: Int
        get() = sp.getInt("pool_size", 4)
        set(value) = sp.edit().putInt("pool_size", value).apply()

    var username: String
        get() = sp.getString("username", "").orEmpty()
        set(value) = sp.edit().putString("username", value).apply()

    var password: String
        get() = sp.getString("password", "").orEmpty()
        set(value) = sp.edit().putString("password", value).apply()
}
