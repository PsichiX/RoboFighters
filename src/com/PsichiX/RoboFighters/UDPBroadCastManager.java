package com.PsichiX.RoboFighters;

import com.PsichiX.XenonCoreDroid.XeUtils.*;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.ByteArrayOutputStream;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.util.Log;

public class UDPBroadCastManager implements BroadcastManagerInterface
{
	private static final String TAG = UDPBroadCastManager.class.getName();
	private static int PORT = 14444;

	private DatagramSocket _socket;
	private final Context _context;
	private ReceivingThread _threadReceiving;
	private final InetAddress _address;
	
	public UDPBroadCastManager(Context context)
	{
		_context = context;
		try
		{
			Log.v(TAG, "Opening datagram socket");
			_socket = new DatagramSocket(PORT);
			_socket.setSoTimeout(200);
			_address = getBroadcastAddress(_context);
		}
		catch (Exception e)
		{
			Log.e(TAG, e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}
	
	protected void finalize() throws Throwable
	{
		release();
	}
	
	public void release()
	{
		stop();
	}

	private static InetAddress getBroadcastAddress(Context context) throws IOException
	{
		WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		MulticastLock ml = wifi.createMulticastLock("some tag");
		ml.acquire();
		DhcpInfo dhcp = wifi.getDhcpInfo();
		if(dhcp == null)
			return null;
		int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
		byte[] quads = new byte[4];
		for(int k = 0; k < 4; k++)
			quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
		return InetAddress.getByAddress(quads);
	}
	
	public void sendMessage(byte[] data)
	{
//		if(_threadSending != null)
//			_threadSending.sendMessage(data);
		sendBroadcastBuffer(data);
	}
	
	public void sendMessage(ByteArrayOutputStream stream)
	{
//		if(_threadSending != null)
//			_threadSending.sendMessage(stream);
		sendBroadcastStream(stream);
	}
	
	private void sendBroadcastStream(ByteArrayOutputStream stream)
	{
		if(stream != null)
			sendBroadcastBuffer(stream.toByteArray());
	}
	
	private void sendBroadcastBuffer(byte[] data)
	{
//		try
//		{
//			_socket.setBroadcast(true);
//			// TODO: is better to get broadcast address somewhere else
//			DatagramPacket packet = new DatagramPacket(data, data.length, getBroadcastAddress(_context), PORT);
//			_socket.send(packet);
//		}
//		catch (IOException e)
//		{
//			Log.e(TAG, e.getMessage(), e);
//		}
		final byte[] fdata = data;
		new Thread()
		{
			public void run()
			{
				try
				{
					_socket.setBroadcast(true);
					// TODO: is better to get broadcast address somewhere else
					DatagramPacket packet = new DatagramPacket(fdata, fdata.length, _address, PORT);
					_socket.send(packet);
				}
				catch (Exception e)
				{
					Log.e(TAG, e.getMessage(), e);
				}
			}
		}.start();
	}
	
//	private ByteArrayInputStream receiveBroadcastStream(int bufflength)
//	{
//		byte[] b = receiveBroadcastBuffer(bufflength, false);
//		if(b != null)
//		{
//			ByteArrayInputStream s = new ByteArrayInputStream(b);
//			return s;
//		}
//		return null;
//	}
	
	private byte[] receiveBroadcastBuffer(int bufflength)
	{
		bufflength = Math.max(0, bufflength);
		byte[] buf = new byte[bufflength];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		try
		{
			_socket.receive(packet);
		}
		catch (InterruptedIOException ioe)
		{
			Log.v(TAG, "Timeout");
			return null;
		}
		catch (IOException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		int len = packet.getLength();
		byte [] newArray = new byte[len];
		byte [] oldArray = packet.getData();
		System.arraycopy(oldArray, 0, newArray, 0, len);
		return newArray;
	}

	public void destroy()
	{
		_socket.close();
	}
	
	public void onCommand(Object sender, String cmd, Object data)
	{
		if(sender == this)
		{
			if(cmd.equals("SendBuffer") && data instanceof byte[])
				sendBroadcastBuffer((byte[])data);
			else if(cmd.equals("SendStream") && data instanceof ByteArrayOutputStream)
				sendBroadcastStream((ByteArrayOutputStream)data);
		}
	}
	
	public void start(CommandQueue rcv, long receivingInterval)
	{
		stop();
		_threadReceiving = new ReceivingThread(this, rcv, receivingInterval);
		_threadReceiving.start();
	}
	
	public void stop()
	{
		if(_threadReceiving != null)
			_threadReceiving.die();
	}
	
	private class ReceivingThread extends Thread
	{
		private volatile boolean _running = false;
		private final UDPBroadCastManager _owner;
		private final CommandQueue _receiver;
		private final long _interval;
		
		public ReceivingThread(UDPBroadCastManager o, CommandQueue rcv, long interval)
		{
			super();
			_owner = o;
			_receiver = rcv;
			_interval = interval;
		}
		
		public void run()
		{
			_running = true;
			while(_running)
			{
				byte[] b = _owner.receiveBroadcastBuffer(1024);
				_receiver.queueCommand(_owner, "ReceiveBuffer", b);
				if(_interval > 0)
				{
					try {
						Thread.sleep(_interval);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		public void die()
		{
			_running = false;
		}
	}
}
