package com.personal.utility

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PersonalUtilityApp() }
    }
}

private enum class AppTab { Files, Browser, Utilities, Settings }

@Composable
private fun PersonalUtilityApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("personal_utility", android.content.Context.MODE_PRIVATE)
    }
    var darkMode by remember { mutableStateOf(preferences.getBoolean("dark_mode", false)) }
    var selectedTab by remember { mutableStateOf(AppTab.Files) }
    var rootUri by remember { mutableStateOf<Uri?>(preferences.getString("tree_uri", null)?.let(Uri::parse)) }
    var currentUri by remember { mutableStateOf(rootUri) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var showCreateFolder by remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                rootUri = uri
                currentUri = uri
                preferences.edit().putString("tree_uri", uri.toString()).apply()
                message = "Storage folder connected"
            } catch (e: Exception) {
                message = "Could not retain folder access: ${e.localizedMessage}"
            }
        }
    }

    val colors = if (darkMode) darkColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFFE5E5E5),
        onPrimary = androidx.compose.ui.graphics.Color(0xFF171717),
        background = androidx.compose.ui.graphics.Color(0xFF171717),
        surface = androidx.compose.ui.graphics.Color(0xFF202020),
        onSurface = androidx.compose.ui.graphics.Color(0xFFF4F4F4),
        secondary = androidx.compose.ui.graphics.Color(0xFFA3A3A3)
    ) else lightColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF171717),
        onPrimary = androidx.compose.ui.graphics.Color.White,
        background = androidx.compose.ui.graphics.Color(0xFFF4F4F4),
        surface = androidx.compose.ui.graphics.Color.White,
        onSurface = androidx.compose.ui.graphics.Color(0xFF171717),
        secondary = androidx.compose.ui.graphics.Color(0xFF737373)
    )

    MaterialTheme(colorScheme = colors) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    AppTab.values().forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = { Text(tab.name.first().toString()) },
                            label = { Text(tab.name) }
                        )
                    }
                }
            },
            topBar = {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PERSONAL UTILITY", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary)
                            Text(
                                when (selectedTab) {
                                    AppTab.Files -> "File Manager"
                                    AppTab.Browser -> "Browser"
                                    AppTab.Utilities -> "Utilities"
                                    AppTab.Settings -> "Settings"
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (selectedTab == AppTab.Files) {
                            TextButton(onClick = { folderPicker.launch(rootUri) }) {
                                Text(if (rootUri == null) "Choose folder" else "Change folder")
                            }
                        }
                    }
                }
            }
        ) { padding ->
            when (selectedTab) {
                AppTab.Files -> FileManagerScreen(
                    modifier = Modifier.padding(padding),
                    rootUri = rootUri,
                    currentUri = currentUri,
                    refreshKey = refreshKey,
                    onOpenFolder = { currentUri = it },
                    onGoRoot = { currentUri = rootUri },
                    onRefresh = { refreshKey++ },
                    onCreateFolder = { showCreateFolder = true },
                    onMessage = { message = it }
                )
                AppTab.Browser -> PlaceholderScreen(
                    Modifier.padding(padding),
                    "Browser",
                    "The integrated browser is planned for a later development phase."
                )
                AppTab.Utilities -> PlaceholderScreen(
                    Modifier.padding(padding),
                    "Utility Center",
                    "Notes, calculator, document tools and device utilities will be added progressively."
                )
                AppTab.Settings -> SettingsScreen(
                    Modifier.padding(padding),
                    darkMode = darkMode,
                    onDarkModeChange = {
                        darkMode = it
                        preferences.edit().putBoolean("dark_mode", it).apply()
                    }
                )
            }
        }

        if (showCreateFolder) {
            AlertDialog(
                onDismissRequest = { showCreateFolder = false; folderName = "" },
                title = { Text("Create folder") },
                text = {
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("Folder name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val parent = currentUri?.let { DocumentFile.fromTreeUri(context, it) }
                        val created = if (folderName.isNotBlank()) parent?.createDirectory(folderName.trim()) else null
                        message = if (created != null) "Folder created" else "Could not create folder"
                        refreshKey++
                        showCreateFolder = false
                        folderName = ""
                    }) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateFolder = false; folderName = "" }) { Text("Cancel") }
                }
            )
        }

        message?.let { msg ->
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(2500)
                message = null
            }
            SnackbarHost(hostState = remember { SnackbarHostState() }) {
                Snackbar { Text(msg) }
            }
        }
    }
}

@Composable
private fun FileManagerScreen(
    modifier: Modifier,
    rootUri: Uri?,
    currentUri: Uri?,
    refreshKey: Int,
    onOpenFolder: (Uri) -> Unit,
    onGoRoot: () -> Unit,
    onRefresh: () -> Unit,
    onCreateFolder: () -> Unit,
    onMessage: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var files by remember { mutableStateOf<List<DocumentFile>>(emptyList()) }
    var sortByName by remember { mutableStateOf(true) }

    LaunchedEffect(currentUri, refreshKey) {
        files = currentUri?.let { uri ->
            DocumentFile.fromTreeUri(context, uri)?.listFiles()?.toList().orEmpty()
                .sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name?.lowercase().orEmpty() })
        }.orEmpty()
    }

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (rootUri == null) "Storage access not set up" else if (currentUri == rootUri) "Selected storage" else "Folder contents",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRefresh, enabled = rootUri != null) { Text("Refresh") }
            TextButton(onClick = onCreateFolder, enabled = rootUri != null) { Text("+ Folder") }
        }
        if (rootUri != null && currentUri != rootUri) {
            TextButton(onClick = onGoRoot) { Text("← Storage root") }
        }
        if (rootUri == null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Connect a storage folder", style = MaterialTheme.typography.titleLarge)
                    Text("Choose a folder using Android's system picker. The app can then list its contents and create folders where permission allows.")
                    Button(onClick = { onMessage("Use Choose folder at the top to connect storage") }) {
                        Text("How to connect")
                    }
                }
            }
        } else if (files.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("This folder is empty", color = MaterialTheme.colorScheme.secondary)
            }
        } else {
            Text("${files.size} items", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(vertical = 8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(files, key = { it.uri.toString() }) { file ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (file.isDirectory) onOpenFolder(file.uri)
                            else onMessage("${file.name ?: "File"} · ${formatSize(file.length())}")
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (file.isDirectory) "▰" else "▤",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(end = 12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(file.name ?: "Unnamed", fontWeight = FontWeight.Medium, maxLines = 1)
                                Text(
                                    if (file.isDirectory) "Folder" else "${file.typeLabel()} · ${formatSize(file.length())}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            if (file.isDirectory) Text("›", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }
    }
}

private fun DocumentFile.typeLabel(): String {
    val mime = type ?: "File"
    return mime.substringAfterLast('/').uppercase()
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024L * 1024L * 1024L -> "${bytes / (1024 * 1024)} MB"
    else -> "${bytes / (1024L * 1024L * 1024L)} GB"
}

@Composable
private fun SettingsScreen(
    modifier: Modifier,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit
) {
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(shape = RoundedCornerShape(18.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Dark appearance", fontWeight = FontWeight.Medium)
                    Text("Use the monochromatic dark theme", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary)
                }
                Switch(checked = darkMode, onCheckedChange = onDarkModeChange)
            }
        }
        Text("Version 0.1.0 · Foundation build", color = MaterialTheme.colorScheme.secondary)
        Text("More settings will be introduced as modules are implemented.",
            color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun PlaceholderScreen(modifier: Modifier, title: String, description: String) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(description, color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(14.dp))
        Text("Planned", style = MaterialTheme.typography.labelMedium)
    }
}
