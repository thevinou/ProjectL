/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.taskmanager;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.L2WorldRegion;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;

public class KnownListUpdateTaskManager
{
	protected static final Logger _log = Logger.getLogger(KnownListUpdateTaskManager.class.getName());
	
	private static final int FULL_UPDATE_TIMER = 100;
	
	protected boolean updatePass = true;
	protected int _fullUpdateTimer = FULL_UPDATE_TIMER;
	
	protected KnownListUpdateTaskManager()
	{
		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new KnownListUpdate(), 1000, Config.KNOWNLIST_UPDATE_INTERVAL);
	}
	
	public static KnownListUpdateTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private class KnownListUpdate implements Runnable
	{
		public KnownListUpdate()
		{
		}
		
		@Override
		public void run()
		{
			boolean fullUpdate = (_fullUpdateTimer == FULL_UPDATE_TIMER);
			for (L2WorldRegion regions[] : L2World.getInstance().getAllWorldRegions())
			{
				for (L2WorldRegion r : regions) // go through all world regions
				{
					if (!r.isActive()) // and check only if the region is active
						continue;
					
					for (L2Object object : r.getVisibleObjects().values())
					{
						if (!(object instanceof L2Character) || !object.isVisible())
							continue; // skip dying objects, don't busy about objects lower than L2Character.
							
						// Mobs need faster knownlist update.
						final boolean needFastUpdate = object instanceof L2Attackable;
						fullUpdate = fullUpdate || object instanceof L2Playable;
						
						if (updatePass)
							object.getKnownList().forgetObjects(fullUpdate || needFastUpdate);
						else
						{
							for (L2WorldRegion regi : r.getSurroundingRegions())
							{
								if (fullUpdate || (needFastUpdate && regi.isActive()))
								{
									for (L2Object o : regi.getVisibleObjects().values())
									{
										if (o != object)
											object.getKnownList().addKnownObject(o);
									}
								}
							}
						}
					}
				}
			}
			updatePass = !updatePass;
			
			if (_fullUpdateTimer > 0)
				_fullUpdateTimer--;
			else
				_fullUpdateTimer = FULL_UPDATE_TIMER;
		}
	}
	
	private static class SingletonHolder
	{
		protected static final KnownListUpdateTaskManager _instance = new KnownListUpdateTaskManager();
	}
}