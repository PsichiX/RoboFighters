package com.PsichiX.RoboFighters;

public interface BroadcastManagerInterface
{
	public abstract void destroy();
	public abstract void sendBroadcast(byte[] data);
	public abstract byte[] receiveBroadcast(int bufflength);
}