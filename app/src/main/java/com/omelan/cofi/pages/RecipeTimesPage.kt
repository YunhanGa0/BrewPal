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
        }
    }
}

// 添加跳转到配方详情页的函数
// fun navigateToRecipeDetail(navController: NavController, recipeId: Int) {
//     navController.navigate("recipe/$recipeId")
// }
