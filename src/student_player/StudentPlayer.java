package student_player;

import boardgame.Move;

import Saboteur.SaboteurPlayer;

import java.util.ArrayList;

import Saboteur.SaboteurBoardState;
import Saboteur.SaboteurMove;
import java.math.*;

/** A player file submitted by a student. */
public class StudentPlayer extends SaboteurPlayer {

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
        
        // get all legal moves and choose the best
        ArrayList<SaboteurMove> legal_moves = boardState.getAllLegalMoves();
        int x = 0;
        
        for(SaboteurMove m: legal_moves) {
        	System.out.println("move "+x+" "+m.toPrettyString());
        	x++;
        }
       
        Move myMove = legal_moves.get((int)(legal_moves.size() * Math.random()));

        // Return your move to be processed by the server.
        return myMove;
    }
}