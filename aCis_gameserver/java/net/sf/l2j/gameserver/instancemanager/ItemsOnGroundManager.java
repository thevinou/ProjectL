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
package net.sf.l2j.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.type.EtcItemType;
import net.sf.l2j.gameserver.taskmanager.ItemsAutoDestroyTaskManager;

/**
 * This class manage all items on ground
 * @author DiezelMax - original idea
 * @author Enforcer - actual build
 */
public class ItemsOnGroundManager
{
	static final Logger _log = Logger.getLogger(ItemsOnGroundManager.class.getName());
	
	protected List<ItemInstance> _items = null;
	private final StoreInDb _task = new StoreInDb();
	
	protected ItemsOnGroundManager()
	{
		if (Config.SAVE_DROPPED_ITEM)
			_items = new ArrayList<>();
		
		if (Config.SAVE_DROPPED_ITEM_INTERVAL > 0)
			ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(_task, Config.SAVE_DROPPED_ITEM_INTERVAL, Config.SAVE_DROPPED_ITEM_INTERVAL);
		
		load();
	}
	
	public static final ItemsOnGroundManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private void load()
	{
		// If SaveDroppedItem is false, may want to delete all items previously stored to avoid add old items on reactivate
		if (!Config.SAVE_DROPPED_ITEM && Config.CLEAR_DROPPED_ITEM_TABLE)
			emptyTable();
		
		if (!Config.SAVE_DROPPED_ITEM)
			return;
		
		// if DestroyPlayerDroppedItem was previously false, items curently protected will be added to ItemsAutoDestroy
		if (Config.DESTROY_DROPPED_PLAYER_ITEM)
		{
			try (Connection con = L2DatabaseFactory.getInstance().getConnection())
			{
				String str = null;
				if (!Config.DESTROY_EQUIPABLE_PLAYER_ITEM) // Recycle misc. items only
					str = "UPDATE itemsonground SET drop_time = ? WHERE drop_time = -1 AND equipable = 0";
				else if (Config.DESTROY_EQUIPABLE_PLAYER_ITEM) // Recycle all items including equipable
					str = "UPDATE itemsonground SET drop_time = ? WHERE drop_time = -1";
				
				PreparedStatement statement = con.prepareStatement(str);
				statement.setLong(1, System.currentTimeMillis());
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error while updating table ItemsOnGround " + e.getMessage(), e);
			}
		}
		
		// Add items to world
		ItemInstance item;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			Statement s = con.createStatement();
			ResultSet result;
			
			int count = 0;
			result = s.executeQuery("SELECT object_id,item_id,count,enchant_level,x,y,z,drop_time,equipable FROM itemsonground");
			
			while (result.next())
			{
				item = new ItemInstance(result.getInt(1), result.getInt(2));
				L2World.getInstance().storeObject(item);
				
				if (item.isStackable() && result.getInt(3) > 1) // this check and..
					item.setCount(result.getInt(3));
				
				if (result.getInt(4) > 0) // this, are really necessary?
					item.setEnchantLevel(result.getInt(4));
				
				item.getPosition().setWorldPosition(result.getInt(5), result.getInt(6), result.getInt(7));
				item.getPosition().setWorldRegion(L2World.getInstance().getRegion(item.getPosition().getWorldPosition()));
				item.getPosition().getWorldRegion().addVisibleObject(item);
				item.setDropTime(result.getLong(8));
				item.setProtected(result.getLong(8) == -1);
				item.setIsVisible(true);
				
				L2World.getInstance().addVisibleObject(item, item.getPosition().getWorldRegion());
				_items.add(item);
				count++;
				
				// add to ItemsAutoDestroy only items not protected
				if (!Config.LIST_PROTECTED_ITEMS.contains(item.getItemId()))
				{
					if (result.getLong(8) > -1)
					{
						if ((Config.ITEM_AUTO_DESTROY_TIME > 0 && item.getItemType() != EtcItemType.HERB) || (Config.HERB_AUTO_DESTROY_TIME > 0 && item.getItemType() == EtcItemType.HERB))
							ItemsAutoDestroyTaskManager.getInstance().addItem(item);
					}
				}
			}
			result.close();
			s.close();
			
			if (count > 0)
				System.out.println("ItemsOnGroundManager: restored " + count + " items.");
			else
				System.out.println("Initializing ItemsOnGroundManager.");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error while loading ItemsOnGround " + e.getMessage(), e);
		}
		
		if (Config.EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD)
			emptyTable();
	}
	
	public void save(ItemInstance item)
	{
		if (Config.SAVE_DROPPED_ITEM)
			_items.add(item);
	}
	
	public void removeObject(ItemInstance item)
	{
		if (Config.SAVE_DROPPED_ITEM && _items != null)
			_items.remove(item);
	}
	
	public void saveInDb()
	{
		_task.run();
	}
	
	public void cleanUp()
	{
		_items.clear();
	}
	
	public void emptyTable()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement del = con.prepareStatement("DELETE FROM itemsonground");
			del.execute();
			del.close();
		}
		catch (Exception e1)
		{
			_log.log(Level.SEVERE, "Error while cleaning table ItemsOnGround " + e1.getMessage(), e1);
		}
	}
	
	protected class StoreInDb extends Thread
	{
		@Override
		public synchronized void run()
		{
			if (!Config.SAVE_DROPPED_ITEM)
				return;
			
			emptyTable();
			
			if (_items.isEmpty())
			{
				if (Config.DEBUG)
					_log.warning("ItemsOnGroundManager: nothing to save.");
				return;
			}
			
			try (Connection con = L2DatabaseFactory.getInstance().getConnection())
			{
				PreparedStatement statement = con.prepareStatement("INSERT INTO itemsonground(object_id,item_id,count,enchant_level,x,y,z,drop_time,equipable) VALUES(?,?,?,?,?,?,?,?,?)");
				
				for (ItemInstance item : _items)
				{
					if (item == null)
						continue;
					
					if (CursedWeaponsManager.getInstance().isCursed(item.getItemId()))
						continue; // Cursed Items not saved to ground, prevent double save
						
					try
					{
						statement.setInt(1, item.getObjectId());
						statement.setInt(2, item.getItemId());
						statement.setInt(3, item.getCount());
						statement.setInt(4, item.getEnchantLevel());
						statement.setInt(5, item.getX());
						statement.setInt(6, item.getY());
						statement.setInt(7, item.getZ());
						
						if (item.isProtected())
							statement.setLong(8, -1); // item will be protected
						else
							statement.setLong(8, item.getDropTime()); // item will be added to ItemsAutoDestroy
							
						if (item.isEquipable())
							statement.setLong(9, 1); // set equipable
						else
							statement.setLong(9, 0);
						
						statement.execute();
						statement.clearParameters();
					}
					catch (Exception e)
					{
						_log.log(Level.SEVERE, "Error while inserting into table ItemsOnGround: " + e.getMessage(), e);
					}
				}
				statement.close();
			}
			catch (SQLException e)
			{
				_log.log(Level.SEVERE, "SQL error while storing items on ground: " + e.getMessage(), e);
			}
			
			if (Config.DEBUG)
				_log.warning("ItemsOnGroundManager: " + _items.size() + " items on ground saved");
		}
	}
	
	private static class SingletonHolder
	{
		protected static final ItemsOnGroundManager _instance = new ItemsOnGroundManager();
	}
}