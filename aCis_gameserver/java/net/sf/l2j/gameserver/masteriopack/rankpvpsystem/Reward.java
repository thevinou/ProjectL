package net.sf.l2j.gameserver.masteriopack.rankpvpsystem;

/**
 * @author Masterio
 */
public class Reward 
{
	private int _id = 0;				// reward id
	private int _itemId = 0;			// game item id
	private long _itemAmount = 0;		// amount of the game item
	private int _rankId = 0;			// required rank id
	
	public int getId()
	{
		return _id;
	}

	public void setId(int id) 
	{
		_id = id;
	}

	public int getItemId() 
	{
		return _itemId;
	}
	
	public void setItemId(int itemId) 
	{
		_itemId = itemId;
	}
	
	public long getItemAmount()
	{
		return _itemAmount;
	}
	
	public void setItemAmount(long itemAmount)
	{
		_itemAmount = itemAmount;
	}

	public int getRankId()
	{
		return _rankId;
	}

	public void setRankId(int rankId) 
	{
		_rankId = rankId;
	}

}

