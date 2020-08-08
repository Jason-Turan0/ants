package hippo;
/**
 * Represents type of hills evidence
 */
public enum HillTypes {
	NO_HILL,			/** seen in the first turn, other than hill ... for symmetry testing */
	LAND, 
	MY_HILL,			/** created during update seen MY_HILL MyHills maintained 
	 						in AfterUpdate when MyHive is 0 and MyExpectedHills not seen are converted to MyHills
	 						MyExpectedHills which were seen are converted to LAND as well as unseen Hills when MyHive>0 after update.
	 						*/
	EXPECTED_MY_HILL,	/** my hill from previous turn. Created during clearMap, MyExpectedHills maintained
	 						converted to MY_HILL or to DEAD in AfterUpdate (if seen or according to MyHive)*/
	ENEMY_HILL,				/** created during update, was enemy_hill when last seen ... EnemyHills maintained */
	EXPECTED_ENEMY_HILL,	/** created during clearMap was an ENEMY_HILL before. EnemyExectedHills maintained. 
							Converted to DEAD in AfterUpdate when seen.*/
	SYMMETRY_ENEMY_HILL,	/** from symmetry added to EnemyExpectedHills, but converted to LAND when seen */
	DEAD;				/** dead hill */
}
