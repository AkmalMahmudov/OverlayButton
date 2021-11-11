package uz.gita.overlaydemo

import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.view.*
import android.view.View.OnTouchListener
import androidx.annotation.RequiresApi
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import uz.gita.overlaydemo.databinding.OverlaybuttonViewBinding
import java.util.*

class OverlayService : LifecycleService() {
    private val coroutineScope = CoroutineScope(Dispatchers.Main.immediate + Job())
    private var _viewBinding: OverlaybuttonViewBinding? = null
    private val binding: OverlaybuttonViewBinding get() = _viewBinding!!
    private var _windowManager: WindowManager? = null
    private val windowManager: WindowManager get() = _windowManager!!
    private var clockVisibility: Boolean = false
    private val timeLiveData = MutableLiveData<String>()

    companion object {
        private val windowType =
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        private const val windowFlag =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        private const val windowFormat = PixelFormat.TRANSLUCENT
        private val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            windowFlag,
            windowFormat
        )
    }

    override fun onCreate() {
        super.onCreate()
        createOverlayView()
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    private fun createOverlayView() {
        _viewBinding = OverlaybuttonViewBinding.inflate(LayoutInflater.from(this))
        _windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        layoutParams.gravity = Gravity.START

        coroutineScope.launch {
            while (true) {
                delay(1000)
                val date: Date = Calendar.getInstance().time
                timeLiveData.postValue(date.toString())
            }
        }
        timeLiveData.observe(this) {
            binding.date.text = " $it "
        }
        binding.image.setOnClickListener {
            if (clockVisibility) {
                binding.date.visibility = View.GONE
                clockVisibility = !clockVisibility
            } else {
                binding.date.visibility = View.VISIBLE
                clockVisibility = !clockVisibility
            }
        }

        windowManager.addView(binding.root, layoutParams)
        binding.image.setOnTouchListener(
            object : OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f

                @RequiresApi(Build.VERSION_CODES.S)
                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            //remember the initial position.
                            initialX = layoutParams.x
                            initialY = layoutParams.y

                            //get the touch location
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                        }

                        MotionEvent.ACTION_MOVE -> {
                            //Calculate the X and Y coordinates of the view.
                            layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                            layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()

                            //Update the layout with new X & Y coordinate
                            windowManager.updateViewLayout(binding.root, layoutParams)
                        }
                        MotionEvent.ACTION_UP -> {
                            val displayMetrics = DisplayMetrics()
                            windowManager.defaultDisplay.getMetrics(displayMetrics)
                            val width = displayMetrics.widthPixels

                            if (initialX + (event.rawX - initialTouchX).toInt() < width / 2) {
                                layoutParams.x = 0
                            } else {
                                layoutParams.x = width
                            }
                            layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                            windowManager.updateViewLayout(binding.root, layoutParams)
                        }
                    }
                    return false
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.removeView(binding.root)
        _viewBinding = null
        _windowManager = null
    }
}