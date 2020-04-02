package student_player;

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
}