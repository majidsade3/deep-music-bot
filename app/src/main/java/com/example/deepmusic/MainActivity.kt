package com.example.deepmusic

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    private lateinit var player: MusicPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = MusicPlayer(applicationContext)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0A0F)
                ) {
                    MusicScreen(player)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.stop()
    }
}

@Composable
fun MusicScreen(player: MusicPlayer) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var trackNumber by remember { mutableIntStateOf(0) }
    var bass by remember { mutableFloatStateOf(1.0f) }
    var kick by remember { mutableFloatStateOf(1.0f) }
    var bpm by remember { mutableIntStateOf(126) }

    LaunchedEffect(isPlaying) {
        player.onNewTrack = { trackNumber = it }
        player.onSaved = { path ->
            val msg = if (path != null) "ذخیره شد: $path" else "خطا در ذخیره"
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
        if (isPlaying) player.start() else player.stop()
    }

    LaunchedEffect(bass) { player.bassIntensity = bass }
    LaunchedEffect(kick) { player.kickIntensity = kick }
    LaunchedEffect(bpm) { player.bpm = bpm }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        Text("◈ DEEP ◈", fontSize = 36.sp, color = Color(0xFF6B5B95),
            fontWeight = FontWeight.Bold)
        Text("philosophical electronic", fontSize = 12.sp,
            color = Color(0xFF4A4A5A), letterSpacing = 4.sp)

        Spacer(Modifier.height(32.dp))

        Text(
            text = if (isPlaying) "TRACK ${String.format("%03d", trackNumber)}" else "STANDBY",
            fontSize = 18.sp, color = Color(0xFFB0B0C0),
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.height(8.dp))
        Text("BPM: $bpm", fontSize = 12.sp, color = Color(0xFF6B5B95))

        Spacer(Modifier.height(32.dp))

        ControlSlider(
            label = "🎚️ BASS INTENSITY",
            value = bass,
            range = 0f..2f,
            onChange = { bass = it }
        )

        ControlSlider(
            label = "🥁 KICK INTENSITY",
            value = kick,
            range = 0f..2f,
            onChange = { kick = it }
        )

        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("⚡ TEMPO: $bpm BPM", color = Color(0xFFB0B0C0),
                fontSize = 13.sp, letterSpacing = 1.sp)
            Slider(
                value = bpm.toFloat(),
                onValueChange = { bpm = it.toInt() },
                valueRange = 80f..180f,
                steps = 99,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF6B5B95),
                    activeTrackColor = Color(0xFF6B5B95),
                    inactiveTrackColor = Color(0xFF2A1A3A)
                )
            )
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { isPlaying = !isPlaying },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPlaying) Color(0xFF2A1A3A) else Color(0xFF6B5B95),
                contentColor = Color(0xFFE0E0F0)
            ),
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                if (isPlaying) "◼ TERMINATE" else "▶ INITIATE",
                fontSize = 16.sp, letterSpacing = 3.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                player.requestSaveNextTrack()
                Toast.makeText(context,
                    "قطعه بعدی ذخیره می‌شود...", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF6B5B95)
            )
        ) {
            Text("💾 SAVE NEXT TRACK", fontSize = 14.sp, letterSpacing = 2.sp)
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "Saved to: /Music/DeepMusic/",
            fontSize = 11.sp, color = Color(0xFF4A4A5A),
            fontFamily = FontFamily.Monospace
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun ControlSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color(0xFFB0B0C0), fontSize = 13.sp, letterSpacing = 1.sp)
            Text(String.format("%.1fx", value), color = Color(0xFF6B5B95),
                fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF6B5B95),
                activeTrackColor = Color(0xFF6B5B95),
                inactiveTrackColor = Color(0xFF2A1A3A)
            )
        )
    }
}
