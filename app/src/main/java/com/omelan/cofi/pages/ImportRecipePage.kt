import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.omelan.cofi.R
import com.omelan.cofi.components.QRCodeScannerDialog
import com.omelan.cofi.share.model.RecipeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportRecipePage(
    viewModel: RecipeViewModel,
    onImportComplete: () -> Unit
) {
    var importText by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shareUtils = remember { RecipeShareUtils() }
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                // 添加加载状态提示
                Toast.makeText(
                    context,
                    "正在处理图片...",
                    Toast.LENGTH_SHORT
                ).show()
                
                // 使用新的图片解码方式
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                
                // 确保图片不会太大，可能需要压缩
                val scaledBitmap = if (bitmap.width > 1024 || bitmap.height > 1024) {
                    val scale = min(1024f / bitmap.width, 1024f / bitmap.height)
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * scale).toInt(),
                        (bitmap.height * scale).toInt(),
                        true
                    )
                } else {
                    bitmap
                }
                
                val image = InputImage.fromBitmap(scaledBitmap, 0)
                val scanner = BarcodeScanning.getClient()
                
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isEmpty()) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.qr_code_not_found),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@addOnSuccessListener
                        }
                        
                        barcodes[0].rawValue?.let { value ->
                            scope.launch {
                                try {
                                    val (recipe, steps) = shareUtils.decodeRecipe(value)
                                    val recipeId = viewModel.insertRecipe(recipe.copy(id = 0, times = 0))
                                    val stepsWithRecipeId = steps.map { step ->
                                        step.copy(
                                            recipeId = recipeId.toInt(),
                                            id = 0,
                                            orderInRecipe = steps.indexOf(step)
                                        )
                                    }
                                    viewModel.insertSteps(stepsWithRecipeId)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.import_recipe_success),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onImportComplete()
                                    showScanner = false
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.import_recipe_error),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        Toast.makeText(
                            context,
                            context.getString(R.string.qr_code_scan_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    context,
                    context.getString(R.string.qr_code_scan_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_recipe_title)) },
                navigationIcon = {
                    IconButton(onClick = onImportComplete) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.import_recipe_text_hint)) },
                    trailingIcon = {
                        Row {
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.primaryClip?.getItemAt(0)?.text?.let {
                                    importText = it.toString()
                                }
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_content_paste),
                                    contentDescription = stringResource(R.string.import_recipe_paste)
                                )
                            }
                            if (importText.isNotEmpty()) {
                                IconButton(onClick = { importText = "" }) {
                                    Icon(Icons.Default.Clear, stringResource(R.string.import_recipe_clear))
                                }
                            }
                        }
                    }
                )
            }

            item {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val (recipe, steps) = shareUtils.decodeRecipe(importText)
                                val recipeId = viewModel.insertRecipe(recipe.copy(id = 0, times = 0))
                                val stepsWithRecipeId = steps.map { step ->
                                    step.copy(
                                        recipeId = recipeId.toInt(),
                                        id = 0,
                                        orderInRecipe = steps.indexOf(step)
                                    )
                                }
                                viewModel.insertSteps(stepsWithRecipeId)
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.import_recipe_success),
                                    Toast.LENGTH_SHORT
                                ).show()
                                onImportComplete()
                                showScanner = false
                            } catch (e: Exception) {
                                e.printStackTrace()
                                showError = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = importText.isNotEmpty()
                ) {
                    Text(stringResource(R.string.step_add_save))
                }
            }

            item {
                Divider()
            }

            item {
                OutlinedButton(
                    onClick = { showScanner = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_qrscanner),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(stringResource(R.string.import_recipe_scan))
                }
            }
        }

        // 保持对话框部分不变
        if (showError) {
            AlertDialog(
                onDismissRequest = { showError = false },
                title = { Text(stringResource(R.string.import_recipe_error)) },
                confirmButton = {
                    TextButton(onClick = { showError = false }) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            )
        }

        if (showScanner) {
            QRCodeScannerDialog(
                onDismiss = { 
                    showScanner = false
                },
                onPickImage = { imagePicker.launch("image/*") },
                onResult = { result ->
                    scope.launch {
                        try {
                            val (recipe, steps) = shareUtils.decodeRecipe(result)
                            val recipeId = viewModel.insertRecipe(recipe.copy(id = 0, times = 0))
                            val stepsWithRecipeId = steps.map { step ->
                                step.copy(
                                    recipeId = recipeId.toInt(),
                                    id = 0,
                                    orderInRecipe = steps.indexOf(step)
                                )
                            }
                            viewModel.insertSteps(stepsWithRecipeId)
                            // 添加延迟确保数据库操作完成
                            delay(100)
                            Toast.makeText(
                                context,
                                context.getString(R.string.import_recipe_success),
                                Toast.LENGTH_SHORT
                            ).show()
                            showScanner = false
                            onImportComplete()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(
                                context,
                                context.getString(R.string.import_recipe_error),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
        }
    }
} 
