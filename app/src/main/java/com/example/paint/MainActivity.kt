package com.example.paint

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.RangeSlider
import com.example.paint.databinding.ActivityMainBinding
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorListener

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val startActivityForResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    if (it.data != null && it.data?.data != null) {
                        val bmp = binding.drawView.save()
                        it.data?.data?.let { uri ->
                            contentResolver.openOutputStream(uri)?.use { op ->
                                bmp?.compress(Bitmap.CompressFormat.PNG, 100, op)
                            }
                        }
                    } else {
                        Toast.makeText(this, "Some error ocured", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        //the undo button will remove the most recent stroke from the canvas
        binding.btnUndo.setOnClickListener { binding.drawView.undo() }

        //the undo button long click remove everything from the canvas
        binding.btnUndo.setOnLongClickListener {
            binding.drawView.undoAll()
            false
        }

        //the save button will save the current canvas which is actually a bitmap
        //in form of PNG, in the storage
        binding.btnSave.setOnClickListener {
            createFile("sample.png", startActivityForResult)
        }
        //the color button will allow the user to select the color of his brush
        binding.btnColor.setOnClickListener { showColorPickerDialog() }

        // the button will toggle the visibility of the RangeBar/RangeSlider
        binding.btnStroke.setOnClickListener {
            if (binding.rangebar.visibility == View.VISIBLE)
                binding.rangebar.visibility = View.GONE
            else binding.rangebar.visibility = View.VISIBLE
        }

        //set the range of the RangeSlider
        binding.rangebar.valueFrom = 0.0f
        binding.rangebar.valueTo = 100.0f
        //adding a OnChangeListener which will change the stroke width
        //as soon as the user slides the slider
        binding.rangebar.addOnChangeListener(RangeSlider.OnChangeListener { slider, value, fromUser ->
            binding.drawView.setStrokeWidth(
                value.toInt()
            )
        })

        //pass the height and width of the custom view to the init method of the DrawView object
        val vto: ViewTreeObserver = binding.drawView.viewTreeObserver
        vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.drawView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val width = binding.drawView.measuredWidth
                val height = binding.drawView.measuredHeight
                binding.drawView.init(height, width)
            }
        })
    }

    private fun showColorPickerDialog() {
        val pickerDialog = ColorPickerDialog.Builder(this)
            .setPreferenceName("MyColorPickerDialog")
            .attachAlphaSlideBar(false) // the default value is true.
            .attachBrightnessSlideBar(false) // the default value is true.
            .setBottomSpace(12) // set a bottom space between the last slidebar and buttons.
        val alertDialog = pickerDialog.show()
        pickerDialog.colorPickerView.setColorListener(object : ColorListener {
            override fun onColorSelected(color: Int, fromUser: Boolean) {
                binding.drawView.setColor(color)
                if (fromUser) alertDialog.dismiss()
            }
        })
    }

    private fun createFile(fileName: String, launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        // file type
        intent.type = "image/*"
        // file name
        intent.putExtra(Intent.EXTRA_TITLE, fileName)
        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        )
        launcher.launch(intent)
    }
}