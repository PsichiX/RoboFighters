package com.PsichiX.RoboFighters;

import java.util.HashMap;

import android.annotation.SuppressLint;

import com.PsichiX.XenonCoreDroid.XeApplication.*;
import com.PsichiX.XenonCoreDroid.Framework.Graphics.*;
import com.PsichiX.XenonCoreDroid.Framework.Actors.*;
import com.PsichiX.XenonCoreDroid.XePhoton.DrawMode;
import com.PsichiX.XenonCoreDroid.XeUtils.*;

public class GameState extends State implements CommandQueue.Delegate
{
	private Camera2D _cam;
	private Scene _scn;
	private ActorsManager _actors = new ActorsManager(this);
	private CommandQueue _cmds = new CommandQueue();
	private Gesture _gesture = new Gesture();
	private Robot _me;
	@SuppressLint("UseSparseArrays")
	private HashMap<Integer, Shape> _traces = new HashMap<Integer, Shape>();
	
	@Override
	public void onEnter()
	{
		_cmds.setDelegate(this);
		_gesture.setReceiver(_cmds);
		
		_scn = (Scene)getApplication().getAssets().get(R.raw.scene, Scene.class);
		_cam = (Camera2D)_scn.getCamera();
		
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
			int[] grid = convertLocationScreenToGrid(loc[0], loc[1]);
			_gesture.start(t.getId(), grid[0], grid[1]);
		}
		t = ev.getTouchByState(Touch.State.IDLE);
		if(t != null)
		{
			float[] loc = MathHelper.convertLocationUiToScreen(getApplication().getPhoton().getView(), t.getX(), t.getY());
			int[] grid = convertLocationScreenToGrid(loc[0], loc[1]);
			_gesture.moveTo(t.getId(), grid[0], grid[1]);
		}
		t = ev.getTouchByState(Touch.State.UP);
		if(t != null)
		{
//			float[] loc = MathHelper.convertLocationUiToScreen(getApplication().getPhoton().getView(), t.getX(), t.getY());
//			int[] grid = convertLocationScreenToGrid(loc[0], loc[1]);
//			_gesture.moveTo(t.getId(), grid[0], grid[1]);
			_gesture.validateOnPath(t.getId());
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
		if(cmd.equals("MovementPath") && data instanceof Gesture.MovementPath)
		{
			Gesture.MovementPath path = (Gesture.MovementPath)data;
			Shape trace = _traces.containsKey(path.getId()) ? _traces.get(path.getId()) : null;
			if(trace != null)
				_scn.detach(trace);
			trace = new Shape();
			trace.setDrawMode(DrawMode.LINES);
			trace.setMaterial((Material)getApplication().getAssets().get(R.raw.mockup_material, Material.class));
			int vc = path.getPath().size() * 2;
			float[] vd = new float[vc * 2];
			float[] loc = convertLocationGridToScreen(path.getStartX(), path.getStartY());
			loc = _cam.convertLocationScreenToWorld(loc[0], loc[1], -1.0f);
			int index = 0;
			for(int i = 0; i < path.getPath().size(); i++)
			{
				vd[index++] = loc[0];
				vd[index++] = loc[1];
				int[] pos = path.getPath().get(i);
				loc = convertLocationGridToScreen(pos[0], pos[1]);
				loc = _cam.convertLocationScreenToWorld(loc[0], loc[1], -1.0f);
				vd[index++] = loc[0];
				vd[index++] = loc[1];
			}
			/*ArrayList<float[]> bezier = new ArrayList<float[]>();
			float[] loc = convertLocationGridToScreen(path.getStartX(), path.getStartY());
			loc = _cam.convertLocationScreenToWorld(loc[0], loc[1], -1.0f);
			bezier.add(loc);
			for(int i = 0; i < path.getPath().size(); i++)
			{
				int[] pos = path.getPath().get(i);
				loc = convertLocationGridToScreen(pos[0], pos[1]);
				loc = _cam.convertLocationScreenToWorld(loc[0], loc[1], -1.0f);
				bezier.add(loc);
			}
			loc = new float[]{
				MathHelper.vecListBezier(bezier, 0.0f, 0),
				MathHelper.vecListBezier(bezier, 0.0f, 1)
			};
			int index = 0;
			float dt = 1.0f / (float)path.getPath().size();
			float t = dt;
			for(int i = 0; i < path.getPath().size(); i++)
			{
				vd[index++] = loc[0];
				vd[index++] = loc[1];
				loc = new float[]{
					MathHelper.vecListBezier(bezier, t, 0),
					MathHelper.vecListBezier(bezier, t, 1)
				};
				vd[index++] = loc[0];
				vd[index++] = loc[1];
				t += dt;
			}*/
			trace.setVertexArray("vPosition", getApplication().getPhoton().createVertexArray(2, vc, vd));
			_scn.attach(trace);
			_traces.put(path.getId(), trace);
		}
	}
	
	private int[] convertLocationScreenToGrid(float x, float y)
	{
		return new int[]{
				(int)Math.round(x * 10.0f * getApplication().getPhoton().getRenderer().getScreenAspect()),
				(int)Math.round(y * 10.0f)
		};
	}
	
	private float[] convertLocationGridToScreen(int x, int y)
	{
		float[] loc =  new float[]{
				(float)(x / getApplication().getPhoton().getRenderer().getScreenAspect() * 0.1f),
				(float)(y * 0.1f)
		};
		return MathHelper.convertLocationScreenToUi(getApplication().getPhoton().getView(), loc[0], loc[1]);
	}
}
