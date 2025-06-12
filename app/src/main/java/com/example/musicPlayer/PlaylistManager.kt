package com.example.musicPlayer

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Converters {
    @TypeConverter
    fun fromListLong(value: List<Long>): String {
        return if (value.isEmpty()) "" else value.joinToString(",")
    }

    @TypeConverter
    fun toListLong(value: String): List<Long> {
        return if (value == "") emptyList() else value.split(",").map { it.toLong() }
    }
}

@Entity
data class AudioEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val albumArtUri: String,
    val data: String
)

@Entity
data class PlaylistEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val audiosId: List<Long>
)

@Dao
interface AudioDao {
    @Insert
    suspend fun insert(audio: AudioEntity)

    @Update
    suspend fun update(audio: AudioEntity)

    @Query("DELETE FROM audioentity WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM audioentity")
    suspend fun getAllAudios(): List<AudioEntity>

    @Query("SELECT * FROM audioentity WHERE id=:id LIMIT 1")
    suspend fun getAudioById(id: Long): AudioEntity?
}

@Dao
interface PlaylistDao {
    @Insert
    suspend fun insert(playlist: PlaylistEntity)

    @Update
    suspend fun update(playlist: PlaylistEntity)

    @Delete
    suspend fun delete(playlist: PlaylistEntity)

    @Query("DELETE FROM playlistentity WHERE id = :playlistId")
    suspend fun deleteById(playlistId: Long)

    @Query("SELECT * FROM playlistentity")
    suspend fun getAllPlaylists(): List<PlaylistEntity>
}


@Database(entities = [AudioEntity::class, PlaylistEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audioDao(): AudioDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "app_database").build()
            }
            return INSTANCE!!
        }
    }
}

class PlaylistManager(private val context: Context, onLoadFinish: () -> Unit) {

    companion object {
        private val playlists = mutableListOf<Playlist>()
        private val audioFiles = mutableListOf<AudioFile>()
        private var firstLoad = true

        fun getPlaylists(): MutableList<Playlist> {
            return playlists
        }
        fun getPlaylistById(playlistId: Long): Playlist? {
            return playlists.find { it.id == playlistId }
        }

        fun getAudioFiles(): MutableList<AudioFile> {
            return audioFiles
        }
        fun getAudioFileById(audioId: Long): AudioFile? {
            return audioFiles.find { it.id ==  audioId }
        }
    }

    private var loadFinish = false

    init {
        // Obtenez une instance de la base de données
        if (firstLoad) {
            firstLoad = false
            val db = AppDatabase.getDatabase(context)
            val playlistDao = db.playlistDao()
            val audioDao = db.audioDao()
            val scope = CoroutineScope(Dispatchers.IO)
            val job = scope.launch {
                val playlistsEntity = playlistDao.getAllPlaylists()
                playlistsEntity.forEach { playlist ->
                    playlists.add(
                        Playlist(
                            id = playlist.id,
                            name = playlist.name,
                            audios = getAudiosListById(playlist.audiosId)
                        )
                    )
                }
                val audioListEntity = audioDao.getAllAudios()
                audioListEntity.forEach { audio ->
                    audioFiles.add(audioEntityToAudioFile(audio))
                }
            }
            job.invokeOnCompletion {
                loadFinish = true
                onLoadFinish()
            }
        } else {
            loadFinish = true
            onLoadFinish()
        }
    }

    private suspend fun getAudiosListById(audiosId: List<Long>): MutableList<AudioFile> {
        val audioList: MutableList<AudioFile> = mutableListOf()
        val db = AppDatabase.getDatabase(context)
        val audioDao = db.audioDao()
        for (id in audiosId) {
            val audioEntity: AudioEntity? = audioDao.getAudioById(id)
            if (audioEntity != null) {
                audioList.add(audioEntityToAudioFile(audioEntity))
            }
        }
        return audioList
    }

    private fun audioEntityToAudioFile(audioEntity: AudioEntity): AudioFile {
        return AudioFile(
            id = audioEntity.id,
            title = audioEntity.title,
            artist = audioEntity.artist,
            duration = audioEntity.duration,
            albumArtUri = audioEntity.albumArtUri,
            data = audioEntity.data
        )
    }
    private fun audioFileToAudioEntity(audio: AudioFile): AudioEntity {
        return AudioEntity(
            id = audio.id,
            title = audio.title,
            artist = audio.artist,
            duration = audio.duration,
            albumArtUri = audio.albumArtUri,
            data = audio.data
        )
    }
    private fun playlistToPlaylistEntity(playlist: Playlist): PlaylistEntity {
        return PlaylistEntity(
            id = playlist.id,
            name = playlist.name,
            audiosId = playlist.audios.map { it.id }
        )
    }

    fun createPlaylist(name: String): Playlist {
        val newPlaylist = Playlist(System.currentTimeMillis(), name, mutableListOf())
        playlists.add(newPlaylist)

        val db = AppDatabase.getDatabase(context)
        val playlistDao = db.playlistDao()

        CoroutineScope(Dispatchers.IO).launch {
            playlistDao.insert(playlistToPlaylistEntity(newPlaylist))
        }

        return newPlaylist
    }

    fun deletePlaylistById(id: Long) {
        val index = playlists.indexOfFirst { it.id == id }
        if (index != -1) {
            playlists.removeAt(index)
            val db = AppDatabase.getDatabase(context)
            val playlistDao = db.playlistDao()
            CoroutineScope(Dispatchers.IO).launch {
                playlistDao.deleteById(id)
            }
        }
    }

    private suspend fun addAudioInDatabase(audio: AudioEntity) {
        val db = AppDatabase.getDatabase(context)
        val audioDao = db.audioDao()
        val audioDb: AudioEntity? = audioDao.getAudioById(audio.id)
        if (audioDb == null) {
            audioDao.insert(audio)
        }
    }

    fun addAudioToPlaylist(playlistId: Long, audio: AudioFile, onFinish: () -> Unit) {
        addAudioListToPlaylist(playlistId, listOf(audio), onFinish)
    }

    fun addAudioListToPlaylist(playlistId: Long, audioList: List<AudioFile>, onFinish: () -> Unit) {
        val playlist = playlists.find { it.id == playlistId }
        for (audio in audioList) playlist?.audios?.add(audio)

        val db = AppDatabase.getDatabase(context)
        val playlistDao = db.playlistDao()

        CoroutineScope(Dispatchers.IO).launch {
            // Insérer la piste audio
            for (audio in audioList) {
                addAudioInDatabase(audioFileToAudioEntity(audio))
            }
            // Mettre à jour la playlist
            playlist?.let { pl ->
                val playlistEntity = PlaylistEntity(
                    id = pl.id,
                    name = pl.name,
                    audiosId = pl.audios.map { it.id }
                )
                playlistDao.update(playlistEntity)
            }
            onFinish()
        }
    }

    fun removeAudioToPlaylist(playlistId: Long, audio: AudioFile, onFinish: () -> Unit) {
        val playlist = playlists.find { it.id == playlistId }
        val index: Int = playlist?.audios?.indexOfFirst { it.id == audio.id }?: -1
        if (index != -1) {
            playlist?.audios?.removeAt(index)
            val db = AppDatabase.getDatabase(context)
            val playlistDao = db.playlistDao()
            CoroutineScope(Dispatchers.IO).launch {
                // Mettre à jour la playlist
                playlist?.let { pl ->
                    val playlistEntity = PlaylistEntity(
                        id = pl.id,
                        name = pl.name,
                        audiosId = pl.audios.map { it.id }
                    )
                    playlistDao.update(playlistEntity)
                }
                onFinish()
            }
        }
    }

    suspend fun syncAudioFiles(audioFilesDisk: List<AudioFile>) {
        val db = AppDatabase.getDatabase(context)
        val audioDao = db.audioDao()

        val audioFilesDiskSort = audioFilesDisk.sortedBy { it.id }
        val audioFilesDbSort = getAudioFiles().sortedBy { it.id }
        var dbI = 0
        var diskI = 0
        while (dbI < audioFilesDbSort.size && diskI < audioFilesDiskSort.size) {
            if (audioFilesDbSort[dbI].id < audioFilesDiskSort[diskI].id) {
                audioDao.deleteById(audioFilesDbSort[dbI].id)
                val index = audioFiles.indexOfFirst { it.id == audioFilesDbSort[dbI].id }
                audioFiles.removeAt(index)
                dbI++
            } else if (audioFilesDbSort[dbI].id > audioFilesDiskSort[diskI].id) {
                audioDao.insert(audioFileToAudioEntity(audioFilesDiskSort[diskI]))
                audioFiles.add(audioFilesDiskSort[diskI])
                diskI++
            } else {
                dbI++
                diskI++
            }
        }
        while (dbI < audioFilesDbSort.size) {
            audioDao.deleteById(audioFilesDbSort[dbI].id)
            val index = audioFiles.indexOfFirst { it.id == audioFilesDbSort[dbI].id }
            audioFiles.removeAt(index)
            dbI++
        }
        while (diskI < audioFilesDiskSort.size) {
            audioDao.insert(audioFileToAudioEntity(audioFilesDiskSort[diskI]))
            audioFiles.add(audioFilesDiskSort[diskI])
            diskI++
        }
    }
}
