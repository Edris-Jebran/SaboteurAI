package student_player;

import boardgame.Move;
import Saboteur.SaboteurBoard;
import Saboteur.cardClasses.*;
import Saboteur.SaboteurPlayer;
import Saboteur.SaboteurBoardState;
import Saboteur.SaboteurMove;

import java.util.ArrayList;
import java.math.*;

/** A player file submitted by a student. */
public class StudentPlayer extends SaboteurPlayer {
	
	private static int numMovesDone = 0;
	private static boolean nuggetFound = false;
	private static final int student_BOARD_SIZE = 14;
    private static final int student_originPos = 5;
	private static final int[][] student_hiddenPos = {{student_originPos+7,student_originPos-2},{student_originPos+7,student_originPos},{student_originPos+7,student_originPos+2}};
	private static boolean[] student_hiddenRevealed = {false,false,false};
	private static int goalPosition[] = {student_originPos+7,student_originPos}; // Initially set to the middle hidden tile, updated after map used
	
	private static ArrayList<SaboteurMove> expendableMoves; // in the event where have to drop card, chose from begining of list
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
    public Move chooseMove(SaboteurBoardState boardState) {
       
        System.out.println("Student player acting as player number: "+boardState.getTurnPlayer());
        
        // Get all data necessary to paint an image of what the board looks like, curhand
        // End goal is to do a local search and return the best possible move.
        
        Move chosenMove = null;
        
        int player_id = boardState.getTurnPlayer();
        
        // get all legal moves and choose the best
        ArrayList<SaboteurMove> legal_moves = boardState.getAllLegalMoves();
        ArrayList<SaboteurCard> curHand = boardState.getCurrentPlayerCards();
        
        int [][] board = boardState.getHiddenIntBoard(); // curent state of the board, can't see hidden objects
        SaboteurTile[][] curHiddenBoard = boardState.getHiddenBoard();
        
//        for(SaboteurMove move: legal_moves) {
//        	if(move.getCardPlayed().getName().equalsIgnoreCase("map")) {
//        		System.out.println("The initial Move is "+move.toPrettyString());
//        		SaboteurMove m = new SaboteurMove(move.getCardPlayed(), 12,5, player_id);
//        		System.out.println("Using map "+ m.toPrettyString());
//        		return m;
//        	}
//        }
//        
//        System.exit(0);
//        return chosenMove;
        
        /*
         * Heuristic: 
         * 1) Analyze board, see if nugget/objective has been found or not. If yes, go to search algorithm, else
         * see if map card is in current hand. Play Map card to find nugget and update goalState.
         * 
         * 2) Search algorithm:  do a local search to find best move
         * Local Search function will find best possible move according to the state of the board, as well as the cards in hand
         *
         */
        
        // If nuggetFound is false, call nuggetFind which will see if nugget was found and updates goal
        // if no nugget is found search cards for Map card and play it
        if(!nuggetFound) {
        	int nuggetTile = nuggetFind(boardState); // If prreviously, Map revealed nugget, this updates flag
        	
        	if(nuggetTile != -1) { // nugget position is known so update goalPosition and call localSearch
        		int i = nuggetTile;
        		goalPosition[0] = student_hiddenPos[i][0];
        		goalPosition[1] = student_hiddenPos[i][1];
        		chosenMove = localSearchFunc(boardState, player_id);
        		return chosenMove;
        		
        	}else { // else see if can play map card
        		chosenMove = mapFind(curHand, player_id); // returns move to play if map card is found else null
        		if(chosenMove != null) {
        			return chosenMove;
        		}else {
        			return localSearchFunc(boardState, player_id);
        		}
        	}
        }else { // nugget position is known, conduct localSerach for best move
        
        	chosenMove = localSearchFunc(boardState, player_id);
        	return chosenMove;
        	
        }
    }
    
   /* 
    * Helper function used before conducting localSearch
    * Seaches the current hand to see if map card we is present or not
    * 
    * @param: legal_moves: All moves currently possible to AI agent 
    * @param: card_name: Name of the card we are searching for
    * @return: If map card is found, it returns a move and updates the corresponding hiddenReveal
    * 
    * */
   public static Move mapFind(ArrayList<SaboteurCard> cards, int playerId) {
	   
	   Move moveChosen = null;  
       boolean mapCardFound = false;
       SaboteurCard  mapCard = null;
       
	   for(SaboteurCard card: cards) {     	
           if(card instanceof SaboteurMap) {
           	System.out.println("Map card found: "+card.getName());
           	mapCardFound = true;
           	mapCard = card;
           	break;
           }
       }
	   
	   // If map card was found play to reveal hiddenTile
	   if(mapCardFound) {
		   for(int i=0; i<3;i++) { // loop over the hidden tiles to see where it can be played
			   if(!student_hiddenRevealed[i]) {
				   // set hiddenToRevealed since map will be used
				   student_hiddenRevealed[i] = true;
				   
				   // build move that will use map card on correct hidden position
				   int x = student_hiddenPos[i][0];
				   int y = student_hiddenPos[i][1];
				   
				   SaboteurMove theMove = new SaboteurMove(mapCard, x,y, playerId);
				   moveChosen = theMove;
				   break;
			   }
		   }
	   }
	   return moveChosen;
   }
   
   /* 
    * Helper function used before conducting localSearch
    * Seaches the current hand to see if target card( Bonus/Malus) is present or not.
    * 
    * 
    * @param: legal_moves: All moves currently possible to AI agent 
    * @param: cards: current hand of cards
    * @param: targetCard: target card
    * @return: If card is found, it returns move, else null
    * 
    * */
   public static Move cardFind(ArrayList<SaboteurMove> legal_moves, String cardName, int playerId) {
	   
	   Move moveChosen = null;  
       
       for(SaboteurMove move: legal_moves) { 
    	   SaboteurCard card = move.getCardPlayed();
           if(card.getName().equalsIgnoreCase(cardName)) {
           	System.out.println(cardName+ " card found: "+card.getName());
           	moveChosen = move;
           	break;
           }
       }
	   
	   return moveChosen;
   }
   
 /*
  * @nuggetFound(SaboteurBoardState): Analyze the board to return the position of nugget if it is revealed, else returns -1
  * @paramL SaboteurBoardState: the current board state
  * @return: If nugget is hidden return -1, else the position 0,1,2 from which goal position can be computed
  * 
  */
 public static int nuggetFind(SaboteurBoardState boardState) {
	   
	   for(int i=0; i<2;i++) {
		   SaboteurTile hiddenTile = boardState.getHiddenBoard()[boardState.hiddenPos[i][0]][boardState.hiddenPos[i][1]];
		   if(hiddenTile.getIdx().equalsIgnoreCase("nugget")) {
			   // set nuggetFound to true
			   System.out.println("**********************Found the freaking nugget*********************");
			   nuggetFound = true;
			   return i;
		   }
	   }
	   return -1;
   }
   
    // TODO: Main logic of local search
    //@param: boardState, the current state of the board, with givenGoal state
    //@param: curHand, the cards currently in the AI agents possesion
    //@return: best possible move that can be done
   public static Move localSearchFunc(SaboteurBoardState boardState, int player_id) {
	   
	   ArrayList<SaboteurMove> legal_moves = boardState.getAllLegalMoves();
	   
	   Move chosenMove = null;
	   /*
	   * LocalSearch Part 1: Malus and Bonus cards
	   * If opponent has played Malus, play bonus, or drop a card
	   * 
	   */
	   
       boolean isBlocked = boardState.getNbMalus(player_id)>0; // if the opponent has played malus, isBlocked is true
       if(isBlocked) {
    	   String bonus = "bonus";
    	   chosenMove = cardFind(legal_moves, bonus, player_id);
       }
       
       // conduct local search
	   return boardState.getRandomMove();
	   
   }
}