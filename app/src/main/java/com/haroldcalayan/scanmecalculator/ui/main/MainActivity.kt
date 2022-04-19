package com.haroldcalayan.scanmecalculator.ui.main

import android.os.Bundle
import androidx.activity.viewModels
import com.haroldcalayan.scanmecalculator.R
import com.haroldcalayan.scanmecalculator.base.BaseActivity
import com.haroldcalayan.scanmecalculator.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.app.ActivityCompat.startActivityForResult

import android.provider.MediaStore

import android.content.Intent
import com.haroldcalayan.scanmecalculator.BuildConfig


@AndroidEntryPoint
class MainActivity : BaseActivity<MainViewModel, ActivityMainBinding>() {

    override val layoutId = R.layout.activity_main
    override val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViews()
        observe()
    }
    override fun initViews() {
        super.initViews()

        binding.buttonAddInput.setOnClickListener {
            val allowCamera = BuildConfig.ALLOW_IMAGE_FROM_CAMERA ?: false
            if (allowCamera) openCamera() else openGallery()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_OPEN_CAMERA -> {
                if (resultCode == RESULT_OK) {

                }
            }
            REQUEST_CODE_OPEN_GALLERY -> {
                if (resultCode == RESULT_OK) {

                }
            }
            else -> {
            }
        }
    }

    private fun openCamera() {
        val takePicture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(takePicture, REQUEST_CODE_OPEN_CAMERA)
    }

    private fun openGallery() {
        val pickPhoto = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(pickPhoto, REQUEST_CODE_OPEN_GALLERY)
    }

    companion object {
        const val REQUEST_CODE_OPEN_CAMERA = 1000
        const val REQUEST_CODE_OPEN_GALLERY = 1001
    }
}