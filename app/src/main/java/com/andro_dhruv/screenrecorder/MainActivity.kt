package com.andro_dhruv.screenrecorder

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.SparseIntArray
import android.view.Menu
import android.view.MenuItem
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.File
import java.io.IOException


class MainActivity : AppCompatActivity(), VideoFileAdapter.OnItemClickListener {
    private val myAdapter = VideoFileAdapter(this)
    private var mScreenDensity: Int = 0
    private val mMediaRecorder by lazy {
        MediaRecorder()
    }
    private val mProjectionManager: MediaProjectionManager by lazy {
        getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mMediaProjection: MediaProjection? = null
    private var mMediaProjectionCallback: MediaProjectionCallback? = null
    private var isRecording = false
    private val DISPLAY_WIDTH = 720
    private val DISPLAY_HEIGHT = 1280
    private val mFrameRate = 16
    private val folder = "${Environment.getExternalStorageDirectory()}/ScreenRecorder"

    private val ORIENTATIONS = SparseIntArray().apply {
        append(Surface.ROTATION_0, 90)
        append(Surface.ROTATION_90, 0)
        append(Surface.ROTATION_180, 270)
        append(Surface.ROTATION_270, 180)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        initializeStuff()
        getVideoFiles()

        recycler_view.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = myAdapter
        }

        fab.setOnClickListener {
            checkPermissions()
        }
    }

    fun onToggleScreenShare() {
        if (!isRecording) {
            initRecorder()
            shareScreen()

        } else {
            mMediaRecorder.stop()
            mMediaRecorder.reset()
            stopScreenSharing()
            getVideoFiles()
        }
    }

    private fun initRecorder() {
        val file = File(folder)
        if (!file.exists()) file.mkdir()
        try {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mMediaRecorder.setOutputFile("$folder/${System.currentTimeMillis()}.mp4")
            mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT)
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mMediaRecorder.setVideoFrameRate(mFrameRate)
            val rotation = windowManager.defaultDisplay.rotation
            val orientation = ORIENTATIONS.get(rotation + 90)
            mMediaRecorder.setOrientationHint(orientation)
            mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
            mMediaRecorder.setVideoEncodingBitRate(3000000);
            mMediaRecorder.prepare()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun shareScreen() {
        if (mMediaProjection == null) {
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), SCREEN_CAPTURE_REQUEST_CODE)
            return
        }
        mVirtualDisplay = createVirtualDisplay()
        mMediaRecorder.start();
        isRecording = true;
        fabIconChange()
    }

    private fun stopScreenSharing() {
        if (mVirtualDisplay == null) {
            return
        }
        mVirtualDisplay?.release();
        destroyMediaProjection();
        isRecording = false;
        fabIconChange()
    }

    private fun destroyMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection?.unregisterCallback(mMediaProjectionCallback)
            mMediaProjection?.stop()
            mMediaProjection = null
        }
    }

    private fun createVirtualDisplay(): VirtualDisplay? =
        mMediaProjection?.createVirtualDisplay(
            "Virtual Display",
            DISPLAY_WIDTH,
            DISPLAY_HEIGHT,
            mScreenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mMediaRecorder.surface,
            null,
            null
        )

    private fun initializeStuff() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        mScreenDensity = displayMetrics.densityDpi
    }

    override fun onItemClickListener(videoFile: File) {
        val intent = Intent(this, VideoPlayerActivity::class.java)
        intent.putExtra(VIDEO_URI, videoFile.absolutePath)
        startActivity(intent)
    }

    private fun getVideoFiles(){
        val folder = File(folder)
        val files = folder.listFiles()
        if (!files.isNullOrEmpty())
            myAdapter.updateData(files.toList())
    }

    private fun fabIconChange() {
        fab.setImageResource(if (isRecording) R.drawable.ic_stop else R.drawable.ic_videocam)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            SCREEN_CAPTURE_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    mMediaProjectionCallback = MediaProjectionCallback()
                    mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data)
                    mMediaProjection?.registerCallback(mMediaProjectionCallback, null)
                    mVirtualDisplay = createVirtualDisplay()
                    mMediaRecorder.start()
                    isRecording = true
                    fabIconChange()

                } else {
                    Toast.makeText(this, "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
                    isRecording = false;
                    fabIconChange()
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun checkPermissions() {
        val perm = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )

        if (!checkAllPermissions(this, perm)) {
            ActivityCompat.requestPermissions(this, perm, PERMISSION_REQUEST_CODE)
        } else {
            onToggleScreenShare()
        }
    }

    private fun checkAllPermissions(context: Context, permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onToggleScreenShare()
        } else {
            isRecording = false
            fabIconChange()
            Snackbar.make(
                findViewById(android.R.id.content), "Please enable Microphone and Storage permissions.",
                Snackbar.LENGTH_INDEFINITE
            ).setAction("ENABLE") {
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                intent.data = Uri.parse("package:" + getPackageName())
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                startActivity(intent)
            }.show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                Snackbar.make(item.actionView, "Setting", Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private inner class MediaProjectionCallback : MediaProjection.Callback() {
        override fun onStop() {
            if (isRecording) {
                isRecording = false
                fabIconChange()
                mMediaRecorder.stop()
                mMediaRecorder.reset()
            }
            mMediaProjection = null
            stopScreenSharing()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyMediaProjection()
    }

    override fun onBackPressed() {
        if (isRecording) {
            Snackbar.make(
                findViewById<View>(android.R.id.content), "Wanna Stop recording and exit?",
                Snackbar.LENGTH_SHORT
            ).setAction("Stop") {
                mMediaRecorder.stop()
                mMediaRecorder.reset()
                Log.v(TAG, "Stopping Recording")
                stopScreenSharing()
                finish()
            }.show()
        } else {
            finish()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val VIDEO_URI = "video_uri"
        private const val PERMISSION_REQUEST_CODE = 2
        private const val SCREEN_CAPTURE_REQUEST_CODE = 1000
    }
}
