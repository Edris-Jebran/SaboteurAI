package student_player;

import boardgame.Move;
import Saboteur.SaboteurBoard;
import Saboteur.cardClasses.*;
import Saboteur.SaboteurPlayer;
import Saboteur.SaboteurBoardState;
import Saboteur.SaboteurMove;

import java.math.*;
import java.util.*;


/** A player file submitted by a student. */
public class StudentPlayer extends SaboteurPlayer {
	
	private static SaboteurBoardState bs;
	private static ArrayList<SaboteurCard> hand;
	private static ArrayList<SaboteurMove> legal_moves;
	private static int playerId;
	private static double distanceToGoal;
	private static final int THRESHOLD_DISTANCE = 3;
	
	private static int numMovesDone = 0;
	private static boolean nuggetFound = false;
	private static final int student_BOARD_SIZE = 14;
    private static final int student_originPos = 5;
	private static final int[][] student_hiddenPos = {{student_originPos+7,student_originPos-2},{student_originPos+7,student_originPos},{student_originPos+7,student_originPos+2}};
	private static boolean[] student_hiddenRevealed = {false,false,false};
	private static int goalPosition[] = {student_originPos+7,student_originPos}; // Initially set to the middle hidden tile, updated after map used
	
	private static String dropTileIdx = "1,2,2_flip,3,3_flip,4,4_flip,11,11_flip,12,12_flip,13,14,14_flip,15";
	private static String optimalTileIdx = "0,5,5_flip,6,6_flip,7,7_flip,8,9,9_flip,10";
	
	
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
    	final long START_TIME = System.currentTimeMillis();
       
        System.out.println("Student player acting as player number: "+boardState.getTurnPlayer());
        
        Move chosenMove = null;
        
        // setting global variables/data structures to be used by methods
        bs = boardState;
        hand = boardState.getCurrentPlayerCards();
        legal_moves = boardState.getAllLegalMoves();
        playerId = boardState.getTurnPlayer();
        distanceToGoal = 5;
        
        
//        int [][] board = boardState.getHiddenIntBoard(); // curent state of the board, can't see hidden objects
//        SaboteurTile[][] curHiddenBoard = boardState.getHiddenBoard();
        
        
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
        	int nuggetTile = nuggetFind(); // If prreviously, Map revealed nugget, this updates flag
        	
        	if(nuggetTile != -1) { // nugget position is known so update goalPosition and call localSearch
        		int i = nuggetTile;
        		goalPosition[0] = student_hiddenPos[i][0];
        		goalPosition[1] = student_hiddenPos[i][1];
        		chosenMove = localSearchFunc();
        		//return chosenMove;
        		
        	}else { // else see if can play map card
        		chosenMove = mapFind(); // returns move to play if map card is found else null
        		if(chosenMove != null) {
        			//return chosenMove;
        		}else {
        			//return 
        			chosenMove = localSearchFunc();
        		}
        	}
        }else { // nugget position is known, conduct localSerach for best move
        
        	chosenMove = localSearchFunc();
        	//return chosenMove;
        	
        }
        final long END_TIME = System.currentTimeMillis();
        System.out.println("DURATION OF MOVE (ms) = "+(END_TIME - START_TIME));
        return chosenMove;
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
   public static Move mapFind() {
	   
	   Move moveChosen = null;  
       boolean mapCardFound = false;
       SaboteurCard  mapCard = null;
       
	   for(SaboteurCard card: hand) {     	
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
   public static Move cardFind(String cardName) {
	   
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
 public static int nuggetFind() {
	   
	   for(int i=0; i<2;i++) {
		   SaboteurTile hiddenTile = bs.getHiddenBoard()[bs.hiddenPos[i][0]][bs.hiddenPos[i][1]];
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
   public static Move localSearchFunc() {
	   
	   Move chosenMove = null;
	   /*
	   * LocalSearch Part 1: Malus and Bonus cards
	   * If opponent has played Malus, play bonus, or drop a card
	   * 
	   */
	   
       boolean isBlocked = bs.getNbMalus(playerId)>0; // if the opponent has played malus, isBlocked is true
       if(isBlocked) {
    	   String bonus = "bonus";
    	   System.out.println("Player blocked, gotta play bonus or drop");
    	   chosenMove = cardFind(bonus);
    	   
    	   if(chosenMove != null) {
    		   return chosenMove; // if we have bonus card play it, else drop a useless card
    	   }else {
    		   // drop bad card
    		   chosenMove = optimalDrop();
    		   return chosenMove;
    	   }
       }
       
       // if not blocked by malus, check if Agent has malus card
   	   int numMalus = 0;
   	   for(SaboteurCard card: hand) {
   		   if(card instanceof SaboteurMalus) {
   			   numMalus++;
   		   }   
   	   }
   	   // if nugget found and distance to goal < threshold or numMalus >2, play malus
   	   if((numMalus>0 && distanceToGoal<=THRESHOLD_DISTANCE) || numMalus>2) {
   		   
   		String malus = "malus";
        chosenMove = cardFind(malus);
        
        if(chosenMove == null) {
     	   chosenMove = optimalMove();
     	   return chosenMove;
          }   
   	   }
   	    
       // if the above conditions aren't meet, call optimalMove func
       chosenMove = optimalMove();
	   return chosenMove;
	   
   }
   
  public static Move optimalMove() {
	  
	  ArrayList<String> drop_idx = new ArrayList<String>(Arrays.asList(dropTileIdx.split(",")));
	  ArrayList<String> optimalTile_idx = new ArrayList<String>(Arrays.asList(optimalTileIdx.split(",")));
      
	  SaboteurMove mv = null;
	  
	  
	  boolean goodMove = false;
	  
	  if(goodMove) {
		  
	  }else {
		  
		// If no good tile was found to be placed on the board, drop worst cards
		  // bad tiles -> destroy card -> unused map
	      int i = 0, badTileIndex = -1;
	      for(SaboteurCard card: hand) {
	    	  if(card instanceof SaboteurTile) {
	    		  if(drop_idx.contains(((SaboteurTile) card).getIdx())) {
	    	      	badTileIndex = i;
	    	      	break;
	    	      	}
	    	  }
	      	i++;
	      }
	    
	      if(badTileIndex != -1) {
	    	 System.out.println("Optimal move -> dropping: "+hand.get(badTileIndex).getName());
	         mv = (new SaboteurMove(new SaboteurDrop(), badTileIndex, 0, playerId));
	          
	      }else { // If bad tile was not found, conduct search for Destroy card, or Map card

	      	 int j = 0, destroyIndex = -1, mapIndex = -1;
	         for(SaboteurCard card: hand) {
	      
	       	  if(card instanceof SaboteurDestroy) {
	       	      destroyIndex = j;
	       	      break;
	       	      	}
	       	  if(card instanceof SaboteurMap) {
	       		// if no destroy card, we can drop map card
		      	mapIndex = j;
	       	  }
	         	j++;
	         }

	         // if Destroy card was found chose move that will drop it
	       
	         if(destroyIndex != -1) {
	        	System.out.println("Optimal move -> dropping: "+hand.get(destroyIndex).getName());
	        	mv = (new SaboteurMove(new SaboteurDrop(), destroyIndex, 0, playerId));
	            
	         } // else see if there is a map card to drop
	         else if(mapIndex != -1) {
	        	System.out.println("Optimal move -> dropping: "+hand.get(mapIndex).getName());
	        	mv = (new SaboteurMove(new SaboteurDrop(), mapIndex, 0, playerId));
	                   	 
	         }else {
	        	 
	        	 mv = bs.getRandomMove();
	             System.out.println("random move "+mv.toPrettyString());
	        	 
	         }  
	      }		  
	  }
	System.out.println("Reached the end");
    return mv;
           
  }
  
// Evaluates current set of moves and returns move that drops the most useless card
public static Move optimalDrop() {
	
	//ArrayList<SaboteurMove> moves = bs.getAllLegalMoves();
	ArrayList<String> drop_idx = new ArrayList<String>(Arrays.asList(dropTileIdx.split(",")));
	
	// The bad tile's idx are in the list drop_idx, if current hand has one of them, it can be dropped
	for(SaboteurMove move: legal_moves) {
		SaboteurCard card = move.getCardPlayed();
		if(card instanceof SaboteurTile) {
			if(drop_idx.contains(((SaboteurTile) card).getIdx())) {
				System.out.println("Found shit card to drop");
				return move;
			}
		}
	}
	
	// If we don't currently have the bad tile cards in current hand we can drop Map or destroy cards
	
	// 1: If the nugget position is known, then any map card can be dropped
	if(nuggetFound) { 
		
		for(SaboteurMove move: legal_moves) {
			SaboteurCard card = move.getCardPlayed();
			if(card instanceof SaboteurMap) { 
				// useless map card found, can be dropped
				return move;
			}
		}
	}else { // If current hand doesn't have bad tile cards, useless map card, check to see if there is a destroy card
		
		for(SaboteurMove move: legal_moves) {
			SaboteurCard card = move.getCardPlayed();
			if(card instanceof SaboteurDestroy) { 
				// useless destroy card found, can be dropped
				return move;
			}
		}
	}
	// worst case if no condition is met, drop random
			return bs.getRandomMove();
  }
}

