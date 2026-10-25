package edu.utesa.directorio

import android.app.Application
import androidx.room.Room
import edu.utesa.directorio.data.*

class DirectoryApp : Application() {
    val repository by lazy {
        val db = Room.databaseBuilder(this, DirectoryDatabase::class.java, "directory.db").build()
        DirectoryRepository(DirectoryApi.create(), db.dao())
    }
    val connectivity by lazy { Connectivity(this) }
}
