package com.example.currency.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url
import javax.inject.Inject
import javax.inject.Singleton

// ---------- Network ----------
@Serializable data class RatesDto(val rates: Map<String, Double>)
interface RatesApi { @GET suspend fun latest(@Url url: String): RatesDto }

// ---------- Room ----------
@Entity data class RateEntity(@PrimaryKey val base: String, val json: String, val ts: Long)
@Entity data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val from: String, val to: String, val amount: Double, val result: Double, val ts: Long)

@Dao interface AppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveRates(r: RateEntity)
    @Query("SELECT * FROM RateEntity WHERE base = :base") suspend fun rates(base: String): RateEntity?
    @Query("SELECT * FROM HistoryEntity ORDER BY ts DESC LIMIT 50") fun history(): Flow<List<HistoryEntity>>
    @Insert suspend fun addHistory(h: HistoryEntity)
    @Query("DELETE FROM HistoryEntity WHERE id = :id") suspend fun deleteHistory(id: Long)
}
@Database(entities = [RateEntity::class, HistoryEntity::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() { abstract fun dao(): AppDao }

// ---------- Settings ----------
private val Context.ds by preferencesDataStore("settings")
class Prefs @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val THEME = intPreferencesKey("theme")          // 0 system, 1 light, 2 dark
    private val OFFLINE = booleanPreferencesKey("offline")
    private val FROM = stringPreferencesKey("from")
    private val TO = stringPreferencesKey("to")
    val theme: Flow<Int> = ctx.ds.data.map { it[THEME] ?: 0 }
    val offlineMode: Flow<Boolean> = ctx.ds.data.map { it[OFFLINE] ?: false }
    val from: Flow<String> = ctx.ds.data.map { it[FROM] ?: "USD" }
    val to: Flow<String> = ctx.ds.data.map { it[TO] ?: "EUR" }
    suspend fun setTheme(v: Int) { ctx.ds.edit { it[THEME] = v } }
    suspend fun setOffline(v: Boolean) { ctx.ds.edit { it[OFFLINE] = v } }
    suspend fun setPair(f: String, t: String) { ctx.ds.edit { it[FROM] = f; it[TO] = t } }
}

// ---------- Repository ----------
data class RatesResult(val rates: Map<String, Double>, val ts: Long, val offline: Boolean)

@Singleton
class Repository @Inject constructor(private val api: RatesApi, private val dao: AppDao, private val json: Json) {
    private val ser = MapSerializer(String.serializer(), Double.serializer())
    // Primary API first, then fallback, then Room cache.
    private val urls = listOf("https://open.er-api.com/v6/latest/%s", "https://api.exchangerate-api.com/v4/latest/%s")

    suspend fun rates(base: String, cacheOnly: Boolean): RatesResult? {
        if (!cacheOnly) for (u in urls) {
            try {
                val r = api.latest(u.format(base)).rates
                val now = System.currentTimeMillis()
                dao.saveRates(RateEntity(base, json.encodeToString(ser, r), now))
                return RatesResult(r, now, false)
            } catch (e: CancellationException) { throw e } catch (_: Exception) { }
        }
        return dao.rates(base)?.let { RatesResult(json.decodeFromString(ser, it.json), it.ts, true) }
    }
    fun history() = dao.history()
    suspend fun addHistory(h: HistoryEntity) = dao.addHistory(h)
    suspend fun deleteHistory(id: Long) = dao.deleteHistory(id)
}

// ---------- DI ----------
@Module @InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun json() = Json { ignoreUnknownKeys = true }
    @Provides @Singleton fun api(json: Json): RatesApi = Retrofit.Builder()
        .baseUrl("https://open.er-api.com/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build().create(RatesApi::class.java)
    @Provides @Singleton fun db(@ApplicationContext c: Context) = Room.databaseBuilder(c, AppDb::class.java, "app.db").build()
    @Provides fun dao(db: AppDb) = db.dao()
}

// ---------- Currencies (30+) ----------
data class Cur(val code: String) {
    /** Flag emoji built from the first two letters (ISO country code; EUR -> EU). */
    val flag: String get() = code.take(2).map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
    val name: String get() = runCatching { java.util.Currency.getInstance(code).displayName }.getOrDefault(code)
}
val CURRENCIES = listOf("USD","EUR","GBP","JPY","CNY","CHF","CAD","AUD","NZD","RUB","BYN","UAH","PLN","CZK","SEK",
    "NOK","DKK","TRY","INR","BRL","MXN","ZAR","KRW","SGD","HKD","AED","SAR","KZT","GEL","ILS","THB").map(::Cur)
