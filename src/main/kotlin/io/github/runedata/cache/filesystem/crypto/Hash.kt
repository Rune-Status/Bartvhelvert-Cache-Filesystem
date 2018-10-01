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

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.MessageDigest
import java.security.Security

fun djb2Hash(str: String): Int {
    var hash = 0
    for (i in 0 until str.length) {
        hash = str[i].toInt() + ((hash shl 5) - hash)
    }
    return hash
}

fun whirlPoolHash(data: ByteArray):ByteArray {
    Security.addProvider(BouncyCastleProvider())
    val messageDigest = MessageDigest.getInstance("Whirlpool")
    return messageDigest.digest(data)
}