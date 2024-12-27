package com.blackstraw.shelfauditsdk

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import com.blackstraw.shelfauditsdk.ml.MLModelHandler
import com.blackstraw.shelfauditsdk.ui.screens.ARSceneView
import com.blackstraw.shelfauditsdk.ui.theme.CollectionSDKSampleAppTheme
import com.blackstraw.shelfauditsdk.utils.drawBoundingBoxesOnBitmap
import com.blackstraw.shelfauditsdk.utils.getBitmapFromAssets

class CollectionSDKLauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CollectionSDKSampleAppTheme {
                MLModelHandler.init(this).also {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Column {
                            var bitmap by remember { mutableStateOf<Bitmap?>(null) }
                            Button(
                                onClick = {
                                    val output = MLModelHandler.getDetectionOutputForBitmap(getBitmapFromAssets(this@CollectionSDKLauncherActivity, "images/sample_frame_1.jpg"))
                                    Log.d("CollectionSDKLauncherActivity", "Output: ${output.size}")
                                    val modifiedBitmap = drawBoundingBoxesOnBitmap(getBitmapFromAssets(this@CollectionSDKLauncherActivity, "images/sample_frame_1.jpg"), output)
                                    Log.d("CollectionSDKLauncherActivity", "Modified Bitmap: $modifiedBitmap")
                                    bitmap = modifiedBitmap
                                },
                                modifier = Modifier.padding(innerPadding)
                            ) {
                                Text("Click me")

                            }
                            ARSceneView()
//                        // Show the bitmap in ImageView
//                        bitmap?.let {
//                            Image(
//                                bitmap = it.asImageBitmap(),
//                                contentDescription = null,
//                                modifier = Modifier.fillMaxSize()
//                            )
//                        }

                        }
                    }
                }




            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CollectionSDKSampleAppTheme {
        Greeting("Android")
    }
}