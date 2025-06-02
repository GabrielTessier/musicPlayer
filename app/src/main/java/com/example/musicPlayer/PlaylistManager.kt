package com.example.musicPlayer

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
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
        return value.joinToString(",")
    }

    @TypeConverter
    fun toListLong(value: String): List<Long> {
        return value.split(",").map { it.toLong() }
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

    @Query("SELECT * FROM audioentity")
    suspend fun getAllAudios(): List<AudioEntity>

    @Query("SELECT * FROM audioentity WHERE id=:id LIMIT 1")
    suspend fun getAudioById(id: Long): AudioEntity
}

@Dao
interface PlaylistDao {
    @Insert
    suspend fun insert(playlist: PlaylistEntity)

    @Update
    suspend fun update(playlist: PlaylistEntity)

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
            /*return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }*/
        }
    }
}

class PlaylistManager(private val context: Context, onLoadFinish: (MutableList<Playlist>) -> Unit) {

    private val playlists = mutableListOf<Playlist>()
    var loadFinish = false

    init {
        // Obtenez une instance de la base de données
        val db = AppDatabase.getDatabase(context)
        val playlistDao = db.playlistDao()
        val scope = CoroutineScope(Dispatchers.IO)
        val job = scope.launch {
            val playlistsEntity = playlistDao.getAllPlaylists()
            playlistsEntity.forEach { playlist ->
                playlists.add(
                    Playlist(
                        id = playlist.id,
                        name = playlist.name,
                        audios = getAudios(playlist.audiosId)
                    )
                )
            }
        }
        job.invokeOnCompletion {
            loadFinish = true
            onLoadFinish(playlists)
        }
    }

    fun getPlaylists(): MutableList<Playlist> {
        return playlists
    }

    private suspend fun getAudios(audiosId: List<Long>): MutableList<AudioFile> {
        val audioList: MutableList<AudioFile> = mutableListOf<AudioFile>()
        val db = AppDatabase.getDatabase(context)
        val audioDao = db.audioDao()
        for (id in audiosId) {
            val audioEntity: AudioEntity = audioDao.getAudioById(id)
            audioList.add(audioEntityToAudioFile(audioEntity))
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

    fun createPlaylist(name: String): Playlist {
        val newPlaylist = Playlist(System.currentTimeMillis(), name, mutableListOf())
        playlists.add(newPlaylist)
        return newPlaylist
    }

    fun addAudioToPlaylist(playlistId: Long, audio: AudioFile) {
        val playlist = playlists.find { it.id == playlistId }
        playlist?.audios?.add(audio)

        val db = AppDatabase.getDatabase(context)
        val audioDao = db.audioDao()
        val playlistDao = db.playlistDao()

        CoroutineScope(Dispatchers.IO).launch {
            // Insérer la piste audio
            audioDao.insert(audioFileToAudioEntity(audio))

            // Mettre à jour la playlist
            playlist?.let { pl ->
                val playlistEntity = PlaylistEntity(
                    id = pl.id,
                    name = pl.name,
                    audiosId = pl.audios.map { it.id }
                )
                playlistDao.update(playlistEntity)
            }
        }
    }

    fun getPlaylist(playlistId: Long): Playlist? {
        return playlists.find { it.id == playlistId }
    }
}
