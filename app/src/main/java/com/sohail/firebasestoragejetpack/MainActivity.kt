package com.sohail.firebasestoragejetpack

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import coil.compose.rememberImagePainter
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.sohail.firebasestoragejetpack.ui.theme.FirebaseStorageJetpackTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class MainActivity : ComponentActivity() {

    private var imageUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FirebaseStorageJetpackTheme {
                Scaffold(
                    content = { MainScreen(this) }
                )
            }
        }
    }

    private val selectImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            imageUri.value = uri
        }

    @Composable
    fun MainScreen(context: ComponentActivity) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(3f)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        modifier = Modifier.align(Alignment.BottomStart),
                        onClick = {
                            selectImage.launch("image/*")
                        }) {
                        Icon(
                            Icons.Filled.Add,
                            "add",
                            tint = Color.Blue
                        )
                    }

                    if (imageUri.value != null) {
                        Image(
                            modifier = Modifier.fillMaxSize(),
                            painter = rememberImagePainter(
                                data = imageUri.value
                            ),
                            contentDescription = "image"
                        )
                        IconButton(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            onClick = {
                                GlobalScope.launch(Dispatchers.IO) {
                                    val compressedImage = compressImage(
                                        context,
                                        imageUri.value!!
                                    )
                                    uploadPhoto(
                                        compressedImage!!,
                                        "image.jpg",
                                        "image/jpg"
                                    ) {
                                        GlobalScope.launch(Dispatchers.Main) {
                                            Toast.makeText(context,"File uploaded!",Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }

                            }) {
                            Icon(
                                Icons.Default.PlayArrow,
                                "scan",
                                tint = Color.Blue
                            )
                        }
                    }

                }
            }
        }
    }

    private fun compressImage(context: ComponentActivity,uri: Uri): Uri? {
        val bitmap = if(Build.VERSION.SDK_INT < 28) {
             MediaStore.Images.Media.getBitmap(
                this.contentResolver,
                uri
            )
        } else {
            val source = ImageDecoder.createSource(this.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        }
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, bytes)
        val path: String = MediaStore.Images.Media.insertImage(
            context.contentResolver,
            bitmap,
            "Title",
            null
        )
        return Uri.parse(path)
    }

    private suspend fun uploadPhoto(uri: Uri, name: String, mimeType: String?,callback: (url: String) -> Unit) {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference
        val fileRef = storageRef.child("images/$name")

        val metadata = mimeType?.let {
            StorageMetadata.Builder()
                .setContentType(mimeType)
                .build()
        }
        if (metadata != null) {
            fileRef.putFile(uri, metadata).await()
        } else {
            fileRef.putFile(uri).await()
        }

        callback(fileRef.downloadUrl.await().toString())
    }
}