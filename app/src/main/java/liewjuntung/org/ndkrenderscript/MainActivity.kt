package liewjuntung.org.ndkrenderscript

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.renderscript.*
import android.support.annotation.DrawableRes
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.BitmapCompat
import android.util.Log
import android.view.View.X
import android.view.View.Y
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_main.*
import liewjuntung.org.ndkrenderscript.R.id.image1
import org.apache.commons.io.IOUtils
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.*
import java.nio.ByteBuffer
import java.util.*

class MainActivity : AppCompatActivity() {

    val TAG = MainActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        val mBitmapIn = loadBitmap(R.drawable.image)
        // Example of a call to a native method
        //image2.setImageBitmap(mBitmapIn)
//        object : AsyncTask<Void, Void, Void>() {
//
//            override fun doInBackground(vararg voids: Void): Void? {
//                convertBitmap(this@MainActivity.cacheDir.toString(),
//                        mBitmapIn.width, mBitmapIn.height,
//                        mBitmapIn, mBitmapOut)
//                return null
//            }
//
//            override fun onPostExecute(result: Void?) {
//                image2.setImageBitmap(mBitmapOut)
//            }
//        }.execute()
//        camera_activity.setOnClickListener {
//            startActivity(Intent(this, Sample_Single::class.java))
//        }

        if (savedInstanceState == null) {
            //Check permissions
            ensurePermissions(Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        val assetManager = assets
        val fut = doAsync {
            for(i in 0..100){
                val num = assetManager.createNativeSplitBitmap()
                uiThread {
                    timer_text.text = num
                }
            }
        }


//        assetManager.loadRgba()
    }

    fun AssetManager.loadRgba(width: Int = 3840, height: Int = 2160) {
        val mBitmapOut = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val inputStream = open("progress.jpg")
        val byteBuffer = inputStream.byteBuffer()
        byteBuffer.rewind()
        mBitmapOut.copyPixelsFromBuffer(byteBuffer)
        image1.setImageBitmap(mBitmapOut)
    }

    fun AssetManager.createNativeBitmap(width: Int = 3840, height: Int = 2160) {
        val mBitmapOut = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val inputStream = open("progress.yuv")
        val byteArray = IOUtils.toByteArray(inputStream)
        val byteBuffer = ByteBuffer.allocateDirect(byteArray.size)
        byteBuffer.put(byteArray)
        image1.setImageBitmap(mBitmapOut)
//        image1.setImageBitmap(assets.createBitmap())
        convertYuvBitmap(this@MainActivity.cacheDir.toString(),
                byteArray.size.toFloat(),
                width, height,
                byteBuffer, mBitmapOut)
    }

    val aList = arrayListOf<Long>()

    fun AssetManager.createNativeSplitBitmap(width: Int = 3840, height: Int = 2160): String {
        val mBitmapOut = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888)

        val yInputStream = open("y.yuv")
        val uInputStream = open("v.yuv")
        val vInputStream = open("u.yuv")

        // 155 ms
        val yByteBuffer = yInputStream.byteBuffer()
        val uByteBuffer = uInputStream.byteBuffer()
        val vByteBuffer = vInputStream.byteBuffer()


        val ySize = yByteBuffer.rewind().remaining()
        val uSize = uByteBuffer.rewind().remaining()
        val vSize = vByteBuffer.rewind().remaining()


        val emptyByteBuffer = ByteBuffer.allocateDirect((ySize +
                uSize +
                vSize))
        val startTimeJni = System.currentTimeMillis()
        val jni = getStringFromJNI()
        val endTimeJni = System.currentTimeMillis()
        Log.d(TAG, "processImage: put $jni: " + (endTimeJni - startTimeJni) + "ms")

        val startTime = System.currentTimeMillis()
        val buffer = convertSplitYuvBitmap(this@MainActivity.cacheDir.toString(),
                width, height,
                yByteBuffer,
                uByteBuffer,
                vByteBuffer,
                ySize,
                uSize,
                vSize
        )
        val endTime1 = System.currentTimeMillis()
        val diff = (endTime1 - startTime)
        aList.add(diff)
        return "Time: $diff ms. \nAvg: ${aList.average()} ms."
//        val buffSize = buffer.remaining()
//        val buff = ByteBuffer.allocateDirect(buffSize)
//        buff.put(buffer)
//        buff.rewind()
//        mBitmapOut.copyPixelsFromBuffer(buff)
//        image1.setImageBitmap(mBitmapOut)
//        try {
//            val os = DataOutputStream(FileOutputStream(getFileStreamPath("progress.dat")))
//            os.write(buff.array())
//            os.close()
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
    }

    fun InputStream.byteBuffer(): ByteBuffer {
        val byteArray = IOUtils.toByteArray(this)
        val byteBuffer = ByteBuffer.allocateDirect(byteArray.size)
        byteBuffer.put(byteArray)
        return byteBuffer
    }

    fun AssetManager.createBitmap(width: Int = 4032, height: Int = 2277): Bitmap {

        val inputStream = open("pink.yuv")

        val byteArray = IOUtils.toByteArray(inputStream)

        val byteArrayOutputStream = ByteArrayOutputStream()
        byteArrayOutputStream.write(byteArray)
        val outAlloc = YUV_TO_RGB(byteArrayOutputStream, width, height)
        val bitmap = getOutAllocBitmap(width, height, outAlloc)
        return bitmap
    }

    fun getOutAllocBitmap(width: Int, height: Int, outAlloc: Allocation): Bitmap {
        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outAlloc.copyTo(outBitmap)
        return outBitmap
    }


    private var inAlloc: Allocation? = null

    private var outAlloc: Allocation? = null

    // private val outputBytes = ByteArrayOutputStream()

    private fun YUV_TO_RGB(outputBytes: ByteArrayOutputStream, width: Int = 4032, height: Int = 2277): Allocation {
        val rs: RenderScript = RenderScript.create(this)
        val yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8(rs))

        if (inAlloc == null || inAlloc!!.bytesSize < outputBytes.size()) {

            val yuvType = Type.Builder(rs, Element.U8(rs)).setX(outputBytes.size())
                    .create()
            inAlloc = Allocation.createTyped(rs, yuvType, Allocation.USAGE_SCRIPT)
        }

        if (outAlloc?.element?.bytesSize == null || outAlloc!!.bytesSize < outAlloc!!.element.bytesSize * width * height) {
            val rgbaType = Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height)
            outAlloc = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT)
        }

        inAlloc!!.copyFrom(outputBytes.toByteArray())

        yuvToRgbIntrinsic.setInput(inAlloc)
        yuvToRgbIntrinsic.forEach(outAlloc)
        outputBytes.reset()
//        val convertEndTime = System.currentTimeMillis()
//        Log.d(TAG, "processImage: YUV TO RGB: " + (convertEndTime - convertStartTime) + "ms" )
        return outAlloc!!
    }

    private fun ensurePermissions(vararg permissions: String) {
        val deniedPermissionList = ArrayList<String>()

        for (permission in permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, permission)) deniedPermissionList.add(permission)
        }

        if (!deniedPermissionList.isEmpty()) ActivityCompat.requestPermissions(this, deniedPermissionList.toTypedArray(), 0)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                showAlertDialog("Requested permission is not granted.", true)
            }
        }
    }


    /**
     * Shows alert dialog.
     */
    private fun showAlertDialog(message: String, finishActivity: Boolean) {
        AlertDialogFragment.newInstance(android.R.drawable.ic_dialog_alert,
                "Alert",
                message,
                finishActivity).show(fragmentManager, "alert_dialog")
    }

    /**
     * Alert Dialog Fragment
     */
    class AlertDialogFragment : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
            val finishActivity = arguments.getBoolean("finish_activity")

            return AlertDialog.Builder(activity)
                    .setTitle(arguments.getCharSequence("title"))
                    .setIcon(arguments.getInt("icon"))
                    .setMessage(arguments.getCharSequence("message"))
                    .setPositiveButton(android.R.string.ok, if (finishActivity)
                        DialogInterface.OnClickListener { dialog, which ->
                            dialog.dismiss()
                            activity.finish()
                        }
                    else
                        null)
                    .setCancelable(false)
                    .create()
        }

        companion object {
            fun newInstance(@DrawableRes iconId: Int, title: CharSequence, message: CharSequence, finishActivity: Boolean): AlertDialogFragment {
                val fragment = AlertDialogFragment()

                val args = Bundle()
                args.putInt("icon", iconId)
                args.putCharSequence("title", title)
                args.putCharSequence("message", message)
                args.putBoolean("finish_activity", finishActivity)

                fragment.arguments = args
                return fragment
            }
        }
    }

    private fun loadBitmap(resource: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        return BitmapFactory.decodeResource(resources, resource, options)
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun convertBitmap(cacheDir: String, X: Int, Y: Int, input: Bitmap, out: Bitmap)

    external fun convertYuvBitmap(cacheDir: String, size: Float, X: Int, Y: Int, input: ByteBuffer, out: Bitmap)
    external fun convertSplitYuvBitmap(cacheDir: String,
                                       X: Int,
                                       Y: Int,
                                       yBuffer: ByteBuffer,
                                       uBuffer: ByteBuffer,
                                       vBuffer: ByteBuffer,
                                       ySize: Int,
                                       uSize: Int,
                                       vSize: Int
    )

    external fun convertYuv(cacheDir: String, size: Int, X: Int, Y: Int, input: ByteBuffer): ByteBuffer
    external fun getStringFromJNI(): String

    companion object {

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}
