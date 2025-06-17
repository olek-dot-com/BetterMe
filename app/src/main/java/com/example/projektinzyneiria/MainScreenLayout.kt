
//import androidx.compose.material.Button
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
//import com.example.projektinzyneiria.UsageScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.example.projektinzyneiria.BlackAndWhiteSchedule
import com.example.projektinzyneiria.SecondBreathScreen
import com.example.projektinzyneiria.UsageScreen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview(showBackground = true)
fun MainApp() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedItem by remember { mutableStateOf("mainContent") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(200.dp)) {
                IconButton(onClick = { scope.launch { drawerState.close() } },
                    modifier = Modifier.align(Alignment.Start)) {
                    Icon(Icons.Default.Close, contentDescription = "Close Drawer")
                }
                DrawerContent(navController, drawerState, scope) // Use the DrawerContent composable from MainScreenLayout.kt
            }
        },
    ) {
        MainScreen(
            onDrawerIconClicked = { scope.launch { drawerState.open() } }, // Pass the callback
            content = { innerModifier ->
                NavHost(
                    navController = navController,
                    startDestination = "usageScreen",
                    modifier = innerModifier
                ) {
                    composable("usageScreen") {
                        UsageScreen()
                    }

                    composable("BlackAndWhiteSchedule") {
                        BlackAndWhiteSchedule()
                    }
                    composable("SecondBreath") {
                        SecondBreathScreen()
                    }
                }
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onDrawerIconClicked: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit // Pass Modifier to content
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Screen Report") },
                navigationIcon = {
                    IconButton(onClick = onDrawerIconClicked) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Drawer"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = { BottomBar() },
        modifier = modifier
    ) { innerPadding ->
        // Use the provided content composable and pass the padding
        content(Modifier.padding(innerPadding))
    }
}

@Composable
fun DrawerContent(navController: NavController, drawerState: androidx.compose.material3.DrawerState, scope: CoroutineScope) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {
            navController.navigate("usageScreen")
            scope.launch { drawerState.close() }
        }) {
            Text("Screen report", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            navController.navigate("SecondBreath")
            scope.launch { drawerState.close() }
        }) {
            Text("Second breath", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            navController.navigate("BlackAndWhiteSchedule")
            scope.launch { drawerState.close() }
        }) {
            Text("B&W schedule", style = MaterialTheme.typography.titleMedium)
        }
    }
}

//To do
@Composable
fun BottomBar() {
    BottomNavigation(
        backgroundColor = Color(0xFF6200EE),
        contentColor = Color.White
    ) {
        BottomNavigationItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            selected = false,
            onClick = { /* Handle Home click */ }
        )
        BottomNavigationItem(
            icon = { Icon(Icons.Default.Notifications, contentDescription = "Notifications") },
            selected = false,
            onClick = { /* Handle Notifications click */ }
        )
        BottomNavigationItem(
            icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Profile") },
            selected = false,
            onClick = { /* Handle Profile click */ }
        )
    }
}
