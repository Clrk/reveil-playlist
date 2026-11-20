package fr.clrk.reveil

import okhttp3.OkHttpClient
import okhttp3.Request

/** Blocking GET: the body when the server answers 2xx, null on any failure. */
fun OkHttpClient.text(url: String, headers: Map<String, String> = emptyMap()): String? = runCatching {
    val request = Request.Builder().url(url).apply { headers.forEach { (name, value) -> header(name, value) } }.build()
    newCall(request).execute().use { if (it.isSuccessful) it.body.string() else null }
}.getOrNull()
