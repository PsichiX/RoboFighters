package com.PsichiX.RoboFighters;

import com.PsichiX.XenonCoreDroid.Framework.Actors.ActorSprite;
import com.PsichiX.XenonCoreDroid.Framework.Graphics.Material;

public class Robot extends ActorSprite
{
	public Robot()
	{
		super((Material)MainActivity.app.getAssets().get(R.raw.mockup_material, Material.class));
		setSize(20.0f, 60.0f);
		setOffsetFromSize(0.5f, 0.5f);
	}	
}
