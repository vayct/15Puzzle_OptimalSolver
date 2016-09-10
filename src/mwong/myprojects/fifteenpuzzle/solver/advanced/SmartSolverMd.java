package mwong.myprojects.fifteenpuzzle.solver.advanced;

import mwong.myprojects.fifteenpuzzle.solver.SmartSolverExtra;
import mwong.myprojects.fifteenpuzzle.solver.SolverConstants;
import mwong.myprojects.fifteenpuzzle.solver.ai.ReferenceRemote;
import mwong.myprojects.fifteenpuzzle.solver.components.Board;
import mwong.myprojects.fifteenpuzzle.solver.components.Direction;
import mwong.myprojects.fifteenpuzzle.solver.standard.SolverMd;

import java.rmi.RemoteException;

/**
 * SmartSolverMd extends SolverMd.  The advanced version extend the standard solver
 * using the reference boards collection to boost the initial estimate.
 *
 * <p>Dependencies : Board.java, Direction.java, ReferenceRemote.java,
 *                   SmartSolverExtra.java, SolverConstants.java, SolverMd.java
 *
 * @author Meisze Wong
 *         www.linkedin.com/pub/macy-wong/46/550/37b/
 */
public class SmartSolverMd extends SolverMd {
    /**
     * Initializes SmartSolverMd object.
     *
     * @param refConnection the given ReferenceRemote connection object
     */
    public SmartSolverMd(ReferenceRemote refConnection) {
        this(!SolverConstants.isTagLinearConflict(), refConnection);
    }

    /**
     * Initializes SmartSolverMd object.  If refConnection is null or empty,
     * it will act as standard version.
     *
     * @param lcFlag boolean flag for linear conflict feature
     * @param refConnection the given ReferenceRemote connection object
     */
    public SmartSolverMd(boolean lcFlag, ReferenceRemote refConnection) {
        super(lcFlag);
        try {
            if (refConnection == null || refConnection.getActiveMap() == null) {
                System.out.println("Attention: Refereence board collection unavailable."
                        + " Advanced estimate will use standard estimate.");
            } else {
                activeSmartSolver = true;
                extra = new SmartSolverExtra();
                this.refConnection = refConnection;
            }
        } catch (RemoteException ex) {
            System.out.println("Attention: Server connection failed."
                    + " Advanced estimate will use standard estimate.");
        }
    }

    /**
     * Returns the heuristic value of the given board based on the solver setting.
     *
     * @param board the initial puzzle Board object to solve
     * @return byte value of the heuristic value of the given board
     */
    @Override
    public byte heuristic(Board board) {
        return heuristic(board, flagAdvancedVersion, tagSearch);
    }

    // overload method to calculate the heuristic value of the given board and conditions
    private byte heuristic(Board board, boolean isAdvanced, boolean isSearch) {
        if (!board.isSolvable()) {
            return -1;
        }

        if (!board.equals(lastBoard)) {
            priorityGoal = super.heuristic(board);
        } else if (isSearch) {
            zeroX = board.getZeroX();
            zeroY = board.getZeroY();
            tiles = board.getTiles();
            tilesSym = board.getTilesSym();
            setLastDepthSummary(board);
        }

        if (!isAdvanced) {
            return priorityGoal;
        } else if (!isSearch && priorityAdvanced != -1) {
            return priorityAdvanced;
        }

        try {
            setPriorityAdvanced(board, isSearch);
        } catch (RemoteException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
        return priorityAdvanced;
    }

    /**
     * Returns the original heuristic value of the given board.
     *
     * @return byte value of the original heuristic value of the given board
     */
    @Override
    public byte heuristicStandard(Board board) {
        if (board == null) {
            throw new IllegalArgumentException("Board is null");
        }

        if (!board.isSolvable()) {
            return -1;
        }
        return heuristic(board, tagStandard, tagReview);
    }

    /**
     * Returns the advanced heuristic value of the given board.
     *
     * @return byte value of the advanced heuristic value of the given board
     */
    @Override
    public byte heuristicAdvanced(Board board) {
        if (board == null) {
            throw new IllegalArgumentException("Board is null");
        }

        if (!board.isSolvable()) {
            return -1;
        }

        if (!activeSmartSolver) {
            heuristic(board, tagStandard, tagReview);
        }
        return heuristic(board, tagAdvanced, tagReview);
    }

    // solve the puzzle using interactive deepening A* algorithm
    @Override
    protected void idaStar(int limit) {
        if (solutionMove[1] != null) {
            advancedSearch(limit);
            return;
        }

        int countDir = 0;
        for (int i = 0; i < rowSize; i++) {
            if (lastDepthSummary[i + rowSize] > 0) {
                countDir++;
            }
        }

        // quick scan for advanced priority, determine the start order for optimization
        if (flagAdvancedVersion && countDir > 1) {
            int initLimit = priorityGoal;
            while (initLimit < limit) {
                idaCount = 0;
                dfsStartingOrder(zeroX, zeroY, initLimit, priorityGoal);
                initLimit += 2;

                boolean overload = false;
                for (int i = rowSize; i < rowSize * 2; i++) {
                    if (lastDepthSummary[i] > 10000) {
                        overload = true;
                        break;
                    }
                }
                if (overload) {
                    break;
                }
            }
        }
        super.idaStar(limit);
    }

    // skip the first 8 moves from stored record then solve the remaining puzzle
    // using depth first search with exact number of steps of optimal solution
    private void advancedSearch(int limit) {
        Direction[] dupSolution = new Direction[limit + 1];
        Board board = prepareAdvancedSearch(limit, dupSolution);
        heuristic(board, tagStandard, tagSearch);
        setLastDepthSummary(dupSolution[numPartialMoves]);

        idaCount = numPartialMoves;
        if (flagMessage) {
            System.out.print("ida limit " + limit);
        }
        dfsStartingOrder(zeroX, zeroY, limit - numPartialMoves + 1, priorityGoal);
        searchNodeCount = idaCount;
        afterAdvancedSearch(limit, dupSolution);
    }
}
