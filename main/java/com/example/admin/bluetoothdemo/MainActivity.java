package com.example.admin.bluetoothdemo;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {
    // 返回值（设备名称 | 设备地址）
    public static String EXTRA_DEVICE_INFO = "device_info";
    // 本地蓝牙适配器
    private BluetoothAdapter mBtAdapter;
    // 已配对设备列表
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    // 未配对设备列表
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();

    }
    private void init(){
// 初始化设备列表
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item);

        // 已配对设备列表及事件绑定
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // 未配对设备列表及事件绑定
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // 注册搜索到设备广播
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // 注册搜索完成广播
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        // 搜索已配对设备
        getPairedDevices();

        // 搜索按钮事件
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                Toast.makeText(getApplicationContext(), "点击了", Toast.LENGTH_SHORT).show();
                // 搜索已配对设备
                getPairedDevices();
                // 搜索未配对设备
                doDiscovery();
            }
        });
    }
    //openBuletooth
    private void openBlueTooth( BluetoothAdapter  mBtAdapter){
        if( !mBtAdapter.isEnabled()){
            mBtAdapter.enable();
        }

    }

    // 搜索已配对设备
    private void getPairedDevices()
    {
        // 获取本地蓝牙适配器
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        openBlueTooth( mBtAdapter );
        // 获取已配对设备
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        // 清空原有设备集合
        mPairedDevicesArrayAdapter.clear();

        if (pairedDevices.size() > 0)
        {
            for (BluetoothDevice device : pairedDevices)
            {
                mPairedDevicesArrayAdapter.add(device.getName() + " | "+ device.getAddress());
            }
        }
        else
        {
            String noDevices = "无可配对设备";
            mPairedDevicesArrayAdapter.add(noDevices);
        }
    }

    // 界面销毁时释放资源
    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if (mBtAdapter != null)
        {
            mBtAdapter.cancelDiscovery();
        }

        // 注销广播监听
        this.unregisterReceiver(mReceiver);
    }
    // 启动广播搜索远程设备
    private void doDiscovery()
    {
        // 显示搜索进度
        setProgressBarIndeterminateVisibility(true);
        // 标题变为“正在搜索..”
        setTitle("正在搜索");

        // 如果正在搜索则停止后再搜索
        if (mBtAdapter.isDiscovering())
        {
            mBtAdapter.cancelDiscovery();
        }

        // 清空原有搜索
        mNewDevicesArrayAdapter.clear();
        // 启动搜索设备
        mBtAdapter.startDiscovery();
    }

    // 设备列表项点击事件，包含已配对和未配对设备
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener()
    {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3)
        {
            // 停止搜索设备
            mBtAdapter.cancelDiscovery();
            // 获取选择设备信息（设备名 | 设备地址）
            String info = ((TextView) v).getText().toString();
            Intent intent = new Intent(MainActivity.this, NewsActivity.class);
            intent.putExtra("string_data", info );
            startActivity(intent);
//            finish();
        }
    };

    // 广播接收器
    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            // 收到的广播类型
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) 	// 搜索到蓝牙设备
            {
                // 从intent中获取搜索到的蓝牙设备
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // 只取未配对的设备，因为已配对设备不在此展示
                if (device.getBondState() != BluetoothDevice.BOND_BONDED)
                {
                    mNewDevicesArrayAdapter.add(device.getName() + " - " + device.getAddress());
                }
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) 	// 搜索结束
            {
                // 隐藏搜索进度
                setProgressBarIndeterminateVisibility(false);
                // 标题设为“选择设备”
                setTitle("请选择设备");
                if (mNewDevicesArrayAdapter.getCount() == 0)
                {
                    // 未搜索到可用设备
                    String noDevices = "未搜索到可用设备";
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };

}
