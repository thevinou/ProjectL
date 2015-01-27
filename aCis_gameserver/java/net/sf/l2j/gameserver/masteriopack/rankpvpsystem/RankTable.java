package net.sf.l2j.gameserver.masteriopack.rankpvpsystem;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Masterio
 */
public class RankTable 
{
	private static RankTable _instance = null;
	
	/** Map &lt;rankId, Rank&gt; - store all Ranks as Rank objects by rank id. */
	private static Map<Integer, Rank> _rankList = new LinkedHashMap<>();

	private RankTable()
	{

	}
	
	public static RankTable getInstance()
	{
		if(_instance == null)
		{
			_instance = new RankTable();
		}
		
		return _instance;
	}
	
	public Map<Integer, Rank> getRankList()
	{
		return _rankList;
	}

	public void setRankList(Map<Integer, Rank> rankList)
	{
		_rankList = rankList;
	}
	
	/**
	 * Returns Rank object by rank id, if not founded returns null.
	 * @param id
	 * @return
	 */
	public Rank getRankById(int id)
	{
		return _rankList.get(id);
	}

}
