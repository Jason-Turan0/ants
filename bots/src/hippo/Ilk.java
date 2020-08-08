package hippo;
/**
 * Represents type of tile on the game map.
 */
public enum Ilk {
	SYMMETRY_WATER, /** never seen, water expected */ 
	WATER,	/** Water tile. After looking at it forever */
	FOOD,	/** Food tile.	Created during update, FoodTiles (reset before ClearMap) maintained.
							During planning converted to PLANNED_FOOD.
							ClearMap in BeforeUpdate converts it to EXPECTED_FOOD.*/
	PLANNED_FOOD,	/** Food tile. an ant was planned for
	 						Created in planning stage.
	 						ClearMap in BeforeUpdate converts it to EXPECTED_FOOD.*/
	PLANNED_SYMMETRY_FOOD,
	PLANNED_EXPLORATION_FOOD,	/** The same method of planning as for SYMMETRY_FOOD has to be applied on PSEUDO_FOOD, except
	 							whole symmetry orbit is planned at once!*/
	EXPLORATION_FOOD,	/**
	 					Could be put on UNKNOWN or LAND never on WATER
						Exploration support ... long symmetry not seen place has to have PSEUDO_FOOD put on it. 
						All visible squares around should not have PSEUDO_FOOD on it. Old PSEUDO_FOOD should disappear similarly 
						as old SYMMETRY_FOOD. PSEUDO_FOOD disappears when SYMMETRY_SEEN!
						The unseen places surrounded from all sides by WATER should be converted to WATER to speed PSEUDO_FOOD planning.
						*/
	SYMMETRY_FOOD,	/** TODO Food tile expected from symmetry. If an enemy ant seen on path to the food the tile will
		be changed to LAND generation in visible region leads to "symmetry exception"!
				Should be created in calcSymmetry in AfterUpdate, ExpFoodTiles (reset before ClearMap) maintain this as well. 
				During planning converted to PLANNED_FOOD. If not planned, resists as SYMMETRY_FOOD.
		*/
	EXPECTED_FOOD, /** Food tile expected either from symmetry or seen in past. If an enemy ant seen on path to the food the tile will
	 				be changed to LAND symmetry must not generate EXPECTED_FOOD in visible region!
	 						Created in ClearMap in BeforeUpdate, ExpFoodTiles (reset before ClearMap) maintained. 
	 						(plan to do it in Calc Symmetry in AfterUpdate as well)
	 						In AfterUpdate converted to LAND and may be MyHive is increased. At end of AfterUpdate cleared.
	 						During planning converted to PLANNED_FOOD.
	 				*/
	LAND,	/** Land tile.
	 						Default
	 						ClearMap in BeforeUpdate converts there ENEMY_ANTS and DEAD.
	 						ExpFoodTiles converted to it in AfterUpdate when not converted to FOOD in update.
	 						*/
	UNKNOWN, /** never seen */
	SYMMETRY_LAND, /** never seen, but land expected */
	DEAD,	/** Dead ant tile.
							Created in Update according seen Death.
							MyAntsPlanned converted there in AfterUpdate when not converted to MY_ANTS.
	 						*/
	ENEMY_ANT,	/** Enemy ant tile. ... no matter which enemy ... owner decides whose ant it is
	 						Created in Update. EnemyAnts (reset before ClearMap) maintained.
	 						Converted to LAND by ClearMaps in BeforeUpdate.
	 						*/
	MY_ANT,	/** My ant tile. 
	 						Created in Update. MyAnts (reset before ClearMap) maintained.
	 						If created from something else than MY_ANT_PLANNED myHive is decreased.
	 						During planning converted to MY_ANT_PLANNED if 
	 						A) not moving or 
	 						B) when moved from hill producing ant ... in that case MyHive is decreased
	 						or to LAND otherwise.
	 						*/
	MY_TMP_ANT, /** subset of MY_ANT to easily pick up*/
	MY_ANT_PLANNED;	/** Planned position for my ant 
							Created during planning. MyAntsPlanned  maintained.
							During update converted to MY_ANT or to DEAD.
							If not converted during update, converted to DEAD in AfterUpdate.
							MyAntsPlanned cleared at the end of AfterUpdate
	 						*/
	/* Each maintained set only incremented. During it's traversal it must be tested the property still holds.
	*/

	/**
	 * Checks if this type of tile is passable, which means it is not a water
	 * tile.
	 * 
	 * @return <code>true</code> if this is not a water tile, <code>false</code>
	 *         otherwise
	 */
	public boolean isPassable() {
		return ordinal() > WATER.ordinal();
	}

	/**
	 * Checks if this type of tile allows stepping into now (not in future), which means it is not a water
	 * tile. It is not food tile and there is no ants there.
	 * 
	 * @return <code>true</code> if this is not a water tile, <code>false</code>
	 *         otherwise
	 */
	public boolean isStepable() {
		return (ordinal() > PLANNED_FOOD.ordinal()) && (ordinal() < ENEMY_ANT.ordinal());
	}
	
	/**
	 * Checks if this type of tile is unoccupied, which means it is a land tile
	 * or a dead ant tile.
	 * 
	 * @return <code>true</code> if this is a land tile or a dead ant tile,
	 *         <code>false</code> otherwise
	 */
	public boolean isUnoccupied() {
		return this == LAND || this == DEAD;
	}
}
