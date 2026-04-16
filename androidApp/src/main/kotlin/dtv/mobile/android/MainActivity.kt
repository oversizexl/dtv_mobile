package dtv.mobile.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import dtv.mobile.App
import dtv.mobile.repo.android.AndroidDtvRepository
import dtv.mobile.util.AppLog

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    AppLog.init(applicationContext)
    setContent {
      val repo = remember { AndroidDtvRepository(applicationContext) }
      App(repo = repo)
    }
  }
}
