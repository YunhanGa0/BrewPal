import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import android.provider.MediaStore

@Composable
fun ShareRecipeDialog(
    recipe: Recipe,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val shareUtils = remember { RecipeShareUtils() }
    var showQRCode by remember { mutableStateOf(false) }
    
    val stepsViewModel: StepsViewModel = viewModel()
    val steps by stepsViewModel.getAllStepsForRecipe(recipe.id).observeAsState(listOf())
    
    val shareString = remember(recipe, steps) { 
        shareUtils.encodeRecipe(recipe, steps) 
    }
    
    if (showQRCode) {
        QRCodeDialog(
            content = shareString,
            onDismiss = { showQRCode = false }
        )
    } else {
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
