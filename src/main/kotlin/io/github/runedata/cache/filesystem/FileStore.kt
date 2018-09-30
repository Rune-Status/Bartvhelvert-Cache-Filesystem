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

import mu.KLogging
import java.io.Closeable
import java.io.EOFException
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/** Serves as an access point to the underlying filesystem. */
class FileStore(
    val dataFile: FileChannelNamePair,
    val indexFiles: Array<FileChannelNamePair>,
    val metaFile: FileChannelNamePair
) : Closeable {
    /** Reads an archive from this [FileStore] */
    fun readArchive(indexFileId: Int, archiveId: Int) = readArchive(readIndex(indexFileId, archiveId))

    /** Reads an index from this [FileStore] */
    fun readIndex(indexFileId: Int, archiveId: Int): Index {
        if ((indexFileId < 0 || indexFileId >= indexFiles.size) && indexFileId != 255) throw FileNotFoundException()
        val indexFile = if (indexFileId == 255) metaFile else indexFiles[indexFileId]
        val ptr = archiveId.toLong() * Index.SIZE.toLong()
        if (ptr < 0 || ptr >= indexFile.channel.size()) throw FileNotFoundException()
        val buffer = ByteBuffer.allocate(Index.SIZE)
        indexFile.channel.readFully(buffer, ptr)
        return Index.decode(
            indexFileId,
            archiveId,
            buffer.flip() as ByteBuffer
        )
    }

    /** Reads an archive from this [FileStore] */
    fun readArchive(index: Index): ByteBuffer {
        val rawArchiveData = ByteBuffer.allocate(index.size)
        var amountOfSegmentsRead = 0
        var dataLeftToRead = index.size
        var nextSegmentId = index.startSector.toLong() * DataSegment.SIZE.toLong()
        val tempBuffer = ByteBuffer.allocate(DataSegment.SIZE)
        do {
            tempBuffer.clear()
            dataFile.channel.readFully(tempBuffer, nextSegmentId)
            val dataSegment = DataSegment.decode(
                index.archiveId,
                tempBuffer.flip() as ByteBuffer
            )
            if (dataLeftToRead > dataSegment.data.size) {
                dataSegment.validate(index.indexFileId, index.archiveId, amountOfSegmentsRead)
                amountOfSegmentsRead++
                rawArchiveData.put(dataSegment.data, 0, dataSegment.data.size)
                dataLeftToRead -= dataSegment.data.size
                nextSegmentId = dataSegment.nextSegment.toLong() * DataSegment.SIZE.toLong()
            } else {
                rawArchiveData.put(dataSegment.data, 0, dataLeftToRead)
                dataLeftToRead = 0
            }
        } while (dataLeftToRead > 0)
        return rawArchiveData.flip() as ByteBuffer
    }

    /** Reads everything in the [FileChannel] starting from [startPosition] */
    private fun FileChannel.readFully(buffer: ByteBuffer, startPosition: Long) {
        var ptr = startPosition
        while (buffer.remaining() > 0) {
            val read = read(buffer, ptr).toLong()
            if (read < -1) {
                throw EOFException()
            } else {
                ptr += read
            }
        }
    }

    /** Returns whether the data file contains data */
    fun hasData(): Boolean {
        return dataFile.channel.size() > 0
    }

    /** Returns the amount of index entries in an index file */
    fun getIndexEntries(type: Int): Int {
        if ((type < 0 || type >= indexFiles.size) && type != 255) {
            throw FileNotFoundException()
        }
        return if (type == 255) {
            (metaFile.channel.size() / Index.SIZE).toInt()
        } else {
            (indexFiles[type].channel.size() / Index.SIZE).toInt()
        }
    }

    /** Returns the amount of index files */
    fun getIndexFileCount(): Int {
        return indexFiles.size
    }

    /** Closes the [FileStore] */
    override fun close() {
        dataFile.close()
        for (file in indexFiles) {
            file.close()
        }
        metaFile.close()
    }

    companion object : KLogging() {
        private const val DEFAULT_FILE_NAME = "main_file_cache"
        private const val DATA_FILE_EXTENSION = ".dat2"
        private const val INDEX_FILE_EXTENSION = ".idx"
        private const val METACHANNEL_INDEX_ID = 255

        /** Searches a [rootPath] and opense a [FileStore] */
        fun open(rootPath: String): FileStore {
            return open(File(rootPath))
        }

        /** Searches a [root] folder and opens a [FileStore] using the [DEFAULT_FILE_NAME]*/
        fun open(root: File): FileStore {
            val dataFile = File(root, "$DEFAULT_FILE_NAME$DATA_FILE_EXTENSION")
            if (!dataFile.exists()) throw FileNotFoundException()
            val indexFiles = mutableListOf<File>()
            for (indexFileId in 0 until METACHANNEL_INDEX_ID) {
                val file = File(root, "$DEFAULT_FILE_NAME$INDEX_FILE_EXTENSION$indexFileId")
                if(!file.exists()) break
                indexFiles.add(file)
            }
            if (indexFiles.isEmpty()) throw FileNotFoundException()
            val metaFile = File(root, "$DEFAULT_FILE_NAME$INDEX_FILE_EXTENSION$METACHANNEL_INDEX_ID")
            if (!metaFile.exists()) throw FileNotFoundException()
            return open(dataFile, indexFiles, metaFile)
        }

        /** Opens a [FileStore] */
        fun open(dataFile: File, indexFiles: List<File>, metaFile: File): FileStore {
            val dataChannel = FileChannelNamePair(dataFile.name, RandomAccessFile(dataFile, "rw").channel)
            val indexChannels = mutableListOf<FileChannelNamePair>()
            for (indexFile in indexFiles) {
                indexChannels.add(FileChannelNamePair(indexFile.name, RandomAccessFile(indexFile, "rw").channel))
            }
            val metaChannel = FileChannelNamePair(metaFile.name, RandomAccessFile(metaFile, "rw").channel)
            logger.info("Loaded cache with ${indexChannels.size} index files")
            return FileStore(dataChannel, indexChannels.toTypedArray(), metaChannel)
        }
    }
}

class FileChannelNamePair(val name: String, val channel: FileChannel) : Closeable {
    override fun close() {
        channel.close()
    }
}


