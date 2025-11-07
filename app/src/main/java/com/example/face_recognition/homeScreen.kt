package com.example.face_recognition

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable

import androidx.compose.runtime.remember

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.ripple.rememberRipple



import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
@Composable
fun HomeScreen(
    user:User,

    onHeartRateClick: () -> Unit,
    onStressClick: () -> Unit,
    onBpClick: () -> Unit,
    onSpO2Click: () -> Unit,
    onLogoutClick: () -> Unit
) {

    var isLoading by remember { mutableStateOf(true) }

    // Simulate loading
    LaunchedEffect(Unit) {
        delay(700)
        isLoading = false
    }

//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(MaterialTheme.colorScheme.background)
//            .padding(horizontal = 16.dp)
//    ) {
        // Header with reactive user name
//        HomeScreenHeader(userName = user.name , onLogoutClick = onLogoutClick)
//
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        Box(
//            modifier = Modifier.fillMaxSize(),
//            contentAlignment = Alignment.TopCenter
//        ) {
        Crossfade(targetState = isLoading) { loading ->
            if (loading) {
                // ----------- Shimmer Loading State -----------
                val shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.View)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(
                        modifier = Modifier.height(
                            WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp
                        )
                    )

                    // Fake header shimmer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(30.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .shimmer(shimmer)
                            .background(Color.LightGray.copy(alpha = 0.4f))
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Fake subtitle shimmer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(20.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .shimmer(shimmer)
                            .background(Color.LightGray.copy(alpha = 0.4f))
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Grid shimmer
                    ShimmerGridPlaceholder()
                }
            } else {
                val cardItems = listOf(
                    HealthCard("Heart Rate", "Check your BPM", "heart.json", onHeartRateClick),
                    HealthCard("Stress Level", "Analyze stress", "stress.json", onStressClick),
                    HealthCard("Blood Pressure", "Estimate BP", "blood_pressure.json", onBpClick),
                    HealthCard("Oxygen Level", "Measure SpO₂", "oxygen.json", onSpO2Click)
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    HomeScreenHeader(userName = user.name, onLogoutClick = onLogoutClick)
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        
                        items(cardItems, key = { it.title }) { card ->
                            HealthFeatureCard(card, modifier = Modifier.height(220.dp))
                        }
                    }
                }
            }
        }
    }




    @Composable
    fun HealthFeatureCard(card: HealthCard, modifier: Modifier = Modifier) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val bg = when (card.title) {
            "Heart Rate" -> Color(0xFFFFFDE7)
            "Stress Level" -> Color(0xFFE8F5E9)
            "Blood Pressure" -> Color(0xFFE3F2FD)
            else -> Color(0xFFF3E5F5)
        }
        val titleColor = when (card.title) {
            "Heart Rate" -> Color(0xFF2E7D32)
            "Stress Level" -> Color(0xFF2E7D32)
            "Blood Pressure" -> Color(0xFF1565C0)
            else -> Color(0xFF6A1B9A)
        }



        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.97f else 1f,
            animationSpec = tween(120)
        )
        Card(
            modifier = modifier
//            .fillMaxWidth()
//            .aspectRatio(1.05f)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { card.onClick() }
                ),
            colors = CardDefaults.cardColors(containerColor = bg),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Lottie Animation (if needed)

                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .padding(top = 8.dp)
                ) {
                    HealLensLottieAnimation(
                        assetName = card.lottieAsset,
                        modifier = Modifier.fillMaxSize(),
                        loop = true
                    )
                }


                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.8f)
                        .padding(bottom = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = titleColor
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = card.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.DarkGray
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }


    @Composable
    fun ShimmerGridPlaceholder() {
        val shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.View)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(4) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .shimmer(shimmer)
                        .background(Color.LightGray.copy(alpha = 0.4f))
                )
            }
        }
    }

@Composable
fun HomeScreenHeader(userName: String, onLogoutClick: () -> Unit) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 20.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            ),
        horizontalAlignment = Alignment.Start
    ) {
        // Main Welcome Text
        Row(
            modifier = Modifier
                .fillMaxWidth(),

            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Welcome, ${userName }", // reactive, no Guest
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1B5E20)
                )
            )

            IconButton(onClick = onLogoutClick) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Supporting Text / Subtitle
        Text(
            text = "Ready for a quick health scan?\nSelect a feature below to begin",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                color = Color(0xFF4E4E4E).copy(alpha = 0.85f),
                lineHeight = 22.sp
            )
        )
    }
}