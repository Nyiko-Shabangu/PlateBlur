/**
 * MainActivity for License Plate Detection and Blurring Application
 *
 * This application allows users to:
 * 1. Select images from gallery or capture new photos
 * 2. Detect South African license plates using ML Kit Text Recognition
 * 3. Automatically blur detected license plates
 * 4. Save processed images with systematic naming convention
 */
package com.example.plateblur

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    // UI Components
    private lateinit var imageView: ImageView
    private lateinit var processButton: Button
    private lateinit var selectButton: Button
    private lateinit var captureButton: Button
    private lateinit var progressBar: ProgressBar

    // Image handling variables
    private var currentImageUri: Uri? = null
    private var imageUri: Uri? = null
    private var lotNumber = 1
    private var imageNumber = 1

    // ML Kit text recognizer instance
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Regular expression pattern for South African license plates
     * Supports various formats including:
     * - Standard provincial (CA 123 GP)
     * - Custom/Personalized plates
     * - Legacy formats
     * - Special formats for different provinces
     */

    /**
     * Enhanced regex patterns for different license plate formats
     * Split into separate patterns for better matching accuracy
     */
    private val licensePlatePatterns = listOf(
        // Standard provincial format (e.g., CA 123 GP)
        Regex("\\b[A-Z]{2}\\s*\\d{1,3}\\s*[A-Z]{2,3}\\b"),

        // Newer format with additional characters (e.g., DN 88 RB GP)
        Regex("\\b[A-Z]{2}\\s*\\d{2}\\s*[A-Z]{2}\\s*[A-Z]{2}\\b"),

        // Custom/Personalized format
        Regex("\\b[A-Z0-9]{1,7}\\s*[A-Z]{2,3}\\b"),

        // Western Cape format (e.g., CJ 123 456)
        Regex("\\b[A-Z]{2}\\s*\\d{3}\\s*\\d{3}\\b"),

        // Limpopo format (e.g., B 123 ABC L)
        Regex("\\b[A-Z]\\s*\\d{3}\\s*[A-Z]{3}\\s*[A-Z]\\b"),

        // Legacy format (e.g., T 12345)
        Regex("\\b[A-Z]\\s*\\d{1,5}\\b"),

        // Additional format with dashes (e.g., ND-123-456)
        Regex("\\b[A-Z]{2}-\\d{3}-\\d{3}\\b"),

        // GP format (e.g., CF 90 MW GP)
        Regex("\\b[A-Z]{2}\\s*\\d{2}\\s*[A-Z]{2}\\s*[A-Z]{2}\\b"),

        // Custom format (e.g., xyp927)
        Regex("\\b[a-zA-Z]{3}\\d{3}\\b")


    )


    // Activity result handler for camera capture
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
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

    // Activity result handler for gallery selection
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            currentImageUri = it
            imageView.setImageURI(it)
            processButton.isEnabled = true
            showMessage("Image selected successfully")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupViews()
        setupListeners()
    }

    /**
     * Initialize and set up UI components
     */
    private fun setupViews() {
        imageView = findViewById(R.id.imageView)
        processButton = findViewById(R.id.processButton)
        selectButton = findViewById(R.id.selectButton)
        captureButton = findViewById(R.id.captureButton)
        progressBar = findViewById(R.id.progressBar)
        processButton.isEnabled = false
    }

    /**
     * Set up click listeners for buttons
     */
    private fun setupListeners() {
        selectButton.setOnClickListener {
            getContent.launch("image/*")
        }

        captureButton.setOnClickListener {
            checkCameraPermission()
        }

        processButton.setOnClickListener {
            currentImageUri?.let { uri ->
                showLoading(true)
                processImage(uri)
            } ?: showMessage("No image available to process")
        }
    }

    /**
     * Check and request camera permissions if needed
     */
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

    /**
     * Handle camera permission result
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    captureNewImage()
                } else {
                    showMessage("Camera permission is required")
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }


    /**
     * Create and start camera intent with proper null safety handling
     */
    private fun captureNewImage() {
        try {
            val authority = "${applicationContext.packageName}.provider"
            val imageFile = createImageFile()

            FileProvider.getUriForFile(
                applicationContext,
                authority,
                imageFile
            ).also { uri ->
                currentImageUri = uri
                takePicture.launch(uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showMessage("Failed to setup camera: ${e.localizedMessage}")
        }
    }



    /**
     * Create a new file for storing the image with proper naming convention
     */
    private fun createImageFile(): File {
        try {
            val imagesDir = File(getExternalFilesDir(null), "ProcessedImages").apply {
                if (!exists()) mkdirs()
            }

            return File(imagesDir, "$lotNumber-$imageNumber.jpg").apply {
                if (exists()) delete()
                createNewFile()
            }.also {
                imageNumber++
            }
        } catch (e: Exception) {
            throw IllegalStateException("Cannot create image file: ${e.message}")
        }
    }




    /**
     * Process the image with enhanced license plate detection
     */
    private fun processImage(uri: Uri) {
        try {
            val image = InputImage.fromFilePath(this, uri)

            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    try {
                        // Get original bitmap and orientation
                        val (originalBitmap, orientation) = getOriginalBitmapWithOrientation(uri)
                        val mutableBitmap = if (orientation != ExifInterface.ORIENTATION_NORMAL) {
                            getRotatedBitmap(originalBitmap, orientation)
                        } else {
                            originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
                        }

                        val canvas = Canvas(mutableBitmap)
                        val detectedPlates = mutableSetOf<DetectedPlate>()

                        // Process text blocks with multiple strategies
                        processTextBlocks(visionText, detectedPlates)

                        // Apply blur to detected plates
                        for (plate in detectedPlates) {
                            blurLicensePlate(canvas, plate.boundingBox)
                        }

                        // Save and display results
                        val savedFile = saveProcessedImage(mutableBitmap, orientation)
                        imageView.setImageBitmap(mutableBitmap)
                        showLoading(false)
                        showMessage("Found and blurred ${detectedPlates.size} license plates. Saved to: ${savedFile?.name}")

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

    /**
     * Data class to store detected plate information
     */
    private data class DetectedPlate(
        val text: String,
        val boundingBox: Rect?,
        val confidence: Float
    )

    /**
     * Process text blocks with multiple strategies for better detection
     */
    private fun processTextBlocks(
        visionText: com.google.mlkit.vision.text.Text,
        detectedPlates: MutableSet<DetectedPlate>
    ) {
        val textBlocks = visionText.textBlocks

        // First pass: Check individual lines
        for (block in textBlocks) {
            for (line in block.lines) {
                processTextLine(line, detectedPlates)
            }
        }

        // Second pass: Check merged lines within blocks
        for (block in textBlocks) {
            val mergedText = block.lines.joinToString(" ") { it.text }
            processText(mergedText, block.boundingBox, block.lines[0].confidence, detectedPlates)
        }

        // Third pass: Check text with different cleaning strategies
        for (block in textBlocks) {
            for (line in block.lines) {
                processTextWithVariations(line, detectedPlates)
            }
        }
    }

    /**
     * Process individual text line
     */
    private fun processTextLine(
        line: com.google.mlkit.vision.text.Text.Line,
        detectedPlates: MutableSet<DetectedPlate>
    ) {
        // Process original text
        processText(line.text, line.boundingBox, line.confidence, detectedPlates)

        // Process without spaces
        processText(
            line.text.replace(" ", ""),
            line.boundingBox,
            line.confidence,
            detectedPlates
        )
    }

    /**
     * Process text with different variations to improve detection
     */
    private fun processTextWithVariations(
        line: com.google.mlkit.vision.text.Text.Line,
        detectedPlates: MutableSet<DetectedPlate>
    ) {
        val variations = listOf(
            line.text,                              // Original text
            line.text.replace(" ", ""),             // No spaces
            line.text.replace("0", "O"),            // Common OCR mistake: 0 to O
            line.text.replace("O", "0"),            // Common OCR mistake: O to 0
            line.text.replace("I", "1"),            // Common OCR mistake: I to 1
            line.text.replace("1", "I"),            // Common OCR mistake: 1 to I
            line.text.replace("S", "5"),            // Common OCR mistake: S to 5
            line.text.replace("5", "S"),            // Common OCR mistake: 5 to S
            line.text.replace("B", "8"),            // Common OCR mistake: B to 8
            line.text.replace("8", "B"),            // Common OCR mistake: 8 to B
            line.text.uppercase()                   // Force uppercase
        )

        for (variant in variations) {
            processText(variant, line.boundingBox, line.confidence, detectedPlates)
        }
    }

    /**
     * Process text with multiple patterns
     */
    private fun processText(
        text: String,
        boundingBox: Rect?,
        confidence: Float,
        detectedPlates: MutableSet<DetectedPlate>
    ) {
        for (pattern in licensePlatePatterns) {
            if (pattern.containsMatchIn(text)) {
                pattern.find(text)?.value?.let { plateText ->
                    detectedPlates.add(
                        DetectedPlate(
                            text = plateText,
                            boundingBox = boundingBox,
                            confidence = confidence
                        )
                    )
                }
            }
        }
    }

    /**
     * Get the original bitmap and its orientation from the URI
     * @return Pair of Bitmap and orientation value
     */
    private fun getOriginalBitmapWithOrientation(uri: Uri): Pair<Bitmap, Int> {
        val inputStream = contentResolver.openInputStream(uri)
        val orientation = inputStream?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        // Reset input stream for bitmap decoding
        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
        return Pair(bitmap, orientation)
    }

    /**
     * Rotate bitmap according to EXIF orientation
     */
    private fun getRotatedBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
        }

        return if (orientation != ExifInterface.ORIENTATION_NORMAL) {
            val rotatedBitmap = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
            // Recycle the original bitmap if we created a new one
            if (rotatedBitmap != bitmap) {
                bitmap.recycle()
            }
            rotatedBitmap.copy(Bitmap.Config.ARGB_8888, true)
        } else {
            bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
    }

    /**
     * Apply blur effect to the detected license plate area
     */
    private fun blurLicensePlate(canvas: Canvas, box: Rect?) {
        box?.let {
            val padding = 40
            val rect = RectF(
                (it.left - padding).toFloat(),
                (it.top - padding).toFloat(),
                (it.right + padding).toFloat(),
                (it.bottom + padding).toFloat()
            )
            val paint = Paint().apply {
                color = Color.BLACK
                maskFilter = BlurMaskFilter(60f, BlurMaskFilter.Blur.NORMAL)
                style = Paint.Style.FILL
            }
            canvas.drawRect(rect, paint)
        }
    }


    /**
     * Save the processed image while preserving orientation
     */
    private fun saveProcessedImage(bitmap: Bitmap, orientation: Int): File? {
        return try {
            val imageFile = createImageFile()
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }

            // Save the orientation in the output file's EXIF data
            val exif = ExifInterface(imageFile.absolutePath)
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            exif.saveAttributes()

            imageFile
        } catch (e: Exception) {
            handleError("Error saving image: ${e.message}")
            null
        }
    }


    /**
     * Show/hide loading indicator and disable/enable buttons
     */
    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        processButton.isEnabled = !isLoading
        selectButton.isEnabled = !isLoading
        captureButton.isEnabled = !isLoading
    }

    /**
     * Display a message to the user
     */
    private fun showMessage(message: String) {
        Snackbar.make(imageView, message, Snackbar.LENGTH_LONG).show()
    }

    /**
     * Handle and display errors
     */
    private fun handleError(error: String) {
        showLoading(false)
        showMessage(error)
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }
}