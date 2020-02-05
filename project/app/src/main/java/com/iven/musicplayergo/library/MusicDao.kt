package com.iven.musicplayergo.library

import androidx.room.*
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.models.Music

@Dao
interface MusicDao {
    @Query("SELECT * FROM music")
    fun getAll(): MutableList<Music>

    @Transaction
    fun updateData(music: MutableList<Music>) {
        if (goPreferences.reloadDB) {
            deleteAll()
            goPreferences.reloadDB = false
        }
        insertAll(music)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(music: MutableList<Music>)

    @Query("DELETE FROM music")
    fun deleteAll()
}
