package com.divyanshu.draw.activity

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import com.divyanshu.draw.R
import com.divyanshu.draw.widget.DrawView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class DrawingActivity : AppCompatActivity() {

    private val background_image by lazy { findViewById<ImageView>(R.id.background_img_view) }
    private val draw_color_palette by lazy { findViewById<LinearLayout>(R.id.draw_color_palette) }
    private val draw_tools by lazy { findViewById<CardView>(R.id.draw_tools) }
    private val draw_view by lazy { findViewById<DrawView>(R.id.draw_view) }
    private val fab_close_drawing by lazy { findViewById<FloatingActionButton>(R.id.fab_close_drawing) }
    private val fab_send_drawing by lazy { findViewById<FloatingActionButton>(R.id.fab_send_drawing) }
    private val image_color_black by lazy { findViewById<ImageView>(R.id.image_color_black) }
    private val image_color_blue by lazy { findViewById<ImageView>(R.id.image_color_blue) }
    private val image_color_brown by lazy { findViewById<ImageView>(R.id.image_color_brown) }
    private val image_color_green by lazy { findViewById<ImageView>(R.id.image_color_green) }
    private val image_color_pink by lazy { findViewById<ImageView>(R.id.image_color_pink) }
    private val image_color_red by lazy { findViewById<ImageView>(R.id.image_color_red) }
    private val image_color_yellow by lazy { findViewById<ImageView>(R.id.image_color_yellow) }
    private val image_draw_color by lazy { findViewById<ImageView>(R.id.image_draw_color) }
    private val image_draw_redo by lazy { findViewById<ImageView>(R.id.image_draw_redo) }
    private val image_draw_undo by lazy { findViewById<ImageView>(R.id.image_draw_undo) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawing)

        if (intent.action != Intent.ACTION_EDIT && intent.data == null) {
            Toast.makeText(this, R.string.no_image_supplied, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        fab_close_drawing.setOnClickListener {
            finish()
        }

        fab_send_drawing.setOnClickListener {
            val origBitmap = ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(contentResolver, intent.data!!)
            )
            val bitmap = draw_view.getTransparentBitmap(origBitmap.height, origBitmap.width)
            val editableBitmap = origBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(editableBitmap)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            saveImageToFile(editableBitmap)
            setResult(Activity.RESULT_OK, Intent())
            finish()
        }

        setUpDrawTools()

        if (intent.action == Intent.ACTION_EDIT) {
            CoroutineScope(Dispatchers.Default).run {
                intent.data?.let {
                    setupEdit(it)
                }
            }
        }

        colorSelector()
    }

    private fun setupEdit(uri: Uri) {
        draw_view.setBackgroundColor(Color.TRANSPARENT)
        background_image.setImageBitmap(
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
        )
    }

    private fun saveImageToFile(bitmap: Bitmap) {
        val uri = intent.data!!
        val datetime = contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttribute(ExifInterface.TAG_DATETIME)
        }.orEmpty()

        contentResolver.openOutputStream(uri, "wt")?.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        contentResolver.openFileDescriptor(uri, "rw")?.use {
            ExifInterface(it.fileDescriptor).apply {
                setAttribute(ExifInterface.TAG_DATETIME, datetime)
                setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, datetime)
                saveAttributes()
            }
        }
    }

    private fun setUpDrawTools() {
        image_draw_color.setOnClickListener {
            if (draw_tools.translationY == (56).toPx) {
                toggleDrawTools(draw_tools, true)
            } else if (draw_tools.translationY == (0).toPx && draw_color_palette.visibility == View.VISIBLE) {
                toggleDrawTools(draw_tools, false)
            }
            draw_color_palette.visibility = View.VISIBLE
        }
        image_draw_undo.setOnClickListener {
            draw_view.undo()
            toggleDrawTools(draw_tools, false)
        }
        image_draw_redo.setOnClickListener {
            draw_view.redo()
            toggleDrawTools(draw_tools, false)
        }
    }

    private fun toggleDrawTools(view: View, showView: Boolean = true) {
        if (showView) {
            view.animate().translationY((0).toPx)
        } else {
            view.animate().translationY((56).toPx)
        }
    }

    private fun colorSelector() {
        image_color_black.setOnClickListener {
            val color = ResourcesCompat.getColor(resources, R.color.color_black, null)
            draw_view.setColor(color)
            scaleColorView(image_color_black)
        }
        image_color_red.setOnClickListener {
            val color = ResourcesCompat.getColor(resources, R.color.color_red, null)
            draw_view.setColor(color)
            scaleColorView(image_color_red)
        }
        image_color_yellow.setOnClickListener {
            val color = ResourcesCompat.getColor(resources, R.color.color_yellow, null)
            draw_view.setColor(color)
            scaleColorView(image_color_yellow)
        }
        image_color_green.setOnClickListener {
            val color = ResourcesCompat.getColor(resources, R.color.color_green, null)
            draw_view.setColor(color)
            scaleColorView(image_color_green)
        }
        image_color_blue.setOnClickListener {
            val color = ResourcesCompat.getColor(resources, R.color.color_blue, null)
            draw_view.setColor(color)
            scaleColorView(image_color_blue)
        }
        image_color_pink.setOnClickListener {
            val color = ResourcesCompat.getColor(resources, R.color.color_pink, null)
            draw_view.setColor(color)
            scaleColorView(image_color_pink)
        }
        image_color_brown.setOnClickListener {
            val color = ResourcesCompat.getColor(resources, R.color.color_brown, null)
            draw_view.setColor(color)
            scaleColorView(image_color_brown)
        }
    }

    private fun scaleColorView(view: View) {
        //reset scale of all views
        image_color_black.scaleX = 1f
        image_color_black.scaleY = 1f

        image_color_red.scaleX = 1f
        image_color_red.scaleY = 1f

        image_color_yellow.scaleX = 1f
        image_color_yellow.scaleY = 1f

        image_color_green.scaleX = 1f
        image_color_green.scaleY = 1f

        image_color_blue.scaleX = 1f
        image_color_blue.scaleY = 1f

        image_color_pink.scaleX = 1f
        image_color_pink.scaleY = 1f

        image_color_brown.scaleX = 1f
        image_color_brown.scaleY = 1f

        //set scale of selected view
        view.scaleX = 1.5f
        view.scaleY = 1.5f
    }

    private val Int.toPx: Float
        get() = (this * Resources.getSystem().displayMetrics.density)
}
