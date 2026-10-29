package com.verdant.app.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CenterFocusWeak
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.app.R
import com.verdant.app.data.preferences.UserPreferencesRepository
import com.verdant.app.ui.components.AuroraBackground
import com.verdant.app.ui.components.LeafCluster
import com.verdant.app.ui.components.PulseRings
import com.verdant.app.ui.theme.Forest
import com.verdant.app.ui.theme.ForestDeep
import com.verdant.app.ui.theme.Leaf
import com.verdant.app.ui.theme.Lime
import com.verdant.app.ui.theme.Mint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.absoluteValue

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository,
) : ViewModel() {
    fun complete(onDone: () -> Unit) {
        viewModelScope.launch {
            preferences.setOnboardingComplete(true)
            onDone()
        }
    }
}

private data class Page(@StringRes val title: Int, @StringRes val body: Int, val icon: ImageVector, val orbit: List<ImageVector>)

private val pages = listOf(
    Page(R.string.onboarding_title_1, R.string.onboarding_body_1, Icons.Rounded.CenterFocusWeak, listOf(Icons.Rounded.Spa, Icons.Rounded.WbSunny)),
    Page(R.string.onboarding_title_2, R.string.onboarding_body_2, Icons.Rounded.HealthAndSafety, listOf(Icons.Rounded.Spa, Icons.Rounded.WaterDrop)),
    Page(R.string.onboarding_title_3, R.string.onboarding_body_3, Icons.Rounded.Spa, listOf(Icons.Rounded.WaterDrop, Icons.Rounded.WbSunny)),
)

/** Always dark and immersive, independent of the app theme, for a striking first impression. */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    OnboardingContent(onFinish = { viewModel.complete(onFinished) })
}

@Composable
internal fun OnboardingContent(onFinish: () -> Unit, initialPage: Int = 0) {
    val pagerState = rememberPagerState(initialPage) { pages.size }
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex
    val finish = onFinish

    AuroraBackground(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ForestDeep, Forest, ForestDeep))),
        colors = listOf(Leaf.copy(alpha = 0.55f), Lime.copy(alpha = 0.18f), Leaf.copy(alpha = 0.35f)),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp),
                )
                if (!isLast) {
                    TextButton(onClick = finish) {
                        Text(stringResource(R.string.onboarding_skip), color = Mint)
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { index ->
                val page = pages[index]
                val offset = ((pagerState.currentPage - index) + pagerState.currentPageOffsetFraction).absoluteValue
                Column(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 1f - offset.coerceIn(0f, 1f) * 0.6f
                            val s = 1f - offset.coerceIn(0f, 1f) * 0.12f
                            scaleX = s; scaleY = s
                        }
                        .padding(horizontal = 28.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Hero(page, Modifier.align(Alignment.CenterHorizontally))
                    Spacer(Modifier.height(40.dp))
                    Text(stringResource(page.title), style = MaterialTheme.typography.displaySmall, color = Color.White)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        stringResource(page.body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = Mint, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.onboarding_privacy), style = MaterialTheme.typography.bodySmall, color = Mint)
            }
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(pages.size) { i ->
                        val selected = i == pagerState.currentPage
                        val width by animateDpAsState(if (selected) 28.dp else 8.dp, label = "dot")
                        val color by animateColorAsState(if (selected) Lime else Color.White.copy(alpha = 0.3f), label = "dotColor")
                        Box(
                            Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(color),
                        )
                    }
                }
                Button(
                    onClick = {
                        if (isLast) finish() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Forest),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                ) {
                    AnimatedContent(isLast, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "cta") { last ->
                        Text(
                            stringResource(if (last) R.string.onboarding_get_started else R.string.onboarding_next),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun Hero(page: Page, modifier: Modifier = Modifier) {
    Box(modifier.size(280.dp), contentAlignment = Alignment.Center) {
        PulseRings(Modifier.size(280.dp), color = Lime)
        Box(
            Modifier
                .size(190.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Leaf.copy(alpha = 0.9f), Forest))),
            contentAlignment = Alignment.Center,
        ) {
            LeafCluster(Modifier.size(140.dp), primary = Lime, secondary = Mint.copy(alpha = 0.7f), vein = Forest)
        }
        // Main badge
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 30.dp, bottom = 34.dp)
                .size(64.dp)
                .clip(CircleShape)
                .background(Lime),
            contentAlignment = Alignment.Center,
        ) {
            Icon(page.icon, contentDescription = null, tint = Forest, modifier = Modifier.size(32.dp))
        }
        page.orbit.forEachIndexed { i, icon ->
            Box(
                Modifier
                    .align(if (i == 0) Alignment.TopStart else Alignment.CenterStart)
                    .padding(start = if (i == 0) 36.dp else 4.dp, top = if (i == 0) 36.dp else 80.dp)
                    .size(44.dp)
                    .rotate(if (i == 0) -8f else 8f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Mint, modifier = Modifier.size(22.dp))
            }
        }
    }
}
