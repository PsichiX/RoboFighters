package com.PsichiX.RoboFighters;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.LinkedList;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.util.Log;

public class UDPBroadCastManager implements BroadcastManagerInterface
{
	public static interface ChainedReceiver
	{
		public void onMessageReceived(byte[] msg);
	}

	private static final String TAG = UDPBroadCastManager.class.getName();
	private static int PORT = 14444;

	private DatagramSocket _socket;
	private Context _context;
	private LinkedList<ChainedReceiver> _chainedReceiverList = new LinkedList<ChainedReceiver>();

	public UDPBroadCastManager(Context context)
	{
		_context = context;
		try
		{
			Log.v(TAG, "Opening datagram socket");
			_socket = new DatagramSocket(PORT);
			_socket.setSoTimeout(200);
		}
		catch (SocketException e)
		{
			Log.e(TAG, e.getMessage(), e);
			throw new RuntimeException(e);
		}
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

	/*public void sendBroadcast(byte[] data)
	{
		final Context fcontext = _context;
		final byte[] fdata = data;
		new Thread()
		{
			public void run()
			{
				try
				{
					_socket.setBroadcast(true);
					// TODO: is better to get broadcast address somewhere else
					DatagramPacket packet = new DatagramPacket(fdata, fdata.length, getBroadcastAddress(fcontext), PORT);
					_socket.send(packet);
				}
				catch (IOException e)
				{
					Log.e(TAG, e.getMessage(), e);
				}
			}
		}.start();
	}*/
	
	public void sendBroadcast(byte[] data)
	{
		try
		{
			_socket.setBroadcast(true);
			// TODO: is better to get broadcast address somewhere else
			DatagramPacket packet = new DatagramPacket(data, data.length, getBroadcastAddress(_context), PORT);
			_socket.send(packet);
		}
		catch (IOException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
	}

	public byte[] receiveBroadcast(int bufflength)
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
		for (ChainedReceiver receiver : _chainedReceiverList)
			receiver.onMessageReceived(newArray);
		return newArray;
	}

	public void destroy()
	{
		_socket.close();
	}

	public void addChainedReceiver(ChainedReceiver receiver)
	{
		if(!_chainedReceiverList.contains(receiver))
			_chainedReceiverList.add(receiver);
	}
	
	public void removeChainedReceiver(ChainedReceiver receiver)
	{
		if(_chainedReceiverList.contains(receiver))
			_chainedReceiverList.remove(receiver);
	}
}
