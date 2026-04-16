package dtv.mobile.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@Composable
actual fun QrCodeImage(
  data: String,
  modifier: Modifier,
) {
  val bitmap = remember(data) { generateQrBitmap(data) }
  if (bitmap != null) {
    Image(
      bitmap = bitmap.asImageBitmap(),
      contentDescription = "QR",
      modifier = modifier,
    )
  }
}

private fun generateQrBitmap(data: String, size: Int = 720): Bitmap? {
  if (data.isBlank()) return null
  val writer = QRCodeWriter()
  val matrix = runCatching { writer.encode(data, BarcodeFormat.QR_CODE, size, size) }.getOrNull() ?: return null
  val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
  for (x in 0 until size) {
    for (y in 0 until size) {
      bmp.setPixel(x, y, if (matrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
    }
  }
  return bmp
}

