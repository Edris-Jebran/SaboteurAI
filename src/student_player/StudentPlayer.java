package student_player;

import Saboteur.SaboteurBoard;
import Saboteur.SaboteurMove;
import Saboteur.cardClasses.*;
import boardgame.Move;

import Saboteur.SaboteurPlayer;
import Saboteur.SaboteurBoardState;

import java.util.ArrayList;
import java.util.Arrays;

/** A player file submitted by a student. */
public class StudentPlayer extends SaboteurPlayer {

    private static SaboteurBoardState bSS = null; //TODO: replace bS in private functions
    private static double[][] distances = MyTools.getPositionDistances(-1);
    // timing constants to prevent loop from timing out
    private static final int TIME_LIMIT_MS = 2000;
    private static final int GRACE_PERIOD_MS = 1500; //TODO: is this necessary?

    private static final double THRESHOLD_DISTANCE = 3; //TODO: play to figure out optimal value
    private static final double DELTA = 0.5; //TODO: play to figure out optimal value

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
        final long START_TIME = System.currentTimeMillis();
        SaboteurMove myMove = bS.getRandomMove(); //DEFAULT

        //1. Parse BS: goal state found? Cards in Hand?
        int player_id = bS.getTurnPlayer();
        int opp_id = 1 - player_id;

        ArrayList<SaboteurCard> hand = bS.getCurrentPlayerCards(); //TODO: necessary?
        ArrayList<SaboteurMove> legalMoves = bS.getAllLegalMoves();
        SaboteurTile[][] hiddenBoard = bS.getHiddenBoard();
        int[][] hiddenIntBoard = bS.getHiddenIntBoard();

        //TODO: should the be function calls or replaced with function body?
        boolean[] playerHiddenRevealed = getHiddenRevealed(bS, hiddenBoard);
        int nuggetPos = getNuggetPosition(bS, hiddenBoard);
        distances = MyTools.getPositionDistances(nuggetPos);

        double[] optimalTile = getMinDistance(legalMoves); //Move index, Cost of Move
        double distanceToGoal = optimalTile[1];

        //if(distanceToGoal < THRESHOLD_DISTANCE){
        System.out.println("\n printing HIDDEN INT BOARD");
        for(int[] row : hiddenIntBoard) {
            System.out.println(Arrays.toString(row)); //TODO: remove + remove import of Arrays
        }
        //}

        /* 1 Map Heuristic:
         * If distance to goal/middle is over some threshold + don't already know nugget location
         *       play MAP card as soon as possible
         * TODO: investigate if threshold is necessary especially w/r.p.t being blocked
         *  e.g. add surrounding if statement to skip map card if blocked and within 2 plays from goal
         */
        if(nuggetPos == -1) { //I.e. the nugget is not already discovered
            for (SaboteurCard card : hand) {
                if (card instanceof SaboteurMap) {
                    for(int i=0; i < playerHiddenRevealed.length; i++){//TODO: investigate order to reveal objectives
                        if (!playerHiddenRevealed[i]){
                            myMove = new SaboteurMove(card,SaboteurBoardState.hiddenPos[i][0],SaboteurBoardState.hiddenPos[i][1],player_id);
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
        if (bS.getNbMalus(player_id) > 0) {
            for(SaboteurCard card : hand){
                if(card instanceof SaboteurBonus){
                    myMove = new SaboteurMove(card,0,0,player_id);
                    logging(START_TIME);
                    return getLegalMove(bS, myMove); //ISSUE: if for some reason this isn't a legal move it'll return a random card
                }
            }
        }


        /* 3. Malus Heuristic:
         * Save as many as possible (2+) till distance under threshold
         * Play malus right before setting up 2nd to last tile to goal
         * Blocks opponent allowing agent to place 2nd to last tile without opponent stealing last tile
         */
        int reqNumMalus = (nuggetPos == -1) ? 2:1; //if goal is nugget just need 1 malus, if goal is unknown center need 2 active
        if(distanceToGoal < THRESHOLD_DISTANCE  && bS.getNbMalus(opp_id) < reqNumMalus) { //TODO: update statement on distance
            for(SaboteurCard card : hand){
                if(card instanceof SaboteurMalus){
                    myMove = new SaboteurMove(card,0,0,player_id);
                    logging(START_TIME);
                    return getLegalMove(bS, myMove); //ISSUE: if for some reason this isn't a legal move it'll return a random card
                }
            }
        }

        /* 4. Tile Heuristic:
         * 1. USE THRESHOLD to calculate target board location.
         *  TODO: 2. ONCE selected board location, select tile via looking at maintaining path to origin.
         */
        // Return your move to be processed by the server.
        int moveIndex = (int) optimalTile[0];
        if (moveIndex>=0) { //Note: necessary because of edge case when no tile cards in deck
            myMove = legalMoves.get((int) optimalTile[0]); //new SaboteurMove(card,0,0,player_id);
            MyTools.Graph g = new MyTools.Graph(10,hiddenIntBoard, hiddenBoard);
            logging(START_TIME);
            return getLegalMove(bS, myMove); //ISSUE: if for some reason this isn't a legal move it'll return a random card
        }
        return getLegalMove(bS, myMove);
        /* X. Drop Heuristic:
         * TODO
         *  if found goal, drop any map cards
         */

        /* X. Destroy Heuristic:
         * TODO: develop usage + resistance e.g. when to use and how to handle broken paths (affects tile heuristic)
         *  iDEA: destroy tiles that break path to origin
         */

        //while (System.currentTimeMillis() - START_TIME < TIME_LIMIT_MS - GRACE_PERIOD_MS) {} //TODO: remove while loop and figure out best way to do timeout

    }

    //TODO: maybe return mincost + ORDERED list of moves.
    private double[] getMinDistance(ArrayList<SaboteurMove> legalMoves){
        double[] retVal = new double[2];// index of move, cost
        double minCost=0;
        int minIndex=-1, ctr = 0;
        for(SaboteurMove move : legalMoves){
            SaboteurCard card = move.getCardPlayed();
            if(card instanceof SaboteurTile){ //TODO: make sure the casting doesn't create an issue in getCardPlayed()
                int[] pos = move.getPosPlayed();
                double cost = distances[pos[0]][pos[1]];
                if (minIndex < 0 || cost < minCost){
                    minCost = cost;
                    minIndex = ctr;
                }
            }
            ctr ++;
        }

        retVal[0] = minIndex;
        retVal[1] = minCost;
        return retVal;
    }

    private static void logging(long START_TIME){
        final long END_TIME = System.currentTimeMillis();
        System.out.println("DURATION OF MOVE (ms) = "+(END_TIME - START_TIME));
    }


    private static SaboteurMove getLegalMove(SaboteurBoardState bS, SaboteurMove move){
        if(!bS.isLegal(move)) System.out.println("\t ERROR: Illegal Move "+move.getCardPlayed().getName()+" At "+move.getPosPlayed()); //TODO: remove logging
        return bS.isLegal(move) ? move : bS.getRandomMove();
    }

    private static boolean[] getHiddenRevealed(SaboteurBoardState bS, SaboteurTile[][] hiddenBoard){
        boolean[] hiddenRevealed = new boolean[3]; //NOTE: hardcoded 3 because assuming that is invariable
    	for(int h=0;h<3;h++){
    	    int[] hiddenPos = SaboteurBoardState.hiddenPos[h];
            hiddenRevealed[h] = !"8".equals(hiddenBoard[hiddenPos[0]][hiddenPos[1]].getIdx());
        }
    	return hiddenRevealed;
	}

    /**
     * Finds the index of the nugget within the hidden hidden cards
     * //TODO: investigate potentiall issue of nuggetPos = -1 despite nugget having been found...
     * @param bS
     * @param hiddenBoard
     * @return int -1 for not found, 0-2 for index of SaboteurBoardState.hiddenPos located at
     */
	private static int getNuggetPosition(SaboteurBoardState bS, SaboteurTile[][] hiddenBoard){
        int nuggetPos = -1;
    	for(int h=0;h<3;h++){
    	    int[] hiddenPos = SaboteurBoardState.hiddenPos[h];
    	    if("nugget".equals(hiddenBoard[hiddenPos[0]][hiddenPos[1]].getIdx())){
    	        nuggetPos = h;
    	        break;
            }
        }
    	return nuggetPos;
	}

}