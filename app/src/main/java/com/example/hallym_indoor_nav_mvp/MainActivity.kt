package com.example.hallym_indoor_nav_mvp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hallym_indoor_nav_mvp.ui.theme.Hallym_indoor_nav_MVPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Hallym_indoor_nav_MVPTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        onNavigateToMap = { navigateToMap() }
                    )
                }
            }
        }
    }

    private fun navigateToMap() {
        val intent = Intent(this, MapActivity::class.java)
        startActivity(intent)
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier, onNavigateToMap: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = { onNavigateToMap() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("도면 선택")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    Hallym_indoor_nav_MVPTheme {
        MainScreen(onNavigateToMap = {})
    }
}
