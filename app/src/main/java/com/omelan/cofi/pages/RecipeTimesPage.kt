import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.omelan.cofi.share.model.Recipe
import com.omelan.cofi.share.model.RecipeViewModel
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeTimesPage(
    navigateToRecipe: (recipeId: Int) -> Unit,
    goBack: () -> Unit,
    recipeViewModel: RecipeViewModel = viewModel()
) {
    val recipes by recipeViewModel.getAllRecipes().observeAsState(initial = emptyList())
    val sortedRecipes = recipes.sortedByDescending { it.times }

    // 生成颜色列表
    val colors = sortedRecipes.map { Color(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Brew History") },
                navigationIcon = {
                    IconButton(onClick = goBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sortedRecipes) { recipe ->
                Card(
                    onClick = { navigateToRecipe(recipe.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = recipe.recipeIcon.icon),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = recipe.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = "${recipe.times} Times",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 使用 Row 来并排显示 PieChart 和图例
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // PieChart
                    PieChart(
                        data = sortedRecipes.map { it.name to it.times },
                        colors = colors,
                        modifier = Modifier.size(200.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp)) // 添加间距

                    // 图例
                    Column(
                        modifier = Modifier
                            .weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        sortedRecipes.forEachIndexed { index, recipe ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(colors[index])
                                )
                                Text(
                                    text = recipe.name,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 在最后添加 AI 总结
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "AI Analysis",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        val summary = generateAISummary(sortedRecipes)
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun generateAISummary(recipes: List<Recipe>): String {
    if (recipes.isEmpty()) {
        return "Start brewing your first coffee to see your preferences analysis!"
    }

    val totalBrews = recipes.sumOf { it.times }
    val mostUsed = recipes.maxByOrNull { it.times }
    val recentlyUsed = recipes.filter { it.times > 0 }
    
    return buildString {
        append("Based on your brewing history of $totalBrews total brews, ")
        
        if (mostUsed != null && mostUsed.times > 0) {
            append("your go-to recipe is ${mostUsed.name} (${mostUsed.times} times). ")
        }
        
        if (recentlyUsed.size > 1) {
            append("You've explored ${recentlyUsed.size} different brewing methods, ")
            append("showing your interest in diverse coffee experiences. ")
        }
        
        // 添加使用频率分析
        val frequentRecipes = recipes.filter { it.times >= totalBrews * 0.2 }  // 使用次数占总次数20%以上
        if (frequentRecipes.isNotEmpty()) {
            append("You frequently use ${frequentRecipes.joinToString(", ") { it.name }}, ")
            append("which represents your core brewing preferences. ")
        }
        
        // 添加建议
        append("Consider ")
        if (recipes.any { it.times == 0 }) {
            append("trying out your unused recipes to expand your coffee journey!")
        } else {
            append("exploring new recipes to add to your collection!")
        }
    }
}

