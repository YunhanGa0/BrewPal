package com.omelan.cofi.components

import android.annotation.SuppressLint
import androidx.annotation.FloatRange
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omelan.cofi.R
import com.omelan.cofi.share.components.StepNameText
import com.omelan.cofi.share.components.TimeText
import com.omelan.cofi.share.components.TimerValue
import com.omelan.cofi.share.model.Step
import com.omelan.cofi.share.model.StepType
import com.omelan.cofi.ui.Spacing
import com.omelan.cofi.ui.green600

@Composable
fun Track(
    modifier: Modifier = Modifier,
    @FloatRange(from = 0.0, to = 1.0) progress: Float,
    color: Color,
    backgroundColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    strokeWidth: Dp,
) {
    val stroke = with(LocalDensity.current) {
        Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Bevel)
    }
    val trackOffset by remember(progress) {
        derivedStateOf {
            if (progress == 0f) 0f else 6f
        }
    }

    Canvas(
        modifier
            .progressSemantics(progress)
            .aspectRatio(1f),
    ) {
        val startAngle = 270f
        val sweep = progress * 360f
        val diameterOffset = stroke.width / 2
        val arcDimen = size.width - 2 * diameterOffset
        drawArc(
            color = backgroundColor,
            startAngle = sweep + startAngle + (trackOffset),
            sweepAngle = 360f - sweep - (trackOffset * 2),
            useCenter = false,
            topLeft = Offset(diameterOffset, diameterOffset),
            size = Size(arcDimen, arcDimen),
            style = stroke,
        )
        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(diameterOffset, diameterOffset),
            size = Size(arcDimen, arcDimen),
            style = stroke,
        )
    }
}

@Composable
fun Timer(
    modifier: Modifier = Modifier,
    currentStep: Step?,
    allSteps: List<Step> = emptyList(),
    alreadyDoneWeight: Float = 0f,
    animatedProgressValue: Animatable<Float, AnimationVector1D>,
    animatedProgressColor: Animatable<Color, AnimationVector4D>,
    isInPiP: Boolean,
    isDone: Boolean = false,
    weightMultiplier: Float = 1.0f,
    timeMultiplier: Float = 1.0f,
) {
    val strokeWidth = if (isInPiP) {
        10.dp
    } else {
        15.dp
    }
    val calculatedAnimatedProgress = remember {
        derivedStateOf {
            animatedProgressValue.value * timeMultiplier
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        AnimatedVisibility(
            visible = currentStep == null && !isDone,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            RecipeInfo(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxSize(),
                steps = allSteps,
                timeMultiplier = timeMultiplier,
                weightMultiplier = weightMultiplier,
            )
        }
        AnimatedVisibility(visible = isDone, enter = fadeIn(), exit = fadeOut()) {
            Column(
                modifier = Modifier
                    .padding(strokeWidth)
                    .animateContentSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(id = R.string.timer_enjoy),
                    style = if (isInPiP) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.headlineMedium
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isInPiP) 2 else Int.MAX_VALUE,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("timer_enjoy"),
                )
                if (!isInPiP) {
                    Spacer(modifier = Modifier.height(Spacing.normal))
                }
                Icon(painter = painterResource(id = R.drawable.ic_coffee), contentDescription = "")
            }
        }
        AnimatedVisibility(
            visible = currentStep != null && !isDone,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .padding(strokeWidth)
                    .aspectRatio(1f)
                    .align(Alignment.Center)
                    .fillMaxSize()
                    .animateContentSize(),
                Arrangement.Center,
                Alignment.CenterHorizontally,
            ) {
                if (currentStep != null) {
                    TimeText(
                        modifier = Modifier.weight(1f, true),
                        currentStep = currentStep,
                        animatedProgressValue = (animatedProgressValue.value * timeMultiplier),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (isInPiP) 2 else Int.MAX_VALUE,
                        style = if (isInPiP) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.headlineMedium
                        },
                        paddingHorizontal = if (isInPiP) Spacing.xSmall else Spacing.normal,
                        showMillis = !isInPiP,
                    )
                    HorizontalDivider()
                    StepNameText(
                        currentStep = currentStep,
                        timeMultiplier = timeMultiplier,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = if (isInPiP) {
                            MaterialTheme.typography.titleSmall
                        } else {
                            MaterialTheme.typography.titleLarge
                        },
                        maxLines = if (isInPiP) 1 else Int.MAX_VALUE,
                        paddingHorizontal = if (isInPiP) Spacing.xSmall else Spacing.normal,
                    )
                    HorizontalDivider()
                    TimerValue(
                        modifier = Modifier.weight(1f, true),
                        currentStep = currentStep,
                        animatedProgressValue = calculatedAnimatedProgress.value,
                        alreadyDoneWeight = alreadyDoneWeight,
                        weightMultiplier = weightMultiplier,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (isInPiP) 1 else Int.MAX_VALUE,
                        style = if (isInPiP) {
                            MaterialTheme.typography.titleLarge
                        } else {
                            MaterialTheme.typography.headlineMedium
                        },
                    )
                }
            }
        }
        Track(
            progress = if (isDone) 1f else animatedProgressValue.value,
            color = animatedProgressColor.value,
            strokeWidth = strokeWidth,
        )
    }
}

@SuppressLint("UnrememberedAnimatable")
@Preview
@Composable
fun TimerPreview() {
    val animatedProgressValue = remember { Animatable(0f) }
    LaunchedEffect(key1 = true) {
        animatedProgressValue.animateTo(
            1f,
            tween(durationMillis = 5 * 1000, easing = LinearEasing),
        )
    }
    Timer(
        currentStep = Step(
            id = 1,
            name = "ExperimentalAnimatedInsets ExperimentalAnimatedInsets " +
                "ExperimentalAnimatedInsets ExperimentalAnimatedInsets",
            time = 5 * 1000,
            type = StepType.OTHER,
            orderInRecipe = 0,
        ),
        animatedProgressValue = animatedProgressValue,
        animatedProgressColor = Animatable(green600),
        isInPiP = false,
        isDone = false,
    )
}

@SuppressLint("UnrememberedAnimatable")
@Preview(widthDp = 150, showBackground = true)
@Composable
fun TimerPreviewPiP() {
    Timer(
        currentStep = Step(
            id = 1,
            name = "ExperimentalAnimatedInsets ExperimentalAnimatedInsets ",
            time = 5 * 1000,
            type = StepType.WATER,
            value = 300.0f,
            orderInRecipe = 0,
        ),
        animatedProgressValue = Animatable(0.2f),
        animatedProgressColor = Animatable(StepType.WATER.color),
        isInPiP = true,
        isDone = false,
    )
}

@SuppressLint("UnrememberedAnimatable")
@Preview
@Composable
fun TimerPreviewDone() {
    Timer(
        currentStep = null,
        animatedProgressValue = Animatable(0.5f),
        animatedProgressColor = Animatable(green600),
        isInPiP = false,
        isDone = true,
    )
}

@SuppressLint("UnrememberedAnimatable")
@Preview(widthDp = 150, showBackground = true)
@Composable
fun TimerPreviewDonePip() {
    Timer(
        currentStep = null,
        animatedProgressValue = Animatable(0.5f),
        animatedProgressColor = Animatable(green600),
        isInPiP = true,
        isDone = true,
    )
}
