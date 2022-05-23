package com.divyanshu.draw.activity


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.exifinterface.media.ExifInterface
import com.divyanshu.draw.R
import com.google.android.material.card.MaterialCardView
import org.signal.imageeditor.core.ImageEditorView
import org.signal.imageeditor.core.ImageEditorView.TapListener
import org.signal.imageeditor.core.Renderer
import org.signal.imageeditor.core.RendererContext.Invalidate
import org.signal.imageeditor.core.RendererContext.TypefaceProvider
import org.signal.imageeditor.core.model.EditorElement
import org.signal.imageeditor.core.model.EditorModel
import org.signal.imageeditor.core.renderers.MultiLineTextRenderer
import org.signal.imageeditor.core.renderers.UriRenderer


@SuppressLint("WrongViewCast")
class DrawingActivity : AppCompatActivity() {
//    private val background_image by lazy { findViewById<ImageView>(R.id.background_img_view) }
    private val draw_color_palette by lazy { findViewById<MaterialCardView>(R.id.draw_color_palette) }
    private val draw_tools by lazy { findViewById<MaterialCardView>(R.id.draw_tools) }
    private val draw_view by lazy { findViewById<ImageEditorView>(R.id.draw_view) }
    private val image_draw_color by lazy { findViewById<ImageView>(R.id.image_draw_color) }
    private val image_draw_crop by lazy { findViewById<ImageView>(R.id.image_draw_crop) }
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
            (findViewById<ImageView>(R.id.image_color_white)) to R.color.color_brown,
        )
    }

    private var paletteHidden = true
    private fun MaterialCardView.togglePalette() {
        animate().translationY(if (paletteHidden) 0f else height.toFloat()).start()
        paletteHidden = !paletteHidden
    }

    private val typefaceProvider =
        TypefaceProvider { _: Context?, _: Renderer?, _: Invalidate? ->
            return@TypefaceProvider Typeface.Builder("")
                .setFallback("sans-serif")
                .setWeight(900)
                .build()
        }

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawing)


        if (intent.action != Intent.ACTION_EDIT || (intent.data == null && intent.clipData == null)) {
            Toast.makeText(this, R.string.no_image_supplied, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val uri = (intent.data ?: intent.clipData?.getItemAt(0)?.uri)!!


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
                        startActivity(Intent.createChooser(Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_STREAM, uri)
                            type = "image/*"
                        }, null))
                    }
                }
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                saveBitmap()
                finish()
            }

            private fun saveBitmap() {
                val bitmap = draw_view.model.render(applicationContext,typefaceProvider)
                saveImageToFile(bitmap, uri)
            }
        })
        draw_view.setTypefaceProvider(typefaceProvider)
        draw_view.setUndoRedoStackListener { undoAvailable: Boolean, redoAvailable: Boolean ->
            Log.d(
                "ALAN",
                String.format(
                    "Undo/Redo available: %s, %s",
                    if (undoAvailable) "Y" else "N",
                    if (redoAvailable) "Y" else "N"
                )
            )
            image_draw_undo.alpha = (if (undoAvailable) 1f else 0.2f)
            image_draw_redo.alpha = (if (redoAvailable) 1f else 0.2f)
        }
        var model: EditorModel? = null
        if (savedInstanceState != null) {
            model = savedInstanceState.getParcelable("MODEL")
            Log.d("ALAN", "Restoring instance " + (model?.hashCode() ?: 0))
        }
        if (model == null) {
            model = initialModel(uri)
            Log.d("ALAN", "New instance created " + model.hashCode())
        }
        draw_view.model = model
        draw_view.setTapListener(object : TapListener {
            override fun onEntityDown(editorElement: EditorElement?) {
                Log.d("ALAN", "Entity down $editorElement")
            }

            override fun onEntitySingleTap(editorElement: EditorElement?) {
                Log.d("ALAN", "Entity single tapped $editorElement")
            }

            override fun onEntityDoubleTap(editorElement: EditorElement) {
                Log.d("ALAN", "Entity double tapped $editorElement")
                if (editorElement.renderer is MultiLineTextRenderer) {
                    draw_view.startTextEditing(editorElement)
                } else {
                    draw_view.deleteElement(editorElement)
                }
            }
        })
        setUpDrawTools()


        colorSelector()

        draw_view.setDrawingBrushColor(getColor(R.color.color_blue))
        draw_view.startDrawing(0.02f, Paint.Cap.ROUND, false)
    }

    private fun initialModel(uri: Uri): EditorModel {

        val model = EditorModel.create()
        val image =
            EditorElement(UriRenderer(uri))
        image.flags.setSelectable(false).persist()
        model.addElement(image)
//        draw_view.setBackgroundColor(Color.TRANSPARENT)
//        background_image.setImageBitmap(
//            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
//        )
//        background_image.drawable.let {
//            requestedOrientation = if (it.intrinsicHeight > it.intrinsicWidth)
//                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
//        }
        return model
    }

    private fun saveImageToFile(bitmap: Bitmap, uri: Uri) {
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
        draw_color_palette.setCardBackgroundColor(window.navigationBarColor)
        draw_tools.setCardBackgroundColor(window.navigationBarColor)
        image_draw_color.setOnClickListener {
            draw_color_palette.togglePalette()
        }
        image_draw_crop.setOnClickListener {
            if ( draw_view.model.isCropping ){
                draw_view.model.doneCrop()
                draw_view.mode = ImageEditorView.Mode.Draw
            }else{
                draw_view.mode = ImageEditorView.Mode.MoveAndResize
                draw_view.model.startCrop()
            }
        }
        image_draw_undo.setOnClickListener {
            draw_view.model.undo()
        }
        image_draw_redo.setOnClickListener {
            draw_view.model.redo()
        }

    }

    private fun colorSelector() {
        dots.forEach { (view, id) ->
            view.setOnClickListener {
                draw_color_palette.togglePalette()
                draw_view.setDrawingBrushColor(getColor(id))
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
    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("MODEL", draw_view!!.model)
    }

//
//    private fun saveBmp(bitmap: Bitmap): Uri {
//        val path = Environment.getExternalStorageDirectory().toString()
//        val filePath = File(path)
//        val imageEditor = File(filePath, "ImageEditor")
//        if (!imageEditor.exists()) {
//            imageEditor.mkdir()
//        }
//        var counter = 0
//        var file: File
//        do {
//            counter++
//            file = File(imageEditor, String.format(Locale.US, "ImageEditor_%03d.jpg", counter))
//        } while (file.exists())
//        return try {
//            FileOutputStream(file).use { stream ->
//                bitmap.compress(
//                    Bitmap.CompressFormat.JPEG,
//                    90,
//                    stream
//                )
//            }
//            Uri.parse(
//                MediaStore.Images.Media.insertImage(
//                    contentResolver,
//                    file.absolutePath,
//                    file.name,
//                    file.name
//                )
//            )
//        } catch (e: FileNotFoundException) {
//            throw RuntimeException(e)
//        } catch (e: IOException) {
//            throw RuntimeException(e)
//        }
//    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
