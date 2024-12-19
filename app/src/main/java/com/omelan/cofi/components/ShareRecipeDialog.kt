import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.omelan.cofi.R
import com.omelan.cofi.share.model.Recipe
import com.omelan.cofi.share.model.StepsViewModel
import android.media.MediaScannerConnection
import android.os.Environment
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import com.omelan.cofi.share.model.Step
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalView
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import android.view.ViewTreeObserver

@Composable
fun ShareRecipeDialog(
    recipe: Recipe,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val shareUtils = remember { RecipeShareUtils() }
    var showQRCode by remember { mutableStateOf(false) }
    var showShareCard by remember { mutableStateOf(false) }
    
    val stepsViewModel: StepsViewModel = viewModel()
    val steps by stepsViewModel.getAllStepsForRecipe(recipe.id).observeAsState(listOf())
    
    val shareString = remember(recipe, steps) { 
        shareUtils.encodeRecipe(recipe, steps) 
    }
    
    when {
        showQRCode -> {
            QRCodeDialog(
                content = shareString,
                onDismiss = { showQRCode = false }
            )
        }
        showShareCard -> {
            ShareCardDialog(
                recipe = recipe,
                steps = steps,
                qrContent = shareString,
                onDismiss = { showShareCard = false }
            )
        }
        else -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.share_recipe)) },
                text = {
                    Column {
                        TextButton(onClick = { 
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareString)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, null))
                            onDismiss()
                        }) {
                            Text(stringResource(R.string.share_recipe_text))
                        }
                        TextButton(onClick = { showQRCode = true }) {
                            Text(stringResource(R.string.share_recipe_qr))
                        }
                        TextButton(onClick = { showShareCard = true }) {
                            Text("Share as Beautiful Card")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun QRCodeDialog(
    content: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val qrCodeBitmap = remember {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.share_recipe_qr)) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = qrCodeBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(256.dp)
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = {
                    // 保存二维码图片到相册
                    val fileName = "cofi_qr_${System.currentTimeMillis()}.png"
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }
                    
                    try {
                        context.contentResolver.insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues
                        )?.let { uri ->
                            context.contentResolver.openOutputStream(uri)?.use { out ->
                                qrCodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                            Toast.makeText(context, R.string.qr_code_saved, Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, R.string.qr_code_save_failed, Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(stringResource(R.string.save))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        }
    )
}

@Composable
fun ShareCardDialog(
    recipe: Recipe,
    steps: List<Step>,
    qrContent: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val qrCodeBitmap = remember { generateQRCode(qrContent, 512) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share Recipe Card") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // 配方图标和名称
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = recipe.recipeIcon.icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = recipe.name,
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }
                            
                            if (recipe.description.isNotEmpty()) {
                                Text(
                                    text = recipe.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            
                            Divider(modifier = Modifier.padding(vertical = 16.dp))
                            
                            // 步骤列表
                            steps.forEachIndexed { index, step ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            modifier = Modifier.padding(4.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = step.name,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        step.time?.let { time ->
                                            Text(
                                                text = "${time}ms",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // 二维码居中显示
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = qrCodeBitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(200.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        // TODO: 实现保存功能
                        Toast.makeText(context, "Save to Gallery clicked", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save to Gallery")
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        }
    )
}

fun generateQRCode(content: String, size: Int): Bitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
    val width = bitMatrix.width
    val height = bitMatrix.height
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    
    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bitmap
}

fun saveCardToGallery(context: Context, recipeName: String) {
    val fileName = "cofi_recipe_card_${System.currentTimeMillis()}.png"
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
    }
    
    try {
        context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )?.let { uri ->
            context.contentResolver.openOutputStream(uri)?.use { out ->
                // 这里需要实现保存卡片图片到相册的逻辑
            }
            Toast.makeText(context, R.string.qr_code_saved, Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, R.string.qr_code_save_failed, Toast.LENGTH_SHORT).show()
    }
}
