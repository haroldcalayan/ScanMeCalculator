package com.haroldcalayan.scanmecalculator.ui.main

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.TextRecognizer
import com.haroldcalayan.scanmecalculator.BuildConfig
import com.haroldcalayan.scanmecalculator.R
import com.haroldcalayan.scanmecalculator.base.BaseActivity
import com.haroldcalayan.scanmecalculator.databinding.ActivityMainBinding
import com.haroldcalayan.scanmecalculator.util.ReadPathUtils
import com.haroldcalayan.scanmecalculator.util.containsLetter
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


@AndroidEntryPoint
class MainActivity : BaseActivity<MainViewModel, ActivityMainBinding>() {

    override val layoutId = R.layout.activity_main
    override val viewModel: MainViewModel by viewModels()

    private lateinit var textRecognizer: TextRecognizer
    private var currentPath: String? = null

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

        binding.buttonCalculate.setOnClickListener {
            calculateEquations()
            binding.buttonCalculate.isEnabled = false
        }

        binding.buttonCalculate.isEnabled = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Timber.d("onActivityResult() requestCode: $requestCode resultCode: $resultCode")
        when (requestCode) {
            REQUEST_CODE_OPEN_CAMERA -> {
                if (resultCode == RESULT_OK) {
                    detectTextFromImage()
                }
            }
            REQUEST_CODE_OPEN_GALLERY -> {
                if (resultCode == RESULT_OK) {
                    if (intent.data != null) {
                        currentPath = convertUriToPath(intent.data ?: Uri.EMPTY)
                    } else {
                        val totalImageCount = intent.clipData?.itemCount?.minus(1)
                        for (index in 0..(totalImageCount ?: 0)) {
                            val uri = intent.clipData?.getItemAt(index)?.uri
                            uri?.let { currentPath = convertUriToPath(it) }
                        }
                    }
                    detectTextFromImage()
                }
            }
            else -> {
            }
        }
    }

    private fun openCamera() {
        val takePicture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if(takePicture.resolveActivity(packageManager) != null) {
            var photoFile: File? = null
            try {
                photoFile = createImage()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            if (photoFile != null) {
                var photoUri = FileProvider.getUriForFile(this, "com.haroldcalayan.scanmecalculator.fileprovider", photoFile)
                takePicture.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                startActivityForResult(takePicture, REQUEST_CODE_OPEN_CAMERA)
            }
        }
    }

    private fun openGallery() {
        //val intent = Intent().apply {
        //    type = "image/*"
        //    action = Intent.ACTION_GET_CONTENT
        //}
        //startActivityForResult(Intent.createChooser(intent, "Select image"), REQUEST_CODE_OPEN_GALLERY)

        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE_OPEN_GALLERY)
    }

    private fun createImage(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd__HHmmss").format(Date())
        val imageName = "JPEG_"+timeStamp+"_"
        var storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        var image = File.createTempFile(imageName, ".jpg", storageDir)
        currentPath = image.absolutePath
        return image
    }

    private fun detectTextFromImage() {
        Timber.d("detectTextFromImage() currentPath: $currentPath")
        if (currentPath == null) {
            Toast.makeText(this, "There is no picture to be detected.", Toast.LENGTH_SHORT).show()
        } else {
            //Enter when there is picture found
            //Change Image to Bitmap Type
            var bitmap = BitmapFactory.decodeFile(currentPath)
            var frame = Frame.Builder().setBitmap(bitmap).build()

            //  Create Text Recognizer
            textRecognizer = TextRecognizer.Builder(this).build()

            if (!textRecognizer.isOperational) {
                Toast.makeText(this, "Could not get the text.", Toast.LENGTH_SHORT).show()
            }
            else {
                // Detector Processor (To make available camera to text)
                val items = textRecognizer.detect(frame)
                val stringBuilder = StringBuilder()
                for (i in 0 until items.size()) {
                    //Find value of text in location i
                    val item = items.valueAt(i)
                    //Add the value to the stringBuilder previously made
                    stringBuilder.append(item.value)
                    stringBuilder.append("\n")
                }
                //Display the text detected
                binding.textviewData.text = "Raw Input: $stringBuilder"

                if (stringBuilder.toString() == "") {
                    Toast.makeText(this, "There is no text in the picture captured.", Toast.LENGTH_SHORT).show()
                } else if (stringBuilder.toString().containsLetter()) {
                    Toast.makeText(this, "Input is invalid.", Toast.LENGTH_SHORT).show()
                } else {
                    binding.buttonCalculate.isEnabled = true
                }
            }
        }
    }

    // Function to call calculator to calculate result of detected text
    private fun calculateEquations() {
        // Change the detected text to string for next process
        val stringInput = binding.textviewData.text.toString().substring("Raw Input: ".length)
        // To see whether there is equation or not in the string
        var containEquation = false
        // Record the location of the equation for next step
        var equationLocation = 0
        // Search for equation in the detected text
        for (i in stringInput.indices) {
            if(stringInput[i]=='+' || stringInput[i]=='-' || stringInput[i]=='*' || stringInput[i]=='/') {
                equationLocation = i
                containEquation=true
                break
            } else {
                containEquation=false
            }
        }
        // To determine things to do whether there is equation or not
        // Enable calculator if there is an equation
        if (containEquation == true) {
            calculator(stringInput, equationLocation)
        } else {
            // Tell user that there is no equation to be calculated
            Toast.makeText(this, "There is no equation detected.", Toast.LENGTH_SHORT).show()
        }
    }

    // Function that acts like somewhat calculator
    private fun calculator(stringInput: String, equationLocation: Int) {
        // Get the length of the string to do logical things
        val stringLength = stringInput.length
        // Do the simple math equation (addition, substitution, multiplication, and division)
        if (stringInput[equationLocation]=='+') {
            // Take the part of the first number and second number from the detected words string
            var numStringA:String = stringInput.subSequence(0,equationLocation).toString()
            var numStringB:String = stringInput.subSequence(equationLocation+1,stringLength-1).toString()
            numStringB = cleanupSecondArguments(numStringB)

            //Change the substring to Integer so it can be calculated
            var numA = numStringA.toInt()
            var numB = numStringB.toInt()
            //Calculate the equation given
            var result = numA + numB
            //Show the result
            binding.textviewData.text = binding.textviewData.text.toString() + "\nAccepted Input: $numStringA + $numStringB\n\nResult: $result"
        } else if (stringInput[equationLocation]=='-') {
            var numStringA:String = stringInput.subSequence(0,equationLocation).toString()
            var numStringB:String = stringInput.subSequence(equationLocation+1,stringLength-1).toString()
            numStringB = cleanupSecondArguments(numStringB)

            var numA = numStringA.toInt()
            var numB = numStringB.toInt()
            var result = numA - numB
            //Show the result
            binding.textviewData.text = binding.textviewData.text.toString() + "\nAccepted Input: $numStringA - $numStringB\n\nResult: $result"
        } else if (stringInput[equationLocation]=='*') {
            var numStringA:String = stringInput.subSequence(0,equationLocation).toString()
            var numStringB:String = stringInput.subSequence(equationLocation+1,stringLength-1).toString()
            numStringB = cleanupSecondArguments(numStringB)

            var numA = numStringA.toInt()
            var numB = numStringB.toInt()
            var result = numA * numB
            //Show the result
            binding.textviewData.text = binding.textviewData.text.toString() + "\nAccepted Input: $numStringA * $numStringB\n\nResult: $result"
        } else if (stringInput[equationLocation]=='/') {
            var numStringA:String = stringInput.subSequence(0,equationLocation).toString()
            var numStringB:String = stringInput.subSequence(equationLocation+1,stringLength-1).toString()
            numStringB = cleanupSecondArguments(numStringB)

            var numA = numStringA.toDouble()
            var numB = numStringB.toDouble()
            var result = numA / numB
            //Show the result
            binding.textviewData.text = binding.textviewData.text.toString() + "\nAccepted Input: $numStringA / $numStringB\n\nResult: $result"
        }
    }

    private fun cleanupSecondArguments(argument: String): String {
        val plusIndex = argument.indexOf('+')
        if (plusIndex != -1) return argument.substring(0, plusIndex)

        val minusIndex = argument.indexOf('+')
        if (minusIndex != -1) return argument.substring(0, minusIndex)

        val timesIndex = argument.indexOf('+')
        if (timesIndex != -1) return argument.substring(0, timesIndex)

        val divideIndex = argument.indexOf('+')
        if (divideIndex != -1) return argument.substring(0, divideIndex)

        return argument
    }

    private fun convertUriToPath(uri: Uri): String? {
        var path: String? = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                path = ReadPathUtils.getPath(this, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return path
    }

    companion object {
        const val REQUEST_CODE_OPEN_CAMERA = 1000
        const val REQUEST_CODE_OPEN_GALLERY = 1001
    }
}