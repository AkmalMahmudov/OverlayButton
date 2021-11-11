package uz.gita.overlaydemo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import by.kirich1409.viewbindingdelegate.viewBinding
import uz.gita.overlaydemo.databinding.FragmentMainBinding

class MainScreen : Fragment(R.layout.fragment_main) {
    private val binding by viewBinding(FragmentMainBinding::bind)
    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                startOverlay()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.start.setOnClickListener {
            checkPermission {
                startOverlay()
            }
        }
        binding.stop.setOnClickListener { stopOverlay() }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun checkPermission(block: () -> Unit) {
        if (Build.VERSION.SDK_INT < 23) {
            block()
            return
        }
        if (!Settings.canDrawOverlays(requireContext())) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${requireActivity().packageName}")
            )
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                launcher.launch(intent)
            }
        } else {
            block()
        }
    }

    private fun startOverlay() {
        val intent = Intent(requireContext(), OverlayService::class.java)
        requireActivity().startService(intent)
    }

    private fun stopOverlay() {
        val intent = Intent(requireContext(), OverlayService::class.java)
        requireActivity().stopService(intent)
    }
}