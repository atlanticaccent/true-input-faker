@file:Suppress("unused")

package com.crimes_collection.true_input_faker

import java.nio.Buffer
import java.nio.ByteBuffer

fun ByteBuffer.zero() {
    val buf = this as Buffer
    buf.clear()
    put(ByteArray(capacity()) { 0 })
    buf.rewind()
}

fun ByteBuffer.copyToArray(): ByteArray {
    val dupe = duplicate().asReadOnlyBuffer()
    val arr = ByteArray((dupe as Buffer).limit())
    for (idx in arr.indices) {
        arr[idx] = dupe.get(idx)
    }
    return arr
}