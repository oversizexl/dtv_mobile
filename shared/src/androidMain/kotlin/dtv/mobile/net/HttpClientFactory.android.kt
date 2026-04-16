package dtv.mobile.net

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import dtv.mobile.util.AppLog

actual fun createHttpClient(): HttpClient {
  return HttpClient(OkHttp) {
    install(HttpTimeout) {
      requestTimeoutMillis = 15_000
      connectTimeoutMillis = 10_000
      socketTimeoutMillis = 15_000
    }
    install(Logging) {
      logger = object : Logger {
        override fun log(message: String) {
          AppLog.d("DTV-HTTP", message)
        }
      }
      level = LogLevel.HEADERS
    }
    install(ContentNegotiation) {
      json(
        Json {
          ignoreUnknownKeys = true
          isLenient = true
        },
      )
    }
    defaultRequest {
      headers {
        append(HttpHeaders.AcceptLanguage, "zh-CN,zh;q=0.9")
        append(
          HttpHeaders.UserAgent,
          "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
        )
      }
    }
  }
}
