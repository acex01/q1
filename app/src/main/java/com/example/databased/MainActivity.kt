package com.example.databased

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.databased.ui.theme.DatabasedTheme
import com.example.inventory.R

class MainActivity : ComponentActivity() {

    val wordViewModel: WordViewModel by viewModels {
        WordViewModelFactory((application as WordsApplication).repository)
    }
    private fun showNotification(companyName: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, "channel_id")
            .setContentTitle("New Company Added")
            .setContentText("Company added: $companyName")
            .setSmallIcon(R.drawable.ic_launcher_background) // Ensure you have this icon in your drawable resources
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification) // Use a unique ID for each notification
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DatabasedTheme {
                val context = LocalContext.current
                var hasNotificationPermission by remember {
                    mutableStateOf(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }
                    )
                }
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        hasNotificationPermission = isGranted
                    }
                )

                // Request permission at launch for Tiramisu and above
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent()
                }
            }
        }
    }

    @Composable
    fun MainContent() {
        val words = remember { mutableStateListOf<Word>() }
        wordViewModel.allWords.observe(this) { words.clear(); words.addAll(it) }
        var showCompanies by remember { mutableStateOf(false) }

        Column(modifier = Modifier.padding(16.dp)) {
            AddWordSection { word -> words.add(word); wordViewModel.insert(word) }
            ToggleButton(showCompanies) { showCompanies = it }
            if (showCompanies) CompanyList(words)
        }
    }

    @Composable
    fun AddWordSection(onSubmit: (Word) -> Unit) {
        var wordName by remember { mutableStateOf("") }

        OutlinedTextField(
            value = wordName,
            onValueChange = { wordName = it },
            label = { Text("Company Name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        SubmitButton(wordName) {
            val newWord = Word(name = it)
            onSubmit(newWord)
            showNotification(newWord.name) // Show notification when a new company is added
            wordName = "" // Reset the text field
        }
    }

    @Composable
    fun SubmitButton(wordName: String, onSubmit: (String) -> Unit) {
        Button(
            onClick = { onSubmit(wordName) },
            enabled = wordName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Company")
        }
    }

    @Composable
    fun ToggleButton(showCompanies: Boolean, onToggle: (Boolean) -> Unit) {
        Button(
            onClick = { onToggle(!showCompanies) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (showCompanies) "Hide Companies" else "Show Companies")
        }
    }

    @Composable
    fun CompanyList(words: List<Word>) {
        LazyColumn {
            items(words) { WordItem(it) }
            if (words.isEmpty()) {
                item { Text("No companies available") }
            }
        }
    }

    @Composable
    fun WordItem(word: Word) {
        Card(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = "Company Name: ${word.name}",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
