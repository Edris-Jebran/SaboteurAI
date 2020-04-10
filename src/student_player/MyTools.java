package student_player;

import Saboteur.SaboteurBoardState;
import Saboteur.SaboteurMove;
import Saboteur.cardClasses.SaboteurCard;
import Saboteur.cardClasses.SaboteurTile;
import java.util.*;  //TODO: REMOVE

public class MyTools {
    public static class Graph {
        private ArrayList<ArrayList<Integer> > adj;
        private int srcTileNum=75;

        private int nodeCount;
        private int[][] hiddenIntBoard;
        //hashmap: Key = tile#; Value = int[4]{left,up,right,down} where each represents the adj index of the tile default = -1 for no tile
        private TreeMap<Integer, int[]> tileNeighbourhood = new TreeMap<Integer, int[]>();

        // A utility function to add an edge in an
        // undirected graph
        void addEdge(int u, int v){
            if(!this.adj.get(u).contains(v)) this.adj.get(u).add(v);
            if(!this.adj.get(v).contains(u)) this.adj.get(v).add(u);
        }

        /**
         * Temporarily add tile to graph, Can only have 1 overlaid targetNode
         * Thus remove any previously addedTiles
         * @param tileIdx
         * @param tileNumber
         * @return ordered position of nodes within subgraph i.e. [left,up,right,down]
         */
        private void addTile(String tileIdx, int tileNumber ){ //TODO
            boolean isHiddenTile=false;
            int i = tileNumber/SaboteurBoardState.BOARD_SIZE; // gives tile x(row) position
            int j = tileNumber%SaboteurBoardState.BOARD_SIZE; // gives tile y(col) position

            System.out.println("\t Tile: "+tileIdx);
            if( i == SaboteurBoardState.originPos && j == SaboteurBoardState.originPos){
                this.srcTileNum = tileNumber;
            }else if(tileIdx.equals("nugget") || tileIdx.equals("hidden1") || tileIdx.equals("hidden2")){
                System.out.println("Hidden Tile: " + tileIdx);
                isHiddenTile = true;
            }
            boolean isConnected = false;
            int[] tileNodes = new int[]{-1,-1,-1,-1}; //left,up,right,down
            //Loop through path of a given tile
            for (int h = 0; h < 3; h++) {
                for (int k = 0; k < 3; k++) {
                    if((this.hiddenIntBoard[i * 3 + h][j * 3 + k] == SaboteurBoardState.TUNNEL) || isHiddenTile){//NODE
                        System.out.println("\t \t H: "+h+"\t K: "+k);
                        if(h==1 && k==1){ //CENTER CENTER
                            isConnected = true; //Only connected if center of tile is a tunnel
                            continue;
                        }else if(h==0 && k==1){ //TOP CENTER
                            tileNodes[1] = this.nodeCount;
                        }else if(h==1) { //CENTER LEFT
                            tileNodes[k] = this.nodeCount;
                        }else if(h==2 && k==1) { //BOTTOM CENTER
                            tileNodes[3] = this.nodeCount;
                        }else{
                            continue; //TODO: investigate necessity
                        }
                        System.out.println("\t Node #"+this.nodeCount+" @ Tile #: "+(SaboteurBoardState.BOARD_SIZE*i + j));

                        this.adj.add(new ArrayList<Integer>(4));
                        this.nodeCount++;
                    }
                }
            }
            System.out.println("Tile #: "+(tileNumber)+" with nodes at " +Arrays.toString(tileNodes) );
            if(isConnected){ //add strongly connected internal edges
                for(int a=0; a<3; a++){
                    if(tileNodes[a] == -1) continue;
                    for(int b=a+1; b<4; b++){
                        if(tileNodes[b] == -1) continue;
                        addEdge(tileNodes[a], tileNodes[b]);
                    }
                }
            }
            this.tileNeighbourhood.put(tileNumber,tileNodes);
        }

        private void connectTile(int tileNumber){
            int i = tileNumber/SaboteurBoardState.BOARD_SIZE; // gives tile x(row) position
            int j = tileNumber%SaboteurBoardState.BOARD_SIZE; // gives tile y(col) position

            int[] neighbourhood = this.tileNeighbourhood.get(tileNumber);
            System.out.println("Tile #"+tileNumber+" has Neighbourhood = #: "+Arrays.toString(neighbourhood));
            int[] neighbour; //left,up,right,down
            if(neighbourhood[2] != -1 && (j+1)%SaboteurBoardState.BOARD_SIZE > 0){ //exists position to the right and subgraph contains path to right
                neighbour = this.tileNeighbourhood.get(tileNumber+1);
                System.out.println("\t Right neighbour " + Arrays.toString(neighbour));
                if(neighbour != null && neighbour[0] != -1 ){ //next tile is indeed to the right of current position and has path leftwards
                    System.out.println("\t \t (right) edges: "+neighbourhood[2]+", "+neighbour[0]);
                    addEdge(neighbourhood[2],neighbour[0]);
                }
            }
            if(neighbourhood[3] != -1 && i+1 < SaboteurBoardState.BOARD_SIZE){//exists a tile and path to the bottom position that might be connected
                neighbour = this.tileNeighbourhood.get(tileNumber+SaboteurBoardState.BOARD_SIZE);
                System.out.println("\t Bottom neighbour " + Arrays.toString(neighbour));
                if(neighbour != null && neighbour[1] != -1){ //tile beneath has been paved and contains path upwards
                    System.out.println("\t \t (below) Nodes: "+neighbourhood[3]+", "+neighbour[1]);
                    addEdge(neighbourhood[3],neighbour[1]);
                }
            }
        }


        void removeTargetTile(int targetTileNum){
            ArrayList<Integer> neighbours = new ArrayList<Integer>();
            int[] tileNodes = this.tileNeighbourhood.get(targetTileNum);
            for(int tn : tileNodes) neighbours.add(tn);

            //1. remove external edges to tileNodes
            for(int i=0; i<this.adj.size(); i++){
                int finalI = i;
                if (Arrays.stream(tileNodes).anyMatch(j -> j == finalI)) continue; //skip internal nodes
                this.adj.get(i).removeAll(neighbours);
            }
            //2. remove internal nodes
            for(int n : neighbours){
                if(n == -1) continue;
                this.adj.remove(this.adj.size()-1);
                this.nodeCount--;
            }
            this.tileNeighbourhood.remove(targetTileNum);
        }

        /**
         *
         * @param targetTile
         * @return
         */
        private boolean pathToOrigin(int targetTile){
            int[] srcNodes = this.tileNeighbourhood.get(this.srcTileNum); //l,u,r,d
            int[] goalNodes = this.tileNeighbourhood.get(targetTile); //l,u,r,d
            Stack<Integer> nodeStack = new Stack<>();
            ArrayList<Integer> visitedNodes = new ArrayList<>();

            for(int srcNode : srcNodes) {
                if (srcNode == -1) continue;
                nodeStack.push(srcNode);
            }

            while(!nodeStack.isEmpty()){
                int curr = nodeStack.pop();
                if(Arrays.stream(goalNodes).anyMatch(i -> i == curr)) return true; // curr is one of any of the goal nodes

                visitedNodes.add(curr);
                for (int a : this.adj.get(curr)){
                    if(!visitedNodes.contains(a)) nodeStack.add(a);
                }
            }
            return false;
        }

        /**
         * Given a move determine return the minimum score of its resultant neighbours

         * @param move
         * @return
         */
        public double analyse(SaboteurMove move){ //TODO
            SaboteurTile card = (SaboteurTile) move.getCardPlayed();
            String tileIdx = card.getIdx();
            int[] pos = move.getPosPlayed();
            int targetTileNum = SaboteurBoardState.BOARD_SIZE*pos[0]+pos[1];

            double minNeighbourCost = -1;

            int x = targetTileNum/SaboteurBoardState.BOARD_SIZE; // gives tile x(row) position
            int y = targetTileNum%SaboteurBoardState.BOARD_SIZE; // gives tile y(col) position
            //1. Add tile to graph + Connect tile
            for (int k = 0; k < 3; k++) { //Temporarily update hiddenIntBoard
                for (int h = 0; h < 3; h++) {
                    this.hiddenIntBoard[pos[0] * 3 + k][pos[1] * 3 + h] = card.getPath()[h][2-k];
                }
            }
            addTile(tileIdx,targetTileNum);
            if(this.tileNeighbourhood.containsKey(targetTileNum-1)) connectTile(targetTileNum-1);
            if(this.tileNeighbourhood.containsKey(targetTileNum-SaboteurBoardState.BOARD_SIZE)) connectTile(targetTileNum-SaboteurBoardState.BOARD_SIZE);
            connectTile(targetTileNum);

            //2. Verify at least 1 of the move tile nodes in the graph are reachable from one of the source nodes (i.e. valid path)
            if(!pathToOrigin(targetTileNum)){
                System.out.println("\t ERROR: unable to find path to origin for prospective move");
                return minNeighbourCost;
            }

            //3. look at nodes within tile and determine W= minimum weight of neighbouring positions according to positionDistances
            int[] targetNodes = this.tileNeighbourhood.get(targetTileNum);//l,u,r,p
            System.out.println("\t \t TARGET NODES: "+Arrays.toString(targetNodes));
            double score = -1;
            for(int i=0; i<targetNodes.length; i++){
                int n = targetNodes[i];
                if(n == -1) continue;
                System.out.println("\t \t \t Analysing NODES: "+n);
                try {
                    char remove; //TODO: REMOVE
                    if (i == 0 && !this.tileNeighbourhood.containsKey(targetTileNum - 1)) { //l
                        score = positionDistances[x][y - 1];
                        remove='l';
                    } else if (i == 1 && !this.tileNeighbourhood.containsKey(targetTileNum - SaboteurBoardState.BOARD_SIZE)) {//u
                        score = positionDistances[x-1][y];
                        remove='u';
                    } else if (i == 2 && !this.tileNeighbourhood.containsKey(targetTileNum + 1)) {//r
                        score = positionDistances[x][y + 1];
                        remove='r';
                    } else if (i == 3 && !this.tileNeighbourhood.containsKey(targetTileNum + SaboteurBoardState.BOARD_SIZE)) {//d
                        score = positionDistances[x+1][y];
                        remove='d';
                    } else{
                        continue;
                    }
                    System.out.println("\t \t \t SCORE @"+remove+" is: "+score);
                    if(minNeighbourCost==-1 || score<minNeighbourCost){
                        minNeighbourCost=score;
                    }
                } catch(ArrayIndexOutOfBoundsException ex){
                    System.out.println("Caught error in analysis");
                }
            }

            //4. Remove tile + undo temporary changes to hiddenIntBoard
            for (int k = 0; k < 3; k++) {
                for (int h = 0; h < 3; h++) {
                    this.hiddenIntBoard[pos[0] * 3 + k][pos[1] * 3 + h] = SaboteurBoardState.EMPTY;
                }
            }
            removeTargetTile(targetTileNum);
            System.out.println("\t \t \t MIN SCORE is: "+minNeighbourCost);
            return minNeighbourCost; //W
        }

        // A utility function to print the adjacency list
        // representation of graph
        void printGraph() {
            System.out.println("Printing Graph");
            for (int i = 0; i < this.adj.size(); i++) {
                System.out.println("\nAdjacency list of vertex" + i);
                for (int j = 0; j < this.adj.get(i).size(); j++) {
                    System.out.print(" -> "+this.adj.get(i).get(j));
                }
                System.out.println();
            }
        }



        public Graph(int numTiles, int[][] hiddenIntBoard, SaboteurTile[][] hiddenBoard){
             this.adj = new ArrayList<ArrayList<Integer> >(numTiles*4);
             this.nodeCount = 0;
             this.hiddenIntBoard = hiddenIntBoard;


             int tileCount=0;//, nodeCount=0;
            //1. Build internal subgraphs by looping through tiles
             for (int i = 0; i < SaboteurBoardState.BOARD_SIZE; i++) {
                for (int j = 0; j < SaboteurBoardState.BOARD_SIZE; j++) {
                    if(hiddenBoard[i][j] == null){ //TODO: verify this doesn't break indexing.
                        tileCount++;
                        continue;
                    }
                    String tileIdx = hiddenBoard[i][j].getIdx();
                    addTile(tileIdx, tileCount);
                    tileCount++;
                }
            }

             //2. Build links between subgraphs
            for(int t : this.tileNeighbourhood.keySet()){ //for each tile
                connectTile(t);
            }
            printGraph(); //TODO: REMOVE
        }

    }

    private Graph boardGraph;

    private static double[][] positionDistances;

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
            System.out.println("NugPos IS: "+nugPos);
            target[0] = SaboteurBoardState.hiddenPos[nugPos][0];
            target[1] = SaboteurBoardState.hiddenPos[nugPos][1];
        }
        for(int x=0; x<SaboteurBoardState.BOARD_SIZE; x++){
            for(int y=0; y<SaboteurBoardState.BOARD_SIZE; y++) {
                distances[x][y] = 1.2*Math.abs(x-target[0]) + Math.abs(y-target[1]); //TODO tweek with distance calculation formula
            }
        }

        positionDistances = distances;
        return distances;
    }

    /**
     * Given a board, and a list of moves,
     * 1. instantiate a boardGraph,
     * 2. run through moves to decide which once maintains a path to origin
     */
    //private static selectOptimalMove() TODO


}