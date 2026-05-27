package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.StudyViewModel

class MainActivity : ComponentActivity() {
    private var mainViewModel: StudyViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: StudyViewModel = viewModel()
            mainViewModel = viewModel
            val isDarkTheme by viewModel.isDarkMode.collectAsState()
            val currentTab by viewModel.currentTab.collectAsState()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Scaffold(
                    modifier = Modifier.fillMaxSize().testTag("app_main_scaffold"),
                    topBar = {
                        HelloStudyTopBar(
                            isDark = isDarkTheme,
                            onThemeToggle = { viewModel.isDarkMode.value = !isDarkTheme }
                        )
                    },
                    bottomBar = {
                        HelloStudyBottomBar(
                            currentTab = currentTab,
                            onTabSelected = { viewModel.currentTab.value = it }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        when (currentTab) {
                            "CHECKLIST" -> ChecklistScreen(viewModel)
                            "SCRIBBLE" -> ScribbleScreen(viewModel)
                            "NOTES" -> NotesScreen(viewModel)
                            "POMODORO" -> PomodoroScreen(viewModel)
                            "CALCULATOR" -> CalculatorScreen(viewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            mainViewModel?.checkFocusShieldViolation()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun HelloStudyTopBar(
    isDark: Boolean,
    onThemeToggle: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_hello_study_logo_1779903249496),
                        contentDescription = "Hello Study Logo",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    )
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Text(
                            text = "STUDY SUITE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "Hello Study",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDark) MaterialTheme.colorScheme.surfaceVariant
                            else Color(0xFFD3E4FF)
                        )
                        .clickable(onClick = onThemeToggle)
                        .testTag("theme_mode_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Light/Dark Theme",
                        tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF001D35),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                thickness = 1.dp
            )
        }
    }
}

@Composable
fun HelloStudyBottomBar(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        modifier = Modifier.testTag("master_bottom_navbar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        data class NavigationTab(
            val route: String, 
            val icon: androidx.compose.ui.graphics.vector.ImageVector, 
            val title: String
        )

        val navItems = listOf(
            NavigationTab("CHECKLIST", Icons.Default.TaskAlt, "Checklist"),
            NavigationTab("SCRIBBLE", Icons.Default.Gesture, "Scribble"),
            NavigationTab("NOTES", Icons.Default.NoteAlt, "Notes"),
            NavigationTab("POMODORO", Icons.Default.Timer, "Focus"),
            NavigationTab("CALCULATOR", Icons.Default.Calculate, "Calculator")
        )

        navItems.forEach { item ->
            val tab = item.route
            val icon = item.icon
            val label = item.title
            NavigationBarItem(
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
                icon = { 
                    Icon(
                        imageVector = icon, 
                        contentDescription = label,
                        modifier = Modifier.size(22.dp)
                    ) 
                },
                label = { 
                    Text(
                        text = label, 
                        fontSize = 10.sp, 
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                ),
                modifier = Modifier.testTag("nav_item_$tab")
            )
        }
    }
}
