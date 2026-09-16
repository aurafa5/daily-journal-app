package com.example.projectutsmscindy

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Habit(val id: Long, val name: String, val targetFrequency: String)
data class DailyJournal(val id: Long, val date: String, val mood: Int, val description: String)
data class MoodLog(val id: Long, val date: String, val hour: Int, val mood: Int)
data class MoodEntry(val id: Long, val date: String, val hour: Int, val createdTime: String, val mood: Int, val note: String)

class HabitDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "habit_mood_journal.db", null, 4) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE Habit (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL DEFAULT '', nama_kebiasaan TEXT NOT NULL, target_frekuensi TEXT NOT NULL)")
        db.execSQL("CREATE TABLE HabitLog (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL DEFAULT '', habit_id INTEGER NOT NULL, tanggal_eksekusi TEXT NOT NULL, FOREIGN KEY(habit_id) REFERENCES Habit(id) ON DELETE CASCADE, UNIQUE(username, habit_id, tanggal_eksekusi))")
        db.execSQL("CREATE TABLE DailyJournal (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL DEFAULT '', tanggal TEXT NOT NULL, skala_mood INTEGER NOT NULL, uraian_hari TEXT NOT NULL, UNIQUE(username, tanggal))")
        createMoodLogTable(db); createMoodEntryTable(db)
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createMoodLogTable(db)
        if (oldVersion < 3) {
            createMoodEntryTable(db)
            if (oldVersion >= 2) db.execSQL("INSERT INTO MoodEntry (tanggal, jam, waktu_catatan, skala_mood, uraian_hari) SELECT tanggal, jam, printf('%02d:00', jam), skala_mood, '' FROM MoodLog")
        }
        if (oldVersion < 4) {
            try { db.execSQL("ALTER TABLE Habit ADD COLUMN username TEXT NOT NULL DEFAULT ''") } catch(e: Exception){}
            try { db.execSQL("ALTER TABLE HabitLog ADD COLUMN username TEXT NOT NULL DEFAULT ''") } catch(e: Exception){}
            try { db.execSQL("ALTER TABLE DailyJournal ADD COLUMN username TEXT NOT NULL DEFAULT ''") } catch(e: Exception){}
            try { db.execSQL("ALTER TABLE MoodLog ADD COLUMN username TEXT NOT NULL DEFAULT ''") } catch(e: Exception){}
            try { db.execSQL("ALTER TABLE MoodEntry ADD COLUMN username TEXT NOT NULL DEFAULT ''") } catch(e: Exception){}
        }
    }
    private fun createMoodLogTable(db: SQLiteDatabase) = db.execSQL("CREATE TABLE IF NOT EXISTS MoodLog (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL DEFAULT '', tanggal TEXT NOT NULL, jam INTEGER NOT NULL, skala_mood INTEGER NOT NULL, UNIQUE(username, tanggal, jam))")
    private fun createMoodEntryTable(db: SQLiteDatabase) = db.execSQL("CREATE TABLE IF NOT EXISTS MoodEntry (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL DEFAULT '', tanggal TEXT NOT NULL, jam INTEGER NOT NULL, waktu_catatan TEXT NOT NULL, skala_mood INTEGER NOT NULL, uraian_hari TEXT NOT NULL)")

    fun getHabits(username: String): List<Habit> = readableDatabase.rawQuery("SELECT id, nama_kebiasaan, target_frekuensi FROM Habit WHERE username = ? ORDER BY id DESC", arrayOf(username)).use { c -> buildList { while (c.moveToNext()) add(Habit(c.getLong(0), c.getString(1), c.getString(2))) } }
    fun addHabit(username: String, name: String, target: String) = writableDatabase.insert("Habit", null, ContentValues().apply { put("username", username); put("nama_kebiasaan", name); put("target_frekuensi", target) })
    fun deleteHabit(id: Long) = writableDatabase.delete("Habit", "id = ?", arrayOf(id.toString()))
    fun completedToday(username: String): Set<Long> = readableDatabase.rawQuery("SELECT habit_id FROM HabitLog WHERE username = ? AND tanggal_eksekusi = ?", arrayOf(username, today())).use { c -> buildSet { while (c.moveToNext()) add(c.getLong(0)) } }
    fun setCompleted(username: String, habitId: Long, completed: Boolean) { if (completed) writableDatabase.insertWithOnConflict("HabitLog", null, ContentValues().apply { put("username", username); put("habit_id", habitId); put("tanggal_eksekusi", today()) }, SQLiteDatabase.CONFLICT_IGNORE) else writableDatabase.delete("HabitLog", "username = ? AND habit_id = ? AND tanggal_eksekusi = ?", arrayOf(username, habitId.toString(), today())) }
    fun journalToday(username: String): DailyJournal? = readableDatabase.rawQuery("SELECT id, tanggal, skala_mood, uraian_hari FROM DailyJournal WHERE username = ? AND tanggal = ?", arrayOf(username, today())).use { c -> if (c.moveToFirst()) DailyJournal(c.getLong(0), c.getString(1), c.getInt(2), c.getString(3)) else null }

    fun addMoodEntry(username: String, hour: Int, mood: Int, note: String) = writableDatabase.insert("MoodEntry", null, ContentValues().apply { put("username", username); put("tanggal", today()); put("jam", hour); put("waktu_catatan", nowTime()); put("skala_mood", mood); put("uraian_hari", note) })
    fun moodEntriesToday(username: String): List<MoodEntry> = readableDatabase.rawQuery("SELECT id, tanggal, jam, waktu_catatan, skala_mood, uraian_hari FROM MoodEntry WHERE username = ? AND tanggal = ? ORDER BY id DESC", arrayOf(username, today())).use { c -> buildList { while (c.moveToNext()) add(MoodEntry(c.getLong(0), c.getString(1), c.getInt(2), c.getString(3), c.getInt(4), c.getString(5))) } }
    fun moodsForMonth(username: String, year: Int, monthZeroBased: Int): Map<Int, Int> { val prefix = String.format(Locale.US, "%04d-%02d-%%", year, monthZeroBased + 1); return readableDatabase.rawQuery("SELECT tanggal, ROUND(AVG(skala_mood)) FROM MoodEntry WHERE username = ? AND tanggal LIKE ? GROUP BY tanggal", arrayOf(username, prefix)).use { c -> buildMap { while (c.moveToNext()) put(c.getString(0).takeLast(2).toInt(), c.getInt(1)) } } }
    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private fun nowTime() = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}
