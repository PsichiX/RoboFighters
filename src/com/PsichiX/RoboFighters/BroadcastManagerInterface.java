package com.PsichiX.RoboFighters;

import java.io.ByteArrayOutputStream;

public interface BroadcastManagerInterface
{
	public void destroy();
	public void sendMessage(byte[] data);
	public void sendMessage(ByteArrayOutputStream stream);
}