package dtv.mobile.util

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

fun jsonElementToString(value: JsonElement?): String? {
  return when (value) {
    null -> null
    is JsonPrimitive -> {
      if (value.isString) value.content else value.content
    }
    else -> value.toString()
  }
}

fun jsonElementToInt(value: JsonElement?): Int? {
  return when (value) {
    null -> null
    is JsonPrimitive -> value.content.toIntOrNull()
    else -> value.toString().toIntOrNull()
  }
}
