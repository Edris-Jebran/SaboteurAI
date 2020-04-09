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
        
        int player_id = boardState.getTurnPlayer();
        
        // get all legal moves and choose the best
        ArrayList<SaboteurMove> legal_moves = boardState.getAllLegalMoves();
        ArrayList<SaboteurCard> curHand = boardState.getCurrentPlayerCards();
        
        int [][] board = boardState.getHiddenIntBoard(); // curent state of the board, can't see hidden objects
        
        /*
         * Heuristic: Analyze curHand (cards), if have any map card, play it, else do a local search to find best move
         * Local Search function will find best possible move according to the state of the board, as well as the cards in hand
         */
        
        // TODO: Analyze current hand of cards, if contains map card play it and update local board
        
        
        // TODO: If above return null, make call to localSearch function to get best possible move
        
        
        
        int x = 0;
        
        for(SaboteurMove m: legal_moves) {
        	System.out.println("move "+x+" "+m.toPrettyString());
        	x++;
        }
       
        Move myMove = legal_moves.get((int)(legal_moves.size() * Math.random()));

        // Return your move to be processed by the server.
        return myMove;
    }
    // TODO: Complete function
    //@param: boardState, the current state of the board, with hidden objects hidden
    //@param: curHand, the cards currently in the AI agents possesion
    //@return: best possible move that can be done
   public static Move localSearchFunc(SaboteurBoardState boardState, ArrayList<SaboteurCard> curHand) {
	   return null;
	   
   }
}