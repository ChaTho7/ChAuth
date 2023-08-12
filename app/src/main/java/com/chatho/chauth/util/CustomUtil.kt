package com.chatho.chauth.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import java.lang.reflect.Field
import kotlin.coroutines.CoroutineContext

fun findConstantFieldName(clazz: Class<*>, prefix: String, value: Int): String {
    val fields: Array<Field> = clazz.fields
    for (field in fields) {
        if (field.name.startsWith(prefix)) {
            try {
                val fieldValue: Int = field.getInt(null)
                if (fieldValue == value) {
                    return field.name.removePrefix(prefix)
                }
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
    }
    return "UNKNOWN"
}

fun isNullOrDefault(value: Any?): Boolean {
    return value == null || value == defaultValue(value)
}

private fun defaultValue(value: Any): Any {
    return when (value) {
        is String -> ""
        is Long -> 0L
        else -> throw IllegalArgumentException("Unknown type: ${value.javaClass}")
    }
}

fun runInCoroutineScope(context: CoroutineContext, runBlock: suspend CoroutineScope.() -> Unit) {
    CoroutineScope(context).launch(
        context, CoroutineStart.DEFAULT, runBlock
    )
}
