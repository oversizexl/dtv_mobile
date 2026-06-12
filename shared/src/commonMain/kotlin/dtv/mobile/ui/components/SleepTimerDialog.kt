package dtv.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * 定时关闭设置对话框。
 *
 * @param isActive 当前是否有定时器在运行
 * @param remainingText 当前剩余时间文字（如 "12:34"）
 * @param onStart 启动定时器，参数为分钟数
 * @param onCancel 取消定时器
 * @param onDismiss 关闭对话框
 */
@Composable
fun SleepTimerDialog(
  isActive: Boolean,
  remainingText: String?,
  onStart: (Int) -> Unit,
  onCancel: () -> Unit,
  onDismiss: () -> Unit,
) {
  val presets = listOf(15, 30, 45, 60, 90, 120)
  var customMinutes by remember { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
      ),
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = "定时关闭",
          style = MaterialTheme.typography.titleMedium,
        )

        // 当前定时状态
        if (isActive && remainingText != null) {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "剩余 $remainingText",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              TextButton(onClick = {
                onCancel()
                onDismiss()
              }) {
                Text("取消定时")
              }
            }
          }
        }

        // 预设时间
        Text(
          text = "选择时间",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          presets.take(3).forEach { minutes ->
            FilterChip(
              selected = false,
              onClick = {
                onStart(minutes)
                onDismiss()
              },
              label = { Text("${minutes}分钟") },
              modifier = Modifier.weight(1f),
            )
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          presets.drop(3).forEach { minutes ->
            FilterChip(
              selected = false,
              onClick = {
                onStart(minutes)
                onDismiss()
              },
              label = { Text("${minutes}分钟") },
              modifier = Modifier.weight(1f),
            )
          }
        }

        // 自定义时间
        Text(
          text = "自定义时间",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = customMinutes,
            onValueChange = { customMinutes = it.filter { c -> c.isDigit() } },
            modifier = Modifier.weight(1f),
            placeholder = { Text("输入分钟数") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
          )
          Button(
            onClick = {
              val minutes = customMinutes.toIntOrNull()
              if (minutes != null && minutes > 0) {
                onStart(minutes)
                onDismiss()
              }
            },
            enabled = customMinutes.toIntOrNull()?.let { it > 0 } == true,
          ) {
            Text("开始")
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 关闭按钮
        OutlinedButton(
          onClick = onDismiss,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("关闭")
        }
      }
    }
  }
}
