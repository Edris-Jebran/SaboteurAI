package student_player;

import Saboteur.SaboteurBoardState;
import Saboteur.SaboteurMove;
import Saboteur.cardClasses.SaboteurTile;
import java.util.*;

public class MyTools {
    public static class Graph {
        private ArrayList<ArrayList<Integer> > adj; //Adjacency List
        private ArrayList<Integer> goalTileNums;

        private int srcTileNum;
        private int nodeCount;

        private int[][] hiddenIntBoard; //local copy of hiddenIntBoard to be manipulated during move analysis
        //hashmap: Key = tile#; Value = int[4]{left,up,right,down} where each represents the adj index of the tile default = -1 for no tile
        private TreeMap<Integer, int[]> tileNeighbourhood;

        private void addEdge(int u, int v){
            if(!this.adj.get(u).contains(v)) this.adj.get(u).add(v);
            if(!this.adj.get(v).contains(u)) this.adj.get(v).add(u);
        }

        /**
         * Temporarily add tile to graph, Can only have 1 overlaid targetNode
         * Thus remove any previously addedTiles
         * @param tileIdx
         * @param tileNumber
         * Updates tileNeighbourhood: tileNumber, [left,up,right,down] node index
         */
        private void addTile(String tileIdx, int tileNumber ){
            int i = tileNumber/SaboteurBoardState.BOARD_SIZE; // gives tile x(row) position
            int j = tileNumber%SaboteurBoardState.BOARD_SIZE; // gives tile y(col) position
            int[] tileNodes = new int[]{-1,-1,-1,-1}; //left,up,right,down

            boolean isHiddenTile=false;
            if(tileIdx.equals("nugget") || tileIdx.equals("hidden1") || tileIdx.equals("hidden2")){
                isHiddenTile = true;
            }

            boolean isConnected = false;
            //Loop through path of a given tile
            for (int h = 0; h < 3; h++) {
                for (int k = 0; k < 3; k++) {
                    if((this.hiddenIntBoard[i * 3 + h][j * 3 + k] == SaboteurBoardState.TUNNEL) || isHiddenTile){//NODE
                        if(h==1 && k==1){ //CENTER CENTER
                            isConnected = true; //Only connected if center of tile is a tunnel
                            continue;
                        }else if(h==0 && k==1){ //TOP CENTER
                            tileNodes[1] = this.nodeCount;
                        }else if(h==1) { //CENTER LEFT
                            tileNodes[k] = this.nodeCount;
                        }else if(h==2 && k==1) { //BOTTOM CENTER
                            tileNodes[3] = this.nodeCount;
                        }else{ //Due to No diagonal connections ignore
                            continue;
                        }

                        this.adj.add(new ArrayList<Integer>(4));
                        this.nodeCount++;
                    }
                }
            }

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

        /**
         * Connects a tiles sub-graph (internal nodes) to nodes of neighbouring tiles
         * Note: Since tiles are connected (doubly) in reading order, only checks right and below
         *       as left and above would have previously been connected
         * @param tileNumber
         */
        private void connectTile(int tileNumber){
            int i = tileNumber/SaboteurBoardState.BOARD_SIZE; // gives tile x(row) position
            int j = tileNumber%SaboteurBoardState.BOARD_SIZE; // gives tile y(col) position

            int[] neighbourhood = this.tileNeighbourhood.get(tileNumber);
            int[] neighbour; //left,up,right,down
            //exists position to the right and subgraph contains path to right
            if(neighbourhood[2] != -1 && (j+1)%SaboteurBoardState.BOARD_SIZE > 0){
                neighbour = this.tileNeighbourhood.get(tileNumber+1);
                 //next tile is indeed to the right of current position and has path leftwards
                if(neighbour != null && neighbour[0] != -1 ){
                    addEdge(neighbourhood[2],neighbour[0]);
                }
            }
            //exists a tile and path to the bottom position that might be connected
            if(neighbourhood[3] != -1 && i+1 < SaboteurBoardState.BOARD_SIZE){
                neighbour = this.tileNeighbourhood.get(tileNumber+SaboteurBoardState.BOARD_SIZE);
                //tile beneath has been paved and contains path upwards
                if(neighbour != null && neighbour[1] != -1){
                    addEdge(neighbourhood[3],neighbour[1]);
                }
            }
        }

        /**
         * Used for cleaning up board across different move simulations
         * @param targetTileNum
         */
        private void removeTargetTile(int targetTileNum){
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
         * Utilizes modified DFS
         * @param targetTile
         * @return true if a given tile has >= 1 vertex with a path to any one of the origins vertices
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
         * When analysing a move make sure resultant neighbours are not dead-ends
         * @param neighbourTileNum
         */
        private boolean isValidNeighbour(int neighbourTileNum){
            if(!this.tileNeighbourhood.containsKey(neighbourTileNum)){
                return true;
            } else{
                int[] neighbourNodes = this.tileNeighbourhood.get(neighbourTileNum);
                int numNodes = 0;
                for(int n : neighbourNodes){
                    if(n > -1) numNodes++;
                }
                if(numNodes > 2) return true; //=> connected
            }
            return false;
        }

        /**
         * @param move provided by StudentPlayer
         * @return minimum cost of a proposed move's resultant neighbours, -1 for invalid move
         */
        public double analyse(SaboteurMove move){
            SaboteurTile card = (SaboteurTile) move.getCardPlayed();
            String tileIdx = card.getIdx();
            int[] pos = move.getPosPlayed();
            int targetTileNum = SaboteurBoardState.BOARD_SIZE*pos[0]+pos[1];

            double minNeighbourCost = -1; //Default: invalid

            int x = targetTileNum/SaboteurBoardState.BOARD_SIZE; // gives tile x(row) position
            int y = targetTileNum%SaboteurBoardState.BOARD_SIZE; // gives tile y(col) position

            //1. Add tile to graph + Connect tile
            for (int k = 0; k < 3; k++) { //Temporarily update hiddenIntBoard
                for (int h = 0; h < 3; h++) {
                    this.hiddenIntBoard[pos[0] * 3 + k][pos[1] * 3 + h] = card.getPath()[h][2-k];
                }
            }

            addTile(tileIdx,targetTileNum);
            //Note: due to connectTile ordering and adding this tile after graph is built must (conditionally) first connect left and above tiles.
            if(this.tileNeighbourhood.containsKey(targetTileNum-1)) connectTile(targetTileNum-1); //l
            if(this.tileNeighbourhood.containsKey(targetTileNum-SaboteurBoardState.BOARD_SIZE)) connectTile(targetTileNum-SaboteurBoardState.BOARD_SIZE);//u
            connectTile(targetTileNum);

            //2. Verify at least 1 of the move tile nodes in the graph are reachable from one of the source nodes (i.e. valid path)
            if(!pathToOrigin(targetTileNum)){
                return minNeighbourCost; //-1
            }

            //3. look at nodes within tile and determine minimum weight of neighbouring positions according to positionDistances
            int[] targetNodes = this.tileNeighbourhood.get(targetTileNum);//l,u,r,p
            double score = -1;
            for(int i=0; i<targetNodes.length; i++){
                int n = targetNodes[i];
                if(n == -1) continue;
                try {
                    int BS = SaboteurBoardState.BOARD_SIZE;
                    //if it doesn't have a neighbour in that direction or if that neighbour is connected
                    if (i == 0 && isValidNeighbour(targetTileNum-1)) { //l
                        score = positionDistances[x][y - 1];
                    } else if (i == 1 && isValidNeighbour(targetTileNum - BS)) {//u
                        score = positionDistances[x-1][y];
                    } else if (i == 2 && isValidNeighbour(targetTileNum + 1)) {//r
                        score = positionDistances[x][y + 1];
                    } else if (i == 3 && isValidNeighbour(targetTileNum + BS)) {//d
                        score = positionDistances[x+1][y];
                    } else{
                        continue;
                    }
                    if(minNeighbourCost==-1 || score<minNeighbourCost){
                        minNeighbourCost=score;
                    }
                } catch(ArrayIndexOutOfBoundsException ignored){
                }
            }

            //4. Remove tile + undo temporary changes to hiddenIntBoard
            for (int k = 0; k < 3; k++) {
                for (int h = 0; h < 3; h++) {
                    this.hiddenIntBoard[pos[0] * 3 + k][pos[1] * 3 + h] = SaboteurBoardState.EMPTY;
                }
            }
            removeTargetTile(targetTileNum);

            return minNeighbourCost;
        }

        public Graph(int numTiles, int[][] hiddenIntBoard, SaboteurTile[][] hiddenBoard){
            this.nodeCount = 0;
            this.hiddenIntBoard = hiddenIntBoard;
            this.tileNeighbourhood = new TreeMap<Integer, int[]>();
            this.adj = new ArrayList<ArrayList<Integer> >(numTiles*4);
            this.srcTileNum = SaboteurBoardState.BOARD_SIZE*SaboteurBoardState.originPos+SaboteurBoardState.originPos;

            this.goalTileNums = new ArrayList<>(3);
            for(int[] pos : SaboteurBoardState.hiddenPos){
                goalTileNums.add(SaboteurBoardState.BOARD_SIZE*pos[0]+pos[1]);
            }

             int tileCount=0;
            //1. Build internal subgraphs by looping through tiles
             for (int i = 0; i < SaboteurBoardState.BOARD_SIZE; i++) {
                for (int j = 0; j < SaboteurBoardState.BOARD_SIZE; j++) {
                    if(hiddenBoard[i][j] == null){
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
        }

    }

    private static double[][] positionDistances;

    /**
     * Calculates distances of positions on board relative to assumed goal
     * @param nugPos assumed or known nugget location
     * @param hiddenRevealed list of already revealed hidden tiles
     * @return distances to StudentPlayer and updates internal MyTools field positionDistances for Graph use
     */
	public static double[][] getPositionDistances(int nugPos, boolean[] hiddenRevealed){
        double[][] distances = new double[SaboteurBoardState.BOARD_SIZE][SaboteurBoardState.BOARD_SIZE];
        int[] target = new int[2];
        if(nugPos > -1){
            target[0] = SaboteurBoardState.hiddenPos[nugPos][0];
            target[1] = SaboteurBoardState.hiddenPos[nugPos][1];
        } else if(!hiddenRevealed[1]){ //Default target center hidden tile
           target[0] = SaboteurBoardState.hiddenPos[1][0];
           target[1] = SaboteurBoardState.hiddenPos[1][1];
        }else{
            for(int i=0; i<hiddenRevealed.length; i++){
                if(hiddenRevealed[i]){ // assume nugget at first undiscovered hidden objective.
                    target[0] = SaboteurBoardState.hiddenPos[i][0];
                    target[1] = SaboteurBoardState.hiddenPos[i][1];
                    break;
                }
            }
        }

        /* Distances calculated based of Manhattan Distances, with preference for minimizing row
            Magic # 1.2 came from trial/error; Logic is that since non-nugget tiles are closely congregated 8-shape
            It is more advantageous to get onto the correct row and utilize those non-nugget tiles.
         */
        for(int x=0; x<SaboteurBoardState.BOARD_SIZE; x++){
            for(int y=0; y<SaboteurBoardState.BOARD_SIZE; y++) {
                distances[x][y] = 1.2*Math.abs(x-target[0]) + Math.abs(y-target[1]);
            }
        }
        positionDistances = distances;
        return distances;
    }

}