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
package io.github.runedata.cache.filesystem.crypto

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream

enum class Compression(val opcode: Int, val headerSize: Int) {
    NONE(opcode = 0, headerSize = 0) {
        override fun compress(bytes: ByteArray) = bytes

        override fun decompress(bytes: ByteArray) = bytes
    },

    BZIP2(opcode = 1, headerSize = Integer.BYTES) {
        private val blockSize = 1

        override fun compress(bytes: ByteArray): ByteArray {
            var bytesToZip = bytes
            ByteArrayInputStream(bytesToZip).use { inStream ->
                val bout = ByteArrayOutputStream()
                BZip2CompressorOutputStream(bout, blockSize).use { outStream ->
                    val buf = ByteArray(4096)
                    var len = inStream.read(buf, 0, buf.size)
                    while (len != -1) {
                        outStream.write(buf, 0, len)
                        len = inStream.read(buf, 0, buf.size)
                    }
                }
                bytesToZip = bout.toByteArray()
                val bzip2 = ByteArray(bytesToZip.size - 2)
                System.arraycopy(bytesToZip, 2, bzip2, 0, bzip2.size)
                return bzip2
            }
        }

        override fun decompress(bytes: ByteArray): ByteArray {
            val bzip2 = ByteArray(bytes.size + 2)
            bzip2[0] = 'h'.toByte()
            bzip2[1] = '1'.toByte()
            System.arraycopy(bytes, 0, bzip2, 2, bytes.size)

            BZip2CompressorInputStream(ByteArrayInputStream(bzip2)).use { inStream ->
                val outputStream = ByteArrayOutputStream()
                outputStream.use { outStream ->
                    val buf = ByteArray(4096)
                    var len = inStream.read(buf, 0, buf.size)
                    while (len != -1) {
                        outStream.write(buf, 0, len)
                        len = inStream.read(buf, 0, buf.size)
                    }
                }
                return outputStream.toByteArray()
            }
        }
    },

    GZIP2(opcode = 2, headerSize = Integer.BYTES) {
        override fun decompress(bytes: ByteArray): ByteArray {
            val inputStream = ByteArrayInputStream(bytes)
            inputStream.use { inStream ->
                val bout = ByteArrayOutputStream()
                val outputStream = GZIPOutputStream(bout)
                outputStream.use { outStream ->
                    val buf = ByteArray(4096)
                    var len = inStream.read(buf, 0, buf.size)
                    while (len != -1) {
                        outStream.write(buf, 0, len)
                        len = inStream.read(buf, 0, buf.size)
                    }
                }
                return bout.toByteArray()
            }
        }

        override fun compress(bytes: ByteArray): ByteArray {
            val inputStream = GZIPInputStream(ByteArrayInputStream(bytes))
            inputStream.use { inStream ->
                val outputStream = ByteArrayOutputStream()
                outputStream.use { outStream ->
                    val buf = ByteArray(4096)
                    var len = inStream.read(buf, 0, buf.size)
                    while (len!= -1) {
                        outStream.write(buf, 0, len)
                        len = inStream.read(buf, 0, buf.size)
                    }
                }
                return outputStream.toByteArray()
            }
        }
    };

    abstract fun decompress(bytes: ByteArray): ByteArray

    abstract fun compress(bytes: ByteArray): ByteArray
}