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
package net.sf.l2j.gameserver.model.itemcontainer.listeners;

import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class ShadowWeaponListener implements OnEquipListener
{
	private static ShadowWeaponListener instance = new ShadowWeaponListener();
	
	public static ShadowWeaponListener getInstance()
	{
		return instance;
	}
	
	@Override
	public void onEquip(int slot, ItemInstance item, L2Playable playable)
	{
		if (item.isShadowItem())
			item.startTimer(new ShadowLifeTimeTask(item, playable));
	}
	
	@Override
	public void onUnequip(int slot, ItemInstance item, L2Playable playable)
	{
		if (item.isShadowItem())
			item.stopTimer();
	}
	
	protected class ShadowLifeTimeTask implements Runnable
	{
		private final ItemInstance _item;
		private final L2PcInstance _player;
		
		ShadowLifeTimeTask(ItemInstance item, L2Playable actor)
		{
			_item = item;
			_player = (L2PcInstance) actor;
		}
		
		@Override
		public void run()
		{
			if (!_item.isEquipped())
				return;
			
			int mana;
			synchronized (_item)
			{
				_item.setMana(_item.getMana() - 1);
				mana = _item.getMana();
				if (mana <= 0)
					_player.getInventory().destroyItem("ShadowItem", _item, _player, null);
			}
			
			SystemMessage sm = null;
			if (mana == 10)
				sm = SystemMessage.getSystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_10);
			else if (mana == 5)
				sm = SystemMessage.getSystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_5);
			else if (mana == 1)
				sm = SystemMessage.getSystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_1);
			else if (mana <= 0)
				sm = SystemMessage.getSystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_0);
			else
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(_item);
				_player.sendPacket(iu);
			}
			
			if (sm != null)
			{
				sm.addItemName(_item.getItemId());
				_player.sendPacket(sm);
			}
		}
	}
}