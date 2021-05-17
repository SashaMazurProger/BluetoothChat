package com.sashamprog.bluetoothchat

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.sashamprog.bluetoothchat.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : FragmentActivity() {

    private lateinit var mDataBinding: ActivityMainBinding
    private lateinit var mViewModel: MainViewModel
    private var messageAdapter: ArrayAdapter<String>? = null
    private var mBoundedAdapter: ArrayAdapter<String>? = null

    private val mRequestEnableBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                mViewModel.onBluetoothEnabledByRequest()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Для работы приложения нужно включить Bluetooth",
                    Toast.LENGTH_LONG
                )
                    .show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mDataBinding.lifecycleOwner = this

        messageAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        list_view.adapter = messageAdapter

        mViewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        mDataBinding.setVariable(BR.viewModel, mViewModel)

        setupBoundedDevices()
        mViewModel.bondedDevices.observe(this, { devices ->
            if (devices != null) {
                mBoundedAdapter?.clear()
                mBoundedAdapter?.addAll(devices.map { it.name })
                if (mViewModel.boundedDevicePosition != null) {
                    spinner_devices.setSelection(mViewModel.boundedDevicePosition!!)
                }
            }
        })

        mViewModel.messages.observe(this, { messages ->
            messageAdapter?.clear()
            val dateFormat = SimpleDateFormat("HH:mm:ss")
            messageAdapter?.addAll(messages.map { message ->
                val time = dateFormat.format(Date(message.timeInMillis))
                return@map "${message.text}\n\t${message.authorName} $time"
            })
            messageAdapter?.notifyDataSetChanged()
            list_view.smoothScrollToPosition(messages.lastIndex)
        })

        mViewModel.makeVibrate.observe(this, {
            val service = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= 26) {
                service.vibrate(
                    VibrationEffect.createOneShot(
                        1000,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                service.vibrate(1000)
            }
        })

        mViewModel.toggleWifi.observe(this, {
            val service =
                applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                service.isWifiEnabled = !service.isWifiEnabled
            } else {
                Toast.makeText(this@MainActivity, "Операция недоступна", Toast.LENGTH_LONG)
                    .show()
            }
        })

        mViewModel.enableBluetooth.observe(this, {
            mRequestEnableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        })

        spinner_devices.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                mViewModel.boundedDevicePosition = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                mViewModel.boundedDevicePosition = null
            }

        }
    }

    private fun setupBoundedDevices() {
        mBoundedAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            ArrayList<String>()
        )

        mBoundedAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_devices.adapter = mBoundedAdapter
    }
}
