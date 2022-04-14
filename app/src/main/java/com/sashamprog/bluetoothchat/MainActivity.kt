package com.sashamprog.bluetoothchat

import android.app.Activity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mViewModel: MainViewModel by viewModels()
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
        setContent {
            CreateContent()
        }


//        messageAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
//        list_view.adapter = messageAdapter
//
//
//        setupBoundedDevices()
//        mViewModel.bondedDevices.observe(this, { devices ->
//            if (devices != null) {
//                mBoundedAdapter?.clear()
//                mBoundedAdapter?.addAll(devices.map { it.name })
//                if (mViewModel.boundedDevicePosition != null) {
//                    spinner_devices.setSelection(mViewModel.boundedDevicePosition!!)
//                }
//            }
//        })
//
//        mViewModel.messages.observe(this, { messages ->
//            messageAdapter?.clear()
//            val dateFormat = SimpleDateFormat("HH:mm:ss")
//            messageAdapter?.addAll(messages.map { message ->
//                val time = dateFormat.format(Date(message.timeInMillis))
//                return@map "${message.text}\n\t${message.authorName} $time"
//            })
//            messageAdapter?.notifyDataSetChanged()
//            list_view.smoothScrollToPosition(messages.lastIndex)
//        })
//
//        mViewModel.makeVibrate.observe(this, {
//            val service = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
//            if (Build.VERSION.SDK_INT >= 26) {
//                service.vibrate(
//                    VibrationEffect.createOneShot(
//                        1000,
//                        VibrationEffect.DEFAULT_AMPLITUDE
//                    )
//                )
//            } else {
//                service.vibrate(1000)
//            }
//        })
//
//        mViewModel.toggleWifi.observe(this, {
//            val service =
//                applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
//            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
//                service.isWifiEnabled = !service.isWifiEnabled
//            } else {
//                Toast.makeText(this@MainActivity, "Операция недоступна", Toast.LENGTH_LONG)
//                    .show()
//            }
//        })
//
//        mViewModel.enableBluetooth.observe(this, {
//            mRequestEnableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
//        })
//
//        spinner_devices.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(
//                parent: AdapterView<*>?,
//                view: View?,
//                position: Int,
//                id: Long
//            ) {
//                mViewModel.boundedDevicePosition = position
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>?) {
//                mViewModel.boundedDevicePosition = null
//            }
//
//        }
    }

    @Preview
    @Composable
    fun CreateContent() {
        Column {
            Row {
                Text("viewModel.bluetoothAddress")
                Text("viewModel.bluetoothName")
            }
//            Spinner(this@MainActivity).also { setupBoundedDevices(it) }
        }
    }

    private fun setupBoundedDevices(spinner: Spinner) {
        mBoundedAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            ArrayList<String>()
        )

        mBoundedAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = mBoundedAdapter
    }
}
