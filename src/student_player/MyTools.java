package student_player;

import Saboteur.SaboteurBoardState;
import Saboteur.cardClasses.SaboteurTile;
import java.util.*;  //TODO: REMOVE

public class MyTools {
    public static class Graph {
        private ArrayList<ArrayList<Integer> > adj;
        private ArrayList<Integer> srcNodeIndices;
        private ArrayList<Integer> targetNodeIndices;

        // A utility function to add an edge in an
        // undirected graph
        void addEdge(int u, int v){
            this.adj.get(u).add(v);
            this.adj.get(v).add(u);
        }

        void removeEdge(int u, int v){
            this.adj.get(u).remove(v);
            this.adj.get(v).remove(u);
        }

        void addTile(int targetTile ){ //TODO
            if(this.targetNodeIndices.size() > 0){
                removeTargetNodes();
            }
            this.targetNodeIndices = new ArrayList<Integer>(4);
        }

        void removeTargetNodes(){// TODO
            ArrayList<Integer> neighbours = new ArrayList<Integer>();
            //for(target : targetNodeIndices);
        }

        public boolean pathToOrigin(int targetTile){ //TODO
            boolean pathExists = false;
            return pathExists;
        }
//        public double analyse(SaboteurMove move){ TODO
//            int targetTile =
//        }

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
             this.srcNodeIndices = new ArrayList<Integer>(4);
             this.targetNodeIndices = new ArrayList<Integer>(4);

             //hashmap: Key = tile#; Value = int[4]{left,up,right,down} where each represents the adj index of the tile default = -1 for no tiel
             TreeMap<Integer, int[]> tileNeighbourhood = new TreeMap<Integer, int[]>();
             /*TODO:
                Pass #1 build internal subgraphs
                Pass #2 build links between subgraphs
              */

             boolean isOriginTile = false;
             int tileCount=0, nodeCount=0;
            //1. Build internal subgraphs by looping through tiles
             for (int i = 0; i < SaboteurBoardState.BOARD_SIZE; i++) {
                for (int j = 0; j < SaboteurBoardState.BOARD_SIZE; j++) {
                    if(hiddenBoard[i][j] == null){ //TODO: verify this doesn't break indexing.
                        tileCount++;
                        continue;
                    }
                    String tileIdx = hiddenBoard[i][j].getIdx();
                    System.out.println("\t Tile: "+tileIdx);
                    boolean isHiddenTile=false;
                    if( i == SaboteurBoardState.originPos && j == SaboteurBoardState.originPos){
                        isOriginTile = true;
                        System.out.println("origin");
                    }else if(tileIdx.equals("nugget") || tileIdx.equals("hidden1") || tileIdx.equals("hidden2")){
                        System.out.println("Hidden Tile: " + tileIdx);
                        isHiddenTile = true;
                    }
                    boolean isConnected = false;
                    int[] tileNodes = new int[]{-1,-1,-1,-1}; //left,up,right,down
                    //Loop through path of a given tile
                    for (int h = 0; h < 3; h++) {
                        for (int k = 0; k < 3; k++) {
                            if((hiddenIntBoard[i * 3 + h][j * 3 + k] == SaboteurBoardState.TUNNEL) || isHiddenTile){//NODE
                                System.out.println("\t \t H: "+h+"\t K: "+k);
                                if(h==1 && k==1){ //CENTER CENTER
                                    isConnected = true; //Only connected if center of tile is a tunnel
                                    continue;
                                }else if(h==0 && k==1){ //TOP CENTER
                                    tileNodes[1] = nodeCount;
                                }else if(h==1) { //CENTER LEFT
                                    tileNodes[k] = nodeCount;
                                }else if(h==2 && k==1) { //BOTTOM CENTER
                                    tileNodes[3] = nodeCount;
                                }
                                System.out.println("\t Node #"+nodeCount+" @ Tile #: "+(SaboteurBoardState.BOARD_SIZE*i + j));

                                this.adj.add(new ArrayList<Integer>(4));
                                if(isOriginTile) this.srcNodeIndices.add(nodeCount);
                                nodeCount++;
                            }
                        }
                    }
                    System.out.println("Tile #: "+(SaboteurBoardState.BOARD_SIZE*i + j)+" with nodes at " +Arrays.toString(tileNodes) );
                    if(isConnected){ //add strongly connected internal edges
                        for(int a=0; a<3; a++){
                            if(tileNodes[a] == -1) continue;
                            for(int b=a+1; b<4; b++){
                                if(tileNodes[b] == -1) continue;
                                addEdge(tileNodes[a], tileNodes[b]);
                            }
                        }
                    }
                    isOriginTile = false;
                    isHiddenTile = false;
                    tileNeighbourhood.put(tileCount,tileNodes);
                    tileCount++;
                }
            }

             //2. Build links between subgraphs
            for(int t : tileNeighbourhood.keySet()){ //for each tile
                int i = t/SaboteurBoardState.BOARD_SIZE; // gives tile x(row) position
                int j = t%SaboteurBoardState.BOARD_SIZE; // gives tile y(col) position
                int[] neighbourhood = tileNeighbourhood.get(t);
                System.out.println("Tile #"+t+" has Neighbourhood = #: "+Arrays.toString(neighbourhood));

                int[] neighbour; //left,up,right,down
                if(neighbourhood[2] != -1 && (j+1)%SaboteurBoardState.BOARD_SIZE > 0){ //exists position to the right and subgraph contains path to right
                    neighbour = tileNeighbourhood.get(t+1);
                    System.out.println("\t Right neighbour " + Arrays.toString(neighbour));
                    if(neighbour != null && neighbour[0] != -1 ){ //next tile is indeed to the right of current position and has path leftwards
                        System.out.println("\t \t (right) edges: "+neighbourhood[2]+", "+neighbour[0]);
                        addEdge(neighbourhood[2],neighbour[0]);
                    }
                }
                if(neighbourhood[3] != -1 && i+1 < SaboteurBoardState.BOARD_SIZE){//exists a tile and path to the bottom position that might be connected
                    neighbour = tileNeighbourhood.get(t+SaboteurBoardState.BOARD_SIZE);
                    System.out.println("\t Bottom neighbour " + Arrays.toString(neighbour));
                    if(neighbour != null && neighbour[1] != -1){ //tile beneath has been paved and contains path upwards
                        System.out.println("\t \t (below) Nodes: "+neighbourhood[3]+", "+neighbour[1]);
                        addEdge(neighbourhood[3],neighbour[1]);
                    }
                }
            }
            printGraph(); //TODO: REMOVE
        }

    }

    private Graph boardGraph;

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

    /**
     * Given a board, and a list of moves,
     * 1. instantiate a boardGraph,
     * 2. run through moves to decide which once maintains a path to origin
     */
    //private static selectOptimalMove() TODO


}