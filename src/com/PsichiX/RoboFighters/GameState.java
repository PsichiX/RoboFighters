package com.PsichiX.RoboFighters;

import com.PsichiX.XenonCoreDroid.XeApplication.*;
import com.PsichiX.XenonCoreDroid.Framework.Graphics.*;
import com.PsichiX.XenonCoreDroid.Framework.Actors.*;
import com.PsichiX.XenonCoreDroid.XeUtils.*;

public class GameState extends State implements CommandQueue.Delegate
{
//	private Camera2D _cam;
	private Scene _scn;
	private ActorsManager _actors = new ActorsManager(this);
	private CommandQueue _cmds = new CommandQueue();
	private Gesture _gesture = new Gesture();
	private Robot _me;
	
	@Override
	public void onEnter()
	{
		_cmds.setDelegate(this);
		
		_scn = (Scene)getApplication().getAssets().get(R.raw.scene, Scene.class);
//		_cam = (Camera2D)_scn.getCamera();
		
		_me = new Robot();
		_me.setPosition(0.0f, 0.0f);
		_scn.attach(_me);
		
		getApplication().getPhoton().getRenderer().setClearBackground(true, 1.0f, 1.0f, 1.0f, 1.0f);
	}
	
	@Override
	public void onExit()
	{
		_scn.releaseAll();
		_actors.detachAll();
	}
	
	@Override
	public void onInput(Touches ev)
	{
		Touch t = ev.getTouchByState(Touch.State.DOWN);
		if(t != null)
		{
			float[] loc = MathHelper.convertLocationUiToScreen(getApplication().getPhoton().getView(), t.getX(), t.getY());
			int x = (int)(loc[0] * 10.0f);
			int y = (int)(loc[1] * 10.0f);
			_gesture.start(t.getId(), x, y);
		}
		t = ev.getTouchByState(Touch.State.IDLE);
		if(t != null)
		{
			float[] loc = MathHelper.convertLocationUiToScreen(getApplication().getPhoton().getView(), t.getX(), t.getY());
			int x = (int)(loc[0] * 10.0f);
			int y = (int)(loc[1] * 10.0f);
			_gesture.moveTo(t.getId(), x, y);
		}
		t = ev.getTouchByState(Touch.State.UP);
		if(t != null)
		{
			float[] loc = MathHelper.convertLocationUiToScreen(getApplication().getPhoton().getView(), t.getX(), t.getY());
			int x = (int)(loc[0] * 10.0f);
			int y = (int)(loc[1] * 10.0f);
			_gesture.moveTo(t.getId(), x, y);
			_gesture.stop(t.getId());
		}
		_actors.onInput(ev);
	}
	
	@Override
	public void onUpdate()
	{
		getApplication().getSense().setCoordsOrientation(-1);
		
//		float dt = getApplication().getTimer().getDeltaTime() / 1000.0f;
		float dt = 1.0f / 30.0f;
		
		_cmds.run();
		_actors.onUpdate(dt);
		_scn.update(dt);
	}
	
	public void onCommand(Object sender, String cmd, Object data)
	{
	}
	
}
