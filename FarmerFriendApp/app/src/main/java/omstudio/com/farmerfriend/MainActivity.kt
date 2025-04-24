package omrajstudio.com.farmerfriend

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {
    private lateinit var classifier: Classifier
    private lateinit var bitmap: Bitmap
    private lateinit var myDialog: Dialog

    private var diseaseName: String? = ""
    private var symptoms: String? = ""
    private var management: String? = ""

    private val inputSize = 200
    private val modelPath = "model.tflite"
    private val labelPath = "labels.txt"

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data?.extras?.get("data") as? Bitmap
            data?.let {
                bitmap = scaleImage(it)
                mPhotoImageView.setImageBitmap(bitmap)
                classifier.recognizeImage(bitmap).firstOrNull()?.let { output ->
                    mResultTextView.text = output.title
                    mResultTextView_2.text = output.confidence.toString()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_main)

        classifier = Classifier(assets, modelPath, labelPath, inputSize)
        myDialog = Dialog(this)

        disease_info.setOnClickListener { showDiseaseDialog() }
        mCameraButton.setOnClickListener { checkCameraPermissionAndOpen() }
    }

    private fun showDiseaseDialog() {
        myDialog.apply {
            setContentView(R.layout.detail_dailog_act)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            show()
        }

        val nameView = myDialog.findViewById<TextView>(R.id.pltd_name)
        val symptomsView = myDialog.findViewById<TextView>(R.id.symptoms)
        val manageView = myDialog.findViewById<TextView>(R.id.management)

        nameView.text = mResultTextView.text
        val selectedDisease = nameView.text.toString()

        try {
            val jsonObject = JSONObject(loadJSONFromAsset())
            val jsonArray = jsonObject.getJSONArray("plant_disease")

            for (i in 0 until jsonArray.length()) {
                val plant = jsonArray.getJSONObject(i)
                if (plant.getString("name") == selectedDisease) {
                    symptomsView.text = plant.getString("symptoms")
                    manageView.text = plant.getString("management")
                    break
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun loadJSONFromAsset(): String? {
        return try {
            assets.open("data.json").use { inputStream ->
                val buffer = ByteArray(inputStream.available())
                inputStream.read(buffer)
                String(buffer, StandardCharsets.UTF_8)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun checkCameraPermissionAndOpen() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        } else {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(intent)
        }
    }

    private fun scaleImage(bitmap: Bitmap): Bitmap {
        val matrix = Matrix().apply {
            postScale(inputSize.toFloat() / bitmap.width, inputSize.toFloat() / bitmap.height)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
