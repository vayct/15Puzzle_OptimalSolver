package mwong.myprojects.fifteenpuzzle.solver.standard;

import mwong.myprojects.fifteenpuzzle.solver.AbstractSmartSolver;
import mwong.myprojects.fifteenpuzzle.solver.HeuristicOptions;
import mwong.myprojects.fifteenpuzzle.solver.components.ApplicationMode;
import mwong.myprojects.fifteenpuzzle.solver.components.Board;
import mwong.myprojects.fifteenpuzzle.solver.components.Direction;
import mwong.myprojects.fifteenpuzzle.solver.components.WalkingDistance;

import java.util.HashMap;

/**
 * SolverWd extends AbstractSmartSolver with SmartSolver feature disabled.  It is the 15
 * puzzle optimal solver.  It takes a Board object of the puzzle and solve it with IDA* using
 * Walking Distance.
 *
 * <p>Dependencies : AbstractSolver.java, Board.java, Direction.java, HeuristicOptions.java,
 *                   WalkingDistance.java
 *
 * @author Meisze Wong
 *         www.linkedin.com/pub/macy-wong/46/550/37b/
 */
public class SolverWd extends AbstractSmartSolver {
    protected final boolean forward = true;
    protected final boolean backward = !forward;

    // Walking Distance Components
    protected static HashMap<Integer, Integer> wdRowKeys;
    protected static HashMap<Integer, Integer> wdPtnKeys;
    protected static byte[] wdPattern;
    protected static int[] wdPtnLink;

    protected byte[] tilesSym;
    protected byte wdValueH;
    protected byte wdValueV;
    protected int wdIdxH;
    protected int wdIdxV;
    protected int idaCount;
    protected int searchCountBase;

    /**
     * Initializes SolverWd object.
     */
    public SolverWd() {
        this(ApplicationMode.CONSOLE);
    }

    /**
     * Initializes SolverWd object.
     *
     * @param appMode the given applicationMode for GUI or CONSOLE
     */
    public SolverWd(ApplicationMode appMode) {
        super(appMode);
        inUseHeuristic = HeuristicOptions.WD;
        this.appMode = appMode;
        loadWdComponents();
    }

    // load the walking distance components from the data file
    // if data file not exists, generate a new set
    private void loadWdComponents() {
        WalkingDistance wd = new WalkingDistance(appMode);
        wdRowKeys = wd.getRowKeys();
        wdPtnKeys = wd.getPtnKeys();
        wdPattern = wd.getPattern();
        wdPtnLink = wd.getPtnLink();
    }

    /**
     * Returns the heuristic value of the given board.
     *
     * @param board the initial puzzle Board object to solve
     * @return byte value of the heuristic value of the given board
     */
    @Override
    public byte heuristic(Board board) {
        if (board == null) {
            throw new IllegalArgumentException("Board is null");
        }
        if (!board.isSolvable()) {
            return -1;
        }

        if (!board.equals(lastBoard)) {
            initialize(board);
            tilesSym = board.getTilesSym();

            byte[] ctwdh = new byte[puzzleSize];
            byte[] ctwdv = new byte[puzzleSize];

            for (int i = 0; i < 16; i++) {
                int value = tiles[i];
                if (value != 0) {
                    int col = (value - 1) / rowSize;
                    ctwdh[(i / rowSize) * rowSize + col]++;

                    col = value % rowSize - 1;
                    if (col < 0) {
                        col = rowSize - 1;
                    }
                    ctwdv[(i % rowSize) * rowSize + col]++;
                }
            }

            wdIdxH = getWDPtnIdx(ctwdh, zeroY);
            wdIdxV = getWDPtnIdx(ctwdv, zeroX);
            wdValueH = getWDValue(wdIdxH);
            wdValueV = getWDValue(wdIdxV);

            priorityGoal = (byte) (wdValueH + wdValueV);
        }
        return priorityGoal;
    }

    // solve the puzzle using interactive deepening A* algorithm
    protected void idaStar(int limit) {
        searchCountBase = 0;
        while (limit <= maxMoves) {
            idaCount = 0;
            if (flagMessage) {
                System.out.print("ida limit " + limit);
            }
            dfsStartingOrder(zeroX, zeroY, limit, wdIdxH, wdIdxV, wdValueH, wdValueV);
            searchCountBase += idaCount;
            searchNodeCount = searchCountBase;

            if (timeout) {
                if (flagMessage) {
                    System.out.printf("\tNodes : %-15s timeout\n", Integer.toString(idaCount));
                }
                return;
            } else {
                if (flagMessage) {
                    System.out.printf("\tNodes : %-15s " + stopwatch.currentTime() + "s\n",
                            Integer.toString(idaCount));
                }
                if (solved) {
                    return;
                }
            }
            limit += 2;
        }
    }

    // overload idaStar to solve the puzzle with the given max limit for advancedEstimate
    protected void idaStar(int limit, int maxLimit) {
        while (limit <= maxLimit) {
            dfsStartingOrder(zeroX, zeroY, limit, wdIdxH, wdIdxV, wdValueH, wdValueV);
            if (solved) {
                return;
            }
            limit += 2;
        }
    }

    // recursive depth first search until it reach the goal state or timeout, the least estimate and
    // node counts will be use to determine the starting order of next search
    protected void dfsStartingOrder(int orgX, int orgY, int limit, int idxH, int idxV,
            int valH, int valV) {
        searchDepth = limit;
        int zeroPos = orgY * rowSize + orgX;
        int zeroSym = symmetryPos[zeroPos];
        int[] estimate1stMove = new int[rowSize * 2];
        System.arraycopy(lastDepthSummary, 0, estimate1stMove, 0, rowSize * 2);

        int estimate = limit;
        while (!terminated && estimate != endOfSearch) {
            int firstMoveIdx = -1;
            int nodeCount = Integer.MAX_VALUE;

            estimate = endOfSearch;
            for (int i = 0; i < 4; i++) {
                if (estimate1stMove[i] == endOfSearch) {
                    continue;
                } else if (lastDepthSummary[i] < estimate) {
                    estimate = estimate1stMove[i];
                    nodeCount = lastDepthSummary[i + 4];
                    firstMoveIdx = i;
                } else if (lastDepthSummary[i] == estimate && lastDepthSummary[i + 4] < nodeCount) {
                    nodeCount = lastDepthSummary[i + 4];
                    firstMoveIdx = i;
                }
            }

            if (estimate < endOfSearch) {
                int startCounter = idaCount++;

                switch (Direction.values()[firstMoveIdx]) {
                    case RIGHT:
                        lastDepthSummary[firstMoveIdx] = shiftRight(orgX, orgY, zeroPos, zeroSym,
                                1, limit, idxH, idxV, valH, valV, resetKey);
                        break;
                    case DOWN:
                        lastDepthSummary[firstMoveIdx] = shiftDown(orgX, orgY, zeroPos, zeroSym,
                                1, limit, idxH, idxV, valH, valV, resetKey);
                        break;
                    case LEFT:
                        lastDepthSummary[firstMoveIdx] = shiftLeft(orgX, orgY, zeroPos, zeroSym,
                                1, limit, idxH, idxV, valH, valV, resetKey);
                        break;
                    case UP:
                        lastDepthSummary[firstMoveIdx] = shiftUp(orgX, orgY, zeroPos, zeroSym,
                                1, limit, idxH, idxV, valH, valV, resetKey);
                        break;
                    default:
                        assert false : "Error: starting order switch statement";
                }

                lastDepthSummary[firstMoveIdx + rowSize] = idaCount - startCounter;
                estimate1stMove[firstMoveIdx] = endOfSearch;
            }
        }
    }

    // recursive depth first search until it reach the goal state or timeout
    private int recursiveDFS(int orgX, int orgY, int cost, int limit, int idxH, int idxV,
            int valH, int valV, int swirlKey) {
        idaCount++;
        if (terminated) {
            return endOfSearch;
        }
        if (flagTimeout && stopwatch.currentTime() > searchTimeoutLimit) {
            stopwatch.stop();
            timeout = true;
            terminated = true;
            return endOfSearch;
        }
        //assert stopwatch.isActive() : "stopwatch is not running.";

        int zeroPos = orgY * rowSize + orgX;
        int zeroSym = symmetryPos[zeroPos];
        int costPlus1 = cost + 1;
        int newEstimate = valH + valV;

        boolean nonIdentical = true;
        if (zeroPos == zeroSym) {
            nonIdentical = false;
            for (int i = puzzleSize - 1; i > -1; i--) {
                if (tiles[i] != tilesSym[i]) {
                    nonIdentical = true;
                    break;
                }
            }
        }

        Direction prevMove = solutionMove[cost];
        // hard code order of next moves base on the current move
        switch (prevMove) {
            case RIGHT:
                // RIGHT
                if (orgX < rowSize - 1) {
                    newEstimate = Math.min(newEstimate, shiftRight(orgX, orgY, zeroPos, zeroSym,
                            costPlus1, limit, idxH, idxV, valH, valV, resetKey));
                }
                if (nonIdentical) {
                    // UP
                    if (orgY > 0 && isValidCounterClockwise(swirlKey)) {
                        newEstimate = Math.min(newEstimate, shiftUp(orgX, orgY, zeroPos, zeroSym,
                                costPlus1, limit, idxH, idxV, valH, valV, swirlKey << 2 | ccwKey));
                    }
                    // DOWN
                    if (orgY < rowSize - 1 && isValidClockwise(swirlKey)) {
                        newEstimate = Math.min(newEstimate, shiftDown(orgX, orgY, zeroPos, zeroSym,
                                costPlus1, limit, idxH, idxV, valH, valV, swirlKey << 2 | cwKey));
                    }
                }
                break;
            case DOWN:
                // DOWN
                if (orgY < rowSize - 1) {
                    newEstimate = Math.min(newEstimate, shiftDown(orgX, orgY, zeroPos, zeroSym,
                            costPlus1, limit, idxH, idxV, valH, valV, resetKey));
                }
                if (nonIdentical) {
                    // LEFT
                    if (orgX > 0 && isValidClockwise(swirlKey)) {
                        newEstimate = Math.min(newEstimate, shiftLeft(orgX, orgY, zeroPos, zeroSym,
                                costPlus1, limit, idxH, idxV, valH, valV, swirlKey << 2 | cwKey));
                    }
                    // RIGHT
                    if (orgX < rowSize - 1 && isValidCounterClockwise(swirlKey)) {
                        newEstimate = Math.min(newEstimate, shiftRight(orgX, orgY, zeroPos, zeroSym,
                                costPlus1, limit, idxH, idxV, valH, valV, swirlKey << 2 | ccwKey));
                    }
                }
                break;
            case LEFT:
                // LEFT
                if (orgX > 0) {
                    newEstimate = Math.min(newEstimate, shiftLeft(orgX, orgY, zeroPos, zeroSym,
                            costPlus1, limit, idxH, idxV, valH, valV, resetKey));
                }
                if (nonIdentical) {
                    // DOWN
                    if (orgY < rowSize - 1 && isValidCounterClockwise(swirlKey)) {
                        newEstimate = Math.min(newEstimate, shiftDown(orgX, orgY, zeroPos, zeroSym,
                                costPlus1, limit, idxH, idxV, valH, valV, swirlKey << 2 | ccwKey));
                    }
                    // UP
                    if (orgY > 0 && isValidClockwise(swirlKey)) {
                        newEstimate = Math.min(newEstimate, shiftUp(orgX, orgY, zeroPos, zeroSym,
                                costPlus1, limit, idxH, idxV, valH, valV, swirlKey << 2 | cwKey));
                    }
                }
                break;
            case UP:
                // UP
                if (orgY > 0) {
                    newEstimate = Math.min(newEstimate, shiftUp(orgX, orgY, zeroPos, zeroSym,
                            costPlus1, limit, idxH, idxV, valH, valV, resetKey));
                }
                if (nonIdentical) {
                    // RIGHT
                    if (orgX < rowSize - 1 && isValidClockwise(swirlKey)) {
                        newEstimate = Math.min(newEstimate, shiftRight(orgX, orgY, zeroPos, zeroSym,
                                costPlus1, limit, idxH, idxV, valH, valV, swirlKey << 2 | cwKey));
                    }
                    // LEFT
                    if (orgX > 0 && isValidCounterClockwise(swirlKey)) {
                        newEstimate = Math.min(newEstimate, shiftLeft(orgX, orgY, zeroPos, zeroSym,
                                costPlus1, limit, idxH, idxV, valH, valV, swirlKey << 2 | ccwKey));
                    }
                }
                break;
            default:
                assert false : "Error: recursive DFS switch statement";
        }
        return newEstimate;
    }

    // shift the space to right
    private int shiftRight(int orgX, int orgY, int zeroPos, int zeroSym, int costPlus1, int limit,
            int idxH, int idxV, int valH, int valV, int swirlKey) {
        if (terminated) {
            return endOfSearch;
        }
        searchNodeCount = searchCountBase + idaCount;
        searchTime = stopwatch.currentTime();
        int nextPos = zeroPos + 1;
        byte value = tiles[nextPos];
        int newIdx = getWDPtnIdx(idxV, (value - 1) % rowSize, forward);
        int newValue = getWDValue(newIdx);
        int priority = valH + newValue;
        solutionMove[costPlus1] = Direction.RIGHT;
        return nextMove(orgX + 1, orgY, zeroPos, zeroSym, priority, costPlus1,
                limit, nextPos, zeroSym + rowSize, idxH, newIdx, valH, newValue, swirlKey);
    }

    // shift the space to down
    private int shiftDown(int orgX, int orgY, int zeroPos, int zeroSym, int costPlus1, int limit,
            int idxH, int idxV, int valH, int valV, int swirlKey) {
        if (terminated) {
            return endOfSearch;
        }
        int nextPos = zeroPos + rowSize;
        byte value = tiles[nextPos];
        int newIdx = getWDPtnIdx(idxH, (value - 1) / rowSize, forward);
        int newValue = getWDValue(newIdx);
        int priority = valV + newValue;
        solutionMove[costPlus1] = Direction.DOWN;
        return nextMove(orgX, orgY + 1, zeroPos, zeroSym, priority, costPlus1,
                limit, nextPos, zeroSym + 1, newIdx, idxV, newValue, valV, swirlKey);
    }

    // shift the space to left
    private int shiftLeft(int orgX, int orgY, int zeroPos, int zeroSym, int costPlus1, int limit,
            int idxH, int idxV, int valH, int valV, int swirlKey) {
        if (terminated) {
            return endOfSearch;
        }
        int nextPos = zeroPos - 1;
        byte value = tiles[nextPos];
        int newIdx = getWDPtnIdx(idxV, (value - 1) % rowSize, backward);
        int newValue = getWDValue(newIdx);
        int priority = valH + newValue;
        solutionMove[costPlus1] = Direction.LEFT;
        return nextMove(orgX - 1, orgY, zeroPos, zeroSym, priority, costPlus1,
                limit, nextPos, zeroSym - rowSize, idxH, newIdx, valH, newValue, swirlKey);
    }

    // shift the space to up
    private int shiftUp(int orgX, int orgY, int zeroPos, int zeroSym, int costPlus1, int limit,
            int idxH, int idxV, int valH, int valV, int swirlKey) {
        if (terminated) {
            return endOfSearch;
        }
        int nextPos = zeroPos - rowSize;
        byte value = tiles[nextPos];
        int newIdx = getWDPtnIdx(idxH, (value - 1) / rowSize, backward);
        int newValue = getWDValue(newIdx);
        int priority = valV + newValue;
        solutionMove[costPlus1] = Direction.UP;
        return nextMove(orgX, orgY - 1, zeroPos, zeroSym, priority, costPlus1,
                limit, nextPos, zeroSym - 1, newIdx, idxV, newValue, valV, swirlKey);
    }

    // continue to next move if not reach goal state or over limit
    private int nextMove(int orgX, int orgY, int zeroPos, int zeroSym, int priority, int cost,
            int limit, int nextPos, int nextSym, int idxH, int idxV, int valH, int valV,
            int swirlKey) {
        int updatePrio = priority;
        if (priority == 0) {
            stopwatch.stop();
            steps = (byte) cost;
            solved = true;
            terminated = true;
            updatePrio = endOfSearch;
        } else if (priority < limit) {
            tiles[zeroPos] = tiles[nextPos];
            tiles[nextPos] = 0;
            tilesSym[zeroSym] = tilesSym[nextSym];
            tilesSym[nextSym] = 0;
            updatePrio = Math.min(updatePrio,
                    recursiveDFS(orgX, orgY, cost, limit - 1, idxH, idxV, valH, valV, swirlKey));
            tiles[nextPos] = tiles[zeroPos];
            tiles[zeroPos] = 0;
            tilesSym[nextSym] = tilesSym[zeroSym];
            tilesSym[zeroSym] = 0;
        }
        return updatePrio;
    }

    // take a set of walking distance values and row index of zero position,
    // compress into 32 bit key, and return the key index
    protected int getWDPtnIdx(byte[] ctwd, int zeroRow) {
        int key = 0;
        int count = 0;

        while (count < ctwd.length) {
            int temp = 0;
            for (int i = 0; i < rowSize; i++) {
                temp = (temp << 3) | ctwd[count++];
            }
            key = (key << 6) | wdRowKeys.get(temp);
            assert (wdRowKeys.get(temp) != -1) : " Invalid index : -1";
        }
        key = (key << 4) | zeroRow;
        return wdPtnKeys.get(key);
    }

    // take a key index, the column index of move and direction
    // return the key index after the move
    protected int getWDPtnIdx(int idx, int col, boolean isForward) {
        if (isForward) {
            return wdPtnLink[idx * rowSize * 2 + col * 2];
        } else {
            return wdPtnLink[idx * rowSize * 2 + col * 2 + 1];
        }
    }

    // take a key index and return the value of walking distance
    protected byte getWDValue(int idx) {
        return wdPattern[idx];
    }

    // return horizontal walking distance value
    protected final byte getWdValueH() {
        return wdValueH;
    }

    // return vertical walking distance value
    protected final byte getWdValueV() {
        return wdValueV;
    }

    // return horizontal walking distance index
    protected final int getWdIdxH() {
        return wdIdxH;
    }

    // return vertical walking distance index
    protected final int getWdIdxV() {
        return wdIdxV;
    }
}
