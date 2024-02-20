package com.projects.enzoftware.metalball

import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Point
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MetalBall : AppCompatActivity(), SensorEventListener {

    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null
    private var ground: GroundView? = null
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE)
        }
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        ground = GroundView(this)
        setContentView(ground)

        // Inicializa la referencia a la base de datos de Firebase
        databaseReference = FirebaseDatabase.getInstance().reference.child("movimientos")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // Actualiza la vista con los movimientos
            ground?.updateMe(event.values[1], event.values[0])

            // Envía los datos de los movimientos a Firebase Database
            val movimiento = mapOf("gx" to event.values[1], "gy" to event.values[0])
            databaseReference.setValue(movimiento)
        }
    }


    override fun onResume() {
        super.onResume()
        mSensorManager!!.registerListener(
            this, mAccelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    override fun onPause() {
        super.onPause()
        mSensorManager!!.unregisterListener(this)
    }

    inner class DrawThread(private val surfaceHolder: SurfaceHolder, private val panel: GroundView) : Thread() {
        private var run = false

        fun setRunning(run: Boolean) {
            this.run = run
        }

        override fun run() {
            var c: Canvas? = null
            while (run) {
                c = null
                try {
                    c = surfaceHolder.lockCanvas(null)
                    synchronized(surfaceHolder) {
                        panel.draw(c)
                    }
                } finally {
                    if (c != null) {
                        surfaceHolder.unlockCanvasAndPost(c)
                    }
                }
            }
        }
    }
}

class GroundView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private var cx: Float = 10f
    private var cy: Float = 10f

    private var lastGx: Float = 0f
    private var lastGy: Float = 0f

    private var picHeight: Int = 0
    private var picWidth: Int = 0

    private var icon: Bitmap? = null

    private var windowWidth: Int = 0
    private var windowHeight: Int = 0

    private var noBorderX = false
    private var noBorderY = false

    private var vibratorService: Vibrator? = null
    private var thread: MetalBall.DrawThread? = null

    init {
        holder.addCallback(this)
        val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val size = Point()
        display.getSize(size)
        windowWidth = size.x
        windowHeight = size.y
        icon = BitmapFactory.decodeResource(resources, R.drawable.ball)
        picHeight = icon!!.height
        picWidth = icon!!.width
        vibratorService = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        thread = MetalBall().DrawThread(holder, this)
        thread!!.setRunning(true)
        thread!!.start()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        canvas.drawColor(0xFFAAAAA.toInt())
        canvas.drawBitmap(icon!!, cx, cy, null)
    }

    fun updateMe(inx: Float, iny: Float) {
        lastGx += inx
        lastGy += iny

        cx += lastGx
        cy += lastGy

        if (cx > (windowWidth - picWidth)) {
            cx = (windowWidth - picWidth).toFloat()
            lastGx = 0f
            if (noBorderX) {
                vibratorService!!.vibrate(100)
                noBorderX = false
            }
        } else if (cx < 0) {
            cx = 0f
            lastGx = 0f
            if (noBorderX) {
                vibratorService!!.vibrate(100)
                noBorderX = false
            }
        } else {
            noBorderX = true
        }

        if (cy > (windowHeight - picHeight)) {
            cy = (windowHeight - picHeight).toFloat()
            lastGy = 0f
            if (noBorderY) {
                vibratorService!!.vibrate(100)
                noBorderY = false
            }
        } else if (cy < 0) {
            cy = 0f
            lastGy = 0f
            if (noBorderY) {
                vibratorService!!.vibrate(100)
                noBorderY = false
            }
        } else {
            noBorderY = true
        }

        invalidate()
    }
}
