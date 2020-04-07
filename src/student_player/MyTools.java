package student_player;

import Saboteur.SaboteurBoardState;
import Saboteur.cardClasses.SaboteurTile;

public class MyTools {
    public static double getSomething() {
        return Math.random();
    }
    
    public static double getHeuristicScore(Node node) {

		double winScore = node.getWinScore();

		int visits = node.getVisitCount();

		

		// explore if node is unvisited

		if (visits == 0 || node.isRoot()) {

			return Integer.MAX_VALUE;

		}

		

		int parentVisits = node.getParent().getVisitCount();

		

		return winScore / visits + 100 * Math.sqrt(Math.log(parentVisits)/visits);

	}

	public static double[][] getPositionDistances(int nugPos){
        double[][] distances = new double[SaboteurBoardState.BOARD_SIZE][SaboteurBoardState.BOARD_SIZE];
        int[] target = {SaboteurBoardState.hiddenPos[1][0],SaboteurBoardState.hiddenPos[1][1]}; //x,y default center
        if(nugPos > -1){
            target[0] = SaboteurBoardState.hiddenPos[nugPos][0];
            target[1] = SaboteurBoardState.hiddenPos[nugPos][1];
        }
        for(int x=0; x<SaboteurBoardState.BOARD_SIZE; x++){
            for(int y=0; y<SaboteurBoardState.BOARD_SIZE; y++) {
                distances[x][y] = 1.2*Math.abs(x-target[0]) + Math.abs(y-target[1]); //TODO tweek with distance calculation formula
            }
        }
        return distances;
    }

}