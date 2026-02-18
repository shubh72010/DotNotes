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
import androidx.compose.ui.tooling.preview.Preview
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
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

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
                            animationSpec = tween(300)
                        )
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(300)
                        )
                    },
                    popEnterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(300)
                        )
                    },
                    popExitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(300)
                        )
                    }
                ) {
                    composable("list") {
                        NoteListScreen(
                            viewModel = viewModel,
                            onNoteClick = { id -> navController.navigate("editor/$id") },
                            onAddNoteClick = { navController.navigate("editor/-1") },
                            onSettingsClick = { navController.navigate("settings") }
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
                        route = "editor/{noteId}",
                        arguments = listOf(navArgument("noteId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val noteId = backStackEntry.arguments?.getInt("noteId")
                        NoteEditorScreen(
                            viewModel = viewModel,
                            noteId = noteId,
                            onBack = { navController.popBackStack() },
                            onImageClick = { uri -> 
                                val encodedUri = java.net.URLEncoder.encode(uri, "UTF-8")
                                navController.navigate("image_viewer/$encodedUri") 
                            },
                            onNavigateToNote = { linkedNoteId ->
                                navController.navigate("editor/$linkedNoteId")
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
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DotNotesTheme {
        Greeting("Android")
    }
}