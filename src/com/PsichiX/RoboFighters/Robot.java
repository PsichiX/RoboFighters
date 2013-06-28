package com.PsichiX.RoboFighters;

import com.PsichiX.XenonCoreDroid.XeUtils.MathHelper;
import com.PsichiX.XenonCoreDroid.Framework.Actors.ActorSprite;
import com.PsichiX.XenonCoreDroid.Framework.Graphics.Image;
import com.PsichiX.XenonCoreDroid.Framework.Graphics.Material;

public class Robot extends ActorSprite
{
	private float tx = 0.0f;
	private float ty = 0.0f;
	private float _spd = 750.0f;
	
	public Robot()
	{
		super((Material)MainActivity.app.getAssets().get(R.raw.robot_material, Material.class));
		setSizeFromImage((Image)MainActivity.app.getAssets().get(R.drawable.robot, Image.class));
		setOffsetFromSize(0.5f, 0.5f);
	}	
	
	@Override
	public void onUpdate(float dt)
	{
		float len = MathHelper.vecLength(tx - _x, ty - _y, 0.0f);
		if(len > dt * _spd)
		{
			float[] norm = MathHelper.vecNormalize(tx - _x, ty - _y, 0.0f);
			setPosition(_x + dt * _spd * norm[0], _y + dt * _spd * norm[1]);
		}
		else
			setPosition(tx, ty);
	}
	
	public void setTarget(float x, float y)
	{
		tx = x;
		ty = y;
	}
	
	public boolean contains(float x, float y)
	{
		float minx = _x - getOffsetX();
		float miny = _y - getOffsetY();
		float maxx = minx + getWidth();
		float maxy = miny + getHeight();
		return x >= minx && y >= miny && x <= maxx && y <= maxy;
	}
}
