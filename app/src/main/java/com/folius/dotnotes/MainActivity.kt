package com.folius.dotnotes

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.folius.dotnotes.ui.NoteEditorScreen
import com.folius.dotnotes.ui.NoteListScreen
import com.folius.dotnotes.ui.NoteViewModel
import com.folius.dotnotes.ui.theme.DotNotesTheme
import com.folius.dotnotes.ui.NoteMapScreen
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: NoteViewModel = viewModel()
            val theme by viewModel.theme.collectAsState()
            val isDarkTheme = when (theme) {
                "Light" -> false
                "Dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            DotNotesTheme(theme = theme, darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                
                NavHost(
                    navController = navController, 
                    startDestination = "list",
                    enterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)
                        )
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)
                        )
                    },
                    popEnterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)
                        )
                    },
                    popExitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)
                        )
                    }
                ) {
                    composable("list") {
                        NoteListScreen(
                            viewModel = viewModel,
                            onNoteClick = { id -> navController.navigate("editor/$id") { launchSingleTop = true } },
                            onMapNoteClick = { id -> navController.navigate("map/$id") { launchSingleTop = true } },
                            onAddNoteClick = { tab -> navController.navigate("editor/-1?initialTab=${tab.name}") { launchSingleTop = true } },
                            onSettingsClick = { navController.navigate("settings") { launchSingleTop = true } }
                        )
                    }
                    composable("settings") {
                        com.folius.dotnotes.ui.SettingsScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onSecretNotesClick = { navController.navigate("secret_notes") },
                            onTrashClick = { navController.navigate("trash") }
                        )
                    }
                    composable("trash") {
                        com.folius.dotnotes.ui.TrashScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("secret_notes") {
                        com.folius.dotnotes.ui.SecretNotesScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onNoteClick = { id -> navController.navigate("editor/$id") }
                        )
                    }
                    composable(
                        route = "editor/{noteId}?initialTab={initialTab}",
                        arguments = listOf(
                            navArgument("noteId") { type = NavType.IntType },
                            navArgument("initialTab") { type = NavType.StringType; defaultValue = "NOTES" }
                        )
                    ) { backStackEntry ->
                        val noteId = backStackEntry.arguments?.getInt("noteId")
                        val initialTab = backStackEntry.arguments?.getString("initialTab") ?: "NOTES"
                        NoteEditorScreen(
                            viewModel = viewModel,
                            noteId = noteId,
                            initialTab = initialTab,
                            onBack = { navController.popBackStack() },
                            onImageClick = { uri -> 
                                val encodedUri = java.net.URLEncoder.encode(uri, "UTF-8")
                                navController.navigate("image_viewer/$encodedUri") 
                            },
                            onNavigateToNote = { linkedNoteId ->
                                navController.navigate("editor/$linkedNoteId") { launchSingleTop = true }
                            }
                        )
                    }
                    composable(
                        route = "image_viewer/{imageUri}",
                        arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val imageUri = backStackEntry.arguments?.getString("imageUri")
                        if (imageUri != null) {
                            com.folius.dotnotes.ui.ImageViewerScreen(
                                imageUri = imageUri,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                    composable(
                        route = "map/{noteId}",
                        arguments = listOf(navArgument("noteId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val noteId = backStackEntry.arguments?.getInt("noteId") ?: -1
                        val allNotes by viewModel.allNonDeletedNotes.collectAsState(initial = emptyList())
                        val note = allNotes.find { it.id == noteId }
                        
                        NoteMapScreen(
                            noteId = noteId,
                            viewModel = viewModel,
                            mapItems = note?.mapItems ?: emptyList(),
                            onMapItemsChange = { items -> 
                                viewModel.updateMapItems(noteId, items)
                            },
                            onNavigateToNote = { linkedNoteId ->
                                navController.navigate("editor/$linkedNoteId")
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}