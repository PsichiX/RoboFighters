package com.PsichiX.RoboFighters;

import android.annotation.SuppressLint;
import com.PsichiX.XenonCoreDroid.XeUtils.*;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.Iterator;

public class Gesture
{
	private CommandQueue _rcv;
	@SuppressLint("UseSparseArrays")
	private HashMap<Integer, Instance> _instances = new HashMap<Integer, Instance>();
	private HashMap<String, GridPattern> _grids = new HashMap<String, GridPattern>();
	private int _max = -1;
	
	public void setMaxInstances(int c)
	{
		_max = c;
	}
	
	public void setReceiver(CommandQueue cmds)
	{
		_rcv = cmds;
	}
	
	public GridPattern gridPattern(String id)
	{
		if(_grids.containsKey(id))
			return _grids.get(id);
		GridPattern grid = new GridPattern();
		_grids.put(id, grid);
		return grid;
	}
	
	public void removeGridPattern(String id)
	{
		if(_grids.containsKey(id))
			_grids.remove(id);
	}
	
	public void clearGridPatterns()
	{
		_grids.clear();
	}
	
	public boolean exists(int id)
	{
		return _instances.containsKey(id);
	}
	
	public void validateOnPath(int id)
	{
		if(!_instances.containsKey(id))
			return;
		Instance inst = _instances.get(id);
		ArrayList<int[]> src = inst.get();
		if(src.size() <= 0)
			return;
		ArrayList<int[]> dst = new ArrayList<int[]>();
		for(int i = 0; i < src.size(); i++)
			dst.add(src.get(i));
		_rcv.queueCommand(this, "MovementPath", new MovementPath(id, inst.getStartX(), inst.getStartY(), dst, inst.getUserData()));
	}
	
	public void validateOnGrid(int id)
	{
		if(!_instances.containsKey(id))
			return;
		Instance inst = _instances.get(id);
		if(_rcv != null)
		{
			Iterator<Entry<String, GridPattern>> it = _grids.entrySet().iterator();
			while(it.hasNext())
			{
				Entry<String, GridPattern> en = it.next();
				if(en.getValue().validate(inst.getStartX(), inst.getStartY(), inst.get()))
					_rcv.queueCommand(this, "ValidGridPattern", new String(en.getKey()));
			}
		}
	}
	
	public void start(int id, int x, int y)
	{
		start(id, x, y, null);
	}
	
	public void start(int id, int x, int y, Object userData)
	{
		if((_max < 0 || _instances.size() < _max) && !_instances.containsKey(id))
			_instances.put(id, new Instance(x, y, userData));
	}
	
	public void moveTo(int id, int x, int y)
	{
		if(!_instances.containsKey(id))
			return;
		int[] pos = _instances.get(id).getLast(); 
		if(pos != null && x == pos[0] && y == pos[1])
			return;
		_instances.get(id).add(x, y);
	}
	
	public void stop(int id)
	{
		if(_instances.containsKey(id))
			_instances.remove(id);
	}
	
	public void stopAll()
	{
		_instances.clear();
	}
	
	public void truncate(int id)
	{
		if(!_instances.containsKey(id))
			return;
		_instances.get(id).truncate();
	}
	
	public void truncateAll()
	{
		for(Instance i : _instances.values())
			i.truncate();
	}
	
	private class Instance
	{
		private ArrayList<int[]> _pos = new ArrayList<int[]>();
		private int _startX = 0;
		private int _startY = 0;
		private Object _userData;
		
		public Instance(int x, int y, Object userData)
		{
			_startX = x;
			_startY = y;
			_userData = userData;
		}
		
		public void truncate()
		{
			if(_pos.size() <= 1)
				return;
			int[] t = _pos.get(_pos.size() - 1);
			_pos.clear();
			_pos.add(t);
		}
		
		public void add(int x, int y)
		{
			_pos.add(new int[]{ x, y });
		}
		
		public ArrayList<int[]> get()
		{
			return _pos;
		}
		
		public int getStartX()
		{
			return _startX;
		}
		
		public int getStartY()
		{
			return _startY;
		}
		
		public int[] getLast()
		{
			return _pos.size() > 0 ? _pos.get(_pos.size() - 1) : null;
		}
		
		public Object getUserData()
		{
			return _userData;
		}
	}
	
	public class MovementPath
	{
		private int _id = -1;
		private ArrayList<int[]> _path;
		private int _startX = 0;
		private int _startY = 0;
		private Object _userData;
		
		protected MovementPath(int id, int startX, int startY, ArrayList<int[]> path, Object userData)
		{
			_id = id;
			_startX = startX;
			_startY = startY;
			_path = path;
			_userData = userData;
		}
		
		public int getId()
		{
			return _id;
		}
		
		public int getStartX()
		{
			return _startX;
		}
		
		public int getStartY()
		{
			return _startY;
		}
		
		public ArrayList<int[]> getPath()
		{
			return _path;
		}
		
		public Object getUserData()
		{
			return _userData;
		}
	}
	
	public class GridPattern
	{
		private ArrayList<int[]> _pos = new ArrayList<int[]>();
		
		public boolean validate(int sx, int sy, ArrayList<int[]> moves)
		{
			if(_pos.size() == 0 || moves == null)
				return false;
			int step = 0;
			int[] pos = null;
			for(int[] p : moves)
			{
				int x = p[0] - sx;
				int y = p[1] - sy;
				pos = _pos.get(step);
				if(x == pos[0] && y == pos[1])
				{
					step++;
					if(step >= _pos.size())
						return true;
				}
			}
			return false;
		}
		
		public void add(int x, int y)
		{
			_pos.add(new int[]{ x, y });
		}
		
		public ArrayList<int[]> get()
		{
			return _pos;
		}
	}
}
