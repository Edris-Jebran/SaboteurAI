package student_player;

import Saboteur.SaboteurBoard;
import Saboteur.SaboteurMove;
import Saboteur.cardClasses.*;
import boardgame.Move;

import Saboteur.SaboteurPlayer;
import Saboteur.SaboteurBoardState;

import java.util.ArrayList;

/** A player file submitted by a student. */
public class StudentPlayer extends SaboteurPlayer {

    private static SaboteurBoardState bSS = null; //TODO: replace bS in private functions
    private static double[][] distances = MyTools.getPositionDistances(-1);
    // timing constants to prevent loop from timing out
    private static final int TIME_LIMIT_MS = 2000;
    private static final int GRACE_PERIOD_MS = 1500; //TODO: is this necessary?
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
        final long START_TIME = System.currentTimeMillis();
        SaboteurMove myMove = bS.getRandomMove(); //DEFAULT

        //1. Parse BS: goal state found? Cards in Hand?
        int player_id = bS.getTurnPlayer();
        int opp_id = 1 - player_id;

        ArrayList<SaboteurCard> hand = bS.getCurrentPlayerCards(); //TODO: necessary?
        ArrayList<SaboteurMove> legalMoves = bS.getAllLegalMoves();
        SaboteurTile[][] hiddenBoard = bS.getHiddenBoard();

        //TODO: should the be function calls or replaced with function body?
        boolean[] playerHiddenRevealed = getHiddenRevealed(bS, hiddenBoard);
        int nuggetPos = getNuggetPosition(bS, hiddenBoard);
        distances = MyTools.getPositionDistances(nuggetPos);


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
                    return getLegalMove(bS, myMove); //ISSUE: if for some reason this isn't a legal move it'll return a random card
                }
            }
        }


        /* 3. Malus Heuristic:
         * Save as many as possible (2+) till distance under threshhold
         * Play malus right before setting up 2nd to last tile to goal
         * Blocks opponent allowing agent to place 2nd to last tile without opponent stealing last tile
         */
        int reqNumMalus = (nuggetPos == -1) ? 2:1; //if goal is nugget just need 1 malus, if goal is unknown center need 2 active
        if(bS.getNbMalus(opp_id) < reqNumMalus) { //TODO: update statement on distance
            for(SaboteurCard card : hand){
                if(card instanceof SaboteurMalus){
                    myMove = new SaboteurMove(card,0,0,player_id);
                    return getLegalMove(bS, myMove); //ISSUE: if for some reason this isn't a legal move it'll return a random card
                }
            }
        }

        /* 4. Tile Heuristic:
         * P
         */

        /* X. Drop Heuristic:
         * TODO
         */

        /* X. Destroy Heuristic:
         * TODO: develop usage + resistance e.g. when to use and how to handle broken paths (affects tile heuristic)
         */



        while (System.currentTimeMillis() - START_TIME < TIME_LIMIT_MS - GRACE_PERIOD_MS) { //TODO: remove while loop and figure out best way to do timeout

        }


        // Return your move to be processed by the server.
        return getLegalMove(bS, myMove);
    }


    private static SaboteurMove getLegalMove(SaboteurBoardState bS, SaboteurMove move){
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