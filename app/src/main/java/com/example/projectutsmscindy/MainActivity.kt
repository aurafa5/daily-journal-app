package com.example.projectutsmscindy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.projectutsmscindy.ui.theme.ProjectUTSMsCindyTheme
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

private enum class Tab(val title: String) { TODAY("Hari Ini"), HABITS("Habit"), JOURNAL("Mood") }
private val CardShape = RoundedCornerShape(18.dp)
private val ControlShape = RoundedCornerShape(12.dp)
private val MoodColors = listOf(Color(0xFF8EA3C8), Color(0xFFD68AA3), Color(0xFFFFB84D), Color(0xFF75B987), Color(0xFF59A9D3), Color(0xFFE86A63))
private val MoodNames = listOf("Sedih", "Tidak nyaman", "Netral", "Baik", "Sangat baik", "Marah")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ProjectUTSMsCindyTheme { HabitJournalApp() } }
    }
}

@Composable
private fun HabitJournalApp() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("habit_mood_account", android.content.Context.MODE_PRIVATE) }
    var isLoggedIn by remember { mutableStateOf(preferences.getBoolean("is_logged_in", false)) }
    var showWelcome by rememberSaveable { mutableStateOf(true) }
    var username by remember { mutableStateOf(preferences.getString("username", "") ?: "") }
    var darkTheme by remember { mutableStateOf(preferences.getBoolean("theme_$username", preferences.getBoolean("dark_theme", false))) }

    LaunchedEffect(username) {
        if (username.isNotBlank()) {
            darkTheme = preferences.getBoolean("theme_$username", preferences.getBoolean("dark_theme", false))
        }
    }

    ProjectUTSMsCindyTheme(darkTheme = darkTheme) {
        if (isLoggedIn) AppDashboard(preferences, username, darkTheme, onThemeChange = {
            darkTheme = it
            preferences.edit().putBoolean("theme_$username", it).apply()
        }, onLogout = {
            isLoggedIn = false
            preferences.edit().putBoolean("is_logged_in", false).apply()
        }, onUsernameChanged = { newName ->
            username = newName
        })
        else AuthScreen(
            onAuthenticated = { loggedName ->
                username = loggedName
                isLoggedIn = true
            },
            preferences = preferences
        )
        if (showWelcome) LaunchWelcomeDialog { showWelcome = false }
    }
}

@Composable
private fun AppDashboard(
    preferences: android.content.SharedPreferences,
    username: String,
    darkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onUsernameChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val database = remember { HabitDatabaseHelper(context.applicationContext) }
    var selectedTab by remember { mutableStateOf(Tab.TODAY) }
    var habits by remember { mutableStateOf(emptyList<Habit>()) }
    var completedIds by remember { mutableStateOf(emptySet<Long>()) }
    var journal by remember { mutableStateOf<DailyJournal?>(null) }
    var moodEntries by remember { mutableStateOf(emptyList<MoodEntry>()) }
    var showAddHabitDialog by remember { mutableStateOf(false) }
    var isSettingsScreen by remember { mutableStateOf(false) }

    fun refresh() {
        habits = database.getHabits(username)
        completedIds = database.completedToday(username)
        journal = database.journalToday(username)
        moodEntries = database.moodEntriesToday(username)
    }
    LaunchedEffect(username) { refresh() }

    if (isSettingsScreen) {
        SettingsScreen(
            preferences = preferences,
            currentUsername = username,
            darkTheme = darkTheme,
            onThemeChange = onThemeChange,
            onUsernameChanged = onUsernameChanged,
            onLogout = onLogout,
            onBack = { isSettingsScreen = false }
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(), containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (selectedTab == Tab.HABITS) ExtendedFloatingActionButton(
                onClick = { showAddHabitDialog = true }, shape = ControlShape,
                containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Text("Tambah habit", fontWeight = FontWeight.SemiBold) }
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier.clickable { isSettingsScreen = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👤", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 8.dp))
                        Text(username, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                IconButton(onClick = { isSettingsScreen = true }) {
                    Text("⚙️", style = MaterialTheme.typography.headlineMedium)
                }
            }
            BlueBrandHeader()
            AppTabs(selectedTab) { selectedTab = it }
            Spacer(Modifier.height(22.dp))
            when (selectedTab) {
                Tab.TODAY -> TodayScreen(habits, completedIds, journal, moodEntries) { habit, checked -> database.setCompleted(username, habit.id, checked); refresh() }
                Tab.HABITS -> HabitsScreen(habits) { habit -> database.deleteHabit(habit.id); refresh() }
                Tab.JOURNAL -> MoodScreen(username, database) { refresh() }
            }
        }
    }
    if (showAddHabitDialog) AddHabitDialog(onDismiss = { showAddHabitDialog = false }) { name, target -> database.addHabit(username, name, target); showAddHabitDialog = false; refresh() }
}

@Composable
private fun SettingsScreen(
    preferences: android.content.SharedPreferences,
    currentUsername: String,
    darkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    var showChangeUsername by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var successMsg by remember { mutableStateOf("") }

    if (showChangeUsername) {
        var newUsername by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showChangeUsername = false },
            shape = CardShape,
            title = { Text("Ganti Username") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it; errorMsg = "" },
                        label = { Text("Username Baru") },
                        singleLine = true,
                        shape = ControlShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMsg.isNotBlank()) {
                        Text(errorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newUsername.isBlank()) {
                        errorMsg = "Username tidak boleh kosong."
                    } else {
                        val oldPwd = preferences.getString("pwd_$currentUsername", preferences.getString("password", "")) ?: ""
                        preferences.edit()
                            .putString("username", newUsername.trim())
                            .putString("pwd_${newUsername.trim()}", oldPwd)
                            .apply()
                        onUsernameChanged(newUsername.trim())
                        onThemeChange(darkTheme) // trigger refresh of states
                        showChangeUsername = false
                        successMsg = "Username berhasil diubah!"
                    }
                }, shape = ControlShape) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showChangeUsername = false }) { Text("Batal") }
            }
        )
    }

    if (showChangePassword) {
        var oldPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showChangePassword = false },
            shape = CardShape,
            title = { Text("Ganti Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it; errorMsg = "" },
                        label = { Text("Password Lama") },
                        singleLine = true,
                        shape = ControlShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; errorMsg = "" },
                        label = { Text("Password Baru") },
                        singleLine = true,
                        shape = ControlShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMsg = "" },
                        label = { Text("Ulangi Password Baru") },
                        singleLine = true,
                        shape = ControlShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMsg.isNotBlank()) {
                        Text(errorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val savedPassword = preferences.getString("pwd_$currentUsername", preferences.getString("password", "")) ?: ""
                    if (oldPassword != savedPassword) {
                        errorMsg = "Password lama salah."
                    } else if (newPassword.isBlank()) {
                        errorMsg = "Password baru tidak boleh kosong."
                    } else if (newPassword != confirmPassword) {
                        errorMsg = "Konfirmasi password baru belum sama."
                    } else {
                        preferences.edit().putString("pwd_$currentUsername", newPassword).apply()
                        if (currentUsername == preferences.getString("username", "")) {
                            preferences.edit().putString("password", newPassword).apply()
                        }
                        showChangePassword = false
                        successMsg = "Password berhasil diubah!"
                    }
                }, shape = ControlShape) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showChangePassword = false }) { Text("Batal") }
            }
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("← Kembali", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(16.dp))
                Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Spacer(Modifier.height(8.dp))
                if (successMsg.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(successMsg, modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Medium)
                    }
                }
                Text("Username saat ini: $currentUsername", style = MaterialTheme.typography.bodyLarge)
                
                Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                
                Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onThemeChange(false) },
                        shape = ControlShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!darkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (!darkTheme) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text("Light Mode") }
                    Button(
                        onClick = { onThemeChange(true) },
                        shape = ControlShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (darkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (darkTheme) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text("Dark Mode") }
                }

                Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                Text("Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Button(
                    onClick = { showChangeUsername = true },
                    shape = ControlShape,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Ganti Username") }

                Button(
                    onClick = { showChangePassword = true },
                    shape = ControlShape,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Ganti Password") }
            }

            Column {
                Button(
                    onClick = onLogout,
                    shape = ControlShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().height(50.dp).padding(bottom = 24.dp)
                ) {
                    Text("🚪 Logout", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LaunchWelcomeDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = CardShape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Selamat datang", style = MaterialTheme.typography.headlineSmall) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer))
            Text("Teman kecil untuk merawat kebiasaan dan memahami mood-mu setiap hari.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } },
        confirmButton = { Button(onClick = onDismiss, shape = ControlShape) { Text("Mulai") } }
    )
}

@Composable
private fun AuthScreen(onAuthenticated: (String) -> Unit, preferences: android.content.SharedPreferences) {
    var isRegistering by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Text("H", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
            Text(if (isRegistering) "Buat akun baru" else "Selamat datang kembali", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(if (isRegistering) "Simpan perjalanan habit dan mood-mu di satu tempat." else "Masuk untuk melanjutkan perjalananmu hari ini.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(28.dp))
            OutlinedTextField(value = name, onValueChange = { name = it; message = "" }, modifier = Modifier.fillMaxWidth(), label = { Text("Nama pengguna") }, singleLine = true, shape = ControlShape)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = password, onValueChange = { password = it; message = "" }, modifier = Modifier.fillMaxWidth(), label = { Text("Kata sandi") }, singleLine = true, shape = ControlShape)
            if (isRegistering) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it; message = "" }, modifier = Modifier.fillMaxWidth(), label = { Text("Ulangi kata sandi") }, singleLine = true, shape = ControlShape)
            }
            if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(22.dp))
            Button(onClick = {
                val cleanedName = name.trim()
                if (cleanedName.isBlank() || password.isBlank()) {
                    message = "Nama pengguna dan kata sandi harus diisi."
                } else if (isRegistering) {
                    if (password != confirmPassword) {
                        message = "Konfirmasi kata sandi belum sama."
                    } else {
                        preferences.edit()
                            .putString("username", cleanedName)
                            .putString("pwd_$cleanedName", password)
                            .putBoolean("is_logged_in", true)
                            .apply()
                        onAuthenticated(cleanedName)
                    }
                } else {
                    val savedPassword = preferences.getString("pwd_$cleanedName", preferences.getString("password", null))
                    if (savedPassword == null || savedPassword != password) {
                        message = "Nama pengguna atau kata sandi tidak sesuai."
                    } else {
                        preferences.edit()
                            .putString("username", cleanedName)
                            .putBoolean("is_logged_in", true)
                            .apply()
                        onAuthenticated(cleanedName)
                    }
                }
            }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = ControlShape) { Text(if (isRegistering) "Daftar & masuk" else "Masuk", fontWeight = FontWeight.SemiBold) }
            TextButton(onClick = { isRegistering = !isRegistering; message = "" }, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)) { Text(if (isRegistering) "Sudah punya akun? Masuk" else "Belum punya akun? Daftar") }
        }
    }
}

@Composable
private fun BlueBrandHeader() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 18.dp),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Text("H", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp)); Column {
                Text("Habit & Mood Journal", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text("Ruang kecil untuk harimu", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.82f))
            }
        }
    }
}

@Composable
private fun AppTabs(selectedTab: Tab, onSelect: (Tab) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        Tab.entries.forEachIndexed { index, tab ->
            SegmentedButton(selected = selectedTab == tab, onClick = { onSelect(tab) }, shape = SegmentedButtonDefaults.itemShape(index, Tab.entries.size)) { Text(tab.title, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun TodayScreen(habits: List<Habit>, completedIds: Set<Long>, journal: DailyJournal?, moodEntries: List<MoodEntry>, update: (Habit, Boolean) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
        item { SectionHeading("Kebiasaan hari ini", "Centang kebiasaan yang sudah dilakukan.") }
        if (habits.isEmpty()) item { Notice("Belum ada kebiasaan. Tambahkan pada tab Habit.") }
        items(habits, key = { it.id }) { habit ->
            val isDone = habit.id in completedIds
            WellnessCard { Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isDone, onCheckedChange = { update(habit, it) }, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary, checkmarkColor = MaterialTheme.colorScheme.onPrimary))
                Spacer(Modifier.width(10.dp)); Column {
                    Text(habit.name, style = MaterialTheme.typography.titleMedium, textDecoration = if (isDone) TextDecoration.LineThrough else null, color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(3.dp)); Text("Target: ${habit.targetFrequency}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } }
        }
        item { Spacer(Modifier.height(12.dp)); SectionHeading("Mood hari ini", "Ringkasan catatan untuk hari ini."); Spacer(Modifier.height(10.dp))
            if (moodEntries.isNotEmpty()) MoodEntriesToday(moodEntries)
            else if (journal == null) Notice("Belum ada catatan mood untuk hari ini.")
            else WellnessCard { Column(Modifier.padding(18.dp)) { MoodPill(journal.mood); Spacer(Modifier.height(8.dp)); Text(journal.description.ifBlank { "Tidak ada catatan." }, style = MaterialTheme.typography.bodyMedium) } }
        }
    }
}

@Composable
private fun HabitsScreen(habits: List<Habit>, onDelete: (Habit) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 88.dp)) {
        item { SectionHeading("Daftar kebiasaan", "Kelola rutinitas yang ingin kamu jaga.") }
        if (habits.isEmpty()) item { Notice("Tekan Tambah habit untuk membuat kebiasaan pertama.") }
        items(habits, key = { it.id }) { habit -> WellnessCard { Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(habit.name, style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(3.dp)); Text("Target: ${habit.targetFrequency}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            TextButton(onClick = { onDelete(habit) }) { Text("Hapus", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium) }
        } } }
    }
}

@Composable
private fun MoodScreen(username: String, database: HabitDatabaseHelper, onEntrySaved: () -> Unit) {
    val displayedMonth = remember { Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) } }
    var monthVersion by remember { mutableIntStateOf(0) }
    var dailyMoods by remember { mutableStateOf(emptyMap<Int, Int>()) }
    var selectedMood by remember { mutableStateOf(4) }
    var selectedHour by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    var todayEntries by remember { mutableStateOf(emptyList<MoodEntry>()) }
    var note by remember { mutableStateOf("") }
    fun reloadMoods() {
        dailyMoods = database.moodsForMonth(username, displayedMonth.get(Calendar.YEAR), displayedMonth.get(Calendar.MONTH))
        todayEntries = database.moodEntriesToday(username)
    }
    LaunchedEffect(monthVersion) { reloadMoods() }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 30.dp)) {
        item { SectionHeading("Mood calendar", "Lihat warna ringkasan mood setiap hari.") }
        item { MoodCalendar(displayedMonth, dailyMoods, onPrevious = { displayedMonth.add(Calendar.MONTH, -1); monthVersion++ }, onNext = { displayedMonth.add(Calendar.MONTH, 1); monthVersion++ }) }
        item { SectionHeading("Catat mood per jam", "Pilih jam lalu pilih satu warna mood.") }
        item { HourPicker(selectedHour) { selectedHour = it } }
        item { MoodPalette(selectedMood) { selectedMood = it } }
        item { SectionHeading("Catatan mood", "Setiap catatan akan tersimpan sebagai entri baru.") }
        item { OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth().height(150.dp), label = { Text("Uraian hari") }, placeholder = { Text("Ceritakan harimu...") }, shape = CardShape, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface)) }
        item { Button(onClick = { database.addMoodEntry(username, selectedHour, selectedMood, note); note = ""; reloadMoods(); onEntrySaved() }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = ControlShape, colors = ButtonDefaults.buttonColors(containerColor = MoodColors[selectedMood - 1])) { Text("Simpan catatan mood", fontWeight = FontWeight.SemiBold, color = Color.White) } }
        if (todayEntries.isNotEmpty()) item { MoodEntriesToday(todayEntries) }
    }
}

@Composable
private fun MoodCalendar(month: Calendar, moods: Map<Int, Int>, onPrevious: () -> Unit, onNext: () -> Unit) {
    WellnessCard {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onPrevious, contentPadding = PaddingValues(4.dp)) { Text("‹", style = MaterialTheme.typography.headlineSmall) }
                Text("${DateFormatSymbols(Locale("id")).months[month.get(Calendar.MONTH)]} ${month.get(Calendar.YEAR)}", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onNext, contentPadding = PaddingValues(4.dp)) { Text("›", style = MaterialTheme.typography.headlineSmall) }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) { listOf("M", "S", "S", "R", "K", "J", "S").forEach { day -> Text(day, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            val cells = calendarCells(month)
            cells.chunked(7).forEach { week -> Row(Modifier.fillMaxWidth().padding(top = 7.dp)) { week.forEach { day -> CalendarCell(day, moods[day], Modifier.weight(1f)) } } }
        }
    }
}

@Composable
private fun CalendarCell(day: Int?, mood: Int?, modifier: Modifier) {
    Box(modifier.height(39.dp), contentAlignment = Alignment.Center) {
        if (day != null) Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(day.toString(), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Box(Modifier.size(18.dp).clip(CircleShape).background(mood?.let { MoodColors[it - 1] } ?: MaterialTheme.colorScheme.surfaceVariant))
        }
    }
}

@Composable
private fun HourPicker(selectedHour: Int, onSelect: (Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items((0..23).toList()) { hour ->
            val selected = hour == selectedHour
            Box(Modifier.clip(ControlShape).background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface).clickable { onSelect(hour) }.padding(horizontal = 12.dp, vertical = 9.dp), contentAlignment = Alignment.Center) {
                Text(hourLabel(hour), style = MaterialTheme.typography.labelLarge, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun MoodPalette(selectedMood: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        MoodColors.forEachIndexed { index, color ->
            val mood = index + 1; val selected = mood == selectedMood
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(50.dp).clickable { onSelect(mood) }) {
                Box(Modifier.size(if (selected) 40.dp else 34.dp).clip(CircleShape).background(color).then(if (selected) Modifier.background(color) else Modifier), contentAlignment = Alignment.Center) { if (selected) Text("✓", color = Color.White, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(6.dp)); Text(MoodNames[index], style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MoodEntriesToday(entries: List<MoodEntry>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Catatan mood hari ini", style = MaterialTheme.typography.titleMedium)
        entries.forEach { entry ->
            WellnessCard {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(MoodColors[entry.mood - 1]).padding(top = 3.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(MoodNames[entry.mood - 1], style = MaterialTheme.typography.titleMedium, color = MoodColors[entry.mood - 1])
                            Text(entry.createdTime, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("Mood untuk pukul ${hourLabel(entry.hour)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (entry.note.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(entry.note, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodPill(mood: Int) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(12.dp).clip(CircleShape).background(MoodColors[(mood - 1).coerceIn(0, 5)])); Spacer(Modifier.width(7.dp)); Text(MoodNames[(mood - 1).coerceIn(0, 5)], style = MaterialTheme.typography.titleMedium, color = MoodColors[(mood - 1).coerceIn(0, 5)]) } }

@Composable
private fun AddHabitDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }; var target by remember { mutableStateOf("Setiap hari") }
    AlertDialog(onDismissRequest = onDismiss, shape = CardShape, containerColor = MaterialTheme.colorScheme.surface, title = { Text("Tambah kebiasaan", style = MaterialTheme.typography.headlineSmall) }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama kebiasaan") }, singleLine = true, shape = ControlShape); OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Target frekuensi") }, singleLine = true, shape = ControlShape) } }, confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(name.trim(), target.trim().ifBlank { "Setiap hari" }) }) { Text("Simpan", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } })
}

@Composable private fun SectionHeading(title: String, supportingText: String) { Column { Text(title, style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(4.dp)); Text(supportingText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun WellnessCard(content: @Composable ColumnScope.() -> Unit) { Card(modifier = Modifier.fillMaxWidth(), shape = CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), content = content) }
@Composable private fun Notice(text: String) { WellnessCard { Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(18.dp)) } }
private fun hourLabel(hour: Int) = String.format(Locale.US, "%02d:00", hour)
private fun calendarCells(month: Calendar): List<Int?> { val first = month.clone() as Calendar; first.set(Calendar.DAY_OF_MONTH, 1); val prefix = first.get(Calendar.DAY_OF_WEEK) - 1; val days = first.getActualMaximum(Calendar.DAY_OF_MONTH); return List(prefix) { null } + (1..days).toList() }
