package com.PsichiX.RoboFighters;

import java.util.HashMap;

import android.annotation.SuppressLint;
import android.util.Log;

import com.PsichiX.RoboFighters.Gesture.MovementPath;
import com.PsichiX.XenonCoreDroid.XeApplication.*;
import com.PsichiX.XenonCoreDroid.Framework.Graphics.*;
import com.PsichiX.XenonCoreDroid.Framework.Actors.*;
import com.PsichiX.XenonCoreDroid.XePhoton.DrawMode;
import com.PsichiX.XenonCoreDroid.XeUtils.*;

public class GameState extends State implements CommandQueue.Delegate
{
	private static String name;
	private Camera2D _cam;
	private Scene _scn;
	private ActorsManager _actors = new ActorsManager(this);
	private CommandQueue _cmds = new CommandQueue();
	private Gesture _gesture = new Gesture();
	private Robot _me;
	private Robot _enemy;
	private Sprite _healthBar;
	@SuppressLint("UseSparseArrays")
	private HashMap<Integer, Shape> _traces = new HashMap<Integer, Shape>();
	private UDPBroadCastManager _net;
	private float _health = 1.0f;
	
	@Override
	public void onEnter()
	{
		name = Long.toString(System.currentTimeMillis());
		
		_cmds.setDelegate(this);
		_net = new UDPBroadCastManager(getApplication().getContext());
//		_net.start(_cmds, 0);
		_gesture.setReceiver(_cmds);
		
		_scn = (Scene)getApplication().getAssets().get(R.raw.scene, Scene.class);
		_cam = (Camera2D)_scn.getCamera();
		float w = _cam.getViewWidth();
		float h = _cam.getViewHeight();
		float hw = w * 0.5f;
		float hh = h * 0.5f;
		
		_me = new Robot();
		_me.setPosition(-hw * 0.5f, hh * 0.5f);
		_me.setTarget(_me.getPositionX(), _me.getPositionY());
		_scn.attach(_me);
		_actors.attach(_me);
		
		_enemy = new Robot();
		_enemy.setPosition(hw * 0.5f, hh * 0.5f);
		_enemy.setTarget(_enemy.getPositionX(), _enemy.getPositionY());
		_enemy.setScale(-1.0f, 1.0f);
		_scn.attach(_enemy);
		
		_healthBar = new Sprite((Material)MainActivity.app.getAssets().get(R.raw.mockup_material, Material.class));
		_healthBar.setSize(w, 40.0f);
		_healthBar.setPosition(-hw, -hh);
		_scn.attach(_healthBar);
		
		getApplication().getPhoton().getRenderer().setClearBackground(true, 1.0f, 1.0f, 1.0f, 1.0f);
	}
	
	@Override
	public void onExit()
	{
		_scn.releaseAll();
		_actors.detachAll();
		_net.release();
	}
	
	@Override
	public void onInput(Touches ev)
	{
		Touch t = ev.getTouchByState(Touch.State.DOWN);
		if(t != null)
		{
			float[] loc = _cam.convertLocationScreenToWorld(t.getX(), t.getY(), -1.0f);
			boolean cn = _me.contains(loc[0], loc[1]);
			loc = MathHelper.convertLocationUiToScreen(getApplication().getPhoton().getView(), t.getX(), t.getY());
			int[] grid = convertLocationScreenToGrid(loc[0], loc[1]);
			_gesture.start(t.getId(), grid[0], grid[1], cn ? _me : null);
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
		
//		Xml xml = new Xml();
//		Xml.Element elm = xml.getRoot().add("State");
//		elm.add("name", name);
//		elm.add("x", Integer.toString(Float.floatToRawIntBits(_me.getPositionX())));
//		elm.add("y", Integer.toString(Float.floatToRawIntBits(_me.getPositionY())));
//		_net.sendMessage(xml.toString().getBytes());
//		Log.d("SENT", "name: " + name + "; x: " + _me.getPositionX() + "; y: " + _me.getPositionY());
	}
	
	public void onCommand(Object sender, String cmd, Object data)
	{
		if(cmd.equals("Health") && data instanceof Float)
		{
			_health = (Float)data;
			_healthBar.setSize(_cam.getViewWidth() * _health, 40.0f);
			if(_health <= 0.0f)
				Message.alert(getApplication().getContext(), "Enemy killed", "Enemy killed by you!", "OK", null);
		}
		if(cmd.equals("ReceiveBuffer") && data instanceof byte[])
		{
			String txt = new String((byte[])data);
			Log.d("MESSAGE", txt);
			Xml xml = new Xml();
			xml.parse(txt, "UTF-8");
			String sn = xml.getValue("State", "name");
			String sx = xml.getValue("State", "x");
			String sy = xml.getValue("State", "y");
			float px = Float.intBitsToFloat(Integer.valueOf(sx));
			float py = Float.intBitsToFloat(Integer.valueOf(sy));
			Log.d("RECEIVED", "name: " + sn + "; x: " + px + "; y: " + py);
			if(!sn.equals(name))
				_enemy.setPosition(px, py);
		}
		if(cmd.equals("MovementPath") && data instanceof Gesture.MovementPath)
		{
			Gesture.MovementPath path = (Gesture.MovementPath)data;
			// recognition
			recognizeGesture(path);
			// lines
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
			trace.setVertexArray("vPosition", getApplication().getPhoton().createVertexArray(2, vc, vd));
			_scn.attach(trace);
			_traces.put(path.getId(), trace);
		}
	}
	
	private void recognizeGesture(MovementPath path)
	{
		int[] f = path.getPath().get(0);
		int[] e = path.getPath().get(path.getPath().size() - 1);
		int minxo = (f[0] + e[0]) / 2;
		int minyo = (f[1] + e[1]) / 2;
		int maxxo = minxo;
		int maxyo = minyo;
		int[] cr = null;
		for(int i = 0; i < path.getPath().size(); i++)
		{
			cr = path.getPath().get(i);
			minxo = Math.min(minxo, cr[0]);
			minyo = Math.min(minyo, cr[1]);
			maxxo = Math.max(maxxo, cr[0]);
			maxyo = Math.max(maxyo, cr[1]);
		}
		int w = (maxxo - minxo) + 1;
		int h = (maxyo - minyo) + 1;
		int itx = w / 4;
		int ity = h / 4;
		int minxi = minxo + itx;
		int minyi = minyo + ity;
		int maxxi = maxxo - itx;
		int maxyi = maxyo - ity;
		int cx = (minxo + maxxo) / 2;
		int cy = (minyo + maxyo) / 2;
		boolean fif = isPointInsideFrame(f[0], f[1], minxo, minyo, maxxo, maxyo, minxi, minyi, maxxi, maxyi);
		boolean lif = isPointInsideFrame(e[0], e[1], minxo, minyo, maxxo, maxyo, minxi, minyi, maxxi, maxyi);
		boolean aii = false;
		for(int i = 0; i < path.getPath().size(); i++)
		{
			cr = path.getPath().get(i);
			if(!isPointInsideFrame(cr[0], cr[1], minxo, minyo, maxxo, maxyo, minxi, minyi, maxxi, maxyi))
			{
				aii = true;
				break;
			}
		}
		if(fif && lif && aii)
			onGestureLine(path, f[0], f[1], e[0], e[1]);
		else if(fif && lif && !aii)
			onGestureCircle(path, w, h, cx, cy);
		else
			onGestureUndefined(path);
	}
	
	private void onGestureUndefined(MovementPath path)
	{
		Log.d("GESTURE", "UNDEFINED");
	}

	private void onGestureCircle(MovementPath path, int w, int h, int cx, int cy)
	{
		Log.d("GESTURE", "CIRCLE - w: " + w + "; h: " + h + "; cx: " + cx + "; cy: " + cy);
		float[] loc = convertLocationGridToScreen(cx, cy);
		loc = _cam.convertLocationScreenToWorld(loc[0], loc[1], -1.0f);
		if(_enemy.contains(loc[0], loc[1]))
			_cmds.queueCommand(this, "Health", new Float(_health - 0.25f));
	}

	private void onGestureLine(MovementPath path, int sx, int sy, int ex, int ey)
	{
		Log.d("GESTURE", "LINE - sx: " + sx + "; sy: " + sy + "; ex: " + ex + "; ey: " + ey);
		if(path.getUserData() == _me)
		{
			float[] loc = convertLocationGridToScreen(ex, ey);
			loc = _cam.convertLocationScreenToWorld(loc[0], loc[1], -1.0f);
			_me.setTarget(loc[0], loc[1]);
		}
	}

	private boolean isPointInsideFrame(int x, int y, int minxo, int minyo, int maxxo, int maxyo, int minxi, int minyi, int maxxi, int maxyi)
	{
		boolean insideOuter = x >= minxo && x <= maxxo && y >= minyo && y <= maxyo;
		boolean insideInner = x >= minxi && x <= maxxi && y >= minyi && y <= maxyi;
		return insideOuter && !insideInner;
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
