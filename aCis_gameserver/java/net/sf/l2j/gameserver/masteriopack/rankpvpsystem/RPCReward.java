package net.sf.l2j.gameserver.masteriopack.rankpvpsystem;

/**
 * @author Masterio
 */
public class RPCReward 
{
	private int _id = 0;				// RPC reward id
	private int _itemId = 0;			// game item id
	private long _itemAmount = 0;		// amount of the game item
	private long _rpc = 0;				// total RPC cost for (item * amount)
	
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
	
	public long getRpc()
	{
		return _rpc;
	}
	
	public void setRpc(long rpc)
	{
		_rpc = rpc;
	}
}
