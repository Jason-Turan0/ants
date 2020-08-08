package hippo;
import java.io.IOException;
import java.util.*;

/**
 * TODO symmetry LAND, symmetry WATER could be represented just implicitly ... using BigIntegers it would save a lot of copying
 * ^^ this is must for searching for multilevel symmetries
 * TODO regions with at least 2 ants go immediately to !enemyfree region
 * TODO !!! fight ... enemy fight habbits detection, ants "clustring", fighting of groups at once
 * alpha-beta for fighting groups of ants
 * TODO logging known "map" information (hills), remember food surrounded by enemyfree bitmap as FOOD and not as EXPECTED_FOOD
 * TODO Symmetry ... combined shifts; combined shifts and other symmetries; shifts in coordinate directions
 * TODO initialBFS from myAnts to process targets in better order ??? 
 * 			... dynamic ant removal from preprocessed distances by conversion MY_ANT->MY_ANT_PLANNED
 * TODO exploration according to lastSymmetrySeen ... 
 * TODO if enough ants, prefer gathering by less border ants.
 * TODO "border ant evaluation" positive distance from my hills, highly positive distance to enemy
 * TODO try to increase "border evaluation" unless putting lone ant into danger
 * TODO better food planning ... at least last step choose correct direction, but planning several foods to be gathered by one ant could help.
 * 			using precomputed distances from ants could help a lot
 * TODO ... later :) detect narrow corridors ... defendable with fixed number of ants against an arbitrary number of opponent ants.
 * TODO ... detect one way roads ...
 * 
 * TODO !!!! exploration ... make several points of interest and try to find paths to them ... rather to going with all ants to the same location
 * TODO try optimise food search by from my ants traversal and make my_planned ants new points of interest for another search.
 * TODO nearby opponents ants are points of intereset as well
 * TODO more nearby ants should cooperate
 * TODO !! speedup release hive using distFromMyHill
 * TODO introduce Search.addDelayed to add tiles to be added when BreakTile is read. This could be used to motivate ants to move further 
 *  from my hills and nearer to opponents
 * 
 * Wow food is generated even under ants so stepping sideways allows the food to appear!
 * Let us try to change ants which are nearer to our hill than to enemies ...
 * 
 * TODO food planning from hill when there would be ant released ... MY_BORNING_ANT -> MY_ANT_PLANNED often losing 2 steps in openning by
 * stepping back near the hill just after food consumption. This should be incorporated to the last step to food calculation as well.
 */
public class MyBot extends Bot {
	/**
	 * Main method executed by the game engine for starting the bot.
	 * 
	 * @param args	command line arguments
	 * @throws IOException	if an I/O error occurs
	 */
	private  Ants ants;
	private  boolean eating=true;
	private  int granularity,gcols,grows;
	private  int myAntsSpread[][];
	private  int distFromMyAnts[][];
	private  Search fromMyAntsSearch;
	private  Search correctFromMyAntsSearch;
	private  boolean makingWalls=true;//TODO logic to switch to makingWalls ... but enemyFree would be more important!!!
	private  int turnAgression;
	private  double timeKoef;
	private  int distFromMyAntsBound;
	private  int myMissingHive=0;
	public  boolean maintainFrontLine=true;
	private  int frontLineTest=0;
	private  int numDefenders=0;
	private  Search attackTurnSearch;
	private  TileList antGroup;
	
	public  void init(Ants ants_) {
		ants = ants_;		
		granularity=5;gcols=ants.getCols()/granularity;grows=ants.getRows()/granularity;
		myAntsSpread=new int[grows+1][gcols+1];
		distFromMyAnts=new int[ants.getRows()][ants.getCols()];
		distFromFront=new int[ants.getRows()][ants.getCols()];
		fromMyAntsSearch=new Search();
		correctFromMyAntsSearch=new Search();
		attackTurnSearch=new Search();
		antGroup=new TileList(null,null);
	}
	
	private  void setTimeKoef(){
		double t0=ants.getTimeRemaining();
		timeKoef=Math.max(0, t0/800);
		LogFile.write("Time koef: "+timeKoef);
	}
	
	private  int stopTime(int atMostTime,int stopTime) {
		return (int) (Math.max(timeKoef*stopTime, ants.getTimeRemaining()-timeKoef*atMostTime));
	}
	
	public  void main(String[] args) throws IOException {
		//new MyBot().readSystemInput();
		MyBot bot=new MyBot();
		//bot.readConfig();
		if (AbstractSystemInputReader.turnInput) {
			while (bot.readTurnFileInput()) {;}
		}
		bot.readSystemInput();
	}
	private  void supportSpawning(int stopTime) {
		LogFile.write("SupportSpawning "+ants.getTimeRemaining()+"stop at "+stopTime);
		if (ants.myHive>=ants.getMyHills().size()) {
			for(Tile myHill=ants.getMyHills().getFirstFilter();myHill!=null;myHill=ants.getMyHills().getNextFilter()) {
				if (ants.getTimeRemaining()<stopTime) return;
				if (ants.getIlk(myHill).isUnoccupied()) {
					for (Aim d : Aim.permAim[0]) {
						if (ants.getIlk(myHill,d)==Ilk.MY_ANT) {
							ants.setIlk(myHill, Ilk.MY_ANT_PLANNED);
							ants.getMyAntsPlanned().add(myHill);
							ants.myHive--;
							//LogFile.write("ants.myHive-- supportSpawn ("+myHill.getRow()+","+myHill.getCol()+"):"+ants.myHive);
							break;
						}
					}
				}
			}
		}
	}

	private  void spawnDefense(int stopTime){
		LogFile.write("MySpawnDefense "+ants.getTimeRemaining()+"stop at "+stopTime);
		if (ants.myHive>0) {
			// spawn defense as a last resort ... may be sacrifying one hill to save others would be better
			// so I prefer not to block other hills to spawn longer
			// hmmmm seems the ant could safely left and spawning still defends so let us try collision attack
			// ^^^^ no!! the observation was wrong !!!
			for(Tile myHill=ants.getMyHills().getFirstFilter();myHill!=null;myHill=ants.getMyHills().getNextFilter()) {
				if (ants.getTimeRemaining()<stopTime) return;
				if (ants.getIlk(myHill)==Ilk.MY_ANT) {
					int permId = (Math.abs((int) Ants.random.nextLong())%24);
					for (Aim d : Aim.permAim[permId]) {
						if (ants.getIlk(myHill,d)==Ilk.ENEMY_ANT) {
							ants.setIlk(myHill,Ilk.MY_ANT_PLANNED);
							ants.getMyAntsPlanned().add(myHill);
							break;
						}
					}
				}
			}
		}
	}

	private  void stupidAttack(int stopTime){
		LogFile.write("StupidAttack "+ants.getTimeRemaining()+"stop at "+stopTime);
		for (Tile eHill=ants.getEnemyHills().getFirstFilter();eHill!=null;eHill=ants.getEnemyHills().getNextFilter()) {
			if (ants.getTimeRemaining()<stopTime) return;
			Queue<Tile> attack = ants.myNearbyAnts(eHill, 6);
			if (!attack.isEmpty()) {
				attack = ants.myNearbyAnts(eHill, 10);
				while (!attack.isEmpty()) {
					Tile myAnt = attack.remove();Tile goWhere = attack.remove();
					if (ants.getIlk(myAnt)==Ilk.MY_ANT) {
						if (ants.getIlk(goWhere).isStepable()) {
							Aim direction = ants.getDirection(myAnt,goWhere);
							if (ants.getEnemyAttack(goWhere)<=2) {
								ants.issueOrder(myAnt, direction);
							}
						}
					}
				}
			}
		}
	}

	/* should defend against single ants attacking the hill 
	 * even better would be defend region from which the hill could be seen
	 * such ants should be blocked by 2-3 ants and pushed back (possibly exchanging ant for the attacker) 
	 * */
	private  void defendStupidAttack(int stopTime) {
		//TODO attack ants in danger distance from my hills first danger is to see the hill, final danger is to raze the hill
		//	the biggest problem is open space where current algorithm tries to avoid collision and allows razing my own hill
		//	increasing tendency to attack nearby ants could help a lot, but one has to be carefull not to "push" enemy ant to my own hill
		//	correct strategy seems to be to try to block it from advancing near the hill and "push it out" to safer distance
		//	having ants with nothing to do ... we can send them to such targets.
		ants.enemyAttackSearchLevel=99;
		LogFile.write("DefendStupidAttack "+ants.getTimeRemaining()+"stop at "+stopTime);
		for(Tile myAnt=ants.getMyAnts().getFirstFilter();myAnt!=null;myAnt=ants.getMyAnts().getNextFilter()) {
			//if (Pondering.getMinDistFromMyHill(myAnt)<Pondering.getDistToEnemyHill(myAnt)) {
			if (ants.getTimeRemaining()<stopTime) {
				break;
			}
			Tile enemyAnt = ants.Nearest(myAnt, Ilk.ENEMY_ANT, ants.getView1(), true);
			if (enemyAnt==null) continue;
			Tile myAnt2,myAnt3,myAnt2Where,myAnt3Where=null;// choose 2 noncolliding attackers at first
			Ilk myAnt2WhereIlk;
			myAnt2 = ants.Nearest(enemyAnt, Ilk.MY_ANT, ants.getView1()<<1, true);
			if (myAnt2==null) continue;
			myAnt2Where = ants.targetNeighbour;myAnt2WhereIlk=ants.getIlk(myAnt2Where);
			ants.setIlk(myAnt2Where, Ilk.WATER);// be carefull to undo it !!
			myAnt3 = ants.Nearest(enemyAnt, Ilk.MY_ANT, ants.getView1()<<1, true);
			if (myAnt3!=null) {
				myAnt3Where = ants.targetNeighbour;
				//LogFile.write("A:"+myAnt2+";"+myAnt2Where+";"+myAnt3+";"+myAnt3Where);
				if (myAnt2.equals(myAnt3)) {
					ants.setIlk(myAnt2Where, myAnt2WhereIlk);
					ants.setIlk(myAnt2, Ilk.MY_TMP_ANT);
					myAnt3 = ants.Nearest(enemyAnt, Ilk.MY_ANT, ants.getView1()<<1, true);
					ants.setIlk(myAnt2, Ilk.MY_ANT);
					if (myAnt3!=null) {
						if (myAnt2Where.equals(ants.targetNeighbour)) {
							myAnt2Where=myAnt3Where;
						}
						myAnt3Where=ants.targetNeighbour;
					}
					//LogFile.write("B:"+myAnt2+";"+myAnt2Where+";"+myAnt3+";"+myAnt3Where);						
				}
			}
			ants.setIlk(myAnt2Where, myAnt2WhereIlk);
			if (ants.isPassableOnDangerLevel(myAnt2Where)&&((ants.getEnemyAttack(myAnt2Where)<=0)||(turnAgression==99))) {
				Aim dir=ants.getDirection(myAnt2, myAnt2Where);
				ants.issueOrder(myAnt2, dir);
			} else {
				ants.setIlk(myAnt2, Ilk.MY_TMP_ANT);
				ants.getMyTmpAnts().add(myAnt2);
			}
			if (myAnt3!=null) {
				if (ants.isPassableOnDangerLevel(myAnt3Where)&&((ants.getEnemyAttack(myAnt3Where)<=0)||(turnAgression==99))) {
					Aim dir=ants.getDirection(myAnt3, myAnt3Where);
					ants.issueOrder(myAnt3, dir);
				} else {
					ants.setIlk(myAnt3, Ilk.MY_TMP_ANT);
					ants.getMyTmpAnts().add(myAnt3);
				}
			}
		}
		for(Tile myAnt=ants.getMyAnts().getFirstFilter();myAnt!=null;myAnt=ants.getMyAnts().getNextFilter()) {
			ants.setIlk(myAnt, Ilk.MY_TMP_ANT);
			ants.getMyTmpAnts().add(myAnt);			
		}
		ants.getMyAnts().clear();
		for(Tile myAnt=ants.getMyTmpAnts().getFirstFilter();myAnt!=null;myAnt=ants.getMyTmpAnts().getNextFilter()) {
			ants.setIlk(myAnt, Ilk.MY_ANT);
			ants.getMyAnts().add(myAnt);			
		}
		ants.getMyTmpAnts().clear();
	}
	
	private  void slowAttack(int stopTime){
		final int liveAttackRatio=5,liveNoAttack=5,deadAttackRatio=1,deadNoAttack=0;
		LogFile.write("SlowAttack agression ("+turnAgression+") "+ants.turn+" "+ants.getTimeRemaining()+"stop at "+stopTime);
		if (ants.getEnemyHills().size()==0) return;
		int nrAnts,infty=100;
		if (turnAgression==99) {infty=1000;}
		ants.enemyAttackSearchLevel=turnAgression;
		if (eating) {// only ants not used yet
			nrAnts=(ants.getMyAnts().size())/liveAttackRatio;
			nrAnts-=(nrAnts<liveNoAttack)?nrAnts:0;
		} else {
			nrAnts=(ants.getMyAnts().size())/deadAttackRatio;
			nrAnts-=(nrAnts<deadNoAttack)?nrAnts:0;
		}
		ants.search.clear();int dist=0;
		for (Tile eHill=ants.getEnemyHills().getFirstFilter();eHill!=null;eHill=ants.getEnemyHills().getNextFilter()) {
			ants.search.addNotVisited(eHill);
		}
		boolean insertBreak=true;int i=0;
		while (!ants.search.isEmpty() && (dist<infty) && (i<nrAnts)) {
			if (ants.getTimeRemaining()<stopTime) return;
			if (insertBreak) {
				ants.search.addBreak();
				insertBreak=false;
			}
			Tile searched=ants.search.remove();
			if (searched.equals(Search.breakTile)) {
				dist++; insertBreak=true;
			} else {
				int permId = (Math.abs((int) Ants.random.nextLong())%24);
				for (Aim direction : Aim.permAim[permId]) {
					Tile moved = ants.getTile(searched, direction);
					if (ants.search.notVisited(moved)) {
						if (ants.getIlk(moved).isPassable()) {
							ants.search.addNotVisited(moved);
							if (ants.getIlk(moved)==Ilk.MY_ANT) {
								if (ants.getIlk(searched).isStepable()) {
									if (ants.isPassableOnDangerLevel(searched)) {
										Aim dir = ants.getDirection(moved,searched);
										ants.issueOrder(moved, dir);
										i++;
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private  void granularityCount(int stopTime) {// hill attacking ants are not counted
		LogFile.write("GranularityCount "+ants.getTimeRemaining()+"stop at "+stopTime);
		for (int[] row : myAntsSpread) {
			Arrays.fill(row, 0);
		}
		for (Tile myAnt=ants.getMyAnts().getFirstFilter();myAnt!=null;myAnt=ants.getMyAnts().getNextFilter()) {
			if (ants.getTimeRemaining()<stopTime) return;
			myAntsSpread[myAnt.getRow()/granularity][myAnt.getCol()/granularity]++;
		}
	}

	private  void foodPlanning(TileList foodList, Ilk listIlk, TileList plannedList, Ilk plannedIlk,int infty,int stopTime) {
		LogFile.write("FoodPlanning "+listIlk+" "+foodList+" "+ants.getTimeRemaining()+"stop at "+stopTime);
		int nrMyAnts=ants.getMyAnts().size()-numDefenders;
		if (!eating) {
			nrMyAnts/=10; //just a small portion dedicated for eating}
		}
		for(ants.enemyAttackSearchLevel=ants.safeLevel;ants.enemyAttackSearchLevel<3;ants.enemyAttackSearchLevel++) {
			for (Tile oriFood=foodList.getFirstFilter();oriFood!=null;oriFood=foodList.getNextFilter()) {
				boolean done=false; Tile food=oriFood;
				while (!done) {
					if (nrMyAnts==0) return;
					if (ants.getTimeRemaining()<stopTime) return;
					Tile hungry = ants.Nearest(food, Ilk.MY_ANT, infty, true);
					//LogFile.write("food "+food+"to eat by "+hungry);
					if (hungry!=null) {
						Tile goWhere = ants.targetNeighbour;
						Tile nfood = ants.Nearest(hungry, listIlk, infty, true);
						//LogFile.write("ant "+hungry+"wants to eat "+nfood);
						if (food.equals(nfood)) {
							Aim direction = ants.getDirection(hungry,goWhere);
							//LogFile.write("direction "+direction);
							ants.issueOrder(hungry, direction); nrMyAnts--;
							ants.setIlk(food, plannedIlk);
							plannedList.add(food);
							food=oriFood;
							if (food.equals(nfood)) {
								done = true;
							}
						} else {
							food = nfood;
							if (food==null) {
								done = true;
							}
						}
					} else {
						done = true;
					} 
				}
			}
		}
	}
	
	private  void explorationFoodPlanning(int infty,int stopTime) {
		LogFile.write("ExplorationFoodPlanning "+Ilk.EXPLORATION_FOOD+" "+ants.getTimeRemaining()+"stop at "+stopTime);
		int nrMyAnts=ants.getMyAnts().size()-numDefenders;
		for(ants.enemyAttackSearchLevel=ants.safeLevel;ants.enemyAttackSearchLevel<3;ants.enemyAttackSearchLevel++) {
			for (Tile oriFood=ants.getExplorationFoodTiles().getFirstFilter();oriFood!=null;oriFood=ants.getExplorationFoodTiles().getNextFilter()) {
				boolean done=false; Tile food=oriFood;
				while (!done) {
					if (nrMyAnts==0) return;
					if (ants.getTimeRemaining()<stopTime) return;
					Tile hungry = ants.Nearest(food, Ilk.MY_ANT, infty, true);
					if (hungry!=null) {
						Tile goWhere = ants.targetNeighbour;
						Tile nfood = ants.Nearest(hungry, Ilk.EXPLORATION_FOOD, infty, true);
						if (food.equals(nfood)) {
							Aim direction = ants.getDirection(hungry,goWhere);
							ants.issueOrder(hungry, direction);nrMyAnts--;
							List<Tile> symTs = ants.pondering.actualSymmetry(food);symTs.add(food);
							for(Tile symT:symTs) {
								Ilk symIlk=ants.getIlk(symT);
								if (symIlk==Ilk.EXPLORATION_FOOD) {
									ants.setIlk(symT, Ilk.PLANNED_EXPLORATION_FOOD);
									ants.getPlannedExplorationFoodTiles().add(symT);
								}
							}
							food=oriFood;
							if (food.equals(nfood)) {
								done = true;
							}
						} else {
							food = nfood;
							if (food==null) {
								done = true;
							}
						}
					} else {
						done = true;
					} 
				}
			}
		}
	}
	
	private  void reinforcements(int stopTime) {
		LogFile.write("Reinforcement "+ants.getTimeRemaining()+"stop at "+stopTime);
		int nrMyAnts=ants.getMyAnts().size();
		if (nrMyAnts==0) return;
		for (Tile myAnt=ants.getMyAntsPlanned().getFirstFilter();myAnt!=null;myAnt=ants.getMyAntsPlanned().getNextFilter()) {
			if (ants.getTimeRemaining()<stopTime) return;
			ants.setIlk(myAnt,Ilk.MY_TMP_ANT);
			ants.getMyTmpAnts().add(myAnt);
		}
		ants.getMyAntsPlanned().clear();
		for (Tile myAnt=ants.getMyTmpAnts().getFirstFilter();myAnt!=null;myAnt=ants.getMyTmpAnts().getNextFilter()) {
			if (ants.getTimeRemaining()<stopTime) return;
			if (nrMyAnts==0) break;
			//LogFile.write("looking on ant "+myAnt);
			if (ants.getEnemyAttack(myAnt)>=0) {
				ants.enemyAttackSearchLevel=0;
				Tile reinforce = ants.Nearest(myAnt, Ilk.MY_ANT, 40, true);
				if (reinforce != null) {
					Tile goWhere = ants.targetNeighbour;
					//LogFile.write("reinforce found at "+reinforce+"with neighbour "+goWhere);
					Aim direction = ants.getDirection(reinforce,goWhere);
					ants.issueOrder(reinforce, direction);
					nrMyAnts--;
				}
			}
		}
		for (Tile myAnt=ants.getMyTmpAnts().getFirstFilter();myAnt!=null;myAnt=ants.getMyTmpAnts().getNextFilter()) {
			if (ants.getTimeRemaining()<stopTime) return;
			ants.setIlk(myAnt,Ilk.MY_ANT_PLANNED);
			ants.getMyAntsPlanned().add(myAnt);
		}
		ants.getMyTmpAnts().clear();
		for (Tile myAnt=ants.getMyAnts().getFirstFilter();myAnt!=null;myAnt=ants.getMyAnts().getNextFilter()) {
			if (ants.getTimeRemaining()<stopTime) return;
			if (nrMyAnts<=1) break;
			//LogFile.write("looking on ant "+myAnt);
			if (ants.getEnemyAttack(myAnt)>=0) {
				//LogFile.write("near enemy ant found at "+enemy);
				ants.enemyAttackSearchLevel=0;
				ants.setIlk(myAnt, Ilk.MY_TMP_ANT);
				Tile reinforce = ants.Nearest(myAnt, Ilk.MY_ANT, 40, true);
				ants.setIlk(myAnt, Ilk.MY_ANT);
				if (reinforce != null) {
					Tile goWhere = ants.targetNeighbour;
					//LogFile.write("reinforce found at "+reinforce+"with neighbour "+goWhere);
					Aim direction = ants.getDirection(reinforce,goWhere);
					ants.issueOrder(reinforce, direction);
					nrMyAnts--;
				}
			}
		}
	}
	private  Tile findTileToExplore(Tile t1, int infty, int exploreDelay) {
		int dist=0;ants.search.clear();
		ants.search.addNotVisited(t1);boolean insertBreak=true;
		while (!ants.search.isEmpty() && (dist<infty)) {
			if (ants.getTimeRemaining()<50) break;
			if (insertBreak) {
				ants.search.addBreak();
				insertBreak=false;
			}
			ants.targetNeighbour=ants.search.remove();
			if (ants.targetNeighbour.equals(Search.breakTile)) {
				dist++; insertBreak=true;
			} else {
				int permId = (Math.abs((int) Ants.random.nextLong())%24);
				for (Aim direction : Aim.permAim[permId]) {
					Tile moved = ants.getTile(ants.targetNeighbour, direction);
					if ((ants.turn-ants.pondering.exploration[moved.getRow()][moved.getCol()]>exploreDelay)) {
						Ilk movedIlk=ants.getIlk(moved);
						if ((movedIlk==Ilk.UNKNOWN)||(movedIlk==Ilk.LAND)||(movedIlk==Ilk.SYMMETRY_LAND)) {
							return moved;
						}
					}
					if (ants.isPassableOnDangerLevel(moved)) {
						if (prefereStaying(ants.targetNeighbour,moved)) {
							ants.search.addNotVisitedDelayed(moved);							
						} else {
							ants.search.addNotVisited(moved);
						}
					}
				}
			}
		}
		return null;		
	}	
	private  void exploration(int stopTime) {
		LogFile.write("Exploration"+ants.getTimeRemaining()+"stop at "+stopTime);
		int exploreDelay=8;
		//have to use EXPLORATION_FOOD/PLANNED_EXPLORATION_FOOD for multiple ants with the same target prevention
		for(ants.enemyAttackSearchLevel=ants.safeLevel;ants.enemyAttackSearchLevel<3;ants.enemyAttackSearchLevel++) {
			for (Tile myAnt=ants.getMyAnts().getFirstFilter();myAnt!=null;myAnt=ants.getMyAnts().getNextFilter()) {
				if (ants.getTimeRemaining()<stopTime) return;
				Tile unknown = findTileToExplore(myAnt, 100, exploreDelay);
				if (unknown!=null) {
					ants.pondering.makePlannedExplorationFood(unknown, exploreDelay);
					Tile explore = ants.Nearest(unknown, Ilk.MY_ANT, 100, true);
					if (explore!=null) {
						Tile goWhere = ants.targetNeighbour;
						Aim direction = ants.getDirection(explore,goWhere);
						ants.issueOrder(explore, direction);
					}
				}
			}
		}		
	}

	private  Tile toLessSpread(Tile t1, int optSpread, int infty, int stopTime) {
		ants.search.clear();int dist=0;
		ants.search.addNotVisited(t1);boolean insertBreak=true;
		int bestSpread=myAntsSpread[t1.getRow()/granularity][t1.getCol()/granularity];
		Tile bestTarget=t1;
		while (!ants.search.isEmpty() && (dist<infty) && (bestSpread!=optSpread)) {
			if (ants.getTimeRemaining()<stopTime) return null;
			if (insertBreak) {
				ants.search.addBreak();
				insertBreak=false;
			}
			Tile searched=ants.search.remove();
			if (searched.equals(Search.breakTile)) {
				dist++; insertBreak=true;
			} else {
				for (Aim direction : Aim.values()) {
					Tile moved = ants.getTile(searched, direction);
					if (ants.search.notVisited(moved)) {
						if (ants.isPassableOnDangerLevel(moved)) {
							if (prefereStaying(searched,moved)) {
								ants.search.addNotVisitedDelayed(moved);							
							} else {
								ants.search.addNotVisited(moved);
							}
							int spread=myAntsSpread[moved.getRow()/granularity][moved.getCol()/granularity];
							if (spread<bestSpread) {
								bestSpread=spread;bestTarget=moved;
							}
							if (bestSpread==optSpread) break;
						}
					}
				}
			}
		}
		if (bestTarget.equals(t1)) {
			return null;
		}
		
		dist=ants.travelDistance(bestTarget, t1, dist+2);
		myAntsSpread[bestTarget.getRow()/granularity][bestTarget.getCol()/granularity]+=2;
		//^ to avoid moving a lot of ants across the border
		if (ants.targetNeighbour==Search.breakTile) {return null;}
		return ants.targetNeighbour;
	}
	
	private  void maintainGranularityOri(int stopTime) {
		LogFile.write("MaintainGranularityOri "+ants.getTimeRemaining()+"stop at "+stopTime);
		ants.enemyAttackSearchLevel=2;
		for (Tile myAnt=ants.getMyAnts().getFirstFilter();myAnt!=null;myAnt=ants.getMyAnts().getNextFilter()) {
			if (ants.getTimeRemaining()<stopTime) return;
			int rowg,colg,spread,minspread;
			spread=myAntsSpread[rowg=myAnt.getRow()/granularity][colg=myAnt.getCol()/granularity];
			minspread=Math.min(myAntsSpread[(rowg+grows-1)%grows][(colg+gcols-1)%gcols], myAntsSpread[(rowg+1)%grows][(colg+gcols-1)%gcols]);
			minspread=Math.min(minspread, myAntsSpread[(rowg+grows-1)%grows][(colg+1)%gcols]);
			minspread=Math.min(minspread, myAntsSpread[(rowg+1)%grows][(colg+1)%gcols]);
			if (spread<=minspread) {continue;}
			Tile goWhere = toLessSpread(myAnt, minspread, 25, stopTime);
			if (goWhere!=null) {
				Aim direction = ants.getDirection(myAnt,goWhere);
				if (direction!=null) {
					if (ants.getIlk(myAnt,direction).isPassable()) {
						ants.issueOrder(myAnt, direction);
					}
				}
			}
		}
	}
	
	private  void releaseHive(int stopTime) {
		LogFile.write("ReleaseHive "+ants.getTimeRemaining()+"stop at "+stopTime);
		for (int i=0;i<ants.pondering.getMyOriHills().size();i++){
			Tile myHill=ants.pondering.getMyOriHills().get(i);boolean insertBreak=true;
			if (ants.getHill(myHill)==HillTypes.MY_HILL) {
				if (ants.getIlk(myHill)==Ilk.MY_ANT) {
					boolean blocked=true;
					ants.search.clear();
					ants.search.addNotVisited(myHill);
					while ((!ants.search.isEmpty())) {
						if (insertBreak) {
							ants.search.addBreak();
							insertBreak=false;
						}
						if (ants.getTimeRemaining()<stopTime) return;
						Tile searched=ants.search.remove();
						if (searched.equals(Search.breakTile)) {
							insertBreak=true;
						} else {
							int permId = (Math.abs((int) Ants.random.nextLong())%24);
							for (Aim direction : Aim.permAim[permId]) {
								Tile moved = ants.getTile(searched, direction);
								if (ants.search.notVisited(moved)) {
									Ilk ilk=ants.getIlk(moved);
									if (ilk==Ilk.MY_ANT) {
										if (prefereStaying(searched,moved)) {
											ants.search.addNotVisitedDelayed(moved);
										} else {
											ants.search.addNotVisited(moved);
										}
										//LogFile.write("myAnt ("+moved.getRow()+","+moved.getCol()+")");
									} else if (ilk.isStepable()){
										if (ants.getEnemyAttack(moved)<=2) {
											blocked=false;
											ants.search.clear();
											//LogFile.write("order ("+searched.getRow()+","+searched.getCol()+")"+direction);
											ants.issueOrder(searched, direction);
											if (ants.pondering.distFromMyState==3) {
												int dist=ants.pondering.getDistFromMyHill(i,searched);
												boolean found=true;
												while ((dist>0)&&found) {
													found=false;
													for (Aim dir : Aim.permAim[permId]) {
														Tile backTrace = ants.getTile(searched, dir);
														if (ants.pondering.getDistFromMyHill(i,backTrace)<dist) {
															if (ants.getIlk(backTrace)==Ilk.MY_ANT) {
																//LogFile.write("back order ("+searched.getRow()+","+searched.getCol()+")"+dir.back());
																ants.issueOrder(backTrace, dir.back());
																dist=ants.pondering.getDistFromMyHill(i,backTrace);
																searched=backTrace;
																found=true;
																break;
															}
														}
													}
												}
											}
											break;
										}
									} else if (ilk.ordinal()>Ilk.PLANNED_FOOD.ordinal()){
										//LogFile.write("Blocked by "+ilk+" at ("+moved.getRow()+","+moved.getCol()+")");
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private void detectGroup(Tile myAnt,int stopTime) {
		attackTurnSearch.clear();
		attackTurnSearch.addNotVisited(myAnt);
		antGroup.clear();
		while (!attackTurnSearch.isEmpty()) {
			if (ants.getTimeRemaining()<stopTime) return;
			Tile searched=attackTurnSearch.remove();
			antGroup.add(searched);
			//LogFile.write("adding to group "+searched);
			for(Aim direction:Aim.permAim[0]) {
				Tile moved=ants.getTile(searched, direction);
				if (ants.getIlk(moved)==Ilk.MY_ANT) {
					if (attackTurnSearch.notVisited(moved)) {
						attackTurnSearch.addNotVisited(moved);
						//LogFile.write("adding to search "+moved);
					}
				}
			}
		}		
	}
	
	private void groupAttack(int stopTime) {
		int dirCnt[]=new int[4];
		for(int dirInd=0;dirInd<4;dirInd++) {
			if (ants.getTimeRemaining()<stopTime) return;
			for(Tile groupAnt=antGroup.getFirstNoFilter();groupAnt!=null;groupAnt=antGroup.getNextNoFilter()) {
				Tile moved=ants.getTile(groupAnt, Aim.permAim[0][dirInd]);
				Ilk movedIlk=ants.getIlk(moved);
				if (movedIlk.isPassable()&&(movedIlk!=Ilk.FOOD)) {
					if (ants.getEnemyAttack(moved)>=0) {dirCnt[dirInd]++;}
					if (getDistFromFront(moved)<getDistFromFront(groupAnt)) {dirCnt[dirInd]++;}
					if (ants.pondering.getDistToEnemyHill(moved)<ants.pondering.getDistToEnemyHill(groupAnt)) {dirCnt[dirInd]++;}
				}
			}
		}
		int bestCount=0;Aim bestDir=Aim.NONE;
		for(int dirInd=0;dirInd<4;dirInd++) {
			if (dirCnt[dirInd]>bestCount) {
				bestCount=dirCnt[dirInd];
				bestDir=Aim.permAim[0][dirInd];
			}
		}
		//LogFile.write("bestCount "+bestCount+", bestDir "+bestDir);
		int frontSize=0;
		for(Tile groupAnt=antGroup.getFirstNoFilter();groupAnt!=null;groupAnt=antGroup.getNextNoFilter()) {
			if (ants.getTimeRemaining()<stopTime) return;
			Tile moved=ants.getTile(groupAnt,bestDir);
			Ilk movedIlk=ants.getIlk(moved);
			if (movedIlk.isStepable()) {
				frontSize++;
			}
		}
		if (frontSize<4) return;
		boolean found=true;
		while (found) {
			found=false;
			for(Tile groupAnt=antGroup.getFirstNoFilter();groupAnt!=null;groupAnt=antGroup.getNextNoFilter()) {
				if (ants.getTimeRemaining()<stopTime) return;
				if (ants.getIlk(groupAnt)==Ilk.MY_ANT) {
					Tile moved=ants.getTile(groupAnt,bestDir);
					Ilk movedIlk=ants.getIlk(moved);
					if (movedIlk.isStepable()) {
						found=true;
						ants.issueOrder(groupAnt, bestDir);
					}
				}
			}
		}
		found=true;
		//LogFile.write("BestDir move done, regrouping starts");
		while (found) {
			found=false;
			for(Tile groupAnt=antGroup.getFirstNoFilter();groupAnt!=null;groupAnt=antGroup.getNextNoFilter()) {
				if (ants.getTimeRemaining()<stopTime) return;
				if (ants.getIlk(groupAnt)!=Ilk.MY_ANT) {
					for(Aim dir:Aim.permAim[0]) {
						Tile moved=ants.getTile(groupAnt,dir);
						Ilk movedIlk=ants.getIlk(moved);
						if (movedIlk==Ilk.MY_ANT) {
							found=true;
							ants.issueOrder(moved, dir.back());
						}
					}
				}
			}
		}
		//LogFile.write("Regrouping finished");
	}
	
	private void attackTurn(int stopTime) {
		LogFile.write("Attack turn "+ants.getTimeRemaining()+"stop at "+stopTime);
		//LogFile.write("myAnts: "+ants.getMyAnts()+", myTmpAnts: "+ants.getMyTmpAnts());		
		for(Tile myAnt=ants.getMyAnts().getFirstFilter();myAnt!=null;myAnt=ants.getMyAnts().getNextFilter()) {
			if (ants.getTimeRemaining()<stopTime) return;
			if (ants.getEnemyAttack(myAnt)>=0) {
				//LogFile.write("attacking ant "+myAnt);
				detectGroup(myAnt,stopTime);
				if (antGroup.size()>4) {
					groupAttack(stopTime);
				} 
				for(Tile groupAnt=antGroup.getFirstNoFilter();groupAnt!=null;groupAnt=antGroup.getNextNoFilter()) {
					if (ants.getIlk(groupAnt)==Ilk.MY_ANT) {
						ants.setIlk(groupAnt, Ilk.MY_TMP_ANT);
						ants.getMyTmpAnts().add(groupAnt);
					}
				}
			}
		}
		for(Tile myAnt=ants.getMyAnts().getFirstFilter();myAnt!=null;myAnt=ants.getMyAnts().getNextFilter()) {
			if (ants.getIlk(myAnt)==Ilk.MY_ANT) {
				ants.setIlk(myAnt, Ilk.MY_TMP_ANT);
				ants.getMyTmpAnts().add(myAnt);
			}			
		}
		ants.getMyAnts().clear();
		for(Tile myAnt=ants.getMyTmpAnts().getFirstFilter();myAnt!=null;myAnt=ants.getMyTmpAnts().getNextFilter()) {
			if (ants.getTimeRemaining()<stopTime) return;
			ants.setIlk(myAnt, Ilk.MY_ANT);
			ants.getMyAnts().add(myAnt);
		}
		ants.getMyTmpAnts().clear();
		//LogFile.write("myAnts: "+ants.getMyAnts()+", myTmpAnts: "+ants.getMyTmpAnts());
		turnAgression=2;// no more agression this turn
	}
	
	private  void randomMoves(int stopTime) {
		LogFile.write("Random "+ants.getTimeRemaining()+"stop at "+stopTime);
		Tile moveTo;
		for(ants.enemyAttackSearchLevel=ants.safeLevel;ants.enemyAttackSearchLevel<3;ants.enemyAttackSearchLevel++) {
			for (Tile myAnt=ants.getMyAnts().getFirstFilter();myAnt!=null;myAnt=ants.getMyAnts().getNextFilter()) {
				if (ants.getTimeRemaining()<stopTime) return;
				int permId = (Math.abs((int) Ants.random.nextLong())%120);
				for (Aim direction : Aim.permAim[permId]) {
					if (ants.isPassableOnDangerLevel(ants.getTile(myAnt, direction))&&ants.getIlk(myAnt,direction).isStepable()) {
						if ((ants.getHill(moveTo=ants.getTile(myAnt, direction))!=HillTypes.MY_HILL)||(ants.myHive==0)) {
							if (!prefereStaying(myAnt,moveTo)) {// directed randomness ;)
								ants.issueOrder(myAnt, direction);
								break;
							}
						}
					}
				}
				if (ants.getIlk(myAnt)!=Ilk.MY_ANT) {
					continue;
				}
				for (Aim direction : Aim.permAim[permId]) {
					if (ants.isPassableOnDangerLevel(ants.getTile(myAnt, direction))&&ants.getIlk(myAnt,direction).isStepable()) {
						if ((ants.getHill(ants.getTile(myAnt, direction))!=HillTypes.MY_HILL)||(ants.myHive==0)) {
							ants.issueOrder(myAnt, direction);
							break;
						}
					}
				}
			}
		}		
	}
	
	private  Tile traceBackMyAnt, traceBackMyAntNeighbour;
	private  void traceBackToMyAnt(Tile searched) {//TODO have to work well even for ants planned with midstop!
		traceBackMyAnt=searched;
		traceBackMyAntNeighbour=null;
		int dist=distFromMyAnts[traceBackMyAnt.getRow()][traceBackMyAnt.getCol()];
		while (dist>0) {
			traceBackMyAntNeighbour=traceBackMyAnt;
			int permId = (Math.abs((int) Ants.random.nextLong())%24);
			for (Aim direction : Aim.permAim[permId]) {
				traceBackMyAnt=ants.getTile(traceBackMyAntNeighbour, direction);
				int tmpDist=distFromMyAnts[traceBackMyAnt.getRow()][traceBackMyAnt.getCol()];
				if (tmpDist<dist) {
					if (prefereStaying(traceBackMyAnt,traceBackMyAntNeighbour)) {
						if (tmpDist+2!=dist) {
							//LogFile.write("found non optimal match "+traceBackMyAnt+" "+tmpDist);
							continue;// not the best match
						}
					}
					//LogFile.write("back to "+traceBackMyAnt+" "+tmpDist);
					dist=tmpDist;break;
				}
			}
			if (traceBackMyAntNeighbour.equals(traceBackMyAnt)) {// failed?
				if (ants.getHill(traceBackMyAnt)==HillTypes.MY_HILL) {
					//LogFile.write("BackToMyAnt ended at my hill "+traceBackMyAnt);
					return;
				} else {
					//LogFile.write("BackToMyAnt failed at "+traceBackMyAnt);
					return;
				}
			}
		}
	}
	
	private  void runAway(int stopTime) {
		LogFile.write("RunAway "+ants.getTimeRemaining()+"stop at "+stopTime);
		for (Tile myAnt=ants.getMyAnts().getFirstFilter();myAnt!=null;myAnt=ants.getMyAnts().getNextFilter()) {
			if (ants.getTimeRemaining()<stopTime) return;
			boolean shouldRun=false;
			if (ants.getEnemyAttack(myAnt)>=3) {
				Tile[] attackers=ants.getAttackedByEnemy(myAnt);
				for (Tile attacker:attackers) {
					for (Aim direction : Aim.permAim[0]) {
						Tile attackFrom=ants.getTile(attacker,direction);
						Ilk attackFromIlk=ants.getIlk(attackFrom);
						if (attackFromIlk.isStepable()) {
							if (ants.getMyAttack(attackFrom)==0) {
								shouldRun=true;
								break;
							}
						}
					}
					if (shouldRun) break;
				}
			}
			Aim bestDirection=null;int bestDistFromFront=39999;ArrayList<Tile> neighbourAnts=new ArrayList<Tile>();
			if (shouldRun) {
				int permId = (Math.abs((int) Ants.random.nextLong())%24);
				for (Aim direction : Aim.permAim[permId]) {
					Tile escapeTo=ants.getTile(myAnt,direction);
					Ilk escapeToIlk=ants.getIlk(escapeTo);
					if (escapeToIlk.isStepable()) {
						if (ants.getEnemyAttack(escapeTo)<3) {
							if (getDistFromFront(escapeTo)<bestDistFromFront) {
								bestDistFromFront=getDistFromFront(escapeTo);
								bestDirection=direction;
							}
						}
					} else if (escapeToIlk==Ilk.MY_ANT) {
						neighbourAnts.add(escapeTo);
					}
				}
				if (bestDirection!=null) {
					ants.issueOrder(myAnt,bestDirection);
				} else {
					Tile bestNeighbour=null;
					for(Tile neighbour:neighbourAnts) {
						permId = (Math.abs((int) Ants.random.nextLong())%24);
						for (Aim direction : Aim.permAim[permId]) {
							Tile escapeTo=ants.getTile(neighbour,direction);
							Ilk escapeToIlk=ants.getIlk(escapeTo);
							if (escapeToIlk.isStepable()) {
								if (ants.getEnemyAttack(escapeTo)<3) {
									if (getDistFromFront(escapeTo)<bestDistFromFront) {
										bestDistFromFront=getDistFromFront(escapeTo);
										bestDirection=direction;
										bestNeighbour=neighbour;
									}
								}
							}
						}
					}
					if (bestDirection!=null) {
						ants.issueOrder(bestNeighbour,bestDirection);
						bestDirection = ants.getDirection(myAnt, bestNeighbour);
						ants.issueOrder(myAnt,bestDirection);
					}
				}
			}
		}
	}
	/**
	 * Had to modify fromMyAntsSearch to reflect search started without myAnt the ant should reappear as a richable neighbour of foodToGrab
	 * at appropriate "turn" 
	 * @param myAnt
	 * @param foodToGrab
	 */
	private  void unSearchMyAnt(Tile myAnt,Tile foodToGrab,int stopTime) {//TODO code it
		correctFromMyAntsSearch.clear();
		correctFromMyAntsSearch.addNotVisited(myAnt);
		boolean insertBreak=true;
		while (!correctFromMyAntsSearch.isEmpty()&&(ants.getTimeRemaining()>stopTime)) {
			if (insertBreak) {
				correctFromMyAntsSearch.addBreak();
				insertBreak=false;
			}
			Tile searched=correctFromMyAntsSearch.remove();
			if (searched.equals(Search.breakTile)) {
				insertBreak=true;
			} else {
				boolean undo=true;
				int currDist=distFromMyAnts[searched.getRow()][searched.getCol()];
				distFromMyAnts[searched.getRow()][searched.getCol()]=39999;
				int newDist=currDist+5;
				fromMyAntsSearch.unVisit(searched);
				for (Aim direction : Aim.permAim[0]) {
					Tile moved=ants.getTile(searched, direction);
					Ilk movedIlk=ants.getIlk(moved);
					if (movedIlk.isPassable()) {
						if (correctFromMyAntsSearch.notVisited(moved)) {
							if (fromMyAntsSearch.visited(moved)){
								if (distFromMyAnts[searched.getRow()][searched.getCol()]<=currDist) {
									int tmpDist;
									if(prefereStaying(moved,searched)) {
										tmpDist=distFromMyAnts[moved.getRow()][moved.getCol()]+2;
									} else {
										tmpDist=distFromMyAnts[moved.getRow()][moved.getCol()]+1;
									}
									newDist=Math.min(newDist,tmpDist);
								}
							}
						}
					}
				}
				if (newDist==currDist) {// starts redo process
					undo = false;
				}
				if (newDist==currDist+1) {// start redo for next dist
					correctFromMyAntsSearch.unVisit(searched);
					correctFromMyAntsSearch.addNotVisited(searched);
				}
				if (newDist==currDist+2) {// start redo for nextnext dist
					correctFromMyAntsSearch.unVisit(searched);
					correctFromMyAntsSearch.addNotVisited(searched);
				}
				if (undo) {
					for (Aim direction : Aim.permAim[0]) {
						Tile moved=ants.getTile(searched, direction);
						Ilk movedIlk=ants.getIlk(moved);
						if (movedIlk.isPassable()) {
							if (correctFromMyAntsSearch.notVisited(moved)) {
								if (fromMyAntsSearch.visited(moved)){
									if (distFromMyAnts[moved.getRow()][moved.getCol()]==currDist+2) {// seems went from the ant
										correctFromMyAntsSearch.addNotVisitedDelayed(moved);
									} else if (distFromMyAnts[moved.getRow()][moved.getCol()]==currDist+1) {
										correctFromMyAntsSearch.addNotVisited(moved);
									}
								}
							}
						}
					}
				} 
				if (!undo) {
					fromMyAntsSearch.visit(searched);
					distFromMyAnts[searched.getRow()][searched.getCol()]=currDist;
					for (Aim direction : Aim.permAim[0]) {
						Tile moved=ants.getTile(searched, direction);
						Ilk movedIlk=ants.getIlk(moved);
						if (movedIlk.isPassable()) {
							int tmpDist;
							if(prefereStaying(searched,moved)) {
								tmpDist=currDist+2;
							} else {
								tmpDist=currDist+1;
							}
							if (tmpDist<=distFromMyAntsBound) {
								if (distFromMyAnts[moved.getRow()][moved.getCol()]>tmpDist) {
									distFromMyAnts[moved.getRow()][moved.getCol()]=tmpDist;
									correctFromMyAntsSearch.unVisit(moved);
									if (tmpDist==currDist+1) {
										correctFromMyAntsSearch.addNotVisited(moved);
									} else {
										correctFromMyAntsSearch.addNotVisitedDelayed(moved);
									}
								}
							} else {
								if (tmpDist==distFromMyAntsBound+1) {
									fromMyAntsSearch.addNotVisited(moved);
								} else {//tmpDist=dist+2
									fromMyAntsSearch.addNotVisitedDelayed(moved);
								}
							}
						}
					}
				}
			}
		}
	}
	
	private  void foodSearch(TileList foodList, Ilk listIlk, TileList plannedList, Ilk plannedIlk,int stopTime) {
		LogFile.write("Search "+listIlk+ants.getTimeRemaining()+"stop at "+stopTime);
		boolean insertBreak=false;
		if (distFromMyAntsBound<0) {
			distFromMyAntsBound=0;
			for(int [] row:distFromMyAnts) {
				Arrays.fill(row, 39999);
			}
			fromMyAntsSearch.clear();
			insertBreak=true;
			for(Tile myAnt=ants.getMyAnts().getFirstFilter();myAnt!=null;myAnt=ants.getMyAnts().getNextFilter()) {
				fromMyAntsSearch.addNotVisited(myAnt);
				//LogFile.write("adding ant "+myAnt);
			}
			myMissingHive=ants.getMyHills().size()-ants.myHive;
			for(Tile myHill=ants.getMyHills().getFirstFilter();myHill!=null;myHill=ants.getMyHills().getNextFilter()){
				if (ants.getIlk(myHill)==Ilk.MY_ANT_PLANNED) {
					fromMyAntsSearch.addNotVisitedDelayed(myHill);
					//LogFile.write("adding myHill delayed"+myHill);
					myMissingHive=0;// no further ants generation
				}
			}
		}
		if (distFromMyAntsBound>0) {
			boolean found=true;
			while (found&&(ants.getTimeRemaining()>stopTime)) {
				int minDist=39999;Tile bestFood=null;
				for(Tile food=foodList.getFirstFilter();food!=null;food=foodList.getNextFilter()) {
					if (fromMyAntsSearch.visited(food)) {
						found=true;int tmpDist=distFromMyAnts[food.getRow()][food.getCol()];
						if (tmpDist<minDist) {
							minDist=tmpDist;
							bestFood=food;
						}
					}
				}
				if (bestFood!=null) {
					ants.setIlk(bestFood, plannedIlk);
					plannedList.add(bestFood);
					traceBackToMyAnt(bestFood);
					{// minDist could be small only for FOOD, but FOOD search does start without precomputed distances
						Aim dir=ants.getDirection(traceBackMyAnt, traceBackMyAntNeighbour);
						ants.issueOrder(traceBackMyAnt,dir);
					}
					unSearchMyAnt(traceBackMyAnt,bestFood,stopTime);					
				}
			}
		}
		int foodCnt=foodList.size();
		int antsCnt=ants.getMyAnts().size();
		while (!fromMyAntsSearch.isEmpty()) {
			if ((foodCnt|antsCnt)==0) {
				return;
			}
			if (insertBreak) {
				fromMyAntsSearch.addBreak();
				insertBreak=false;
			}
			if (ants.getTimeRemaining()<stopTime) return;
			Tile searched=fromMyAntsSearch.remove();
			if (searched.equals(Search.breakTile)) {
				distFromMyAntsBound++; insertBreak=true;
				//LogFile.write("distFromMyAntsBound <-"+distFromMyAntsBound);
			} else {
				distFromMyAnts[searched.getRow()][searched.getCol()]=distFromMyAntsBound;
				boolean delay=false;
				if (distFromMyAntsBound==0) {
					for (Aim direction : Aim.permAim[0]) {
						Tile moved=ants.getTile(searched, direction);
						Ilk movedIlk=ants.getIlk(moved);
						if (movedIlk==listIlk) {
							delay=true;
							ants.setIlk(moved, plannedIlk);
							plannedList.add(moved);
							foodCnt--;
							if ((listIlk==Ilk.FOOD)&&(myMissingHive>0)) {
								myMissingHive--;
								if (myMissingHive==0) {
									for(Tile myHill=ants.getMyHills().getFirstFilter();myHill!=null;myHill=ants.getMyHills().getNextFilter()){
										if(ants.getIlk(myHill)==Ilk.LAND) {
											fromMyAntsSearch.addNotVisited(myHill);antsCnt++;
											//LogFile.write("adding myHill spawn "+myHill);
										}
										// this is not exact as some hills will produce ant earlier, and the eating could fail as well ...
									}									
								}
							}
						}
					}
				}
				if (delay) {
					ants.setIlk(searched, Ilk.MY_ANT_PLANNED);
					ants.getMyAntsPlanned().add(searched);
					fromMyAntsSearch.unVisit(searched);
					fromMyAntsSearch.addNotVisited(searched); //plan for next target at distance 1
					//LogFile.write("Adding my planned ant delayed "+searched);
				} else {
					int permId = (Math.abs((int) Ants.random.nextLong())%24);
					for (Aim direction : Aim.permAim[permId]) {
						Tile moved=ants.getTile(searched, direction);
						if (fromMyAntsSearch.notVisited(moved)) {
							Ilk movedIlk=ants.getIlk(moved);
							if (movedIlk.isPassable()) {
								if (movedIlk==listIlk) {
									ants.setIlk(moved, plannedIlk);
									plannedList.add(moved);
									foodCnt--;antsCnt--;
									if ((listIlk==Ilk.FOOD)&&(myMissingHive>0)) {
										myMissingHive--;
										if (myMissingHive==0) {
											for(Tile myHill=ants.getMyHills().getFirstFilter();myHill!=null;myHill=ants.getMyHills().getNextFilter()){
												fromMyAntsSearch.addNotVisited(myHill);antsCnt++;
												// this is not exact as some hills will produce ant earlier, and the eating could fail as well ...
											}									
										}
									}
									//LogFile.write("tracing back from food "+searched);
									traceBackToMyAnt(searched);
									if (distFromMyAntsBound==2) {//TODO have to chose direction
										Aim dir=ants.getDirection(traceBackMyAnt, traceBackMyAntNeighbour);
										ants.issueOrder(traceBackMyAnt,dir);										
									} else {
										Aim dir=ants.getDirection(traceBackMyAnt, traceBackMyAntNeighbour);
										ants.issueOrder(traceBackMyAnt,dir);
									}
									if (antsCnt==0) {// distFromMyAnts will not be needed this turn anymore
										return;
									}
									//LogFile.write("unsearching "+traceBackMyAnt);
									unSearchMyAnt(traceBackMyAnt,moved,stopTime);
								}
								if (prefereStaying(searched,moved)) {
									fromMyAntsSearch.addNotVisitedDelayed(moved);
									//LogFile.write("adding place delayed "+moved);
								} else {
									fromMyAntsSearch.addNotVisited(moved);
									//LogFile.write("adding place "+moved);
								}
							}
						}
					}
				}
			}
		}
	}
	
	private  void explorationFoodSearch(int stopTime) {
		LogFile.write("Search EXPLORATION_FOOD"+ants.getTimeRemaining()+"stop at "+stopTime);
		boolean insertBreak=false;
		if (distFromMyAntsBound<0) {
			distFromMyAntsBound=0;
			for(int [] row:distFromMyAnts) {
				Arrays.fill(row, 39999);
			}
			fromMyAntsSearch.clear();
			insertBreak=true;
			for(Tile myAnt=ants.getMyAnts().getFirstFilter();myAnt!=null;myAnt=ants.getMyAnts().getNextFilter()) {
				fromMyAntsSearch.addNotVisited(myAnt);
			}
			myMissingHive=ants.getMyHills().size()-ants.myHive;
			for(Tile myHill=ants.getMyHills().getFirstFilter();myHill!=null;myHill=ants.getMyHills().getNextFilter()){
				if (ants.getIlk(myHill)==Ilk.MY_ANT_PLANNED) {
					fromMyAntsSearch.addNotVisitedDelayed(myHill);
					myMissingHive=0;// no further ants generation
				}
			}
		}
		if (distFromMyAntsBound>0) {
			boolean found=true;
			while (found&&(ants.getTimeRemaining()>stopTime)) {
				int minDist=39999;Tile bestFood=null;
				for(Tile food=ants.getExplorationFoodTiles().getFirstFilter();food!=null;food=ants.getExplorationFoodTiles().getNextFilter()) {
					if (fromMyAntsSearch.visited(food)) {
						found=true;int tmpDist=distFromMyAnts[food.getRow()][food.getCol()];
						if (tmpDist<minDist) {
							minDist=tmpDist;
							bestFood=food;
						}
					}
				}
				if (bestFood!=null) {
					List<Tile> symTs = ants.pondering.actualSymmetry(bestFood);symTs.add(bestFood);
					for(Tile symT:symTs) {
						Ilk symIlk=ants.getIlk(symT);
						if (symIlk==Ilk.EXPLORATION_FOOD) {
							ants.setIlk(symT, Ilk.PLANNED_EXPLORATION_FOOD);
							ants.getPlannedExplorationFoodTiles().add(symT);
						}
					}
					traceBackToMyAnt(bestFood);
					{// minDist could be small only for FOOD, but FOOD search does start without precomputed distances
						Aim dir=ants.getDirection(traceBackMyAnt, traceBackMyAntNeighbour);
						ants.issueOrder(traceBackMyAnt,dir);
					}
					unSearchMyAnt(traceBackMyAnt,bestFood,stopTime);					
				}
			}
		}
		int foodCnt=ants.getExplorationFoodTiles().size();
		int antsCnt=ants.getMyAnts().size();
		while (!fromMyAntsSearch.isEmpty()) {
			if ((foodCnt|antsCnt)==0) {
				return;
			}
			if (insertBreak) {
				fromMyAntsSearch.addBreak();
				insertBreak=false;
			}
			if (ants.getTimeRemaining()<stopTime) return;
			Tile searched=fromMyAntsSearch.remove();
			if (searched.equals(Search.breakTile)) {
				distFromMyAntsBound++; insertBreak=true;
			} else {
				distFromMyAnts[searched.getRow()][searched.getCol()]=distFromMyAntsBound;
				boolean delay=false;
				if (distFromMyAntsBound==0) {
					for (Aim direction : Aim.permAim[0]) {
						Tile moved=ants.getTile(searched, direction);
						Ilk movedIlk=ants.getIlk(moved);
						if (movedIlk==Ilk.EXPLORATION_FOOD) {
							delay=true;
							List<Tile> symTs = ants.pondering.actualSymmetry(moved);symTs.add(moved);
							for(Tile symT:symTs) {
								Ilk symIlk=ants.getIlk(symT);
								if (symIlk==Ilk.EXPLORATION_FOOD) {
									ants.setIlk(symT, Ilk.PLANNED_EXPLORATION_FOOD);
									ants.getPlannedExplorationFoodTiles().add(symT);
									foodCnt--;
								}
							}
						}
					}
				}
				if (delay) {
					ants.setIlk(searched, Ilk.MY_ANT_PLANNED);
					ants.getMyAntsPlanned().add(searched);
					fromMyAntsSearch.unVisit(searched);
					fromMyAntsSearch.addNotVisited(searched); //plan for next target at distance 1
				} else {
					int permId = (Math.abs((int) Ants.random.nextLong())%24);
					for (Aim direction : Aim.permAim[permId]) {
						Tile moved=ants.getTile(searched, direction);
						if (fromMyAntsSearch.notVisited(moved)) {
							Ilk movedIlk=ants.getIlk(moved);
							if (movedIlk.isPassable()) {
								if (movedIlk==Ilk.EXPLORATION_FOOD) {
									List<Tile> symTs = ants.pondering.actualSymmetry(moved);symTs.add(moved);
									for(Tile symT:symTs) {
										Ilk symIlk=ants.getIlk(symT);
										if (symIlk==Ilk.EXPLORATION_FOOD) {
											ants.setIlk(symT, Ilk.PLANNED_EXPLORATION_FOOD);
											ants.getPlannedExplorationFoodTiles().add(symT);
											foodCnt--;
										}
									}
									antsCnt--;
									traceBackToMyAnt(searched);
									Aim dir=ants.getDirection(traceBackMyAnt, traceBackMyAntNeighbour);
									ants.issueOrder(traceBackMyAnt,dir);
									if (antsCnt==0) {// distFromMyAnts will not be needed this turn anymore
										return;
									}
									unSearchMyAnt(traceBackMyAnt,moved,stopTime);
								}
								if (prefereStaying(searched,moved)) {
									fromMyAntsSearch.addNotVisitedDelayed(moved);							
								} else {
									fromMyAntsSearch.addNotVisited(moved);
								}
							}
						}
					}
				}
			}
		}
	}

	private int enemyToHillDist() {
		int res=40000,dist;
		for(Tile enemyAnt=ants.getEnemyAnts().getFirstFilter();enemyAnt!=null;enemyAnt=ants.getEnemyAnts().getNextFilter()) {
			if ((dist=ants.pondering.getMinDistFromMyHill(enemyAnt))<res) {
				res=dist;
			}
		}
		return res;
	}
	
	private  int distFromFront[][];
	
	private  int getDistFromFrontModOK(int r,int c) {
		return distFromFront[r][c];
	}
	public  int getDistFromFront(int row,int col) {
		int r=(row+ants.safeModShift)%ants.getRows(),c=(col+ants.safeModShift)%ants.getCols();
		return getDistFromFrontModOK(r,c);
	}
	public  int getDistFromFront(Tile t) {
		return getDistFromFrontModOK(t.getRow(),t.getCol());
	}
	
	private void calcDistFromFront(int stopTime){
		LogFile.write("calcDistFromFront (front size "+ants.getFrontLine().size()+")"+ants.getTimeRemaining()+"stop at "+stopTime);
		//LogFile.write("frontline "+ants.getFrontLine());
		//ants.pondering.printDist();
		ants.search.clear();
		for(int[]row:distFromFront) {
			Arrays.fill(row, 39999);
		}
		if (!maintainFrontLine) {
			return;
		}
		for(Tile front=ants.getFrontLine().getFirstNoFilter();front!=null;front=ants.getFrontLine().getNextNoFilter()) {
			if (!ants.getEnemyFree(front)) {
				ants.search.addNotVisited(front);
			}
		}
		boolean insertBreak=true;int dist=0;
		while (!ants.search.isEmpty()) {
			if (ants.getTimeRemaining()<=stopTime) break;
			if (insertBreak) {
				ants.search.addBreak();
				insertBreak=false;
			}
			ants.targetNeighbour=ants.search.remove();
			if (ants.targetNeighbour.equals(Search.breakTile)) {
				dist++; insertBreak=true;
			} else {
				distFromFront[ants.targetNeighbour.getRow()][ants.targetNeighbour.getCol()]=dist;
				for (Aim direction : Aim.permAim[0]) {
					Tile moved = ants.getTile(ants.targetNeighbour, direction);
					if (ants.getEnemyFree(moved)) {
						if (ants.getIlk(moved).isPassable()) {
							ants.search.addNotVisited(moved);
						}
					}
				}
			}
		}
		//ants.pondering.printDist();
	}
	
	//private void invokeBug() {
	//	for(Tile myAnt=ants.getMyAnts().getFirstFilter();myAnt!=null;myAnt=ants.getMyAnts().getNextFilter()) {
	//		ants.issueOrder(myAnt, Aim.permAim[0][0]);
	//	}
	//}
	
	public  boolean prefereStaying(Tile oriAnt,Tile newAnt) {
		//return false;
		return (getDistFromFront(oriAnt)<getDistFromFront(newAnt));
	}
	
	/**
	 * For every ant check every direction in fixed order (N, E, S, W) and move
	 * it if the tile is passable.
	 */
	@Override
	public int doTurn() {
		setTimeKoef();
		//turnAgression=ants.turn%56;turnAgression/=8;turnAgression%=4;
		turnAgression=ants.turn%25;turnAgression/=5;
		distFromMyAntsBound=-1;
		if (turnAgression>=3) {turnAgression=99;}
		eating = ants.getMyHills().size()*(ants.getTurns()-ants.turn)>ants.myHive;
		//makingWalls =(riskHillDist<10)||((ants.turn>100)&&(eating)&&(turnAgression<99)); // test
		//if (riskHillDist<10) {turnAgression=99;}
		ants.enemyAttackSearchLevel=2;
		supportSpawning(stopTime(100,550));
		spawnDefense(stopTime(100,500));
		if (ants.getTimeRemaining()<0) return ants.getTimeRemaining();
		stupidAttack(stopTime(100,450));
		if (ants.getTimeRemaining()<0) return ants.getTimeRemaining();
		//granularityCount(stopTime(200,400));
		calcDistFromFront(stopTime(200,400));
		if (ants.getTimeRemaining()<300) {
			maintainFrontLine=false;
		}
		//ants.pondering.printDist();
		if (ants.getTimeRemaining()<0) return ants.getTimeRemaining();
		if (turnAgression==99) {
			attackTurn(stopTime(400,300));
		}
		if (ants.getTimeRemaining()<0) return ants.getTimeRemaining();
		boolean choice=true;
		if (eating) {
			if (choice) {
				foodPlanning(ants.getFoodTiles(),Ilk.FOOD,ants.getPlannedFoodTiles(),Ilk.PLANNED_FOOD,100,stopTime(800,250));
				if (ants.getTimeRemaining()<0) return ants.getTimeRemaining();
				foodPlanning(ants.getExpectedFoodTiles(),Ilk.EXPECTED_FOOD,ants.getPlannedFoodTiles(),Ilk.PLANNED_FOOD,100,stopTime(800,250));
				if (ants.getTimeRemaining()<0) return ants.getTimeRemaining();
				foodPlanning(ants.getSymmetryFoodTiles(),Ilk.SYMMETRY_FOOD,ants.getPlannedSymmetryFoodTiles(),Ilk.PLANNED_SYMMETRY_FOOD,100,stopTime(200,250));
				if (ants.getTimeRemaining()<0) return ants.getTimeRemaining();				
			} else {
				foodSearch(ants.getFoodTiles(),Ilk.FOOD,ants.getPlannedFoodTiles(),Ilk.PLANNED_FOOD,stopTime(800,250));
				if (ants.getTimeRemaining()<0) return ants.getTimeRemaining();
				foodSearch(ants.getExpectedFoodTiles(),Ilk.EXPECTED_FOOD,ants.getPlannedFoodTiles(),Ilk.PLANNED_FOOD,stopTime(800,250));
				if (ants.getTimeRemaining()<0) return ants.getTimeRemaining();
				foodSearch(ants.getSymmetryFoodTiles(),Ilk.SYMMETRY_FOOD,ants.getPlannedSymmetryFoodTiles(),Ilk.PLANNED_SYMMETRY_FOOD,stopTime(200,250));
				if (ants.getTimeRemaining()<0) return ants.getTimeRemaining();				
			}
		}
		releaseHive(stopTime(100,230));
		if (ants.getTimeRemaining()<0) return ants.getTimeRemaining();
		//maintainGranularityOri(stopTime(100,200));
		//if (ants.getTimeRemaining()<0) return ants.getTimeRemaining();
		defendStupidAttack(stopTime(100,170));
		if (ants.getTimeRemaining()<0) return ants.getTimeRemaining();
		slowAttack(stopTime(100,130));
		if (ants.getTimeRemaining()<0) return ants.getTimeRemaining();
		runAway(stopTime(200,90));
		if (ants.getTimeRemaining()<0) return ants.getTimeRemaining();
		reinforcements(stopTime(80,70));
		if (ants.getTimeRemaining()<0) return ants.getTimeRemaining();
		if (eating) {
			if (choice) {
				explorationFoodPlanning(100,stopTime(50,50));
			} else {
				explorationFoodSearch(stopTime(50,50));
			}
			if (ants.getTimeRemaining()<0) return ants.getTimeRemaining();
			exploration(stopTime(100,40));
			if (ants.getTimeRemaining()<0) return ants.getTimeRemaining();
		}
		randomMoves(stopTime(100,0));
		if (ants.getTimeRemaining()>300) {
			frontLineTest++;
			if (frontLineTest>10) {
				maintainFrontLine=true;
				frontLineTest=0;
			}
		}
		return ants.getTimeRemaining();
	}
}
