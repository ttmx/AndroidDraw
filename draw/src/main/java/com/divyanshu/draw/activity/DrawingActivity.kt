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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import com.divyanshu.draw.R
import com.divyanshu.draw.widget.DrawView
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class DrawingActivity : AppCompatActivity() {

    private val background_image by lazy { findViewById<ImageView>(R.id.background_img_view) }
    private val draw_color_palette by lazy { findViewById<LinearLayout>(R.id.draw_color_palette) }
    private val draw_tools by lazy { findViewById<MaterialCardView>(R.id.draw_tools) }
    private val draw_view by lazy { findViewById<DrawView>(R.id.draw_view) }
    private val image_draw_color by lazy { findViewById<ImageView>(R.id.image_draw_color) }
    private val image_draw_redo by lazy { findViewById<ImageView>(R.id.image_draw_redo) }
    private val image_draw_undo by lazy { findViewById<ImageView>(R.id.image_draw_undo) }

    private val dots by lazy {
        mapOf(
            (findViewById<ImageView>(R.id.image_color_black)) to R.color.color_black,
            (findViewById<ImageView>(R.id.image_color_red)) to R.color.color_red,
            (findViewById<ImageView>(R.id.image_color_yellow)) to R.color.color_yellow,
            (findViewById<ImageView>(R.id.image_color_green)) to R.color.color_green,
            (findViewById<ImageView>(R.id.image_color_blue)) to R.color.color_blue,
            (findViewById<ImageView>(R.id.image_color_pink)) to R.color.color_pink,
            (findViewById<ImageView>(R.id.image_color_brown)) to R.color.color_brown,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawing)

        if (intent.action != Intent.ACTION_EDIT && intent.data == null) {
            Toast.makeText(this, R.string.no_image_supplied, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        supportActionBar?.startActionMode(object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                menuInflater.inflate(R.menu.drawing_menu, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                when (item?.itemId) {
                    R.id.action_delete -> {
                        contentResolver.delete(intent.data!!, null)
                        finish()
                    }
                    R.id.action_share -> {
                        saveBitmap()
                        finish()

                        val uri = intent.data!!
                        val intent = Intent(Intent.ACTION_SEND)
                        intent.putExtra(Intent.EXTRA_STREAM, uri)
                        intent.type = "image/png"

                        startActivity(
                            Intent.createChooser(
                                intent, getString(R.string.abc_shareactionprovider_share_with)
                            )
                        )
                    }
                }
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                saveBitmap()
                finish()
            }

            private fun saveBitmap() {
                val origBitmap = ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(contentResolver, intent.data!!)
                )
                val bitmap = draw_view.getTransparentBitmap(origBitmap.height, origBitmap.width)
                val editableBitmap = origBitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(editableBitmap)
                canvas.drawBitmap(bitmap, 0f, 0f, null)

                saveImageToFile(editableBitmap)
            }
        })

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
        draw_tools.setCardBackgroundColor(window.navigationBarColor)
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
        dots.forEach { (view, id) ->
            view.setOnClickListener {
                draw_view.setColor(getColor(id))
                scaleColorView(view)
            }
        }
    }

    private fun scaleColorView(view: View) {
        //reset scale of all views
        dots.forEach { (view, _) ->
            view.scaleX = 1f
            view.scaleY = 1f
        }

        //set scale of selected view
        view.scaleX = 1.5f
        view.scaleY = 1.5f
    }

    private val Int.toPx: Float
        get() = (this * Resources.getSystem().displayMetrics.density)
}
