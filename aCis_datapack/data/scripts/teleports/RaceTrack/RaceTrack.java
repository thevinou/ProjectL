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
package teleports.RaceTrack;

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;

public class RaceTrack extends Quest
{
	private final static int RACE_MANAGER = 30995;
	
	private final static Map<Integer, Integer> data = new HashMap<>();
	{
		data.put(30320, 1); // RICHLIN
		data.put(30256, 2); // BELLA
		data.put(30059, 3); // TRISHA
		data.put(30080, 4); // CLARISSA
		data.put(30899, 5); // FLAUEN
		data.put(30177, 6); // VALENTIA
		data.put(30848, 7); // ELISA
		data.put(30233, 8); // ESMERALDA
		data.put(31320, 9); // ILYANA
		data.put(31275, 10); // TATIANA
		data.put(31964, 11); // BILIA
		data.put(31210, 12); // RACE TRACK GK
	}
	
	private final static int[][] RETURN_LOCS =
	{
		{
			-80826,
			149775,
			-3043
		},
		{
			-12672,
			122776,
			-3116
		},
		{
			15670,
			142983,
			-2705
		},
		{
			83400,
			147943,
			-3404
		},
		
		{
			111409,
			219364,
			-3545
		},
		{
			82956,
			53162,
			-1495
		},
		{
			146331,
			25762,
			-2018
		},
		{
			116819,
			76994,
			-2714
		},
		
		{
			43835,
			-47749,
			-792
		},
		{
			147930,
			-55281,
			-2728
		},
		{
			87386,
			-143246,
			-1293
		},
		{
			12882,
			181053,
			-3560
		}
	};
	
	public RaceTrack(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30320, 30256, 30059, 30080, 30899, 30177, 30848, 30233, 31320, 31275, 31964, 31210);
		addTalkId(RACE_MANAGER, 30320, 30256, 30059, 30080, 30899, 30177, 30848, 30233, 31320, 31275, 31964, 31210);
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		QuestState st = player.getQuestState(getName());
		int npcId = npc.getNpcId();
		if (data.containsKey(npcId))
		{
			player.teleToLocation(12661, 181687, -3560, 0);
			st.setState(STATE_STARTED);
			st.set("id", String.valueOf(data.get(npcId)));
		}
		else if (st.isStarted() && npcId == RACE_MANAGER)
		{
			// back to start location
			int return_id = st.getInt("id") - 1;
			player.teleToLocation(RETURN_LOCS[return_id][0], RETURN_LOCS[return_id][1], RETURN_LOCS[return_id][2], 0);
			st.exitQuest(true);
		}
		
		return null;
	}
	
	public static void main(String[] args)
	{
		new RaceTrack(-1, "RaceTrack", "teleports");
	}
}