@file:OptIn(ExperimentalFoundationApi::class)

package com.omelan.cofi.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omelan.cofi.share.model.Recipe
import com.omelan.cofi.share.model.Step
import com.omelan.cofi.ui.Spacing
import com.omelan.cofi.ui.card
import com.omelan.cofi.ui.shapes

@Composable
fun RecipeListItemBackground(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        shape = shapes.card,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier.clickable(
                onClick = onClick,
                role = Role.Button,
            ).padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun LazyGridItemScope.RecipeItem(
    recipe: Recipe,
    allSteps: List<Step> = emptyList(),
    onPress: (recipeId: Int) -> Unit,
) {
    RecipeListItemBackground(
        modifier = Modifier.animateItemPlacement(),
        onClick = { onPress(recipe.id) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium)
                .height(75.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(id = recipe.recipeIcon.icon),
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = Spacing.small)
                    .size(28.dp),
            )
            Column(
                modifier = Modifier.padding(
                    vertical = Spacing.big,
                    horizontal = Spacing.medium,
                ),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = recipe.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (recipe.description.isNotBlank()) {
                    Text(
                        text = recipe.description,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        HorizontalDivider()
        RecipeInfo(compactStyle = true, steps = allSteps)
    }
}

// @Preview
// @Composable
// fun PreviewRecipeItem() {
//    RecipeItem(
//        recipe = Recipe(
//            id = 0,
//            name = "Ultimate V60",
//            description = "Recipe by Hoffman",
//        ),
//        allSteps = listOf(
//            Step(
//                name = stringResource(R.string.prepopulate_step_coffee),
//                value = 30,
//                time = 5.toMillis(),
//                type = StepType.ADD_COFFEE,
//            ),
//            Step(
//                name = stringResource(R.string.prepopulate_step_water),
//                value = 60,
//                time = 5.toMillis(),
//                type = StepType.WATER,
//            ),
//            Step(
//                name = stringResource(R.string.prepopulate_step_swirl),
//                time = 5.toMillis(),
//                type = StepType.OTHER,
//            ),
//            Step(
//                name = stringResource(R.string.prepopulate_step_wait),
//                time = 35.toMillis(),
//                type = StepType.WAIT,
//            ),
//            Step(
//                name = stringResource(R.string.prepopulate_step_water),
//                time = 30.toMillis(),
//                type = StepType.WATER,
//                value = 240,
//            ),
//        ),
//        onPress = {},
//    )
// }
