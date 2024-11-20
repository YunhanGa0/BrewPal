package com.omelan.cofi.components

import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.omelan.cofi.R

@Composable
fun CoffeeShopRecommendDialog(
    onDismiss: () -> Unit,
    onNavigateToMap: () -> Unit
) {
    Log.d("CoffeeShopDialog", "对话框组件开始渲染")
    AlertDialog(
        onDismissRequest = {
            Log.d("CoffeeShopDialog", "触发对话框关闭")
            onDismiss()
        },
        icon = {
            Log.d("CoffeeShopDialog", "渲染图标")
            Icon(
                painter = painterResource(id = R.drawable.ic_coffee),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Log.d("CoffeeShopDialog", "渲染标题")
            Text("探索咖啡店")
        },
        text = {
            Log.d("CoffeeShopDialog", "渲染内容")
            Text(
                "喝惯了自己冲的咖啡了？这里有一些只为你推荐的咖啡店，或许能找到你的下一个心头好。",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Log.d("CoffeeShopDialog", "渲染确认按钮")
            TextButton(
                onClick = {
                    Log.d("CoffeeShopDialog", "点击确认按钮")
                    onNavigateToMap()
                    onDismiss()
                }
            ) {
                Text("去看看")
            }
        },
        dismissButton = {
            Log.d("CoffeeShopDialog", "渲染取消按钮")
            TextButton(onClick = onDismiss) {
                Text("稍后再说")
            }
        }
    )
}
