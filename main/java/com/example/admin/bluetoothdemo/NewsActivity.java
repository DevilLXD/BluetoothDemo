package com.example.admin.bluetoothdemo;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.sql.Array;
import java.util.List;


public class NewsActivity extends AppCompatActivity {
private TextView tv;
private TextView tv2;
private ListView lv;
private Button btu;
private String bluetooth_name;
private String sbName;
private String address;
    // 蓝牙串口通讯类
private BluetoothPortSocket mblueSocket;
    // 数据接收缓存
 private List<Integer> mDataReceiveBuffer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);
        bluetooth_name = getIntent().getStringExtra("string_data");
        String[] arr = bluetooth_name.split("-");
        sbName =arr[0];
        address =arr[1];
        init();

    }
    private void init(){
        mblueSocket = new BluetoothPortSocket();
        mblueSocket.dataReceivedEvent(this, "dataReceive");
        mblueSocket.openBluetooth();
        try {
            mblueSocket.connectDevice(address);
        }
      catch (Exception e){
          Toast.makeText(getApplicationContext(), "socket开启错误", Toast.LENGTH_SHORT).show();
      }
        tv = (TextView)findViewById(R.id.bluetooth_name);
        tv2 = (TextView)findViewById(R.id.input);
        btu = (Button)findViewById(R.id.button_scan);
        lv = (ListView)findViewById(R.id.lv);
        tv.setText( sbName);
        btu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tv2.getText();
            }
        });
    }
    // 数据接收并处理
    public void dataReceive(byte[] dataBytes)
    {
        if(mDataReceiveBuffer.size() > 1024)
        {
            mDataReceiveBuffer.clear();
        }
        int length = dataBytes.length;
        for(int i = 0;i < length;i++)
        {
            mDataReceiveBuffer.add(dataBytes[i] & 0xFF);
        }
        mHandler.sendEmptyMessage(555);
    }


    // 多线程异步显示接收数据
    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case 555:		// 接收到数据
                    if (!mblueSocket.isPause)
                    {
                        StringBuffer strBuffer = new StringBuffer();
                        synchronized (mDataReceiveBuffer) 	// 代码块锁定
                        {
                            if (mblueSocket.mCodeType == BluetoothPortSocket.DataType.TEXT) 	// 文本格式显示接收数据
                            {
                                int length = mDataReceiveBuffer.size();
                                byte[] byteArr = new byte[length];
                                for(int i = 0;i < length;i++)
                                {
                                    // 整型强制转换成byte
                                    byteArr[i] = (byte)mDataReceiveBuffer.get(i).intValue();
                                }
                                try
                                {
                                    String byteStr = new String(byteArr,"GBK");
                                    strBuffer.append(byteStr);
                                }
                                catch (UnsupportedEncodingException e)
                                {
                                    e.printStackTrace();
                                }
                            }
                            else if (mblueSocket.mCodeType == BluetoothPortSocket.DataType.HEX) 	// 十六进制显示接收数据
                            {
                                for (int i : mDataReceiveBuffer)
                                {
                                    strBuffer.append(String.format("%02x", i).toUpperCase() + " ");
                                }
                            }
                        }
                        // 显示接收数据
                        tv2.setText(strBuffer.toString());
                    }
                    break;
                default:
                    break;
            }
        }
    };
}
