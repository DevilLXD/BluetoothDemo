package com.example.admin.bluetoothdemo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


public class BluetoothPortSocket
{
	// 当前数据收发类型(0:十六进制；1:文本（默认GBK编码）)
	public DataType mCodeType = DataType.HEX;
	// 是否暂停接收数据
	public boolean isPause = false;

	// 接收到数据提示
	public static final int MSG_RECEIVE_DATA = 3030;
	// Socket连接所用UUID
	private static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
	// 数据接收缓存
	private byte[] mDataReceiveBuffer;
	// 本地蓝牙适配器
	private BluetoothAdapter mBluetoothAdapter;
	// 连接的远程设备
	private BluetoothDevice mBluetoothDevice;
	// Socket连接进程
	private ConnectDeviceThread mConnectDeviceThread;
	// 数据接收进程
	private DataReceiveThread mDataReceiveThread;

	// 外部注册事件
	private Method mMethod;
	// 外部注册的类
	private Object mObject;

	// 构造函数
	public BluetoothPortSocket()
	{
		// 初始化本地蓝牙适配器
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	}

	// 连接设备
	public void connectDevice(String address)
	{
		cancelConnectDevice();

		// 开启进程连接远程蓝牙设备
		mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
		mConnectDeviceThread = new ConnectDeviceThread(mBluetoothDevice);
		mConnectDeviceThread.start();
	}

	// 停止连接设备
	public void cancelConnectDevice()
	{
		// 先关闭原有正在进行的Socket连接
		if (mConnectDeviceThread != null)
		{
			mConnectDeviceThread.cancel();
			mConnectDeviceThread = null;
		}
	}

	// 发送数据
	public void sendData(byte[] buffer)
	{
		if(mDataReceiveThread == null)
			return;

		mDataReceiveThread.write(buffer);
	}

	// 发送数据
	public void sendData(String dataStr)
	{
		if (dataStr != null && !"".equals(dataStr))
		{
			byte[] tmp = null;
			switch (mCodeType)
			{
				case HEX:	// 十六进制
					try
					{
						String[] data = dataStr.split("\\s+");
						tmp = new byte[data.length];
						for (int i = 0; i < data.length; i++)
						{
							tmp[i] = (byte) Integer.parseInt(data[i], 16);
							Log.e("hht",Byte.toString(tmp[i]));
						}
					}
					catch (Exception e)
					{
						return;
					}
					break;
				case TEXT:	// 文本
					// 默认GBK编码
					try
					{
						tmp = dataStr.getBytes("GBK");
					}
					catch (UnsupportedEncodingException e)
					{
						// TODO 自动生成的 catch 块
						e.printStackTrace();
					}
					//tmp = dataStr.getBytes();
					break;
			}

			sendData(tmp);
		}
	}

	public void dataReceivedEvent(Object obj, String methodName)
	{
		mObject = obj;
		try
		{
			mMethod = obj.getClass().getDeclaredMethod(methodName, byte[].class);
		}
		catch (NoSuchMethodException e)
		{
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
	}

	// 打开蓝牙设备
	public void openBluetooth()
	{
		// 如果不支持蓝牙则直接退出
		if (mBluetoothAdapter == null)
		{
			return;
		}

		// 如果蓝牙没有打开，则提示打开
		if (!mBluetoothAdapter.isEnabled())
		{
			// 不做提示，强行打开
			mBluetoothAdapter.enable();
		}
	}

	// 本地蓝牙是否打开
	public boolean bluetoothIsEnabled()
	{
		// 如果不支持蓝牙则直接退出
		if (mBluetoothAdapter == null)
		{
			return false;
		}
		return  mBluetoothAdapter.isEnabled();
	}


	// 多线程异步显示接收数据
	private Handler mHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
				case MSG_RECEIVE_DATA:		// 接收到数据
					try
					{
						if(mObject != null)
						{
							mMethod.invoke(mObject, new Object[]{ mDataReceiveBuffer });
						}
					}
					catch (IllegalAccessException e)
					{
						// TODO 自动生成的 catch 块
						e.printStackTrace();
					}
					catch (IllegalArgumentException e)
					{
						// TODO 自动生成的 catch 块
						e.printStackTrace();
					}
					catch (InvocationTargetException e)
					{
						// TODO 自动生成的 catch 块
						e.printStackTrace();
					}
					break;
				default:
					break;
			}
		}
	};


	// 连接设备进程
	private class ConnectDeviceThread extends Thread
	{
		private final BluetoothSocket mmSocket;

		public ConnectDeviceThread(BluetoothDevice device)
		{
			// 与指定设备建立Socket连接
			BluetoothSocket tmp = null;
			try
			{
				tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID));
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			mmSocket = tmp;
		}

		public void run()
		{
			// 设置线程名
			setName("ConnectThread");

			// 注销搜索设备广播，以免减慢连接
			mBluetoothAdapter.cancelDiscovery();

			// Make a connection to the BluetoothSocket
			try
			{
				// Socket连接
				mmSocket.connect();
			}
			catch (IOException e)
			{
				// 关闭Socket连接
				try
				{
					mmSocket.close();
				}
				catch (IOException e2)
				{
					e2.printStackTrace();
				}
				return;
			}

			mConnectDeviceThread = null;

			// Socket连接成功后启动另一进程，并将连接Socket作为参数传入
			mDataReceiveThread = new DataReceiveThread(mmSocket);
			mDataReceiveThread.start();
		}

		public void cancel()
		{
			try
			{
				mmSocket.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	// Socket连接建立成功的数据接收进程
	private class DataReceiveThread extends Thread
	{
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public DataReceiveThread(BluetoothSocket socket)
		{
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the BluetoothSocket input and output streams
			try
			{
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run()
		{
			byte[] buffer = new byte[1024];
			int receivedByteCount;

			// 循环监听数据接收情况
			while (true)
			{
				try
				{
					// 处理接收数据
					receivedByteCount = mmInStream.read(buffer);

					if(!isPause)
					{
						mDataReceiveBuffer = new byte[receivedByteCount];
						synchronized (mDataReceiveBuffer)
						{
							// 将接收的数据添加到接收缓存中
							for (int i = 0; i < receivedByteCount; i++)
							{
								mDataReceiveBuffer[i] = buffer[i];
							}
						}

						// 触发外部数据接收消息
						mHandler.sendEmptyMessage(MSG_RECEIVE_DATA);
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
					break;
				}
			}
		}

		// 发送数据
		public void write(byte[] buffer)
		{
			try
			{
				mmOutStream.write(buffer);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	// 数据收发类型
	public enum DataType
	{
		HEX,	// 十六进制
		TEXT;	// 文本（默认GBK编码）
	};
}
