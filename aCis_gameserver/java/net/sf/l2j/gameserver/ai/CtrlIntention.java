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
package net.sf.l2j.gameserver.ai;

/** Enumeration of generic intentions of an NPC/PC */
public enum CtrlIntention
{
	IDLE, // Do nothing, disconnect AI of NPC if no players around
	ACTIVE, // Alerted state without goal : scan attackable targets, random walk, etc
	REST, // Rest (sit until attacked)
	ATTACK, // Attack target (cast combat magic, go to target, combat) - may be ignored (another target, invalid zoning, etc)
	CAST, // Cast a spell, depending on the spell - may start or stop attacking
	MOVE_TO, // Just move to another location
	FOLLOW, // Like move, but check target's movement and follow it
	PICK_UP, // Pick up item (go to item, pick up it, become idle)
	INTERACT // Move to target, then interact
}