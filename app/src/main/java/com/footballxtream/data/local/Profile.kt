package com.footballxtream.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverter
import androidx.room.Update
import com.footballxtream.model.XtreamProfile
import kotlinx.coroutines.flow.Flow

/** A value encrypted at rest (see [Crypto]). Stored as ciphertext, exposed here as plain text. */
data class Secret(val value: String)

/** Encrypts on the way into the DB, decrypts on the way out (registered on [AppDatabase]). */
class SecretConverter {
    @TypeConverter
    fun fromSecret(secret: Secret?): String = Crypto.encrypt(secret?.value.orEmpty())

    @TypeConverter
    fun toSecret(stored: String?): Secret = Secret(Crypto.decrypt(stored.orEmpty()))
}

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String = ProfileType.XTREAM,
    // Sensitive fields, encrypted at rest via [SecretConverter]:
    // Xtream (blank for M3U profiles):
    val serverUrl: Secret = Secret(""),
    val username: Secret = Secret(""),
    val password: Secret = Secret(""),
    // M3U URL (blank for Xtream profiles; may itself carry user/pass in the query):
    val m3uUrl: Secret = Secret(""),
) {
    val isM3u: Boolean get() = type == ProfileType.M3U
    val isDirect: Boolean get() = type == ProfileType.DIRECT

    fun toXtreamProfile() = XtreamProfile(
        name = name,
        serverUrl = serverUrl.value,
        username = username.value,
        password = password.value,
    )
}

object ProfileType {
    const val XTREAM = "XTREAM"
    const val M3U = "M3U"

    /** A single directly-playable stream URL (HLS/DASH), bypassing the sports filter. */
    const val DIRECT = "DIRECT"
}

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun count(): Int

    @Query("SELECT * FROM profiles")
    suspend fun allOnce(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun byId(id: Long): ProfileEntity?

    // Plain insert so autoGenerate assigns a fresh id; REPLACE made every new profile collide on
    // id=0 and overwrite the previous one (only one profile ever survived).
    @Insert
    suspend fun upsert(profile: ProfileEntity): Long

    // Update an existing profile in place (matched by primary key) when editing it.
    @Update
    suspend fun update(profile: ProfileEntity)

    @Delete
    suspend fun delete(profile: ProfileEntity)
}
