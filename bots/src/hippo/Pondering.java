package hippo;
import java.math.BigInteger;
import java.util.*;

public class Pondering {
	// TODO not only water for symmetry testing 
	// Better strategy will be to maintain several possible symmetries and check each hill/new food to correspond to the symmetries.
	// so search according water, check according to newfood/hills
	// TODO more symmetry levels (one main symmetry, one secondary  
	// ... when collision detected only the collision level is rolled back.
	// ... this will allow better use of pondering time and it will find more complicated combined symmetries
	// (shift+central and so on) ... this will search the symmetry independently by it's dimensions.
	// each level will require independent traversal of Water and Land TileList so interface to TileList should be changed
	// (get i-th ... returns null if list is shorter)
	// Damn I used to use (x,y) coordinates in this order but (rows,cols) ... let x~row, y~col!
	// TODO rewrite to use superbig Ints to represent whole maps addressing by row+col*(2rows)	
	// TODO Orbit of the symmetry could have nrPlayers*x where x is divisor of startOnePlayerHills. In that case hills are divided to bunches of x
	// each x on the same orbit.
	
	private boolean [][] seenLand;
	// TODO ^ ... the map could have symmetry which is not used for food generation. this symmetry should be canceled as well!
	// be carefull seeing ant instead land is not sufficient condition!!
	private  Ants ants;
	public  int[][] lastSymmetrySeen;	//values greater than symTurn are interesting
	public  int[][] exploration;
	//TODO think ... when x < turn-exploration tile could become EXPLORATION_FOOD that sets exploration in whole visibility disk
	// exploration >= lastSymmetrySeen EXPLORATION_FOOD on UNKNOWN or LAND, never on WATER!
	// x should depend on the length of EXPLORATION_FOOD_TILES longer list longer x (/10?)
	// when turn - exploration > x on EXPLORATION_FOOD the EXPLORATION_FOOD is reverted back to UNKNOWN/LAND.
	// UNKNOWN/LAND could be converted to EXPLORATION_FOOD/PLANNED_EXPLORATION_FOOD by standard exploration ... just to prevent more ants
	// to explore the same symmetric teritory.
	private  int twiceRows;
	private  BigInteger shiftedBy2Row;
	private  BigInteger bitOfFourBoards; // (2^0+2^rows)(2^0+2^(2rows*cols))
	private  BigInteger rowBI;
	private  BigInteger colBI;
	private  BigInteger oneBoard; // (2^(2rows*cols)-1)/(2^rows+2^0)
	private  int gcdSizes; // for switchCordsCases
	private  BigInteger bitOfGcdBoard;
	private  BigInteger chessBoard; // full option for switchCords cases TODO
	// gcdCol=(2^(2rows*gcdSizes)-1)/(2^(2rows)-1)
	// gcdRow=(2^gcdSizes-1)
	// gcdBoard = gcdRow*gcdCol
	// Vstripes = gcdCol*(2^gcdSizes-1)/(2^2-1)
	// Hstripes = gcdRow*(2^(2rows*gcdSizes)-1)/(2^(4rows)-1)
	// chessBoard = vStripes xor hStripes xor gcdBoard
	private  BigInteger land=BigInteger.ZERO;
	private  BigInteger landXSwap=BigInteger.ZERO;
	private  BigInteger landYSwap=BigInteger.ZERO;
	private  BigInteger landCSwap=BigInteger.ZERO;
	private  BigInteger landGcd=BigInteger.ZERO;
	private  BigInteger landGcdRotClock=BigInteger.ZERO;
	private  BigInteger landGcdRotCClock=BigInteger.ZERO;
	private  BigInteger landGcdBSwap=BigInteger.ZERO;
	private  BigInteger landGcdDSwap=BigInteger.ZERO;
	private  BigInteger water=BigInteger.ZERO;
	private  BigInteger waterXSwap=BigInteger.ZERO;
	private  BigInteger waterYSwap=BigInteger.ZERO;
	private  BigInteger waterCSwap=BigInteger.ZERO;
	private  BigInteger waterGcd=BigInteger.ZERO;
	private  BigInteger waterGcdRotClock=BigInteger.ZERO;
	private  BigInteger waterGcdRotCClock=BigInteger.ZERO;
	private  BigInteger waterGcdBSwap=BigInteger.ZERO;
	private  BigInteger waterGcdDSwap=BigInteger.ZERO;

	private  BigInteger[] possibleSym=new BigInteger[SymmetryType.values().length];
	private  BigInteger[] activeSym=new BigInteger[SymmetryType.values().length];
//	public  BigInteger possibleRXSym; // init to oneBoard or rather chessBoard
	// (8)	center could be on half integer coordinates expects CSym, just for rows=cols* case 
	// (v \equiv w) mod 2  ((v+w)/2-y,(w-v)/2+x) and (v-x,y) addressed by rows
//	public  BigInteger possibleXYSym;// init ot oneBoard
	// (4)	(x,w-y) and (v-x,y)
//	public  BigInteger possibleRSym;// init to oneBoard or rather chessBoard
	// (4)	center could be on half integer coordinates expects CSym, just for rows=cols* case
	// addressed by rows (v \equiv w) mod 2  ((v+w)/2-y,(w-v)/2+x)
//	public  BigInteger possibleCSym;// init ot oneBoard
	// (2)	 center could be on half integer coordinates (v-x,w-y) addressed by rows
//	public  BigInteger possibleDSym;// init to (2^rows-2^0) 
	// (2) may be BigInteger would be better across / , just for rows=cols* case (y-k,x+k)
//	public  BigInteger possibleBSym;// init to (2^rows-2^0)
	// (2) may be BigInteger would be better across \ , just for rows=cols* case (k-y,k-x)
//	public  BigInteger possibleYSym;// init to (2^(2rows*cols)-2^0)/(2^(2rows)-2^0)
	// (2)	across - (x,k-y) (2)
//	public  BigInteger possibleXSym;// init to (2^rows-2^0)
	// (2)	across | (k-x,y) (2)
//	public  BigInteger possibleSSym;// possible shift positions
//	public  BigInteger possibleSym;// union of all possibleSym's
	// row=cols* could actually be after parallel shift.
	public  BigInteger possibleShift[]=null; // may be other representation? First coordinate P, k,l in [0,1,..,P-1], k=l=0 is forbidden
	// k is remaining coordinate, l-th bit of possibleShift[P][k] represents availability of [P][k][l].  
	// number of players P must divide both cols and rows, possibleShift = k*rows/P, l*cols/P
	// we should test it for each prime dividing gcd(rows,cols). If prime fails, all it's multiples fail as well.
	// when prime works, we could continue on it's multiples still dividing gcd.
	// TODO would Biginteger be better? representing shift in board notation?
	// what about prime generators and their orbits (primitive subset (directed graph of intersecting orbits) to composed "orbits")
	// cancel of an orbit cancels all followers
	public  TileList newlySeenTiles;
	private  boolean failed=false;	// true means symmetry was used and should be cleared
	public  boolean dirty=false;
	public  int testedX=0,testedY=0;
	public  int shiftPlayers=10, symTurn=0;
	private  int moreSafeTime;
	public  BigInteger landOfActiveSymmetries[]=new BigInteger [16];
	public  BigInteger waterOfActiveSymmetries[]=new BigInteger [16];
	private  BigInteger unknownIntersectOfActiveSymmetries[]=new BigInteger [16];
	private  SymmetryType activeSymmetryType[]=new SymmetryType[16];
	private  int activeSymmetryCoord[]=new int[16];
	private  TileList activeSymmetrySymFood[]=new TileList[16];
	private  boolean activeSymmetryFail[]=new boolean[16];
	private  int activeSymmetryShiftPlayers[]=new int[16];
	public  SymmetryType actualSymmetryType=SymmetryType.S;
	private  int firstKnownIndex;
	private  int lastSymmetryIndex=-1;
	private  boolean seekingForSymmetries=true;
	private  BigInteger toTestSym;
	//* distToEnemy computation
	private  Search distToEnemySearch;
	public  int[][] distToEnemyHill;
	public  int distToEnemyState=0;// 0 not started 1 cleared, hills not added 2 in computation 3 done
	private  int distToEnemy=0;
	
	private  List<Tile> myOriHills = new ArrayList<Tile>();// to maintain the same order of my hills
	private  Search[] distFromMySearch;
	private  int []distFromMy;
	private  int[][][] distFromMyHill;
	private  int[][] minDistFromMyHill;
	private  int[][] sumDistFromMyHill;
	public  int distFromMyState=0;// 0 not started 1 cleared, hills not added 2 in computation 3 done
	private  int distFromMyCalcFilter;// distance from which hills is not computed yet
	private MyBot mybot;
	public  int gcd(int a,int b) {
		int c=a;
		while (b!=0) {
			c=a%b;
			a=b;
			b=c;
		}
		return a;
	}

	private  int getSymmetryIndex(int coord) {
		int minInd=0, maxInd=firstKnownIndex, mid;
		if (maxInd<0) {
			maxInd=lastSymmetryIndex;
		}
		while (minInd<maxInd) {
			mid=(minInd+maxInd)/2;
			if (unknownIntersectOfActiveSymmetries[mid].testBit(coord)) {
				minInd=mid+1;
			} else {
				maxInd=mid;
			}
		}
		return minInd;
	}

	private  int getSymmetryIndex(Tile t) {
		return getSymmetryIndex(t.getRow()+t.getCol()*twiceRows);
	}

	public  Ilk getUnknownIlk(int row,int col) {//TODO test ... and filling the BigInteger arrays
		// called from ants.getIlk for Ilk.UNKNOWN
		int coord=row+col*twiceRows;
		int ind=getSymmetryIndex(coord);
		if (ind<=lastSymmetryIndex) {
			if (activeSymmetryFail[ind]) {
				return Ilk.UNKNOWN; 
			}
			if (landOfActiveSymmetries[ind].testBit(coord)) {
				return Ilk.SYMMETRY_LAND;
			}
			if (waterOfActiveSymmetries[ind].testBit(coord)) {
				return Ilk.SYMMETRY_WATER;
			}
			//Logfile.write("bug in Intersect of activeSymmetries"); 
		}
		return Ilk.UNKNOWN;
	}
	
	public  Ilk getUnknownIlk(Tile t) {
		return getUnknownIlk(t.getRow(),t.getCol());
	}
	
	public  boolean seekingForSymmetries() {
		if (!seekingForSymmetries) {return false;}
		return (lastSymmetryIndex<15);
	}
	
	public  boolean activated() {
		return lastSymmetryIndex>=0;
	}
	
	public  List<Tile> getMyOriHills() {
		return myOriHills;
	}
	
	public  int getDistFromMyHill(int hillNo, Tile t) {
		if (distFromMyState<3) return 40000;
		if (distFromMyHill[hillNo]==null) return 40000;
		return distFromMyHill[hillNo][t.getRow()][t.getCol()];
	}
	
	public  int getMinDistFromMyHill(Tile t) {
		if (distFromMyState<3) return 40000;
		return minDistFromMyHill[t.getRow()][t.getCol()];
	}
	
	public  int getDistToEnemyHill(Tile t) {
		if (distToEnemyState<3) return 40000;
		return distToEnemyHill[t.getRow()][t.getCol()];
	}
	
	public  int getExploration(Tile t) {
		return exploration[t.getRow()][t.getCol()];
	}
	
	public  void setExploration(Tile t,int e) {
		exploration[t.getRow()][t.getCol()]=e;
	}
	private  String padColBits(BigInteger bits) {
		return bits.or(BigInteger.ONE.shiftLeft(ants.getRows()<<1)).toString(2).substring(1);
	}
	
	public  String padRowBits(BigInteger bits) {
		return bits.or(BigInteger.ONE.shiftLeft(ants.getCols()<<1)).toString(2).substring(1);
	}

	/* returns leader with 4 "leading spaces" */
	public  String bigEndianLeader() {
		StringBuilder row=new StringBuilder();
		for(int i=0;i<ants.getCols();i+=5) {
			row.append("  "+(""+(i+1000)).substring(1));
		}
		return row.toString();
	}
	
	/* returns leader with 4 "leading spaces" */
	private  String lowEndianLeader() {
		StringBuilder row=new StringBuilder();
		int mod=ants.getCols()%5; row.append("    ".substring(0, mod));
		for(int i=ants.getCols()-mod;i>=0;i-=5) {
			row.append("  "+(""+(i+1000)).substring(1));
		}
		return row.toString().substring(1);
	}

	private  BigInteger shift2BIOrbit(int XCoord,int YCoord){
		BigInteger ret=BigInteger.ONE;
		for(int x=XCoord,y=YCoord;(x|y)!=0;x=(x+XCoord)%ants.getRows(),y=(y+YCoord)%ants.getCols()) {
			ret=ret.setBit(x+y*twiceRows);
		}
		return ret;
	}
	
	private  boolean multiHillShiftOK(int XCoord,int YCoord,int multi,int nrPlayers) {// tests if each hill contains exactly multi on cycle
		for(Tile t=ants.getMyExpectedHills().getFirstNoFilter();t!=null;t=ants.getMyExpectedHills().getNextNoFilter()) {
			int orbitLen=1,countHillsOnOrbit=1,x0=t.getRow(),y0=t.getCol();
			for(int x=(x0+XCoord)%ants.getRows(),y=(y0+YCoord)%ants.getCols();
				((x-x0)|(y-y0))!=0;
				x=(x+XCoord)%ants.getRows(),y=(y+YCoord)%ants.getCols()) {
				if (ants.hills[x][y]==HillTypes.NO_HILL) return false;
				if (ants.hills[x][y]==HillTypes.MY_HILL) {countHillsOnOrbit++;}
				orbitLen++;
			}
			if (countHillsOnOrbit!=multi) {
				return false;
			}
			if (orbitLen!=multi*nrPlayers) {
				return false;
			}
		}
		return true;
	} 
	
	public  void initFirstTurn() {
		if (ants.turn==0) return;
		//LogFile.write("initShifts");
		int nrPlayers=10;
		while ((ants.getRows()%nrPlayers!=0)&&(ants.getCols()%nrPlayers!=0)) {//TODO this is for 2D shifts, but 1D shifts could be possible
			nrPlayers--;
		}
		int nrHills = ants.getMyExpectedHills().size();
		for(Tile myHill=ants.getMyExpectedHills().getFirstNoFilter();myHill!=null;myHill=ants.getMyExpectedHills().getNextNoFilter()) {
			myOriHills.add(myHill);
		}
		possibleShift = new BigInteger[1+nrPlayers];
		Arrays.fill(possibleShift, BigInteger.ZERO);
		while (nrPlayers>1) {
			for(int multi=1;multi<=nrHills;multi++) {
				if (nrHills%multi==0) {
					if (gcdSizes%(nrPlayers*multi)==0) {
						boolean possibleShiftMap[][]=new boolean [nrPlayers*multi][nrPlayers*multi];
						for(boolean[] row:possibleShiftMap)
							Arrays.fill(row, true);
						possibleShiftMap[0][0]=false;
						for(int r=0;r<nrPlayers*multi;r++) {
							for(int c=0;c<nrPlayers*multi;c++) {
								if (possibleShiftMap[r][c]) {
									int XCoord=(ants.getRows()*r)/(nrPlayers*multi),YCoord=(ants.getCols()*c)/(nrPlayers*multi);
									for (int xi=r,yi=c;xi+yi!=0;xi=(xi+r)%(nrPlayers*multi),yi=(yi+c)%(nrPlayers*multi)) {
										possibleShiftMap[r][c]=false;
									}
									// test multi on my hills:
									if (multiHillShiftOK(XCoord,YCoord,multi,nrPlayers)) {
										possibleSym[indS]=possibleSym[indS].setBit(XCoord+YCoord*twiceRows);
										possibleShift[nrPlayers]=possibleShift[nrPlayers].andNot(shift2BIOrbit(XCoord,YCoord)).setBit(XCoord+YCoord*twiceRows);
									}
								}
							}
						}
					} else if (ants.getRows()%(nrPlayers*multi)==0) {
						int XCoord=ants.getRows()/(nrPlayers*multi),YCoord=0;
						if (multiHillShiftOK(XCoord,YCoord,multi,nrPlayers)) {
							possibleSym[indS]=possibleSym[indS].setBit(XCoord+YCoord*twiceRows);
							possibleShift[nrPlayers]=possibleShift[nrPlayers].andNot(shift2BIOrbit(XCoord,YCoord)).setBit(XCoord+YCoord*twiceRows);
						}
					} else if (ants.getCols()%nrPlayers==0) {
						int XCoord=0,YCoord=ants.getCols()/(nrPlayers*multi);
						if (multiHillShiftOK(XCoord,YCoord,multi,nrPlayers)) {
							possibleSym[indS]=possibleSym[indS].setBit(XCoord+YCoord*twiceRows);
							possibleShift[nrPlayers]=possibleShift[nrPlayers].andNot(shift2BIOrbit(XCoord,YCoord)).setBit(XCoord+YCoord*twiceRows);
						}
					}
				}
			}
			nrPlayers--;
		}
		distToEnemyHill = new int[ants.getRows()][ants.getCols()];
		for(int [] row:distToEnemyHill) {
			Arrays.fill(row, 39999); // game rules infinity
		}
		distFromMyHill = new int[myOriHills.size()][ants.getRows()][ants.getCols()];
		for(int [][] distFromHill:distFromMyHill) {
			for(int [] row:distFromHill) {
				Arrays.fill(row, 39999); // game rules infinity
			}
		}
		minDistFromMyHill = new int[ants.getRows()][ants.getCols()];
		for(int [] row:minDistFromMyHill) {
			Arrays.fill(row, 39999); // game rules infinity
		}
		int sumDist=39999*myOriHills.size();
		sumDistFromMyHill = new int[ants.getRows()][ants.getCols()];
		for(int [] row:sumDistFromMyHill) {
			Arrays.fill(row, sumDist); // game rules infinity
		}
		distFromMy = new int[myOriHills.size()];
		distFromMySearch = new Search[myOriHills.size()];
		Arrays.fill(distFromMy, -1);
		for(int i=0;i<myOriHills.size();i++) {
			distFromMySearch[i]=new Search();
		}
		//LogFile.write("initShifts end");
	}
	
	public  void see(Tile t){
		//LogFile.write("Pondering.see("+t+")");
		List<Tile> symList = actualSymmetry(t);symList.add(t);
		//LogFile.write("sym Tiles:"+symList);
		for(Tile tt:symList) {
			lastSymmetrySeen[tt.getRow()][tt.getCol()]=ants.turn;
			exploration[tt.getRow()][tt.getCol()]=ants.turn;
			Ilk ilk=ants.getIlk(tt);
			if (ilk==Ilk.PLANNED_EXPLORATION_FOOD) {
				if (ants.getLastSeen(tt)<0) {
					ants.setIlk(tt, Ilk.UNKNOWN);
				} else {
					ants.setIlk(tt, Ilk.LAND);
				}
			}
		}
	}
	
 	public  void init(Ants ants_, MyBot _mybot) {
		ants=ants_;
		this.mybot = _mybot;
		distToEnemySearch=new Search();
		lastSymmetrySeen = new int[ants.getRows()][ants.getCols()];
		for (int[] row : lastSymmetrySeen) {
			Arrays.fill(row, -100);
		}
		exploration = new int[ants.getRows()][ants.getCols()];
		for (int[] row : exploration) {
			Arrays.fill(row, -200);
		}
		moreSafeTime=ants.getTurnTime()/5;
		newlySeenTiles = new TileList(null,null);
		twiceRows = ants.getRows()<<1;
		BigInteger minusOne=BigInteger.ONE.negate();
		shiftedBy2Row = BigInteger.ONE.shiftLeft(twiceRows);
		// * in following ... reminder 1 was not subtracted
		BigInteger twiceRowBI= shiftedBy2Row.add(minusOne);
		rowBI= BigInteger.ONE.shiftLeft(ants.getRows()).add(minusOne);
		colBI=BigInteger.ONE.shiftLeft(twiceRows*ants.getCols()).divide(twiceRowBI); // *
		bitOfFourBoards=BigInteger.ONE.shiftLeft(ants.getRows()).add(BigInteger.ONE);
		bitOfFourBoards=bitOfFourBoards.shiftLeft(ants.getCols()*twiceRows).add(bitOfFourBoards); // (2^0+2^rows)(2^0+2^(2rows*cols))
		oneBoard = rowBI.multiply(colBI); // *
		gcdSizes=gcd(ants.getCols(),ants.getRows());
		BigInteger gcdColBI=BigInteger.ONE.shiftLeft(twiceRows*gcdSizes).divide(twiceRowBI); // *
		BigInteger gcdRowBI=BigInteger.ONE.shiftLeft(gcdSizes).add(minusOne);
		BigInteger gcdBoard = gcdRowBI.multiply(gcdColBI);
		bitOfGcdBoard=BigInteger.ONE.shiftLeft(twiceRows*2*ants.getCols()).divide(gcdBoard);// *
		BigInteger oddVStripes = gcdColBI.multiply(BigInteger.ONE.shiftLeft(gcdSizes|1).divide(BigInteger.valueOf(3)));// *
		BigInteger evenHStripes = gcdRowBI.multiply(BigInteger.ONE.shiftLeft(twiceRows*gcdSizes).divide(BigInteger.ONE.shiftLeft(2*twiceRows).add(minusOne))); //*
		chessBoard = oddVStripes.xor(evenHStripes); // zero bit is set
		chessBoard = chessBoard.multiply(BigInteger.ONE.add(BigInteger.ONE.shiftLeft(gcdSizes)));
		possibleSym[indX] = rowBI;		// (2) (v-x,y)=(v,0)+(-x,y) rowBI is sufficient, but oneBoard could speed up other symmetry updates
		possibleSym[indY] = colBI;		// (2) (x,w-y)=(0,w)+(x,-y) colBI is sufficient, but oneBoard could speed up other symmetry updates
		possibleSym[indC] = oneBoard;	// (2) (v-x,w-y)=(v,w)+(-x,-y)
		possibleSym[indD] = gcdRowBI;	// (2) rows=cols (y-v,x+v)=(-v,v)+(y,x)
		possibleSym[indB] = gcdRowBI;	// (2) rows=cols (v-y,v-x)=(v,v)+(-y,-x)
		possibleSym[indR] = chessBoard;	// (4) rows=cols (v+y,w-x)=(v,w)+(y,-x)
		possibleSym[indS] = BigInteger.ZERO; // to be increased
		Set<Ilk> sT=new HashSet<Ilk>();sT.add(Ilk.SYMMETRY_FOOD); sT.add(Ilk.PLANNED_SYMMETRY_FOOD);
		for(int ind=0;ind<16;ind++) {
			activeSymmetrySymFood[ind] = new TileList(sT,null);
		}
		Arrays.fill(landOfActiveSymmetries,BigInteger.ZERO);
		Arrays.fill(waterOfActiveSymmetries,BigInteger.ZERO);
		Arrays.fill(activeSym,BigInteger.ZERO);
		Arrays.fill(unknownIntersectOfActiveSymmetries,oneBoard);
		Arrays.fill(activeSymmetryType,SymmetryType.S);
		Arrays.fill(activeSymmetryFail,false);
		Arrays.fill(activeSymmetryCoord,-1);
		toTestSym=possibleSym[indN]=oneBoard.or(chessBoard);
	}

 	private  List<Tile> generalSymmetry(SymmetryType st, int coord, Tile t) {
		int row=t.getRow(),col=t.getCol();
		coord2XY(coord);
		List<Tile> res=new ArrayList<Tile>();
		switch (st) {
		case R:
			res.add(new Tile(((testedX+testedY)>>1)-col,((testedY-testedX)>>1)+row,ants)); //OK
			res.add(new Tile(((testedX-testedY)>>1)+col,((testedY+testedX)>>1)-row,ants)); //OK
		case C:
			res.add(new Tile(testedX-row,testedY-col,ants)); //OK
			break;
		case Y:
			res.add(new Tile(row,testedY-col,ants)); //OK
			break;
		case X:
			res.add(new Tile(testedX-row,col,ants)); //OK
			break;
		case D:
			res.add(new Tile(col-testedX,testedX+row,ants));//
			break;
		case B:
			res.add(new Tile(testedX-col,testedX-row,ants));//
			break;
		case S:
			for(Tile symT=new Tile(row+testedX,col+testedY,ants);!t.equals(symT);symT=new Tile(symT.getRow()+testedX,symT.getCol()+testedY,ants)) {
				res.add(symT);
			}//OK
		case N:// no symmetry to use
			break;
		}
		return res;
 	}
 	
	public  List<Tile> actualSymmetry(int ind, Tile t) {
		if (!activeSymmetryFail[ind]) {
			return generalSymmetry(activeSymmetryType[ind],activeSymmetryCoord[ind],t);
		}
		return new ArrayList<Tile>();
	}
 	
	public  List<Tile> actualSymmetry(Tile t) {//TODO is multicity in list a problem?
		List<Tile> res=new ArrayList<Tile>(),subRes;
		if (activated()) {
			for(int ind=0;ind<=lastSymmetryIndex;ind++) {
				subRes=actualSymmetry(ind,t);
				//LogFile.write("actualSymmetry["+ind+"]:"+activeSymmetryType[ind]+" Coord:("+testedX+","+testedY+")"+
				//		(activeSymmetryType[ind]==SymmetryType.S?"["+activeSymmetryShiftPlayers[ind]+"]":"")+subRes);
				res.addAll(subRes);
			}
		}
		return res;
	}
	
	private  void activateSymmetry(int coord) {
		if (!possibleSym[actualSymmetryType.ordinal()].testBit(coord)) return;
		lastSymmetryIndex++;
		landOfActiveSymmetries[lastSymmetryIndex]=testedCoordLand[actualSymmetryType.ordinal()];
		waterOfActiveSymmetries[lastSymmetryIndex]=testedCoordWater[actualSymmetryType.ordinal()];
		if (lastSymmetryIndex==0) {
			unknownIntersectOfActiveSymmetries[0]=oneBoard.andNot(landOfActiveSymmetries[0]).andNot(waterOfActiveSymmetries[0]);}
		else {
			unknownIntersectOfActiveSymmetries[lastSymmetryIndex]=unknownIntersectOfActiveSymmetries[lastSymmetryIndex-1].andNot(
					landOfActiveSymmetries[lastSymmetryIndex]).andNot(waterOfActiveSymmetries[lastSymmetryIndex]);
		}
		activeSymmetryType[lastSymmetryIndex]=actualSymmetryType;
		activeSymmetryCoord[lastSymmetryIndex]=coord;
		activeSymmetrySymFood[lastSymmetryIndex].clear();
		activeSymmetryShiftPlayers[lastSymmetryIndex]=shiftPlayers;
		activeSymmetryFail[lastSymmetryIndex]=false;
		activeSym[actualSymmetryType.ordinal()]=activeSym[actualSymmetryType.ordinal()].setBit(coord);
	}
	
	//private  int actualSymmetryXY2coord() {
	//	return twiceRows*actualSymmetryY+actualSymmetryX;
	//}
	/**
	 * sets testedX, testedY from coord
	 */
	private  void coord2XY(int coord) {
		testedX=coord%twiceRows; testedY=coord/twiceRows;
	}
	
	private  void selectFromBI(SymmetryType nextSymmetry) {
		int coord=possibleSym[actualSymmetryType.ordinal()].andNot(
				activeSym[actualSymmetryType.ordinal()]).getLowestSetBit();
		if ((coord>=0)) {
			doTests(coord);
			activateSymmetry(coord);
		} else {
			actualSymmetryType=nextSymmetry;
		}
	}
	
	private  void selectSymmetry() {
		while (seekingForSymmetries()) {
			if (ants.getTimeRemaining()<moreSafeTime) return;
			switch (actualSymmetryType) {
			case S:
				if (shiftPlayers>=possibleShift.length) {
					shiftPlayers = possibleShift.length-1;
				}
				if (shiftPlayers<2) {
					actualSymmetryType=SymmetryType.D;
					break;
				}
				if (possibleShift[shiftPlayers]==null) {
					shiftPlayers--;
				} else {
					int coord;
					possibleShift[shiftPlayers]=possibleShift[shiftPlayers].and(possibleSym[indS]);// required??
					if ((coord=possibleShift[shiftPlayers].andNot(activeSym[actualSymmetryType.ordinal()]).getLowestSetBit())<0) {
						shiftPlayers--;
					} else {
						doTests(coord);
						activateSymmetry(coord);
					}
				}
				break;
			case D:		selectFromBI(SymmetryType.B);	break;
			case B:		selectFromBI(SymmetryType.Y);	break;
			case Y:		selectFromBI(SymmetryType.X);	break;
			case X:		selectFromBI(SymmetryType.R);	break;
			case R:		selectFromBI(SymmetryType.C);	break;
			case C:		selectFromBI(SymmetryType.N);	break;
			case N:		seekingForSymmetries = false;	break;
			}
		}
		if (seekingForSymmetries) {
			symTurn=ants.turn;
		}
	}
	
	public  void newlySeen(Tile tile) {
		newlySeenTiles.add(tile);
	}
	
	public  void newKnowledge() {
		distToEnemyState=distFromMyState=0;
	}
	
	public  void collision(Tile t) {
		failed=true;
		distToEnemyState=0;
		int ind=getSymmetryIndex(t);
		if (ind<=lastSymmetryIndex) {
			if (activeSymmetryFail[ind]) return;
			//Logfile.write("Fail["+ind+"]"+t);
			activeSymmetryFail[ind]=true;
			activeSymmetrySymFood[ind].startTraversal();
		} else {
			//Logfile.write("Collision on unknown?? "+t);
			return;
		}
		//LogFile.write("Collision "+t+" "+ants.getIlk(t)+" Symmetry["+ind+"]"+activeSymmetryType[ind]+"["+shiftPlayers+"]("+activeSymmetryCoord[ind]/twiceRows+","+activeSymmetryCoord[ind]%twiceRows+")");
	}
	
	private  void processFail() {//TODO ... remove failed symmetries!
		if (failed) {
			for(Tile symHill=ants.getEnemyExpectedHills().getFirstFilter();symHill!=null;symHill=ants.getEnemyExpectedHills().getNextFilter()) {
				if (ants.getHill(symHill)==HillTypes.SYMMETRY_ENEMY_HILL) {
					ants.setHill(symHill,HillTypes.LAND);
				}
				if (ants.getTimeRemaining()<moreSafeTime) return;
			}
			for(Tile expl=ants.getPlannedExplorationFoodTiles().getFirstFilter();expl!=null;expl=ants.getPlannedExplorationFoodTiles().getNextFilter()) {
				if (ants.getLastSeen(expl)<0) {
					ants.setIlk(expl, Ilk.UNKNOWN);					
				} else {
					ants.setIlk(expl, Ilk.LAND);
				}
				if (ants.getTimeRemaining()<moreSafeTime) return;
			}
			ants.getPlannedExplorationFoodTiles().clear();
			for(int ind=0;ind<=lastSymmetryIndex;ind++) {
				if (ants.getTimeRemaining()<moreSafeTime) return;
				if (activeSymmetryFail[ind]) {
					for(Tile symFood=activeSymmetrySymFood[ind].getNextFilter();symFood!=null;symFood=activeSymmetrySymFood[ind].getNextFilter()) {
						if (ants.getLastSeen(symFood)<0) {
							ants.setIlk(symFood, Ilk.UNKNOWN);
						} else {
							ants.setIlk(symFood, Ilk.LAND);
						}
						if (ants.getTimeRemaining()<moreSafeTime) return;
					}
					activeSymmetrySymFood[ind].clear();
				}
			}
			int j=-1;
			firstKnownIndex=16;
			for(int i=0;i<=lastSymmetryIndex;i++) {
				if (activeSymmetryFail[i]) {
					activeSym[activeSymmetryType[i].ordinal()]=activeSym[activeSymmetryType[i].ordinal()].clearBit(activeSymmetryCoord[i]);
				}
				else {
					j++;
					if (j!=i) {
						landOfActiveSymmetries[j]=landOfActiveSymmetries[i];
						waterOfActiveSymmetries[j]=waterOfActiveSymmetries[i];
						if (j==0) {
							unknownIntersectOfActiveSymmetries[0]=oneBoard.andNot(landOfActiveSymmetries[0]).andNot(waterOfActiveSymmetries[0]);
						} else {
							unknownIntersectOfActiveSymmetries[j]=unknownIntersectOfActiveSymmetries[j-1].andNot(
									landOfActiveSymmetries[j]).andNot(waterOfActiveSymmetries[j]);
						}
						if (firstKnownIndex==16) {
							if (unknownIntersectOfActiveSymmetries[j].equals(BigInteger.ZERO)) {
								firstKnownIndex=j;
							}
						}
						activeSymmetryType[j]=activeSymmetryType[i];
						activeSymmetryCoord[j]=activeSymmetryCoord[i];
						TileList tl=activeSymmetrySymFood[j];
						activeSymmetrySymFood[j]=activeSymmetrySymFood[i];
						activeSymmetrySymFood[i]=tl;
						activeSymmetryFail[j]=false;
						activeSymmetryShiftPlayers[j]=activeSymmetryShiftPlayers[i];
					}
				}				
			}
			lastSymmetryIndex=j;
			failed=false;
		}
	}
	
	private  void updateSymmetries() {
		firstKnownIndex=16;
		for(int ind=0;ind<=lastSymmetryIndex;ind++) {
			int coord=activeSymmetryCoord[ind];
			doTests(coord);
			if (!possibleSym[activeSymmetryType[ind].ordinal()].testBit(coord)) {
				int symX=testedX,symY=testedY;
				coord2XY(testedCoordLand[activeSymmetryType[ind].ordinal()].and(testedCoordWater[activeSymmetryType[ind].ordinal()]).getLowestSetBit());
				//LogFile.write("updateSymm... ["+ind+"]"+activeSymmetryType[ind]+" Coord:("+symX+","+symY+")"+
				//	(activeSymmetryType[ind]==SymmetryType.S?"["+activeSymmetryShiftPlayers[ind]+"]":"")+" failed after doTests at ("
				//	+testedX+","+testedY+")");
				activeSymmetryFail[ind]=true;
				activeSymmetrySymFood[ind].startTraversal();
				failed=true;
			} else {
				landOfActiveSymmetries[ind]=testedCoordLand[activeSymmetryType[ind].ordinal()];
				waterOfActiveSymmetries[ind]=testedCoordWater[activeSymmetryType[ind].ordinal()];
				if (ind==0) {
					unknownIntersectOfActiveSymmetries[0]=oneBoard.andNot(landOfActiveSymmetries[0]).andNot(waterOfActiveSymmetries[0]);
				} else {
					unknownIntersectOfActiveSymmetries[ind]=unknownIntersectOfActiveSymmetries[ind-1].andNot(
							landOfActiveSymmetries[ind]).andNot(waterOfActiveSymmetries[ind]);
				}
			}
			if (firstKnownIndex==16) {
				if (unknownIntersectOfActiveSymmetries[ind].equals(BigInteger.ZERO)) {
					firstKnownIndex=ind;
				}
			}
		}
	}
	
	private  int swapX(int x) {// compatible with activeSymmetryX==0 flipping
		return x==0?0:ants.getRows()-x;
	}
	
	private  int swapY(int y) {// compatible with activeSymmetryY==0 flipping
		return y==0?0:ants.getCols()-y;
	}
	
	private  int swapG(int g) {
		return g==0?0:gcdSizes-g;
	} 
	
	private  void processNewlySeen() {
		Tile newlySeen;
		//LogFile.write("processNewlySeen");
		while ((newlySeen=newlySeenTiles.getNextNoFilter())!=null) {
			int row=newlySeen.getRow(),col=newlySeen.getCol();
			int xrow=row%gcdSizes, xcol=col%gcdSizes;
			if ((ants.getIlk(newlySeen))==Ilk.WATER) {
				water			=water.or(bitOfFourBoards.shiftLeft(row+col*twiceRows));
				waterXSwap		=waterXSwap.or(bitOfFourBoards.shiftLeft(swapX(row)+col*twiceRows));
				waterYSwap		=waterYSwap.or(bitOfFourBoards.shiftLeft(row+swapY(col)*twiceRows));
				waterCSwap		=waterCSwap.or(bitOfFourBoards.shiftLeft(swapX(row)+swapY(col)*twiceRows));
				waterGcd		=waterGcd.or(bitOfGcdBoard.shiftLeft(xrow+xcol*twiceRows));
				waterGcdRotClock=waterGcdRotClock.or(bitOfGcdBoard.shiftLeft(xcol+swapG(xrow)*twiceRows));
				waterGcdRotCClock=waterGcdRotCClock.or(bitOfGcdBoard.shiftLeft(swapG(xcol)+xrow*twiceRows));
				waterGcdBSwap	=waterGcdBSwap.or(bitOfGcdBoard.shiftLeft(swapG(xcol)+swapG(xrow)*twiceRows));
				waterGcdDSwap	=waterGcdDSwap.or(bitOfGcdBoard.shiftLeft(xcol+xrow*twiceRows));
			} else {
				land			=land.or(bitOfFourBoards.shiftLeft(row+col*twiceRows));
				landXSwap		=landXSwap.or(bitOfFourBoards.shiftLeft(swapX(row)+col*twiceRows));
				landYSwap		=landYSwap.or(bitOfFourBoards.shiftLeft(row+swapY(col)*twiceRows));
				landCSwap		=landCSwap.or(bitOfFourBoards.shiftLeft(swapX(row)+swapY(col)*twiceRows));
				landGcd			=landGcd.or(bitOfGcdBoard.shiftLeft(xrow+xcol*twiceRows));
				landGcdRotClock	=landGcdRotClock.or(bitOfGcdBoard.shiftLeft(xcol+swapG(xrow)*twiceRows));
				landGcdRotCClock=landGcdRotCClock.or(bitOfGcdBoard.shiftLeft(swapG(xcol)+xrow*twiceRows));
				landGcdBSwap	=landGcdBSwap.or(bitOfGcdBoard.shiftLeft(swapG(xcol)+swapG(xrow)*twiceRows));
				landGcdDSwap	=landGcdDSwap.or(bitOfGcdBoard.shiftLeft(xcol+xrow*twiceRows));
			}
			if (ants.getTimeRemaining()<moreSafeTime) return;
		}
		if (newlySeenTiles.size()>0) {
			dirty=true;
			newlySeenTiles.clear();		
		}
		//LogFile.write("processNewlySeen end");
	} 

	/**
	 * Has to declare the original ant position was better to the new one
	 * @param oriAnt
	 * @param newAnt
	 * @return
	 */
	public  void newSymmetryFood(int atMostTime) {
		if (!activated()) return;		
		int stopTime=ants.getTimeRemaining()-atMostTime; if (stopTime<moreSafeTime) {stopTime=moreSafeTime;}
		for(Tile food=ants.getFoodTiles().getFirstFilter();food!=null;food=ants.getFoodTiles().getNextFilter()) {
			if (ants.foodDiscovery[food.getRow()][food.getCol()]==ants.turn) {
				for(int ind=0;ind<=lastSymmetryIndex;ind++) {
					List<Tile> symFoodList = actualSymmetry(ind,food);
					for(Tile symFood:symFoodList) {
						Ilk ilk=ants.getIlk(symFood);
						if (ants.foodDiscovery[symFood.getRow()][symFood.getCol()]<ants.turn) {
							if (ants.lastSeen[symFood.getRow()][symFood.getCol()]<ants.turn) {// Land/Water collision was tested already
								if (ilk.ordinal()>Ilk.EXPECTED_FOOD.ordinal()) {
									//LogFile.write("Symfood "+symFood+" ori discovered "+ants.foodDiscovery[symFood.getRow()][symFood.getCol()]+" "+ilk+" mirrored from "+food+" by "+activeSymmetryType[ind]+" Coord:("+testedX+","+testedY+")"+
									//		(activeSymmetryType[ind]==SymmetryType.S?"["+activeSymmetryShiftPlayers[ind]+"]":"")+" "+"discovered "+ants.turn);
									ants.foodDiscovery[symFood.getRow()][symFood.getCol()]=ants.turn;
									ants.setIlk(symFood, Ilk.SYMMETRY_FOOD);
									ants.getSymmetryFoodTiles().add(symFood);
									activeSymmetrySymFood[ind].add(symFood);
								}
							}
						}
					}
				}
			}
			if (ants.getTimeRemaining()<stopTime) return;			
		}		
	}
	
	private  boolean applySymmetry(int atMostTime) {//Return equals failed;
		if (!activated()) return true;
		int stopTime=ants.getTimeRemaining()-atMostTime; if (stopTime<moreSafeTime) {stopTime=moreSafeTime;}
		//LogFile.write("applySymmetry");
		for(Tile food=ants.getExpectedFoodTiles().getFirstFilter();food!=null;food=ants.getExpectedFoodTiles().getNextFilter()) {
			if ((ants.getIlk(food))==Ilk.EXPECTED_FOOD) {// no call on SYMMETRY_FOOD
				int discovered=ants.foodDiscovery[food.getRow()][food.getCol()];
				for(int ind=0;ind<=lastSymmetryIndex;ind++) {
					List<Tile> symFoodList = actualSymmetry(ind,food);
					for(Tile symFood:symFoodList) {
						Ilk ilk=ants.getIlk(symFood);
						if (ants.getFoodDiscovery(symFood)<discovered) {
							if (ants.getLastSeen(symFood)<discovered) {// Land/Water collision was tested already
								if (ilk.ordinal()>Ilk.EXPECTED_FOOD.ordinal()) {
									//LogFile.write("Symfood ("+symFood.getRow()+","+symFood.getCol()+") ori discovered "+ants.foodDiscovery[symFood.getRow()][symFood.getCol()]+" "+ilk+" mirrored from ("+food.getRow()+","+food.getCol()+")"+"discovered "+discovered);
									ants.setFoodDiscovery(symFood,discovered);
									ants.setIlk(symFood, Ilk.SYMMETRY_FOOD);
									ants.getSymmetryFoodTiles().add(symFood);
									activeSymmetrySymFood[ind].add(symFood);
								}
							}
						}
					}
				}
			}
			if (ants.getTimeRemaining()<stopTime) return false;			
		}
		//LogFile.write("applySymmetry B");
		for(Tile deadHill=ants.getDeadHills().getFirstNoFilter();deadHill!=null;deadHill=ants.getDeadHills().getNextNoFilter()) {
			if (ants.getTimeRemaining()<stopTime) return false;			
			List<Tile> symHillList = actualSymmetry(deadHill);
			for(Tile symHill:symHillList) {
				HillTypes ht=ants.getHill(symHill);
				//LogFile.write("ApplySymmetry deadHill "+deadHill+" -> "+symHill+" "+ht);
				if (ht==HillTypes.NO_HILL) {
					//collision(symHill);//TODO will I identify the collision index well ??? ... hillCollision
				} else if (ht==HillTypes.LAND) {
					if (ants.lastSeen[symHill.getRow()][symHill.getCol()]<0) {
						ants.setHill(symHill, HillTypes.SYMMETRY_ENEMY_HILL);
						ants.getEnemyExpectedHills().add(symHill);
						// symTiles.add(symHill); already added as Land
					}
				}
			}			
		}
		//LogFile.write("applySymmetry C");
		for(Tile myHill=ants.getMyHills().getFirstNoFilter();myHill!=null;myHill=ants.getMyHills().getNextNoFilter()) {
			if (ants.getTimeRemaining()<stopTime) return false;			
			List<Tile> symHillList = actualSymmetry(myHill);
			for(Tile symHill:symHillList) {
				HillTypes ht=ants.getHill(symHill);
				//LogFile.write("ApplySymmetry myHill "+myHill+" -> "+symHill+" "+ht);
				if (ht==HillTypes.NO_HILL) {
					//collision(symHill);//TODO will I identify the collision index well ??? ... hillCollision
				} else if (ht==HillTypes.LAND) {
					if (ants.lastSeen[symHill.getRow()][symHill.getCol()]<0) {
						ants.setHill(symHill, HillTypes.SYMMETRY_ENEMY_HILL);
						ants.getEnemyExpectedHills().add(symHill);
						// symTiles.add(symHill); already added as Land
					}
				}
			}			
		}
		//LogFile.write("applySymmetry D");
		for(Tile myHill=ants.getMyExpectedHills().getFirstNoFilter();myHill!=null;myHill=ants.getMyExpectedHills().getNextNoFilter()) {
			if (ants.getTimeRemaining()<stopTime) return false;			
			List<Tile> symHillList = actualSymmetry(myHill);
			for(Tile symHill:symHillList) {
				HillTypes ht=ants.getHill(symHill);
				//LogFile.write("ApplySymmetry myExpectedHill "+myHill+" -> "+symHill+" "+ht);
				if (ht==HillTypes.NO_HILL) {
					//collision(symHill);//TODO will I identify the collision index well ??? ... hillCollision
				} else if (ht==HillTypes.LAND) {
					if (ants.lastSeen[symHill.getRow()][symHill.getCol()]<0) {
						ants.setHill(symHill, HillTypes.SYMMETRY_ENEMY_HILL);
						ants.getEnemyExpectedHills().add(symHill);
						// symTiles.add(symHill); already added as Land
					}
				}
			}			
		}
		//LogFile.write("applySymmetry E");
		for(Tile enemyHill=ants.getEnemyExpectedHills().getFirstNoFilter();enemyHill!=null;enemyHill=ants.getEnemyExpectedHills().getNextNoFilter()) {
			if (ants.getTimeRemaining()<stopTime) return false;			
			if (ants.getHill(enemyHill)==HillTypes.EXPECTED_ENEMY_HILL) {
				List<Tile> symHillList = actualSymmetry(enemyHill);
				for(Tile symHill:symHillList) {
					HillTypes ht=ants.getHill(symHill);
					//LogFile.write("ApplySymmetry enemyExpectedHill "+enemyHill+" -> "+symHill+" "+ht);
					if (ht==HillTypes.NO_HILL) {
						//collision(symHill);//TODO will I identify the collision index well ??? ... hillCollision
					} else if (ht==HillTypes.LAND) {
						if (ants.lastSeen[symHill.getRow()][symHill.getCol()]<0) {
							ants.setHill(symHill, HillTypes.SYMMETRY_ENEMY_HILL);
							ants.getEnemyExpectedHills().add(symHill);
							// symTiles.add(symHill); already added as Land
						}
					}
				}
			}			
		}
		//LogFile.write("applySymmetry end");
		return false;
	}
	
	//private  BigInteger XSymLand, XSymWater, YSymLand, YSymWater, CSymLand, CSymWater, XYSymLand, XYSymWater, 
	//DSymLand, DSymWater, BSymLand, BSymWater, RSymLand, RSymWater, RXSymLand, RXSymWater;
	
	private  BigInteger[] testedCoordLand=new BigInteger[SymmetryType.values().length];
	private  BigInteger[] testedCoordWater=new BigInteger[SymmetryType.values().length];
	private  final int indX=SymmetryType.X.ordinal();
	private  final int indY=SymmetryType.Y.ordinal();
	private  final int indC=SymmetryType.C.ordinal();
	private  final int indD=SymmetryType.D.ordinal();
	private  final int indB=SymmetryType.B.ordinal();
	private  final int indS=SymmetryType.S.ordinal();
	private  final int indN=SymmetryType.N.ordinal();
	private  final int indR=SymmetryType.R.ordinal();
	
	public  void doTests(int coord) {
		coord2XY(coord);
		int testedYCoord=coord-testedX;
		int testedXCoord=testedX%ants.getRows();
		int testedXGS=testedX%(2*gcdSizes);
		int testedXXSwappedCoord=swapX(testedXCoord),testedYYSwappedCoord=swapY(testedY)*twiceRows;
		int testedXGSCoord = testedXGS;
		int testedXGSGSwappedCoord = swapG(testedXGS);
		if (possibleSym[indX].testBit(testedXCoord)) {
			testedCoordLand[indX]=land.or(landXSwap.shiftRight(testedXXSwappedCoord));
			testedCoordWater[indX]=water.or(waterXSwap.shiftRight(testedXXSwappedCoord));
		} else {
			testedCoordLand[indX]=testedCoordWater[indX]=BigInteger.ONE; // a conflict known
		}
		if (possibleSym[indY].testBit(testedYCoord)) {
			testedCoordLand[indY]=land.or(landYSwap.shiftRight(testedYYSwappedCoord));
			testedCoordWater[indY]=water.or(waterYSwap.shiftRight(testedYYSwappedCoord));
		} else {
			testedCoordLand[indY]=testedCoordWater[indY]=BigInteger.ONE; // a conflict known
		}
		if (possibleSym[indC].testBit(testedXCoord+testedY*twiceRows)) {
			int testedCCoord = testedYYSwappedCoord+testedXXSwappedCoord;
			testedCoordLand[indC]=land.or(landCSwap.shiftRight(testedCCoord));
			testedCoordWater[indC]=water.or(waterCSwap.shiftRight(testedCCoord));
		} else {
			testedCoordLand[indC]=testedCoordWater[indC]=BigInteger.ONE; // a conflict known
		}
		if (possibleSym[indD].testBit(testedXGSCoord)) {
			int testedDCoord = testedXGS+testedXGSGSwappedCoord*twiceRows;
			testedCoordLand[indD]=land.or(landGcdDSwap.shiftRight(testedDCoord));
			testedCoordWater[indD]=water.or(waterGcdDSwap.shiftRight(testedDCoord));
		} else {
			testedCoordLand[indD]=testedCoordWater[indD]=BigInteger.ONE; // a conflict known
		}
		if (possibleSym[indB].testBit(testedXGSCoord)) {
			int testedBCoord = swapG(testedXGS)*(twiceRows+1);
			testedCoordLand[indB]=land.or(landGcdBSwap.shiftRight(testedBCoord));
			testedCoordWater[indB]=water.or(waterGcdBSwap.shiftRight(testedBCoord));
		} else {
			testedCoordLand[indB]=testedCoordWater[indB]=BigInteger.ONE; // a conflict known
		}
		if (possibleSym[indR].testBit(coord)) {
			int testedRotClockCoord=(2*gcdSizes+((testedY-testedX)/2))%gcdSizes+twiceRows*((2*gcdSizes-((testedX+testedY)/2))%gcdSizes),
			testedRotCClockCoord=(2*gcdSizes-((testedX+testedY)/2))%gcdSizes+twiceRows*((2*gcdSizes+((testedX-testedY)/2))%gcdSizes);
			testedCoordLand[indR]=landGcd.or(testedCoordLand[indC]).or(
					landGcdRotClock.shiftRight(testedRotClockCoord).or(
					landGcdRotCClock.shiftRight(testedRotCClockCoord)));
			testedCoordWater[indR]=waterGcd.or(testedCoordWater[indC]).or(
					waterGcdRotClock.shiftRight(testedRotClockCoord).or(
					waterGcdRotCClock.shiftRight(testedRotCClockCoord)));
		} else {
			testedCoordLand[indR]=testedCoordWater[indR]=BigInteger.ONE; // a conflict known
		}
		if (possibleSym[indS].testBit(coord)) {
			int shiftVecX=swapX(testedX),shiftVecY=swapY(testedY);
			testedCoordLand[indS]=land;
			testedCoordWater[indS]=water;
			for (int posX=shiftVecX,posY=shiftVecY;posX+posY>0;
				posX+=shiftVecX,posY+=shiftVecY,
				posX-=(posX>=ants.getRows()?ants.getRows():0),
				posY-=(posY>=ants.getCols()?ants.getCols():0)) {
				int shiftCoord=posX+posY*twiceRows;
				testedCoordLand[indS]=testedCoordLand[indS].or(land.shiftRight(shiftCoord));
				testedCoordWater[indS]=testedCoordWater[indS].or(water.shiftRight(shiftCoord));
			}
		} else {
			testedCoordLand[indS]=testedCoordWater[indS]=BigInteger.ONE; // a conflict known
		}
		testedCoordLand[indN]=testedCoordWater[indN]=possibleSym[indN]; // a conflict known
		//LogFile.write("doTests("+testedX+","+testedY+"):");
		boolean coordStillActive=false;
		for(SymmetryType sym:SymmetryType.values()) {
			if (possibleSym[sym.ordinal()].testBit(coord)) {
				int conflict=oneBoard.and(testedCoordLand[sym.ordinal()].and(testedCoordWater[sym.ordinal()])).getLowestSetBit();
				if (conflict>=0) {
					possibleSym[sym.ordinal()]=possibleSym[sym.ordinal()].clearBit(coord);
				} else {
					coordStillActive=true;
				}
			}
		}
		if (coordStillActive) {
			possibleSym[indN]=possibleSym[indN].setBit(coord);
		}
		//LogFile.write("doTests end ("+testedX+","+testedY+")");
	}
	
	private  void calcDistFromMy(int atMostTime) {
		int stopTime=ants.getTimeRemaining()-atMostTime; if (stopTime<moreSafeTime) {stopTime=moreSafeTime;}
		if (distFromMyState==0) {
			for(int i=0;i<distFromMySearch.length;i++) {
				if (distFromMySearch[i]!=null) {
					distFromMySearch[i].clear();
				}
			}
			distFromMyState=1;
			if (ants.getTimeRemaining()<stopTime) return;
		}
		if (distFromMyState==1) {
			Arrays.fill(distFromMy, 1);
			distFromMyCalcFilter=0; // expecting at most 31 hills for a player
			for(int i=0;i<myOriHills.size();i++) {
				Tile myHill=myOriHills.get(i);
				HillTypes ht=ants.getHill(myHill);
				//LogFile.write("DistFromMyState==1, i=="+i+", ht=="+ht+",distFromMySearch[i] "+(distFromMySearch[i]==null?"null":"not null"));
				if (ht==HillTypes.EXPECTED_MY_HILL) {
					distFromMySearch[i].addNotVisited(myHill);
					distFromMySearch[i].addBreak();
					distFromMyCalcFilter|=(1<<i);
				} else {
					//distFromMySearch[i]=null;
				}
			}
			distFromMyState=2;
			if (ants.getTimeRemaining()<stopTime) return;
		}
		if (distFromMyState==2) {
			int i=32;
			while ((ants.getTimeRemaining()>stopTime)&&(distFromMyCalcFilter!=0)) {
				i++;
				i=Integer.numberOfTrailingZeros((distFromMyCalcFilter>>i)<<i);
				if (i==32) {i=Integer.numberOfTrailingZeros(distFromMyCalcFilter);}
				Tile searched=distFromMySearch[i].remove();
				if (searched.equals(Search.breakTile)) {
					if (distFromMySearch[i].isEmpty()) {
						distFromMyCalcFilter&=~(1<<i);
					} else {
						distFromMy[i]++; 
						distFromMySearch[i].addBreak();
					}
				} else {
					int oriDist=distFromMyHill[i][searched.getRow()][searched.getCol()];
					distFromMyHill[i][searched.getRow()][searched.getCol()]=distFromMy[i];
					sumDistFromMyHill[searched.getRow()][searched.getCol()]-=oriDist-distFromMy[i];
					if (distFromMy[i]<minDistFromMyHill[searched.getRow()][searched.getCol()]) {
						minDistFromMyHill[searched.getRow()][searched.getCol()]=distFromMy[i];
					}
					int permId = 0;
					for (Aim direction : Aim.permAim[permId]) {
						Tile moved = ants.getTile(searched, direction);
						Ilk ilk=ants.getIlk(moved);
						if (ilk==Ilk.UNKNOWN) {
							distFromMyCalcFilter&=~(1<<i);
						} else if (ants.getIlk(moved).isPassable()) {
							distFromMySearch[i].addNotVisited(moved);
						}
					}
				}
			}
			if (distFromMyCalcFilter==0) {distFromMyState=3;}
		}		
	}
	
	private  void calcDistToEnemy() {
		if (distToEnemyState==0) {
			distToEnemySearch.clear();
			distToEnemyState=1;
			if (ants.getTimeRemaining()<moreSafeTime) return;
		}
		if (distToEnemyState==1) {
			distToEnemy=0;
			for(Tile enemyHill=ants.getEnemyExpectedHills().getFirstFilter();enemyHill!=null;enemyHill=ants.getEnemyExpectedHills().getNextFilter()) {
				distToEnemySearch.addNotVisited(enemyHill);
			}
			distToEnemySearch.addBreak();
			distToEnemyState=2;
			if (ants.getTimeRemaining()<moreSafeTime) return;
		}
		if (distToEnemyState==2) {
			while (ants.getTimeRemaining()>moreSafeTime) {
				Tile searched=distToEnemySearch.remove();
				if (searched.equals(Search.breakTile)) {
					if (distToEnemySearch.isEmpty()) {
						distToEnemyState=3;break;
					}
					distToEnemy++; 
					distToEnemySearch.addBreak();
				} else {
					distToEnemyHill[searched.getRow()][searched.getCol()]=distToEnemy;
					int permId = 0;
					for (Aim direction : Aim.permAim[permId]) {
						Tile moved = ants.getTile(searched, direction);
						if (ants.getIlk(moved).isPassable()) {
							distToEnemySearch.addNotVisited(moved);
						}
					}
				}
			}
		}
	}
	
	public  boolean calcSymmetry() {
		if (ants.getTimeRemaining()<moreSafeTime) return false;
		if (possibleShift==null) {
			initFirstTurn();
		}
		if (ants.getTimeRemaining()<moreSafeTime) return false;
		calcDistFromMy(150);
		processFail();
		if (ants.getTimeRemaining()<moreSafeTime) return false;
		processNewlySeen();
		if (ants.getTimeRemaining()<moreSafeTime) return false;
		if (dirty) {
			updateSymmetries();
			dirty=false;
			return true;
		}
		int coord;
		if ((coord=toTestSym.getLowestSetBit())>=0){
			doTests(coord);toTestSym=toTestSym.clearBit(coord);
			return true;
		}
		if (ants.getTimeRemaining()<moreSafeTime) return false;
		selectSymmetry();
		if (ants.getTimeRemaining()<moreSafeTime) return false;
		if (applySymmetry(150)) {
			return true;
		}
		if (ants.getTimeRemaining()<moreSafeTime) return false;
		if (activated()) {
			calcDistToEnemy();
		}
		if (ants.getTimeRemaining()<moreSafeTime) return false;
		for(int ind=0;ind<=lastSymmetryIndex;ind++) {
			for(Tile sFood=activeSymmetrySymFood[ind].getFirstFilter();sFood!=null;sFood=activeSymmetrySymFood[ind].getNextFilter()) {
				// to filter old information out
				if (ants.getTimeRemaining()<moreSafeTime) return false;
			}
		}
		return false;
	}

	public  void makePlannedExplorationFood(Tile t,int delay) {
		List<Tile> symTs = actualSymmetry(t);symTs.add(t);
		for(Tile symT:symTs) {
			Ilk ilk=ants.getIlk(symT);
			if (!((ilk==Ilk.UNKNOWN)||(ilk==Ilk.LAND)||(ilk==Ilk.SYMMETRY_LAND))) {
				//LogFile.write("Avoiding makePlannedExplorationFood on conflicting Ilk("+ilk+") "+t+"->"+symT);
				continue;
			}
			ants.setIlk(symT, Ilk.PLANNED_EXPLORATION_FOOD);
			ants.getPlannedExplorationFoodTiles().add(symT);
			for(int r=-delay;r<=+delay;r++) {
				for(int c=-delay+Math.abs(r);c<+delay-Math.abs(r);c++) {
					Tile tt=new Tile(symT.getRow()+r,symT.getCol()+c,ants);
					if (getExploration(tt)<ants.turn-Math.abs(r)-Math.abs(c)) {
						setExploration(tt,ants.turn-Math.abs(r)-Math.abs(c));
					}
				}
			}
		}
	}
	
	public  void printDist() {
		if (possibleShift==null) return;
		for(int distNo=-5;distNo<distFromMyHill.length;distNo++) {
			printDist(distNo);
		}
	}
	
	public  void printDist(int distNo) {
		List<String> picture=new ArrayList<String>();
		picture.add("Dist ["+distNo+"]");
		if (!AbstractSystemInputReader.logging) return;
		for(int row=0; row<ants.getRows(); row++) {
			StringBuilder info=new StringBuilder();
			String tmp = "  "+row; info.append(tmp.substring(tmp.length()-3)+" ");
			for(int col=0; col<ants.getCols();col++) {
				Ilk ilk=ants.getIlk(row,col); 
				if (ilk==Ilk.WATER) {
					info.append('W');
				} else if (ilk==Ilk.SYMMETRY_WATER) {
					info.append('w');
				} else {
					int dist;
					if (distNo==-5) {
						dist = ants.getEnemyAttack(row, col);
					} else if (distNo==-4) {
						dist = ants.getMyAttack(row, col);
					} else if (distNo==-3) {
						dist = ants.getEnemyFree(row, col)?1:0;
					} else if (distNo==-2) {
						dist = mybot.getDistFromFront(row, col);
					} else if (distNo==-1) {
						dist = distToEnemyHill[row][col];
					} else {
						dist = distFromMyHill[distNo][row][col];
					}
					dist%=10;
					if (dist<0) {
						info.append('-');
					} else {
						info.append(dist);
					}
				}
			}
			picture.add(info.toString());
		}
	}
	
	public  void logSymmetries() {
		LogFile.write("distToEnemyState: "+distToEnemyState);
		LogFile.write("distFromMyState: "+distFromMyState);
		LogFile.write("lastSymmetryIndex: "+lastSymmetryIndex);
		LogFile.write("failed: "+failed);
		LogFile.write("activated(): "+activated());
		LogFile.write("seekingForSymmetries(): "+seekingForSymmetries());
		LogFile.write("firstKnownIndex: "+firstKnownIndex);
		LogFile.write("actualSymmetryType: "+actualSymmetryType+(actualSymmetryType==SymmetryType.S?"["+shiftPlayers+"]":""));
		for(int ind=0;ind<=lastSymmetryIndex;ind++) {
			coord2XY(activeSymmetryCoord[ind]);
			LogFile.write("Symmetry["+ind+"]Fail:"+activeSymmetryFail[ind]+" "+activeSymmetryType[ind]+" Coord:("+testedX+","+testedY+")"+
					(activeSymmetryType[ind]==SymmetryType.S?"["+activeSymmetryShiftPlayers[ind]+"]":""));//+"Food:"+activeSymmetrySymFood[ind]
		}
		LogFile.write("symTurn: "+symTurn);
		LogFile.write("turn: "+ants.turn);
		for(SymmetryType sym:SymmetryType.values()) {
			LogFile.write("Sym "+sym+": "+possibleSym[sym.ordinal()].bitCount());
		}
		int coord=toTestSym.getLowestSetBit();
		if (coord<0) {
			LogFile.write("Scan done");
		} else {
			coord2XY(coord);
			LogFile.write("Scan to ("+testedX+","+testedY+")");
		}
		//LogFile.write("newlySeen: "+newlySeenTiles);
		//LogFile.write("LT:"+landTiles);
		//LogFile.write("WT:"+waterTiles);
		//LogFile.write("FT:"+ants.getFoodTiles());
		//StringBuilder shifts=new StringBuilder();
		//if (possibleShift!=null) {
		//	for(int p=2;p<possibleShift.length;p++) {
		//		if (possibleShift[p]!=null) {
		//			shifts.append(p+":");
		//			for(int r=0;r<p;r++) {
		//				shifts.append(Integer.toString(possibleShift[p][r],2)+" ");
		//			}
		//		}
		//	}
		//	LogFile.write(shifts.toString());
		//}
	}
}
