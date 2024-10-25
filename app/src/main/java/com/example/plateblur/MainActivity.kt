package com.example.plateblur


import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri

import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.FileProvider
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale


import android.Manifest
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var processButton: Button
    private lateinit var selectButton: Button
    private lateinit var captureButton: Button
    private lateinit var progressBar: ProgressBar
    private var currentImageUri: Uri? = null
    private var lotNumber = 1
    private var imageNumber = 1

    private lateinit var imageUri: Uri  // Temporary URI for camera capture


    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Updated regex to capture more South African license plate formats
    private val licensePlatePattern = Regex(
        "(\\b[A-Z]{2}\\s?\\d{2,3}\\s?[A-Z]{2,3}\\b)|" +  // Patterns like "CA 123 GP"
                "(\\b[A-Z]{2}\\s*\\d{2,3}\\s*[A-Z]{2,3}\\b)|" +  // "CA 123 GP", allowing optional spaces
                "(\\b[A-Z]{3}\\s?\\d{3}\\s?[A-Z]{2}\\b)|" +      // Patterns like "XYZ 123 GP"
                "(\\b[A-Z]{2}-\\d{3}-\\d{3}\\b)|" +              // Patterns like "ND-123-456"
                "(\\b[A-Z]{1}\\s?\\d{3}\\s?[A-Z]{3}\\s?[A-Z]{1}\\b)|" + // Patterns like "B 123 ABC L" (Limpopo)
                "(\\b[A-Z]{1}\\s?\\d{1,5}\\b)|" +                // Older patterns like "T 12345" (obsolete)
                "(\\b[A-Z]{2}\\s?\\d{3}\\s?\\d{3}\\b)|" +        // Patterns like "CJ 123 456" (Western Cape)
                "(\\b[A-Z0-9]{1,7}\\s?[A-Z]{2,3}\\b)"            // Personalized/custom plates
    )

/*
    // Register intent to capture a photo using the device camera
    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentImageUri?.let {
                    imageView.setImageURI(it)
                    processButton.isEnabled = true
                    showMessage("Picture taken successfully")
                }
            } else {
                showMessage("Picture capture failed")
            }
        }

 */


    // Register intent to capture a photo using the device camera

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            currentImageUri?.let {
                val croppedBitmap = cropToAspectRatio(it, 4, 3) // Crop to 4:3 aspect ratio
                imageView.setImageBitmap(croppedBitmap)
                processButton.isEnabled = true
                showMessage("Picture captured and cropped to 4:3 aspect ratio")
            }
        } else {
            showMessage("Picture capture failed")
        }
    }


    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            currentImageUri = it
            val croppedBitmap = cropToAspectRatio(it, 4, 3)
            imageView.setImageBitmap(croppedBitmap)
            processButton.isEnabled = true
            showMessage("Image selected and cropped to 4:3 aspect ratio")
        }
    }

    private fun cropToAspectRatio(uri: Uri, aspectX: Int, aspectY: Int): Bitmap {
        val bitmap = decodeSampledBitmapFromUri(uri, 1024, 1024)
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        // Calculate the new dimensions based on the desired aspect ratio
        val targetWidth: Int
        val targetHeight: Int

        if (originalWidth.toFloat() / originalHeight > aspectX.toFloat() / aspectY) {
            targetHeight = originalHeight
            targetWidth = (targetHeight * aspectX / aspectY)
        } else {
            targetWidth = originalWidth
            targetHeight = (targetWidth * aspectY / aspectX)
        }

        // Crop the image to the calculated dimensions
        val xOffset = (originalWidth - targetWidth) / 2
        val yOffset = (originalHeight - targetHeight) / 2

        return Bitmap.createBitmap(bitmap, xOffset, yOffset, targetWidth, targetHeight)
    }

    private fun decodeSampledBitmapFromUri(uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, options)
        }

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false

        return contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, options)!!
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }


    /*
    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                currentImageUri = it
                imageView.setImageURI(it)
                processButton.isEnabled = true
                showMessage("Image selected successfully")
            }
        }


 */


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        imageView = findViewById(R.id.imageView)
        processButton = findViewById(R.id.processButton)
        selectButton = findViewById(R.id.selectButton)
        captureButton = findViewById(R.id.captureButton)
        progressBar = findViewById(R.id.progressBar)

        // Initially disable process button until an image is selected
        processButton.isEnabled = false
    }

    private fun setupListeners() {
        selectButton.setOnClickListener {
            getContent.launch("image/*")
        }

        captureButton.setOnClickListener {
            //captureNewImage()
            checkCameraPermission()
        }

        processButton.setOnClickListener {
            currentImageUri?.let { uri ->
                showLoading(true)
                processImage(uri)
            } ?: showMessage("No image available to process")
        }
    }

   /* private fun captureNewImage() {
        try{
        val authority = "${packageName}.fileprovider"  // Activity has direct access to packageName

        // Create a file to save the image
        val imageFile = createImageFile()


            imageUri = FileProvider.getUriForFile(
                this,  // 'this' refers to the Activity
                authority,
                imageFile
            )

            currentImageUri = imageUri
            takePicture.launch(imageUri)


    }catch (e: Exception) {
            e.printStackTrace()
            showMessage("Failed to setup camera: ${e.message}")
    } }

    */


    private fun captureNewImage() {
        try {
            val authority = "${applicationContext.packageName}.provider"  // Changed to match manifest
            val imageFile = createImageFile()

            imageUri = FileProvider.getUriForFile(
                applicationContext,  // Using applicationContext for safety
                authority,
                imageFile
            )

            currentImageUri = imageUri
            takePicture.launch(imageUri)

        } catch (e: Exception) {
            e.printStackTrace()
            showMessage("Failed to setup camera: ${e.localizedMessage}")
        }
    }



    private fun checkCameraPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            captureNewImage()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    captureNewImage()
                } else {
                    showMessage("Camera permission is required")
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }



    /* companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    captureNewImage()
                } else {
                    showMessage("Camera permission is required to take pictures")
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

     */

    // Make sure you have this function to create the image file
    /*  private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"

        // Get the app's private directory for pictures
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return File.createTempFile(
            imageFileName,  // prefix
            ".jpg",        // suffix
            storageDir     // directory
        )
    } */


    private fun createImageFile(): File {
      try{  // Save file with format "lotNumber-imageNumber.jpg"
        val imagesDir = File(getExternalFilesDir(null), "ProcessedImages")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }


        // Create file with lot and image number
        val imageFile = File(imagesDir, "$lotNumber-$imageNumber.jpg").apply {
            if (exists()) {
                delete() // Delete existing file if it exists
            }
            createNewFile()
        }

        imageNumber++ // Increment the image number for the next image
        return imageFile

    } catch (e: Exception)
    {
        throw IllegalStateException("Cannot create image file: ${e.message}")
    }
}

    private fun processImage(uri: Uri) {
        try {
            val image = InputImage.fromFilePath(this, uri)

            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    try {
                        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                        val canvas = Canvas(mutableBitmap)

                        var platesFound = 0
                        val allText = visionText.text.replace(" ", "")

                        if (licensePlatePattern.containsMatchIn(allText)) {
                            for (block in visionText.textBlocks) {
                                for (line in block.lines) {
                                    val lineText = line.text.replace(" ", "")

                                    if (licensePlatePattern.matches(lineText)) {
                                        platesFound++

                                        val blur = BlurMaskFilter(60f, BlurMaskFilter.Blur.NORMAL)
                                        val paint = Paint().apply {
                                            color = Color.BLACK
                                            maskFilter = blur
                                            style = Paint.Style.FILL
                                        }

                                        line.boundingBox?.let { box ->
                                            val padding = 40
                                            val rect = RectF(
                                                (box.left - padding).toFloat(),
                                                (box.top - padding).toFloat(),
                                                (box.right + padding).toFloat(),
                                                (box.bottom + padding).toFloat()
                                            )
                                            canvas.drawRect(rect, paint)
                                        }
                                    }
                                }
                            }
                        }

                        val savedFile = saveProcessedImage(mutableBitmap)
                        imageView.setImageBitmap(mutableBitmap)
                        showLoading(false)
                        showMessage("Found and blurred $platesFound license plates. Saved to: ${savedFile?.name}")

                    } catch (e: Exception) {
                        handleError("Error processing image: ${e.message}")
                    }
                }
                .addOnFailureListener { e ->
                    handleError("Text recognition failed: ${e.message}")
                }
        } catch (e: Exception) {
            handleError("Error loading image: ${e.message}")
        }
    }

    private fun saveProcessedImage(bitmap: Bitmap): File? {
        return try {
            val imageFile = createImageFile() // Use the naming convention
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            imageFile

        } catch (e: Exception) {
            handleError("Error saving image: ${e.message}")
            null
        }
    }
/*
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        processButton.isEnabled = !show
        selectButton.isEnabled = !show
        captureButton.isEnabled = !show
    }

 */

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        processButton.isEnabled = !isLoading
        selectButton.isEnabled = !isLoading
        captureButton.isEnabled = !isLoading
    }

    private fun showMessage(message: String) {
        Snackbar.make(imageView, message, Snackbar.LENGTH_LONG).show()
    }

    private fun handleError(error: String) {
        showLoading(false)
        showMessage(error)
    }
}
