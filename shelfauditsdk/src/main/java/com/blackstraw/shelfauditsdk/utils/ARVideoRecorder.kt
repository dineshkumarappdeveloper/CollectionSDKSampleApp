package com.blackstraw.shelfauditsdk.utils

import android.media.MediaRecorder
import android.util.Log
import java.io.File

class ARVideoRecorder(private val outputFile: File) {
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false

    fun start() {
        if (isRecording) return

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setOutputFile(outputFile.absolutePath)
            setVideoFrameRate(30)
            setVideoSize(1920, 1080)
            setVideoEncodingBitRate(10000000)
            prepare()
            start()
        }
        isRecording = true
        Log.d("ARVideoRecorder", "Recording started")
    }

    fun pause() {
        if (isRecording) {
            mediaRecorder?.pause()
            Log.d("ARVideoRecorder", "Recording paused")
        }
    }

    fun resume() {
        if (isRecording) {
            mediaRecorder?.resume()
            Log.d("ARVideoRecorder", "Recording resumed")
        }
    }

    fun stop() {
        if (isRecording) {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            mediaRecorder = null
            isRecording = false
            Log.d("ARVideoRecorder", "Recording stopped and saved to ${outputFile.absolutePath}")
        }
    }

    fun restart() {
        stop()
        start()
        Log.d("ARVideoRecorder", "Recording restarted")
    }
}