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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.instancemanager.ItemsOnGroundManager;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.type.EtcItemType;

public class ItemsAutoDestroyTaskManager
{
	protected final List<ItemInstance> _items = new CopyOnWriteArrayList<>();
	
	protected ItemsAutoDestroyTaskManager()
	{
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new CheckItemsForDestroy(), 5000, 5000);
	}
	
	public static ItemsAutoDestroyTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public void addItem(ItemInstance item)
	{
		item.setDropTime(System.currentTimeMillis());
		_items.add(item);
	}
	
	protected class CheckItemsForDestroy extends Thread
	{
		@Override
		public void run()
		{
			if (_items.isEmpty())
				return;
			
			final long curtime = System.currentTimeMillis();
			for (ItemInstance item : _items)
			{
				if (item == null || item.getDropTime() == 0 || item.getLocation() != ItemInstance.ItemLocation.VOID)
					_items.remove(item);
				else
				{
					if ((item.getItemType() == EtcItemType.HERB && (curtime - item.getDropTime()) > Config.HERB_AUTO_DESTROY_TIME) || (item.getItemType() != EtcItemType.HERB && (curtime - item.getDropTime()) > Config.ITEM_AUTO_DESTROY_TIME))
					{
						L2World.getInstance().removeVisibleObject(item, item.getWorldRegion());
						L2World.getInstance().removeObject(item);
						_items.remove(item);
						
						if (Config.SAVE_DROPPED_ITEM)
							ItemsOnGroundManager.getInstance().removeObject(item);
					}
				}
			}
		}
	}
	
	private static class SingletonHolder
	{
		protected static final ItemsAutoDestroyTaskManager _instance = new ItemsAutoDestroyTaskManager();
	}
}