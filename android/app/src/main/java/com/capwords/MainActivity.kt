package com.capwords

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.capwords.ui.navigation.CapWordsNavHost
import com.capwords.ui.theme.CapBackground
import com.capwords.ui.theme.CapWordsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CapWordsTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = CapBackground) {
                    CapWordsNavHost()
                }
            }
        }
    }
}
