package com.personal.fittrack.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.personal.fittrack.ui.theme.FitTrackTheme

/**
 * Health Connect requires every app that reads health data to declare an activity handling
 * "view permission usage" / "show permissions rationale" - it's what Health Connect's own
 * Settings page links to under "See how this app uses your data". Without this declared in
 * the manifest, Health Connect's SDK throws `IllegalStateException: Incorrect health
 * permission state` the moment you try to read anything, even with permission granted.
 * This screen is deliberately tiny: a one-line explanation, nothing else needed.
 */
class HealthPermissionRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitTrackTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "FitTrack reads your Active Calories Burned from Health Connect " +
                                "so it can fold exercise into your daily calorie deficit/surplus. " +
                                "This data is never written back or shared off your device.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}
