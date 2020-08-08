package hippo;
import java.util.*;

// TODO maintain food array

/**
 * Provides basic game state handling.
 */
public abstract class Bot extends AbstractSystemInputParser {
	private Ants ants;

	@Override
	public String getTurnFileName() {
		int turn=0;
		if (ants!=null) {
			turn=1+ants.turn;
		}
		return "T"+((""+(turn+100000)).substring(1));
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setup(int loadTime, int turnTime, int rows, int cols,
			int turns, int viewRadius2, int attackRadius2, int spawnRadius2) {
		setAnts(new Ants(loadTime, turnTime, rows, cols, turns, viewRadius2,
				attackRadius2, spawnRadius2, (MyBot)this));
	}

	/**
	 * Returns game state information.
	 * @return game state information
	 */
	public Ants getAnts() {
		return ants;
	}

	/**
	 * Sets game state information.
	 * @param ants	game state information to be set
	 */
	protected void setAnts(Ants ants) {
		this.ants = ants;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addWater(int row, int col) {
		ants.update(Ilk.WATER, new Tile(row, col));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addAnt(int row, int col, int owner) {
		Tile antTile = new Tile(row, col);
		ants.update(owner > 0 ? Ilk.ENEMY_ANT : Ilk.MY_ANT, antTile);
		ants.owners[row][col]=owner;
		if (owner==0) {
			for (int i=-ants.visibilityQCircle.length+1;i<ants.visibilityQCircle.length;i++) {
				for(int j=-ants.visibilityQCircle[Math.abs(i)];j<=ants.visibilityQCircle[Math.abs(i)];j++) {
					ants.see(row+i,col+j);
				}
			}
			for (int i=-ants.attackQCircle.length+1;i<ants.attackQCircle.length;i++) {
				for(int j=-ants.attackQCircle[Math.abs(i)];j<=ants.attackQCircle[Math.abs(i)];j++) {
					ants.twoToMyAttack(row+i,col+j,antTile);
				}
			}			
		} else {
			if (owner>ants.maxOwner) {
				ants.maxOwner=owner;
				if (owner>1) {ants.safeLevel=1;}
			}
			for (int i=-ants.attackQCircle.length+1;i<ants.attackQCircle.length;i++) {
				for(int j=-ants.attackQCircle[Math.abs(i)];j<=ants.attackQCircle[Math.abs(i)];j++) {
					ants.twoToEnemyAttack(row+i,col+j,antTile);
				}
			}			
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addFood(int row, int col) {
		ants.update(Ilk.FOOD, new Tile(row, col));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeAnt(int row, int col, int owner) {// it makes a lot of harm ...
		Tile dead= new Tile(row, col);
		if ((ants.getIlk(dead)==Ilk.LAND)||(ants.getIlk(dead)==Ilk.MY_ANT_PLANNED)) {
			ants.update(Ilk.DEAD,dead);
		}
		ants.getDeadAnts().add(dead);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addHill(int row, int col, int owner) {
		ants.updateHills(owner, new Tile(row, col));
	}

	public void printMap() {
		// when bitand in given shift is nonzero the shift must be disabled
		// similarly for shifts ... be carefull ... it not only prints, it also updates map ... from MY_NEXTANT
		/* logging the known world
		 * ?-unknown 
		 * W-water 
		 * w-expected water
		 * !-land not visible with possible enemy $ if expected food 
		 * ;-land not visible enemy free + if expected food
		 * .-land visible
		 * *-food (visible or invisible)
		 * +-expected food
		 * abcdefghi - ant of corresponding player
		 * ABCDEFGHI - ant on owned hill of corresponding player
		 * v-hill of any player
		 */
		for (Tile myAnt=ants.getMyAnts().getFirstFilter();myAnt!=null;myAnt=ants.getMyAnts().getNextFilter()) {}
		for (Tile myAnt=ants.getMyAntsPlanned().getFirstFilter();myAnt!=null;myAnt=ants.getMyAntsPlanned().getNextFilter()) {}
		for (Tile myAnt=ants.getMyTmpAnts().getFirstFilter();myAnt!=null;myAnt=ants.getMyTmpAnts().getNextFilter()) {}
		int nrListMyAnts=ants.getMyAnts().size()+ants.getMyAntsPlanned().size()+ants.getMyTmpAnts().size();
		int nrMapMyAnts=0;
		List<String> picture=new ArrayList<String>();
		if (!AbstractSystemInputReader.logging) return;
		for(int row=0; row<ants.getRows(); row++) {
			StringBuilder info=new StringBuilder();
			String tmp = "  "+row; info.append(tmp.substring(tmp.length()-3)+" ");
			String ownerAnt="abcdefghij",ownerAntOnHill="ABCDEFGHIJ";
			for(int col=0; col<ants.getCols();col++) {
				Ilk ilk=ants.getIlk(row,col); HillTypes ht=ants.hills[row][col]; int own=ants.owners[row][col];
				if ((ht==HillTypes.LAND)||(ht==HillTypes.NO_HILL)) {
					if (ilk==Ilk.UNKNOWN) {
						info.append('?');
					} else if (ilk==Ilk.WATER) {
						info.append('W');
					} else if (ilk==Ilk.SYMMETRY_WATER) {
						info.append('w');
					} else if (ilk==Ilk.SYMMETRY_LAND) {
						info.append('_');
					} else if (ants.getLastSeen(row,col)<ants.turn) {
						if (!ants.getEnemyFree(row,col)) {
							if ((ilk.ordinal()>=Ilk.FOOD.ordinal())&&(ilk.ordinal()<=Ilk.EXPECTED_FOOD.ordinal())) {
								if((ilk==Ilk.EXPLORATION_FOOD)||(ilk==Ilk.PLANNED_EXPLORATION_FOOD)) {
									info.append('~');
								} else {
									info.append('=');
								}
							} else {
								info.append('!');
							}
						} else {// TODO there could be food known here!
							if ((ilk.ordinal()>=Ilk.FOOD.ordinal())&&(ilk.ordinal()<=Ilk.EXPECTED_FOOD.ordinal())) {
								if((ilk==Ilk.EXPLORATION_FOOD)||(ilk==Ilk.PLANNED_EXPLORATION_FOOD)) {
									info.append('~');
								} else {
									info.append('+');
								}
							} else {
								info.append(';');
							}
						}
					} else if ((ilk.ordinal()>=Ilk.FOOD.ordinal())&&(ilk.ordinal()<=Ilk.EXPECTED_FOOD.ordinal())) {
						info.append('*');
					} else if ((ilk.ordinal()>=Ilk.MY_ANT.ordinal())&&(ilk.ordinal()<=Ilk.MY_ANT_PLANNED.ordinal())) {
						info.append('a');nrMapMyAnts++;
					} else if (ilk==Ilk.ENEMY_ANT) {
						info.append(ownerAnt.substring(own, own+1));
					} else {int danger=(ants.turn&1)==0?ants.getEnemyAttack(row, col):ants.getMyAttack(row, col);
						if (danger==-1) {
							info.append('.');
						} else if (danger>8){
							info.append('9');
						} else {
							info.append(danger);
						}
					}
				} else if ((ht==HillTypes.MY_HILL)||(ht==HillTypes.EXPECTED_MY_HILL)){
					if ((ilk.ordinal()>=Ilk.MY_ANT.ordinal())&&(ilk.ordinal()<=Ilk.MY_ANT_PLANNED.ordinal())) {
						info.append('A');nrMapMyAnts++;
					} else {
						info.append('v');
					}
				} else if (ht==HillTypes.ENEMY_HILL) {
					if (ilk==Ilk.ENEMY_ANT) {
						info.append(ownerAntOnHill.substring(own, own+1));
					} else {
						info.append('v');
					}					
				} else if (ht==HillTypes.EXPECTED_ENEMY_HILL) {
						info.append('x');
				} else if (ht==HillTypes.SYMMETRY_ENEMY_HILL) {
					info.append('s');
				} else if (ht==HillTypes.DEAD) {
					info.append('X');
				}
			}
			picture.add(info.toString());
		}
		LogFile.write(picture, ants.pondering.bigEndianLeader());
		if (nrMapMyAnts!=nrListMyAnts) {
			LogFile.write(">>>>map ants dont match list ants<<<< map:"+nrMapMyAnts+" list:"+nrListMyAnts);
		}
		ants.pondering.logSymmetries();
		LogFile.write("Expected enemy hills:"+ants.getEnemyExpectedHills());
		LogFile.write("Enemy hills:"+ants.getEnemyHills());
		LogFile.write("Expected my hills:"+ants.getMyExpectedHills());
		LogFile.write("My hills:"+ants.getMyHills());
		LogFile.write("Dead hills:"+ants.getDeadHills());
	}
	/**
	 * {@inheritDoc}
	 */

	@Override
	public void beforeUpdate() {
		ants.turn++;ants.myPrevHive=ants.myHive;
	}

	
	/** 
	 * {@inheritDoc}
	 */
	@Override
	public void afterUpdate() {
		for (Tile eaten=ants.getExpectedFoodTiles().getFirstFilter();eaten!=null;eaten=ants.getExpectedFoodTiles().getNextFilter()) {
			int row=eaten.getRow(),col=eaten.getCol();
			if (ants.getLastSeen(eaten)==ants.turn) {// EXPECTED FOOD place seen, but not converted to FOOD ... it must be LAND
				ants.setIlk(eaten, Ilk.LAND);
				boolean myAnts=false,enemyAnts=false;
				for (int i=-ants.spawnQCircle.length+1;i<ants.spawnQCircle.length;i++) {
					for(int j=-ants.spawnQCircle[Math.abs(i)];j<=ants.spawnQCircle[Math.abs(i)];j++) {
						myAnts=myAnts || (ants.getIlk(row+i,col+j)==Ilk.MY_ANT);
						enemyAnts=enemyAnts || (ants.getIlk(row+i,col+j)==Ilk.ENEMY_ANT);
					}
				}
				if (myAnts&&!enemyAnts) {
					ants.myHive++;
					//LogFile.write("myHive++ ("+eaten.getRow()+","+eaten.getCol()+"):"+ants.myHive);
				}
			} else if (ants.getEnemyFree(eaten)){// the food is sure target now
				ants.setIlk(eaten, Ilk.FOOD);
				ants.getFoodTiles().add(eaten);
			}
		}
		ants.attackUpdate();
		for (Tile myAntPlanned=ants.getMyAntsPlanned().getFirstNoFilter();myAntPlanned!=null;myAntPlanned=ants.getMyAntsPlanned().getNextNoFilter()) {
			if (ants.getIlk(myAntPlanned)!=Ilk.MY_ANT) {
				removeAnt(myAntPlanned.getRow(), myAntPlanned.getCol(), 0);// TODO some opponent knowledge gathering?
			}
		}
		ants.getMyAntsPlanned().clear();
		if (ants.turn==1) {
			ants.myHive=0;
		}
		for(Tile myExpHill=ants.getMyExpectedHills().getFirstFilter();myExpHill!=null;myExpHill=ants.getMyExpectedHills().getNextFilter()){
			if (ants.getHill(myExpHill)==HillTypes.EXPECTED_MY_HILL) {
				if ((ants.getLastSeen(myExpHill)==ants.turn)||ants.myPrevHive>0) {
					ants.setHill(myExpHill, HillTypes.DEAD);
					ants.getDeadHills().add(myExpHill);
					ants.pondering.distFromMyState=0;
				} else {
					ants.setHill(myExpHill, HillTypes.MY_HILL);
					ants.getMyHills().add(myExpHill);
				}
			}
		}
		ants.getMyExpectedHills().clear();
		for(Tile enemyExpHill=ants.getEnemyExpectedHills().getFirstFilter();enemyExpHill!=null;enemyExpHill=ants.getEnemyExpectedHills().getNextFilter()){
			switch (ants.getHill(enemyExpHill)) { 
			case EXPECTED_ENEMY_HILL:
				if (ants.getLastSeen(enemyExpHill)==ants.turn) {
					ants.setHill(enemyExpHill, HillTypes.DEAD);
					ants.getDeadHills().add(enemyExpHill);
					ants.pondering.distToEnemyState=0;
				} else {
					ants.setHill(enemyExpHill, HillTypes.ENEMY_HILL);
					ants.getEnemyHills().add(enemyExpHill);
				}
				break;
			case SYMMETRY_ENEMY_HILL:
				if (ants.getLastSeen(enemyExpHill)==ants.turn) {
					ants.setHill(enemyExpHill, HillTypes.LAND); // could be dead hill, but never seen
					ants.pondering.distToEnemyState=0;
				} else {
					ants.getEnemyHills().add(enemyExpHill);
				}
			}
		}		
		ants.getEnemyExpectedHills().clear();
		for (Tile explore=ants.getPlannedExplorationFoodTiles().getFirstFilter();explore!=null;explore=ants.getPlannedExplorationFoodTiles().getNextFilter()) {
			if (ants.turn-ants.pondering.getExploration(explore)<=8) {
				ants.setIlk(explore, Ilk.EXPLORATION_FOOD);
				ants.getExplorationFoodTiles().add(explore);
			} else if (ants.getLastSeen(explore)>0){
				ants.setIlk(explore, Ilk.LAND);
			} else {
				ants.setIlk(explore, Ilk.UNKNOWN);
			}
		}
		ants.getPlannedExplorationFoodTiles().clear();
		for (Tile unSeen=ants.getSymmetryFoodTiles().getFirstFilter();unSeen!=null;unSeen=ants.getSymmetryFoodTiles().getNextFilter()) {
			int row=unSeen.getRow(),col=unSeen.getCol();
			if (ants.getLastSeen(unSeen)==ants.turn) {// SYMMETRY FOOD place seen, but not converted to FOOD ... it must be LAND
				ants.setIlk(unSeen, Ilk.LAND);
			}
			if (ants.turn-ants.foodDiscovery[row][col]>50) {
				ants.setIlk(unSeen, Ilk.LAND);// remove old food to prevent overload
			}
		}
		if (AbstractSystemInputReader.logging) {
			if (!ants.pondering.seekingForSymmetries()) {
				//int tst = (Math.abs((int) Ants.random.nextLong())%6);
				//if (tst>4) {
				//	printMap();
				//} else if (tst<2) {
				//	LogFile.write("FrontLine: "+ants.getFrontLine());
				//}
			}
		}
		//LogFile.write("enemyAnts "+ants.getEnemyAnts());
		for (Tile enemyAnt=ants.getEnemyAnts().getFirstFilter();enemyAnt!=null;enemyAnt=ants.getEnemyAnts().getNextFilter()) {
			//LogFile.write("Adding to frontLine "+enemyAnt);
			ants.setEnemyFree(enemyAnt,false);
		}
		for (Tile enemyHill=ants.getEnemyHills().getFirstFilter();enemyHill!=null;enemyHill=ants.getEnemyHills().getNextFilter()) {
			//LogFile.write("Adding to frontLine "+enemyAnt);
			if (ants.getEnemyFree(enemyHill)) {
				if (ants.getLastSeen(enemyHill)!=ants.turn) {
					ants.getFrontLine().add(enemyHill);
				}
				ants.setEnemyFree(enemyHill,false);
			}
		}
		ants.frontLineUpdate(ants.getEnemyExpectedHills(),ants.getMyAntsPlanned());
		ants.pondering.newSymmetryFood(100);
		//LogFile.write("Turn:"+ants.turn+" Hive:"+ants.myHive);
	}

	@Override
	public void afterTurn() {
		ants.clearMap();
		//LogFile.write("afterTurn start"+ants.getTimeRemaining());
		if (ants.turn>0) {
			while (ants.pondering.calcSymmetry()) {
			}
		}
		//Pondering.logSymmetries();
		//LogFile.writeFlush("afterTurn end"+ants.getTimeRemaining());
	}
}