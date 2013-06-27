package com.PsichiX.RoboFighters;

import com.PsichiX.XenonCoreDroid.XeActivity;
import com.PsichiX.XenonCoreDroid.XeApplication;
import com.PsichiX.XenonCoreDroid.XePhoton;
import com.PsichiX.XenonCoreDroid.Framework.Utils.Utils;
import com.PsichiX.XenonCoreDroid.Framework.Graphics.Graphics;

public class MainActivity extends XeActivity
{
	public static XeApplication app;
	
	@Override
	public void onCreate(android.os.Bundle savedInstanceState) 
	{
		// setup application before running it
		XeApplication.SETUP_SOUND_STREAMS = 4;
		XeApplication.SETUP_WINDOW_HAS_TITLE = false;
		XeApplication.SETUP_WINDOW_FULLSCREEN = true;
		XeApplication.SETUP_SCREEN_ORIENTATION = XeApplication.Orientation.LANDSCAPE;
		
		// create application
		super.onCreate(savedInstanceState);
		// run state
		app = getApplicationCore();
		Utils.initModule(getApplicationCore().getAssets());
		Graphics.initModule(getApplicationCore().getAssets(), getApplicationCore().getPhoton());
		getApplicationCore().getTimer().setFixedStep(1000 / 30);
		getApplicationCore().getPhoton().getRenderer().getTimer().setFixedStep(1000 / 30);
		getApplicationCore().getPhoton().setRenderMode(XePhoton.RenderMode.QUEUE, true);
		getApplicationCore().getPhoton().getRenderer().setClearBackground(true, 0.0f, 0.0f, 0.0f, 1.0f);
		getApplicationCore().run(new GameState());
	}
}
