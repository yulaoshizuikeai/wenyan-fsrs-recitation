package com.ancient.wenyan.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import com.ancient.wenyan.ui.theme.BambooGreen
import com.ancient.wenyan.ui.theme.CeladonBlue
import com.ancient.wenyan.ui.theme.CinnabarRed
import com.ancient.wenyan.ui.theme.MutedGold
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.Spread
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

@Composable
fun DuolingoStyleCelebration(
    modifier: Modifier = Modifier
) {
    val cRed = CinnabarRed.toArgb()
    val bGreen = BambooGreen.toArgb()
    val mGold = MutedGold.toArgb()
    val cBlue = CeladonBlue.toArgb()

    val classicalColors = remember(cRed, bGreen, mGold, cBlue) {
        listOf(
            cRed,
            bGreen,
            mGold,
            cBlue,
            0xFFE6C229.toInt(), // Imperial Gold
            0xFF2D6A4F.toInt()  // Deep Forest Green
        )
    }

    val parties = remember {
        listOf(
            Party(
                speed = 10f,
                maxSpeed = 30f,
                damping = 0.9f,
                spread = 360,
                colors = classicalColors,
                emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(80),
                position = Position.Relative(0.5, 0.25)
            ),
            Party(
                speed = 20f,
                maxSpeed = 35f,
                damping = 0.9f,
                angle = 45,
                spread = 60,
                colors = classicalColors,
                emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(50),
                position = Position.Relative(0.0, 0.4)
            ),
            Party(
                speed = 20f,
                maxSpeed = 35f,
                damping = 0.9f,
                angle = 135,
                spread = 60,
                colors = classicalColors,
                emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(50),
                position = Position.Relative(1.0, 0.4)
            )
        )
    }

    KonfettiView(
        modifier = modifier.fillMaxSize(),
        parties = parties
    )
}
