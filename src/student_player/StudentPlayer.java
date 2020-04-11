package student_player;

import Saboteur.SaboteurBoard;
import Saboteur.SaboteurMove;
import Saboteur.cardClasses.*;
import boardgame.Move;

import Saboteur.SaboteurPlayer;
import Saboteur.SaboteurBoardState;

import java.util.*;

/** A player file submitted by a student. */
public class StudentPlayer extends SaboteurPlayer {

    // timing constants to prevent loop from timing out
    private static final int TIME_LIMIT_MS = 2000;
    private static final int GRACE_PERIOD_MS = 1500; //TODO: is this necessary?
    
    //private static String optTileIdx = "0,5,5_flip,6,6_flip,7,7_flip,8,9,9_flip,10";
    private static String dropTileIdx = "1,2,2_flip,3,3_flip,4,4_flip,11,11_flip,12,12_flip,13,14,14_flip,15";
    private ArrayList<SaboteurMove> dropMoves;
    private ArrayList<SaboteurCard> hand;
    private static final double MALUS_THRESHOLD = 3; //TODO: play to figure out optimal value
    private static final double DROP_THRESHOLD = 5; //TODO: play to figure out optimal value
    private static final double DELTA = 0.5; //TODO: play to figure out optimal value
    private static int playerId;
    private static int nuggetPos = -1;
    private static boolean[] playerHiddenRevealed = new boolean[]{false,false,false};
    private static double[][] distances = MyTools.getPositionDistances(-1,playerHiddenRevealed);
    //Key = MoveCost; Value = ArrayList<Move> of moves with that cost
    private TreeMap<Double, ArrayList<SaboteurMove>> legalMoveCosts;
    /**
     * You must modify this constructor to return your student number. This is
     * important, because this is what the code that runs the competition uses to
     * associate you with your agent. The constructor should do nothing else.
     */
    public StudentPlayer() {
        super("260742799");
    }

    /**
     * This is the primary method that you need to implement. The ``boardState``
     * object contains the current state of the game, which your agent must use to
     * make decisions.
     */
    public Move chooseMove(SaboteurBoardState bS) {
        System.out.println("Student Player playing as Player number: "+ bS.getTurnPlayer());
        System.out.println("This is Move #: "+ bS.getTurnNumber());
        final long START_TIME = System.currentTimeMillis();
        SaboteurMove myMove = bS.getRandomMove(); //DEFAULT


        this.legalMoveCosts = new TreeMap<Double, ArrayList<SaboteurMove>>();
        this.dropMoves = new ArrayList<SaboteurMove>();

        //1. Parse BS: goal state found? Cards in Hand?
        playerId = bS.getTurnPlayer();
        int oppId = 1 - playerId;

        if(bS.getTurnNumber()<=2){ //TODO: VERIFY => new game
            nuggetPos = -1;
            playerHiddenRevealed = new boolean[]{false,false,false};
        }

        this.hand = bS.getCurrentPlayerCards(); //TODO: necessary?
        SaboteurTile[][] hiddenBoard = bS.getHiddenBoard();
        int[][] hiddenIntBoard = bS.getHiddenIntBoard();

        //TODO: should the be function calls or replaced with function body?
        if(nuggetPos==-1) updateHidden(bS, hiddenBoard);
        distances = MyTools.getPositionDistances(nuggetPos,playerHiddenRevealed);

        getDistances(bS.getAllLegalMoves());
        double minDistanceToGoal;
        try{
            minDistanceToGoal = this.legalMoveCosts.firstKey();
        } catch (NoSuchElementException ex) { //empty treemap
            minDistanceToGoal = -1;
        }


        /* 1 Map Heuristic:
         * If distance to goal/middle is over some threshold + don't already know nugget location
         *       play MAP card as soon as possible
         * TODO: investigate if threshold is necessary especially w/r.p.t being blocked
         *  e.g. add surrounding if statement to skip map card if blocked and within 2 plays from goal
         */
        System.out.println("nuggetPos: "+nuggetPos);
        if(nuggetPos == -1) { //I.e. the nugget is not already discovered
            for (SaboteurCard card : this.hand) {
                if (card instanceof SaboteurMap) {
                    for(int i=0; i < playerHiddenRevealed.length; i++){//TODO: investigate order to reveal objectives
                        if (!playerHiddenRevealed[i]){
                            myMove = new SaboteurMove(card,SaboteurBoardState.hiddenPos[i][0],SaboteurBoardState.hiddenPos[i][1],playerId);
                            logging(START_TIME);
                            return getLegalMove(bS, myMove);
                        }
                    }
                }
            }
        }


        /* 2 Bonus Heuristic:
         * If blocked play bonus card as soon as possible, else save bonus cards
         */
        if (bS.getNbMalus(playerId) > 0) {
            for(SaboteurCard card : this.hand){
                if(card instanceof SaboteurBonus){
                    myMove = new SaboteurMove(card,0,0,playerId);
                    logging(START_TIME);
                    return getLegalMove(bS, myMove); //ISSUE: if for some reason this isn't a legal move it'll return a random card
                }
            }
            if(this.dropMoves.size() > 0) myMove = this.dropMoves.get(0);
            return getLegalMove(bS, myMove);
        }


        /* 3. Malus Heuristic:
         * Save as many as possible (2+) till distance under threshold
         * Play malus right before setting up 2nd to last tile to goal
         * Blocks opponent allowing agent to place 2nd to last tile without opponent stealing last tile
         */
        int reqNumMalus = (nuggetPos == -1) ? 2:1; //if goal is nugget just need 1 malus, if goal is unknown center need 2 active
        int numMalus = 0;
        for(SaboteurCard card : this.hand){
            if(card instanceof SaboteurMalus) {
                myMove = new SaboteurMove(card, 0, 0, playerId);
                numMalus++;
            }
        }
        boolean prepWin = minDistanceToGoal < MALUS_THRESHOLD && bS.getNbMalus(oppId) < reqNumMalus;
        if(numMalus > 0 && ( prepWin|| numMalus > reqNumMalus)) {
            logging(START_TIME);
            return getLegalMove(bS, myMove); //ISSUE: if for some reason this isn't a legal move it'll return a random card
        }

        if(minDistanceToGoal >= DROP_THRESHOLD){
            if(this.dropMoves.size() > 0){
                myMove = this.dropMoves.get(0);
                return getLegalMove(bS, myMove);
            }
        }


        /* 4. Tile Heuristic:
         * 1. USE THRESHOLD to calculate target board location.
         *  TODO: 2. ONCE selected board location, select tile via looking at maintaining path to origin.
         */
        // Return your move to be processed by the server.
        if (minDistanceToGoal != -1) { //Note: necessary because of edge case when no tile cards in deck
            MyTools.Graph g = new MyTools.Graph(10,hiddenIntBoard, hiddenBoard);
            ArrayList<SaboteurMove> optimalDestMoves = new ArrayList<>();
            int CUTOFF = 6, ctr=0;
            for(Map.Entry<Double,ArrayList<SaboteurMove>> entry : this.legalMoveCosts.entrySet()){
                if(ctr >= CUTOFF) break;
                optimalDestMoves.addAll(entry.getValue());
                ctr++;
            }
            //optimalDestMoves.addAll(this) TODO: investigate if should have more than just optimalDest
            ArrayList<Double> optimalNextMoveCosts = new ArrayList<Double>();
            for(SaboteurMove move : optimalDestMoves){
                optimalNextMoveCosts.add(g.analyse(move));
            }
            System.out.println("ANALYSIS COMPLETE");

            Double minCost = -1.0;
            int minIndex = 0;
            for(int i=0; i<optimalNextMoveCosts.size(); i++){
                Double cost = optimalNextMoveCosts.get(i);
                if(minCost <0 || (cost < minCost && cost >= 0) ){
                    minIndex = i;
                    minCost = optimalNextMoveCosts.get(i);
                }
            }
            if(minCost >= 0){
                myMove = optimalDestMoves.get(minIndex);
                logging(START_TIME);
                return getLegalMove(bS, myMove); //ISSUE: if for some reason this isn't a legal move it'll return a random card
            }
        }
        /* X. Drop Heuristic:
         * TODO
         *  if found goal, drop any map cards
         */
        if(this.dropMoves.size() > 0) myMove = this.dropMoves.get(0);

        /* X. Destroy Heuristic:
         * TODO: develop usage + resistance e.g. when to use and how to handle broken paths (affects tile heuristic)
         *  iDEA: destroy tiles that break path to origin
         */

        //while (System.currentTimeMillis() - START_TIME < TIME_LIMIT_MS - GRACE_PERIOD_MS) {} //TODO: remove while loop and figure out best way to do timeout
        return getLegalMove(bS, myMove);
    }

    //TODO: maybe return mincost + ORDERED list of moves.
    private void getDistances(ArrayList<SaboteurMove> legalMoves){
        List<String> dropIdx = new ArrayList<String>(Arrays.asList(dropTileIdx.split(",")));
        //System.out.println("dropIdx's: ");
        //dropIdx.forEach(idx -> System.out.print(" "+idx));
        ArrayList<String> cardsToBeDopped = new ArrayList<>();
        for(SaboteurMove move : legalMoves){
            SaboteurCard card = move.getCardPlayed();
            //String name = card.getName();
            if(card instanceof SaboteurTile){ //TODO: make sure the casting doesn't create an issue in getCardPlayed()
                if(dropIdx.contains(((SaboteurTile) card).getIdx())){
                    //System.out.println("Dropping Move: "+name);
                    cardsToBeDopped.add(card.getName());
                    continue;
                }
                //System.out.println("Adding Move: "+name);
                int[] pos = move.getPosPlayed();
                double cost = distances[pos[0]][pos[1]];
                if (this.legalMoveCosts.containsKey(cost)){
                    this.legalMoveCosts.get(cost).add(move);
                }else{
                    ArrayList<SaboteurMove> moves = new ArrayList<SaboteurMove>();
                    moves.add(move);
                    this.legalMoveCosts.put(cost,moves);
                }
            } else if(card instanceof SaboteurDestroy){
                cardsToBeDopped.add(card.getName());
            }else if(card instanceof  SaboteurMap && nuggetPos >-1){
                cardsToBeDopped.add(0,card.getName());
            }
        }
        for(int i=0; i<this.hand.size(); i++){
            if(cardsToBeDopped.contains(this.hand.get(i).getName())){
                this.dropMoves.add(new SaboteurMove(new SaboteurDrop(), i, 0, playerId ));
            }
        }
    }

    private static void logging(long START_TIME){
        final long END_TIME = System.currentTimeMillis();
        System.out.println("DURATION OF MOVE (ms) = "+(END_TIME - START_TIME));
    }


    private static SaboteurMove getLegalMove(SaboteurBoardState bS, SaboteurMove move){
        if(!bS.isLegal(move)) System.out.println("\t ERROR: Illegal Move "+move.getCardPlayed().getName()+" At "+move.getPosPlayed()); //TODO: remove logging
        return bS.isLegal(move) ? move : bS.getRandomMove();
    }

    /**
     * Finds the index of the nugget within the hidden hidden cards
     * //TODO: investigate potentiall issue of nuggetPos = -1 despite nugget having been found...
     * @param bS
     * @param hiddenBoard
     * @return int -1 for not found, 0-2 for index of SaboteurBoardState.hiddenPos located at
     */
	private static void updateHidden(SaboteurBoardState bS, SaboteurTile[][] hiddenBoard){
        int pos = -1,numHidden = 0;
    	for(int h=0;h<3;h++){
    	    int[] hiddenPos = SaboteurBoardState.hiddenPos[h];
    	    String idx = hiddenBoard[hiddenPos[0]][hiddenPos[1]].getIdx();
    	    if("nugget".equals(idx)){
    	        pos = h;
    	        playerHiddenRevealed[h] = true;
    	        System.out.println("!FOUND NUGGET AT POS = "+h);
    	        break;
            }else if(!"8".equals(idx)){ //previously discovered hidden tile
    	        playerHiddenRevealed[h] = true;
            }else{
    	        numHidden++;
            }
        }
    	nuggetPos = pos;
    	//If only 1 remaining hidden tile and nugget hasn't been found can assume nugget is at that hidden tile
    	if(nuggetPos==-1 && numHidden==1){
    	    for(int h=0;h<3; h++){
    	        if(!playerHiddenRevealed[h]){
    	            nuggetPos = h;
    	            playerHiddenRevealed[h] = true;
    	            System.out.println("!ASSUMING NUGGET AT POS = "+h);
    	            return;
                }
            }
        }
	}

}