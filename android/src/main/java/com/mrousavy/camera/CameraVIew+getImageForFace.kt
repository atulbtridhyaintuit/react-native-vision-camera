package com.mrousavy.camera

import android.util.Log
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.mrousavy.camera.faceDetect.FaceDetectorPluginTest
import com.mrousavy.camera.utils.withPromise
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun CameraView.getImageForFace(options: ReadableMap,promise:Promise)  = coroutineScope {
    val camera = camera ?: throw com.mrousavy.camera.CameraNotReadyError()
    Log.d("Second Step" ,"Get image Trying to Fetch")
    fun faceView(options: ReadableMap): Unit = FaceDetectorPluginTest.register(options);
    faceView(options)
    @Suppress("LiftReturnOrAssignment", "RedundantIf")
    coroutineScope.launch {
        withPromise(promise) {
            val view = faceView(options)
            view;
        }
    }

    return@coroutineScope null
}
