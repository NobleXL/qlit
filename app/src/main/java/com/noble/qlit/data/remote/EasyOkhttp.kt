package com.noble.qlit.data.remote

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * @author: noble
 * @desc: 封装Okhttp
 */
object EasyOkhttp {

    val sessionCookieJar = SessionCookieJar()

    var cookie by mutableStateOf("")

    val okHttpClient: OkHttpClient = OkHttpClient().newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .cookieJar(sessionCookieJar)
        .build()

    private fun sendHttpRequest(address: String, body: RequestBody? = null, callback: Callback) {
        val request =
            if (body != null) {
                Request.Builder()
                    .url(address)
                    .post(body)
                    .build()
            } else {
                Request.Builder()
                    .url(address)
                    .build()
            }
        okHttpClient.newCall(request).enqueue(callback)
    }

    suspend fun request(address: String, body: RequestBody? = null): String {
        return suspendCoroutine {
            sendHttpRequest(address, body, object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    it.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val resBody = response.body?.string()
                    Log.d("HTTP", "onResponse: ${response.code}")
                    if (resBody != null) {
                        it.resume(resBody.toString())
                    } else {
                        it.resumeWithException(RuntimeException("响应正文为空"))
                    }
                }
            })
        }
    }

    // Cookie持久化
    class SessionCookieJar : CookieJar {
        private val cookieStore: HashMap<String, List<Cookie>> = HashMap()
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies
            // 获取cookie值
            cookie = cookies.toString().substringAfter("[").substringBefore("]")
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val cookies = cookieStore[url.host]
            return cookies ?: ArrayList()
        }
    }

}