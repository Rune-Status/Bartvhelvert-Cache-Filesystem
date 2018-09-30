/*
   Copyright 2018 Bart van Helvert

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package io.github.runedata.cache.filesystem

import io.github.runedata.cache.filesystem.crypto.Compression
import io.github.runedata.cache.filesystem.crypto.Xtea
import io.github.runedata.cache.filesystem.util.getByteArray
import io.github.runedata.cache.filesystem.util.toByteArray
import io.github.runedata.cache.filesystem.util.xteaDecipher
import io.github.runedata.cache.filesystem.util.xteaEncipher
import java.io.IOException
import java.nio.ByteBuffer

/**
 * A [ArchiveData] holds the data of an archive in an optionally compressed and encrypted format.
 */
class ArchiveData(val compressionType: Int, val data: ByteBuffer, var version: Int = -1) {
    fun encode(): ByteBuffer {
        return encode(Xtea.NULL_KEYS)
    }

    fun encode(xteaKeys: IntArray): ByteBuffer {
        val compressedData = data.compress()
        val headerSize = 5 + (if (compressionType == Compression.NONE.opcode) 0 else 4) + if (isVersioned()) 2 else 0
        val buffer = ByteBuffer.allocate(headerSize + compressedData.size)
        buffer.put(compressionType.toByte())
        buffer.putInt(compressedData.size)
        if (compressionType != Compression.NONE.opcode) {
            buffer.putInt(data.limit())
        }
        buffer.put(compressedData)
        if (isVersioned()) {
            buffer.putShort(version.toShort())
        }
        return buffer.encryptCompressedData(xteaKeys, compressedData.size).flip() as ByteBuffer
    }

    private fun ByteBuffer.compress(): ByteArray {
        return Compression.values().find { it.opcode == compressionType }?.compress(toByteArray()) ?:
            throw IOException("Unsupported compression method")
    }

    private fun ByteBuffer.encryptCompressedData(xteaKeys: IntArray, compressedLength: Int): ByteBuffer {
        return if (xteaKeys.all { it != 0 }) {
            xteaEncipher(xteaKeys, start = 5, end = compressedLength +
                    if (compressionType == Compression.NONE.opcode) 5 else 9)
        } else {
            return this
        }
    }

    fun isVersioned(): Boolean {
        return version != -1
    }

    fun removeVersion() {
        this.version = -1
    }

    companion object {
        fun decode(buffer: ByteBuffer, xteaKeys: IntArray = Xtea.NULL_KEYS): ArchiveData {
            val compressionType = buffer.get().toInt() and 0xFF
            val compressedSize = buffer.int
            buffer.decryptCompressedData(xteaKeys, compressionType, compressedSize)

            if (compressionType == Compression.NONE.opcode) {
                val data = ByteBuffer.wrap(buffer.getByteArray(compressedSize))
                val version = decodeVersion(buffer)
                return ArchiveData(compressionType, data, version)
            } else {
                val uncompressedSize = buffer.int
                val uncompressed = Compression.values().find { it.opcode == compressionType }?.
                        decompress(buffer.getByteArray(compressedSize))
                        ?: throw IOException("Unsupported decompression method")
                if (uncompressed.size != uncompressedSize) {
                    throw IOException("Uncompressed size mismatch")
                }
                val version = decodeVersion(buffer)
                return ArchiveData(
                        compressionType,
                        ByteBuffer.wrap(uncompressed),
                        version
                )
            }
        }

        private fun decodeVersion(buffer: ByteBuffer) = if (buffer.remaining() >= 2) {
            buffer.short.toInt()
        } else {
            -1
        }

        private fun ByteBuffer.decryptCompressedData(xteaKeys: IntArray, compressionType: Int, compressedLength: Int): ByteBuffer {
            return if (xteaKeys.all { it != 0 }) {
                xteaDecipher(xteaKeys, 5, compressedLength +
                        if (compressionType == Compression.NONE.opcode) 5 else 9)
            } else {
                this
            }
        }
    }
}
