package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FocusRepository
import com.example.ui.CreatedByGalileoFooter
import com.example.ui.theme.MyApplicationTheme
import java.lang.ref.WeakReference

class BlockedActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeInstance = WeakReference(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                BlockedAppScreen(
                    onReturnToStudy = { returnToFocusLock() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        activeInstance = WeakReference(this)
    }

    override fun onResume() {
        super.onResume()
        isForeground = true
        val prefs = getSharedPreferences(FocusRepository.PREFS_NAME, MODE_PRIVATE)
        val lockEnabled = prefs.getBoolean(FocusRepository.KEY_STUDY_APP_LOCK_ENABLED, true)
        val isStopwatch = prefs.getBoolean(FocusRepository.KEY_IS_STOPWATCH, false)
        val sessionStartMs = prefs.getLong(FocusRepository.KEY_SESSION_START_MS, 0L)
        val sessionEndMs = prefs.getLong(FocusRepository.KEY_END, 0L)
        val active = lockEnabled && if (isStopwatch) {
            sessionStartMs > 0L
        } else {
            System.currentTimeMillis() < sessionEndMs
        }
        if (!active) {
            finish()
        }
    }

    override fun onPause() {
        isForeground = false
        super.onPause()
    }

    override fun onDestroy() {
        if (activeInstance?.get() === this) {
            activeInstance = null
        }
        isForeground = false
        super.onDestroy()
    }

    private fun returnToFocusLock() {
        isForeground = false
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        startActivity(intent)
        finish()
    }

    companion object {
        @Volatile
        var isForeground: Boolean = false
            private set

        @Volatile
        private var activeInstance: WeakReference<BlockedActivity>? = null

        fun dismissIfOpen() {
            val activity = activeInstance?.get() ?: return
            activity.runOnUiThread {
                if (!activity.isFinishing && !activity.isDestroyed) {
                    activity.finish()
                }
            }
        }
    }
}

@Composable
fun BlockedAppScreen(
    onReturnToStudy: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Keep the blocked message visible while the user remains inside the blocked app;
    // pressing Back will not reveal the blocked app underneath.
    BackHandler(enabled = true) {
        // Intentionally stay on the blocked screen until the user exits the blocked app
        // or taps "الرجوع للمذاكرة".
    }

    val calmGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0B1730),
            Color(0xFF102A43),
            Color(0xFF0F3D3E)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(calmGradient)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF34D399).copy(alpha = 0.16f),
                border = BorderStroke(1.dp, Color(0xFF6EE7B7).copy(alpha = 0.5f))
            ) {
                Text(
                    text = stringResource(R.string.salawat_banner_text),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA7F3D0),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
            }

            Surface(
                shape = CircleShape,
                color = Color(0xFF38BDF8).copy(alpha = 0.16f),
                border = BorderStroke(1.5.dp, Color(0xFF7DD3FC).copy(alpha = 0.5f)),
                modifier = Modifier.size(92.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.SelfImprovement,
                        contentDescription = null,
                        tint = Color(0xFFBAE6FD),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.blocked_screen_message),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 28.sp,
                    lineHeight = 42.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFFF0F9FF),
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("blocked_message_text")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onReturnToStudy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF34D399),
                    contentColor = Color(0xFF022C22)
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("return_to_study_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.blocked_screen_button),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        CreatedByGalileoFooter(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth(),
            textColor = Color(0xFFCBD5E1)
        )
    }
}
