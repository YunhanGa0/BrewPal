package com.omelan.cofi.pages.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.omelan.cofi.R
import com.omelan.cofi.components.PiPAwareAppBar
import com.omelan.cofi.components.RecipeItem
import com.omelan.cofi.components.createAppBarBehavior
import com.omelan.cofi.share.model.RecipeViewModel
import com.omelan.cofi.share.model.StepsViewModel
import com.omelan.cofi.share.pages.Destinations
import com.omelan.cofi.ui.Spacing
import com.omelan.cofi.utils.FabType
import com.omelan.cofi.utils.getDefaultPadding
import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.compose.ui.res.painterResource
import com.omelan.cofi.components.CoffeeShopRecommendDialog

// 在文件顶部定义companion object
private object RecipeListState {
    var isFirstOpenSinceLastLaunch = true
}

fun NavGraphBuilder.recipeList(navController: NavController) {
    composable(Destinations.RECIPE_LIST) {
        val context = LocalContext.current
        RecipeList(
            navigateToRecipe = { recipeId ->
                navController.navigate(
                    route = Destinations.recipeDetails(
                        recipeId,
                    ),
                )
            },
            addNewRecipe = { navController.navigate(route = Destinations.RECIPE_ADD) },
            goToSettings = { navController.navigate(route = Destinations.SETTINGS) },
            goToTimes = { navController.navigate(route = Destinations.RECIPE_TIMES) },
            goToImport ={ navController.navigate(route = Destinations.RECIPE_IMPORT) } ,
            goToCoffeeMap = { navController.navigate(route = Destinations.COFFEE_SHOP_MAP) },
            onNewButtonClick = {
                // 发送通知
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channelId = "test_channel"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(channelId, "Test Notifications", NotificationManager.IMPORTANCE_DEFAULT)
                    notificationManager.createNotificationChannel(channel)
                }
                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_github_icon)
                    .setContentTitle("新功能测试")
                    .setContentText("您点击了新添加的按钮！")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                notificationManager.notify(1, builder.build())
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeList(
    navigateToRecipe: (recipeId: Int) -> Unit,
    addNewRecipe: () -> Unit,
    goToSettings: () -> Unit,
    onNewButtonClick: () -> Unit,
    goToTimes: () -> Unit,
    goToImport: () -> Unit,
    goToCoffeeMap: () -> Unit,
    recipeViewModel: RecipeViewModel = viewModel(),
    stepsViewModel: StepsViewModel = viewModel(),
    context: Context = LocalContext.current
) {
    val configuration = LocalConfiguration.current
    val recipes by recipeViewModel.getAllRecipes().observeAsState(initial = emptyList())
    val steps by stepsViewModel.getAllSteps().observeAsState(initial = emptyList())
    val stepsByRecipe = steps.groupBy { it.recipeId }
    val scrollBehavior = createAppBarBehavior()
    val isMultiColumn by remember(configuration.screenWidthDp) {
        derivedStateOf { configuration.screenWidthDp > 600 }
    }
    val lazyGridState = rememberLazyGridState()
    val headerInfoBox = createRecipeListHeaderInfo(
        animateToTop = {
            lazyGridState.animateScrollToItem(0)
        },
    )
    var showRecommendDialog by remember { mutableStateOf(false) }
    
    // 检查是否是本次启动后的第一次打开
    LaunchedEffect(Unit) {
        if (RecipeListState.isFirstOpenSinceLastLaunch) {
            showRecommendDialog = true
            RecipeListState.isFirstOpenSinceLastLaunch = false
        }
    }

    if (showRecommendDialog) {
        CoffeeShopRecommendDialog(
            onDismiss = { showRecommendDialog = false },
            onNavigateToMap = goToCoffeeMap
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            PiPAwareAppBar(
                actions = {
                    IconButton(onClick = goToImport) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_import),
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = onNewButtonClick) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add new item")
                    }
                    IconButton(onClick = goToTimes) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_history),
                            contentDescription = "使用统计"
                        )
                    }
                    IconButton(onClick = goToSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = addNewRecipe,
                expanded = scrollBehavior.state.collapsedFraction < 0.9,
                modifier = Modifier.navigationBarsPadding(),
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        tint = LocalContentColor.current,
                        contentDescription = null,
                    )
                },
                text = {
                    Text(text = stringResource(R.string.recipe_create_title))
                },
            )
        },
    ) {
        LazyVerticalGrid(
            contentPadding = getDefaultPadding(it, FabType.Normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.normal),
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background),
            columns = if (isMultiColumn) GridCells.Fixed(2) else GridCells.Fixed(1),
            horizontalArrangement = Arrangement.spacedBy(Spacing.normal),
            state = lazyGridState,
        ) {
            headerInfoBox()
            items(recipes, key = { recipe -> recipe.id }) { recipe ->
                RecipeItem(
                    recipe = recipe,
                    onPress = navigateToRecipe,
                    allSteps = stepsByRecipe[recipe.id] ?: emptyList(),
                )
            }
        }
    }
}

@Preview
@Composable
fun RecipeListPreview() {
    RecipeList(
        navigateToRecipe = {},
        addNewRecipe = {},
        goToSettings = {},
        onNewButtonClick = {},
        goToTimes = {},
        goToImport = {},
        goToCoffeeMap = {}
    )
}
