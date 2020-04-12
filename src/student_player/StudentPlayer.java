package student_player;

import Saboteur.SaboteurMove;
import Saboteur.cardClasses.*;
import boardgame.Move;

import Saboteur.SaboteurPlayer;
import Saboteur.SaboteurBoardState;

import java.util.*;

/** A player file submitted by a student. */
public class StudentPlayer extends SaboteurPlayer {
    private ArrayList<SaboteurMove> dropMoves;
    private ArrayList<SaboteurCard> hand;
    //Key = MoveCost; Value = ArrayList<Move> of moves with that cost
    private TreeMap<Double, ArrayList<SaboteurMove>> legalMoveCosts;

    private static final String dropTileIdx = "1,2,2_flip,3,3_flip,4,4_flip,11,11_flip,12,12_flip,13,14,14_flip,15";
    private static final double MALUS_THRESHOLD = 3;
    private static final double DROP_THRESHOLD = 5;

    private static int playerId;
    private static int nuggetPos = -1;

    private static boolean[] playerHiddenRevealed = new boolean[]{false,false,false};
    private static double[][] distances = MyTools.getPositionDistances(-1,playerHiddenRevealed);
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
        System.out.println("Student Player playing as Player number: "+ bS.getTurnPlayer()); //TODO: DELETE
        System.out.println("This is Move #: "+ bS.getTurnNumber());

        SaboteurMove myMove = bS.getRandomMove(); //DEFAULT


        this.legalMoveCosts = new TreeMap<Double, ArrayList<SaboteurMove>>();
        this.dropMoves = new ArrayList<SaboteurMove>();

        //1. Parse BS: goal state found? Cards in Hand?
        playerId = bS.getTurnPlayer();
        int oppId = 1 - playerId;

        if(bS.getTurnNumber()<=2){ // => new game
            nuggetPos = -1;
            playerHiddenRevealed = new boolean[]{false,false,false};
        }

        this.hand = bS.getCurrentPlayerCards();
        SaboteurTile[][] hiddenBoard = bS.getHiddenBoard();
        int[][] hiddenIntBoard = bS.getHiddenIntBoard();

        if(nuggetPos==-1) updateHidden(bS, hiddenBoard);
        distances = MyTools.getPositionDistances(nuggetPos,playerHiddenRevealed);

        getMoveDistances(bS.getAllLegalMoves());
        double minDistanceToGoal;
        try{
            minDistanceToGoal = this.legalMoveCosts.firstKey();
        } catch (NoSuchElementException ex) { //empty treemap
            minDistanceToGoal = -1;
        }


        /* 1 Map Heuristic:
         * If don't already know nugget location
         *       play MAP card as soon as possible
         */
        System.out.println("nuggetPos: "+nuggetPos);
        if(nuggetPos == -1) { //I.e. the nugget is not already discovered
            for (SaboteurCard card : this.hand) {
                if (card instanceof SaboteurMap) {
                    for(int i=0; i < playerHiddenRevealed.length; i++){
                        if (!playerHiddenRevealed[i]){
                            myMove = new SaboteurMove(card,SaboteurBoardState.hiddenPos[i][0],SaboteurBoardState.hiddenPos[i][1],playerId);
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
                    return getLegalMove(bS, myMove);
                }
            }
            if(this.dropMoves.size() > 0) myMove = this.dropMoves.get(0); //if stuck without bonus, drop card
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
            return getLegalMove(bS, myMove);
        }


        /* 4. Tile Heuristic:
         * 1. prioritize dropping bad cards if far enough away from goal to maximize chance of winning hand
         * 2. analyse each valid tile move and select optimal
         */

        if(minDistanceToGoal >= DROP_THRESHOLD){
            if(this.dropMoves.size() > 0){
                myMove = this.dropMoves.get(0);
                return getLegalMove(bS, myMove);
            }
        }

        if (minDistanceToGoal != -1) { //Note: necessary because of edge case when no tile cards in deck
            //Create graph structure to simulate moves
            MyTools.Graph g = new MyTools.Graph(10,hiddenIntBoard, hiddenBoard);
            ArrayList<SaboteurMove> optimalDestMoves = new ArrayList<>();

            // Select moves at ordered subset of optimal tile positions to analyse
            int CUTOFF = 6, ctr=0;
            for(Map.Entry<Double,ArrayList<SaboteurMove>> entry : this.legalMoveCosts.entrySet()){
                if(ctr >= CUTOFF) break; //TODO: try without cutoff
                optimalDestMoves.addAll(entry.getValue());
                ctr++;
            }
            // Analyse each move
            ArrayList<Double> optimalNextMoveCosts = new ArrayList<Double>();
            for(SaboteurMove move : optimalDestMoves){
                optimalNextMoveCosts.add(g.analyse(move));
            }

            // Select optimal move that (i.e. has min cost subsequent neighbours)
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
                return getLegalMove(bS, myMove); //ISSUE: if for some reason this isn't a legal move it'll return a random card
            }
        }

        /* 5. Drop Heuristic:
         * If no optimal tile move, try dropping cards to maximise hand opportunities for next round
         */
        if(this.dropMoves.size() > 0) myMove = this.dropMoves.get(0);

        /* X. Destroy Heuristic:
         * Ignored, found no decent algorithm to utilize the Destroy mechanic reliably.
         * Therefore return random move
         */
        return getLegalMove(bS, myMove);
    }

    /**
     * For each legal move find and store (ordered) cost in TreeMap this.legalMoveCosts
     * costs determined in terms of end tile location according to distances[][]
     * Additionally updates dropMoves to include targeted drops
     * @param legalMoves taken from boardstate.getAllLegalMoves
     */
    private void getMoveDistances(ArrayList<SaboteurMove> legalMoves){
        List<String> dropIdx = new ArrayList<String>(Arrays.asList(dropTileIdx.split(",")));
        ArrayList<String> cardsToBeDropped = new ArrayList<>();

        for(SaboteurMove move : legalMoves){
            SaboteurCard card = move.getCardPlayed();
            if(card instanceof SaboteurTile){
                //I.e. determined to be bad tile
                if(dropIdx.contains(((SaboteurTile) card).getIdx())){
                    cardsToBeDropped.add(card.getName());
                    continue;
                }

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
                cardsToBeDropped.add(card.getName());
            }else if(card instanceof  SaboteurMap && nuggetPos >-1){
                cardsToBeDropped.add(0,card.getName());
            }
        }

        for(int i=0; i<this.hand.size(); i++){
            if(cardsToBeDropped.contains(this.hand.get(i).getName())){
                this.dropMoves.add(new SaboteurMove(new SaboteurDrop(), i, 0, playerId ));
            }
        }
    }

    /**
     * @param bS original BoardState from server
     * @param move prospective move I want to play
     * @return randomLegalMove if move is illegal
     */
    private static SaboteurMove getLegalMove(SaboteurBoardState bS, SaboteurMove move){
        return bS.isLegal(move) ? move : bS.getRandomMove();
    }

    /**
     * Finds the index of the (discovered) nugget within the hidden hidden cards
     * nuggetPos: -1 for not found, 0-2 for index of SaboteurBoardState.hiddenPos located at
     * @param bS
     * @param hiddenBoard
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
    	            return;
                }
            }
        }
	}

}