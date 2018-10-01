package io.github.runedata.cache.filesystem

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

fun main(args: Array<String>) {
    val cache = CacheStore(FileStore.open("resources/cache"), parseXteaJsonFile(File("resources/xteas.json")))
}

fun parseXteaJsonFile(file: File): Map<Int, IntArray> {
    val mapper = ObjectMapper().registerKotlinModule()
    val keys: List<XteaJson> = mapper.readValue(file, object : TypeReference<List<XteaJson>>(){})
    val keyMap = mutableMapOf<Int, IntArray>()
    for(xtea in keys) {
        keyMap[xtea.region] = xtea.keys
    }
    return keyMap
}

private class XteaJson(val region: Int, val keys: IntArray)