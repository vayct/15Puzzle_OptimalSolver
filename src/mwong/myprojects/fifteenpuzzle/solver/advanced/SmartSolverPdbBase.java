/****************************************************************************
 *  @author   Meisze Wong
 *            www.linkedin.com/pub/macy-wong/46/550/37b/
 *
 *  Compilation  : javac SolverPD.java
 *  Dependencies : Board.java, Direction.java, Stopwatch.java,
 *                 PDElement.java, PDCombo.java, PDPresetPatterns.java,
 *                 SolverAbstract, AdvancedAccumulator.java
 *                 AdvancedBoard.java, AdvancedMoves.java
 *
 *  SolverPD implements SolverInterface.  It take a Board object and solve the
 *  puzzle with IDA* using additive pattern database.
 *
 ****************************************************************************/

package mwong.myprojects.fifteenpuzzle.solver.advanced;

import mwong.myprojects.fifteenpuzzle.solver.HeuristicOptions;
import mwong.myprojects.fifteenpuzzle.solver.SolverConstants;
import mwong.myprojects.fifteenpuzzle.solver.SolverProperties;
import mwong.myprojects.fifteenpuzzle.solver.advanced.ai.ReferenceAccumulator;
import mwong.myprojects.fifteenpuzzle.solver.components.Board;
import mwong.myprojects.fifteenpuzzle.solver.components.Direction;
import mwong.myprojects.fifteenpuzzle.solver.components.PatternOptions;
import mwong.myprojects.fifteenpuzzle.solver.standard.SolverPdb;
import mwong.myprojects.fifteenpuzzle.solver.standard.SolverPdbBase;
import mwong.myprojects.fifteenpuzzle.utilities.Stopwatch;

/**
 * SmartSolverPD extends SolverPD.  The advanced version extend the standard solver
 * using the reference boards collection to boost the initial estimate.
 *
 * <p>Dependencies : AdvancedRecord.java, Board.java, Direction.java, HeuristicOptions.java,
 *                   PatternOptions.java, ReferenceAccumulator.java, SmartSolverConstants.java,
 *                   SmartSolverExtra.java, SolverPD.java, SolverProperties.java, Stopwatch.java
 *
 * @author   Meisze Wong
 *           www.linkedin.com/pub/macy-wong/46/550/37b/
 */
public class SmartSolverPdbBase extends SolverPdb {
    protected final byte numPartialMoves;
    protected final byte refCutoff;
    protected final ReferenceAccumulator refAccumulator;
    protected final SmartSolverExtra extra;
    protected Board lastSearchBoard;

    /**
     * Initializes SolverPD object using default preset pattern.
     *
     * @param refAccumulator the given ReferenceAccumulator object
     */
    public SmartSolverPdbBase(ReferenceAccumulator refAccumulator) {
        this(SolverProperties.getDefaultPattern(), refAccumulator);
    }

    /**
     * Initializes SolverPD object using given preset pattern.
     *
     * @param presetPattern the given preset pattern type
     * @param refAccumulator the given ReferenceAccumulator object
     */
    public SmartSolverPdbBase(PatternOptions presetPattern, ReferenceAccumulator refAccumulator) {
        this(presetPattern, 0, refAccumulator);
    }

    /**
     * Initializes SolverPD object with choice of given preset pattern.  If refAccumlator is null
     * or empty, it will act as standard version.
     *
     * @param presetPattern the given preset pattern type
     * @param choice the number of preset pattern option
     * @param refAccumulator the given ReferenceAccumulator object
     */
    public SmartSolverPdbBase(PatternOptions presetPattern, int choice,
            ReferenceAccumulator refAccumulator) {
        super(presetPattern, choice);
        if (refAccumulator == null || refAccumulator.getActiveMap() == null) {
            System.out.println("Referece board collection unavailable."
                    + " Resume to the 15 puzzle solver standard version.");
            extra = null;
            this.refAccumulator = null;
            refCutoff = 0;
            numPartialMoves = 0;
        } else {
            activeSmartSolver = true;
            extra = new SmartSolverExtra();
            this.refAccumulator = refAccumulator;
            refCutoff = SolverConstants.getReferenceCutoff();
            numPartialMoves = SolverConstants.getNumPartialMoves();
        }
    }

    /**
     * Initializes SolverPD object with user defined custom pattern.  If refAccumlator is null
     * or empty, it will act as standard version.
     *
     * @param customPattern byte array of user defined custom pattern
     * @param elementGroups boolean array of groups reference to given pattern
     * @param refAccumulator the given ReferenceAccumulator object
     */
    public SmartSolverPdbBase(byte[] customPattern, boolean[] elementGroups,
            ReferenceAccumulator refAccumulator) {
        super(customPattern, elementGroups);
        if (refAccumulator == null || refAccumulator.getActiveMap() == null) {
            System.out.println("Referece board collection unavailable."
                    + " Resume to the 15 puzzle solver standard version.");
            extra = null;
            this.refAccumulator = null;
            refCutoff = 0;
            numPartialMoves = 0;
        } else {
            activeSmartSolver = true;
            extra = new SmartSolverExtra();
            this.refAccumulator = refAccumulator;
            refCutoff = SolverConstants.getReferenceCutoff();
            numPartialMoves = SolverConstants.getNumPartialMoves();
        }
    }

    /**
     *  Initializes SolverPdb object with a given concrete class.
     *
     *  @param copySolver an instance of SolverPdb
     */
    public SmartSolverPdbBase(SolverPdbBase copySolver, ReferenceAccumulator refAccumulator) {
        super(copySolver);
        if (refAccumulator == null || refAccumulator.getActiveMap() == null) {
            System.out.println("Referece board collection unavailable."
                    + " Resume to the 15 puzzle solver standard version.");
            extra = null;
            this.refAccumulator = null;
            refCutoff = 0;
            numPartialMoves = 0;
        } else {
            activeSmartSolver = true;
            extra = new SmartSolverExtra();
            this.refAccumulator = refAccumulator;
            refCutoff = SolverConstants.getReferenceCutoff();
            numPartialMoves = SolverConstants.getNumPartialMoves();
        }
    }

    /**
     * Print solver description with in use pattern.
     */
    @Override
    public void printDescription() {
        extra.printDescription(flagAdvancedPriority, inUseHeuristic);
        printInUsePattern();
    }

    /**
     * Find the optimal path to goal state if the given board is solvable.
     * Overload findOptimalPath with given heuristic value (for AdvancedAccumulator)
     *
     * @param board the initial puzzle Board object to solve
     * @param estimate the given initial limit to solve the puzzle
     */
    public void findOptimalPath(Board board, byte estimate) {
        if (board.isSolvable()) {
            clearHistory();
            stopwatch = new Stopwatch();
            lastDepthSummary = new int[rowSize * 2];
            for (int i = 0; i < 4; i++) {
                if (board.getValidMoves()[i] == 0) {
                    lastDepthSummary[i] = endOfSearch;
                } else {
                    lastDepthSummary[i + 4] = board.getValidMoves()[i];
                }
            }
            // initializes the board by calling heuristic function using original priority
            // then solve the puzzle with given estimate instead
            heuristic(board, tagStandard, tagSearch);
            idaStar(estimate);
            stopwatch = null;
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
        return heuristic(board, flagAdvancedPriority, tagSearch);
    }

    // overload method to calculate the heuristic value of the given board and conditions
    protected byte heuristic(Board board, boolean isAdvanced, boolean isSearch) {
        if (!board.isSolvable()) {
            return -1;
        }

        if (!board.equals(lastBoard) || isSearch) {
            priorityAdvanced = -1;
            lastBoard = board;
            tiles = board.getTiles();
            zeroX = board.getZeroX();
            zeroY = board.getZeroY();
            byte[] tilesSym = board.getTilesSym();

            // additive pattern database components
            pdKeys = convert2pd(tiles, tilesSym, szGroup);
            pdValReg = 0;
            pdValSym = 0;
            for (int i = szGroup; i < szGroup * 2; i++) {
                pdValReg += pdKeys[i];
                pdValSym += pdKeys[i +  offsetPdSym];
            }

            priorityGoal = (byte) Math.max(pdValReg, pdValSym);
        }

        if (!isAdvanced) {
            return priorityGoal;
        }

        AdvancedRecord record = extra.advancedContains(board, isSearch, refAccumulator);
        if (record != null) {
            priorityAdvanced = record.getEstimate();
        }
        if (priorityAdvanced != -1) {
            return priorityAdvanced;
        }

        priorityAdvanced = priorityGoal;
        if (priorityAdvanced < refCutoff) {
            return priorityAdvanced;
        }
        //System.out.println("send to advanced estimate");
        priorityAdvanced = extra.advancedEstimate(board, priorityAdvanced, refCutoff,
                refAccumulator.getActiveMap());

        if ((priorityAdvanced - priorityGoal) % 2 == 1) {
            priorityAdvanced++;
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
    protected void idaStar(int limit) {
        if (inUsePattern == PatternOptions.Pattern_78) {
            lastSearchBoard = new Board(tiles);
        }

        int countDir = 0;
        for (int i = 0; i < rowSize; i++) {
            if (lastDepthSummary[i + rowSize] > 0) {
                countDir++;
            }
        }

        // quick scan for advanced priority, determine the start order for optimization
        if (flagAdvancedPriority && countDir > 1) {
            int initLimit = priorityGoal;
            while (initLimit < limit) {
                idaCount = 0;
                dfsStartingOrder(zeroX, zeroY, initLimit, pdValReg, pdValSym);
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

        // start searching for solution
        while (limit <= maxMoves) {
            idaCount = 0;
            if (flagMessage) {
                System.out.print("ida limit " + limit);
            }
            dfsStartingOrder(zeroX, zeroY, limit, pdValReg, pdValSym);
            searchDepth = limit;
            searchNodeCount += idaCount;

            if (timeout) {
                if (flagMessage) {
                    System.out.printf("\tNodes : %-15s timeout\n", Integer.toString(idaCount));
                }
                return;
            } else {
                if (flagMessage) {
                    System.out.printf("\tNodes : %-15s  " + stopwatch.currentTime() + "s\n",
                            Integer.toString(idaCount));
                }
                if (solved) {
                    // if currently using pattern database 7-8 and it takes long than cutoff limit
                    // to solve, add the board and solutions to reference boards collection.
                    if (activeSmartSolver && inUseHeuristic == HeuristicOptions.PD78
                            && stopwatch.currentTime() >  refAccumulator.getCutoffLimit()) {
                        // backup original solutions
                        final Stopwatch backupTime = stopwatch;
                        final byte backupSteps = steps;
                        final int backupIdaCount = searchNodeCount;
                        final Direction[] backupSolution = new Direction[steps + 1];
                        System.arraycopy(solutionMove, 1, backupSolution, 1, steps);

                        searchTime = stopwatch.currentTime();
                        stopwatch = new Stopwatch();
                        // only update cached advanced priority if using original priority search
                        // and the initial board has added to the reference boards
                        if (refAccumulator.addBoard(this)) {
                            priorityAdvanced = backupSteps;
                        }

                        // restore original solutions
                        stopwatch = backupTime;
                        steps = backupSteps;
                        searchNodeCount = backupIdaCount;
                        solutionMove = backupSolution;
                    }
                    return;
                }
            }
            limit += 2;
        }
    }

    /**
     * Returns the boolean represents the advanced priority in use.
     *
     * @return boolean represents the advanced priority in use
     */
    public final boolean getTimeoutFlag() {
        return flagTimeout;
    }

    /**
     * Returns the boolean represents the advanced priority in use.
     *
     * @return boolean represents the advanced priority in use
     */
    public final boolean getMessageFlag() {
        return flagMessage;
    }

    /**
     * Returns the boolean represents the advanced priority in use.
     *
     * @return boolean represents the advanced priority in use
     */
    public final boolean getPriorityFlag() {
        return flagAdvancedPriority;
    }

    /**
     * Returns the board object of last search.
     *
     * @return board object of last search
     */
    public final Board lastSearchBoard() {
        if (inUsePattern != PatternOptions.Pattern_78) {
            throw new UnsupportedOperationException();
        }
        return lastSearchBoard;
    }
}