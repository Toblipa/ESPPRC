package solver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ilog.concert.IloColumn;
import ilog.concert.IloConversion;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;
import model.Customer;
import model.EspprcInstance;
import model.Label;
import model.VRPTWResult;


public class IopSolver {
    /**
     * An instance containing the graph and the necessary information
     */
    private EspprcInstance instance;
    /**
     * A copy of the original instance adapted for the production problem
     */
    private EspprcInstance productionInstance;

    /**
     * MIP solver object
     */
    private IloCplex cplex;

    /**
     * List of columns for routes generated during the resolution of the IOP
     */
    private ArrayList<Label> routeColumns;
    
    /**
     * List of columns for schedules generated during the resolution of the IOP
     */
    private ArrayList<Label> scheduleColumns;
    
    // Node visits constraints
    private IloRange[] nodeRoutesConstraints;

    // Vahicle capacity constraint
    private IloRange vehiclesConstraint;

    // Node production constraints
    private IloRange[] nodeMachinesConstraints;
    
    // Machine capacity constraint
    private IloRange machinesConstraint;
    
    // Stability constraints
    private IloRange[] stabilityConstraints;
    
    // Precedence constraints
    private IloRange[] precedenceConstraints;

    /**
     * Initialize the solver with an ESPPRC instance
     *
     * @param instance
     */
    public IopSolver(EspprcInstance instance) {
        this.instance = instance;
        this.productionInstance = new EspprcInstance( instance );
        this.productionInstance.buildScheduling(false);
        this.routeColumns = new ArrayList<Label>();
        this.scheduleColumns = new ArrayList<Label>();
    }

    /**
     * @param timeLimit
     * @param labelLimit
     * @param writeColumns
     * @param writeDuals
     * @return
     */
    public VRPTWResult runColumnGeneration(int timeLimit, int labelLimit, boolean writeColumns, boolean writeDuals) {
        try {
            cplex = new IloCplex();

            // Small gap for reduced cost
            double costGap = -1e-8;
            // Vehicle float
            int U = instance.getVehicles();
            // Pharmacists
            int W = 10;
            // Large numbers
            double M_omega = 1000;
            double M_psi = 1000;
            // If we relax the elementary contraints
            boolean relax = false;
            
            // min \sum_{r_k \in \Omega} {c_k * x_k} + \gamma * M_{\Omega} + \sum_{p_l \in \Psi}{\psi_l * y_k} + M_{\Psi} * \rho
            // s.t. \sum_{r_k \in \Omega} {a_{ik} * x_k} = 1, (\forall v_i \in  V) (cxn)
            //		\sum_{r_k \in \Omega} {x_k} - \gamma <= U, (cc1)
            //		\sum_{p_l \in \Psi} {b_{il} * y_k} = 1, (\forall v_i \in  V) (cyn)
            //		\sum_{p_l \in \Psi} {y_k} - \rho <= W, (cc1)
            //		\sum_{r_k \in \Omega}{u_ik * x_k} - \sum_{p_l \in \Psi}{(w_{il} + st_i)*b_{il}*y_l} <= 0 (\forall v_i \in V),
            //		\sum_{r_r \in \Omega}{a_{ik}*\omega_k*x_k} - \sum_{p_l \in \Psi}{w_{il}*y_l} >= 0 (\forall v_i \in V),
            //		\gamma >= 0, \rho >= 0.

            // >>> Decision variables
            ArrayList<IloNumVar> x = new ArrayList<IloNumVar>();
            IloNumVar extraVehicles = cplex.numVar(0, Double.MAX_VALUE, "extraVehicles");
            ArrayList<IloNumVar> y = new ArrayList<IloNumVar>();
            IloNumVar extraMachines = cplex.numVar(0, Double.MAX_VALUE, "extraMachines");

            // >>> Objective
            IloLinearNumExpr expression = cplex.linearNumExpr();
            expression.addTerm(M_omega, extraVehicles);
            expression.addTerm(M_psi, extraMachines);

            IloObjective objective = cplex.addMinimize(expression);

            // >>> Constraints
            // Node routes constraints
            nodeRoutesConstraints = getNodesConstraints(relax);

            // Vahicle capacity constraint
            expression = cplex.linearNumExpr();
            expression.addTerm(-1, extraVehicles);
            vehiclesConstraint = cplex.addRange(-Double.MAX_VALUE, expression, U);

            // Node machines constraints
            nodeMachinesConstraints = getNodesConstraints(relax);

            // Machine capacity constraint
            expression = cplex.linearNumExpr();
            expression.addTerm(-1, extraMachines);
            machinesConstraint = cplex.addRange(-Double.MAX_VALUE, expression, W);
            
            // Stability constraints
            stabilityConstraints = getFeasabilityConstraints(false);
            
            // Precedence constraints
            precedenceConstraints = getFeasabilityConstraints(true);
            
            // >>> Add initial columns
            ArrayList<Label> initialRoutes = getInitialCols(instance);
            ArrayList<Label> initialSchedules = getInitialCols(productionInstance);

            addRouteColumns(initialRoutes, x, objective);
            addScheduleColumns(initialSchedules, y, objective);

            // Write dual values
            FileWriter writer = writeDuals ? new FileWriter( getWritableFile("dualValues") ) : null;

            // Time measure
            long endTime = System.currentTimeMillis() + timeLimit * 1000;

            // Pricing problem parameters
            int maxLabels = labelLimit;
            int SPTimeLimit = timeLimit;
            Label minCostRoute = initialRoutes.get(0);
            Label minCostSchedule = initialSchedules.get(0);

            // > Start column generation loop
            int iteration = 0;
            boolean finished = false;
            do {
                iteration++;
                // ======================== Solve Relaxed Master Problem ==============================
                cplex.solve();
                System.out.println("Objective: " + cplex.getObjValue());
                
                // Get dual values
                instance.updateDualValues(cplex.getDuals(nodeRoutesConstraints), cplex.getDual(vehiclesConstraint));
                productionInstance.updateDualValues(cplex.getDuals(nodeMachinesConstraints), cplex.getDual(machinesConstraint));

                // Write down dual values
                writeDualValues(writer, nodeRoutesConstraints);

                // ======================== Solve Subproblem ==============================

                // Update maximum label quantity for the pricing problem
                if (iteration > 1 && minCostRoute.getCost() > costGap) {
                    if (maxLabels > 0) {
                        maxLabels = 0;
                    } else {
                        finished = true;
                    }
                } else if (maxLabels == 0) {
                    maxLabels = labelLimit;
                }
                
                // Route pricing problem
                ArrayList<Label> newRoutes = getNewColumns(SPTimeLimit, maxLabels);
                minCostRoute = newRoutes.get(0);

                // Scheduling pricing problem
                ArrayList<Label> newSchedules = getNewScheduleColumns(SPTimeLimit, maxLabels);
                minCostSchedule = newSchedules.get(0);
                
                // Add columns
                addRouteColumns(newRoutes, x, objective);
//                addMoreRouteColumns(minCostRoute, SPTimeLimit, maxLabels, x, objective);

                System.out.println("Iteration nº " + iteration);
                System.out.println("Generated route " + minCostRoute.getRoute());
                System.out.println("With reduced cost " + minCostRoute.getCost());

            } while (!finished && System.currentTimeMillis() < endTime);

            if (writeDuals) {
                writer.close();
            }

            // Relaxed solution information
            cplex.solve();

            ArrayList<Label> relaxedSolution = getSolutionSet(x, writeColumns);
            double xSum = getSum(x);
            double lowerBound = cplex.getObjValue();

            VRPTWResult result = new VRPTWResult(relaxedSolution,
                    lowerBound,
                    xSum,
                    initialRoutes.size(),
                    routeColumns.size(),
                    iteration,
                    minCostRoute.getCost(),
                    finished
            );

            // ======================== Solve Integer Master Problem ==============================

            mipConversion(x, extraVehicles);

            // We limit time for integer problem
            cplex.setParam(IloCplex.DoubleParam.TiLim, 90);
            cplex.solve();

            double upperBound = cplex.getObjValue();
            System.out.println("Upper bound: " + upperBound);
            System.out.println("Lower bound: " + lowerBound);
            System.out.println("Relative gap: " + (upperBound - cplex.getBestObjValue()) / cplex.getBestObjValue());

            result.setUpperBound(upperBound);
            result.setGap((upperBound - lowerBound) / lowerBound);
            result.setMipGap((upperBound - cplex.getBestObjValue()) / cplex.getBestObjValue());
            result.setIntegerSolution(getSolutionSet(x, false));

            return result;

        } catch (IloException | IOException e) {
            System.err.println("Concert exception caught: " + e);
        }

        return null;
    }

	/**
     * 
     * @param minCostRoute
     * @param SPTimeLimit
     * @param maxLabels
     * @param x
     * @param objective
     * @throws IloException
     */
	private void addMoreRouteColumns(Label minCostRoute, int SPTimeLimit, int maxLabels,
			ArrayList<IloNumVar> x, IloObjective objective) throws IloException {
        instance.deleteRouteNodes(minCostRoute);
        for (int i = 0; i < 5; i++) {
            ArrayList<Label> moreRoutes = getNewColumns(SPTimeLimit, maxLabels);
            Label newMinCostRoute = moreRoutes.get(0);

            // Add columns
            addRouteColumns(moreRoutes, x, objective);
            instance.deleteRouteNodes(newMinCostRoute);
        }
        instance.buildSuccessors();
	}

	/**
     * @param x
     * @param extraVehicles
     * @throws IloException
     */
    private void mipConversion(ArrayList<IloNumVar> x, IloNumVar extraVehicles) throws IloException {
        System.out.println("Converting to MIP...");

        List<IloConversion> mipConversion = new ArrayList<IloConversion>();
        for (IloNumVar decisionVar : x) {
            mipConversion.add(cplex.conversion(decisionVar, IloNumVarType.Bool));
            cplex.add(mipConversion.get(mipConversion.size() - 1));
        }

        mipConversion.add(cplex.conversion(extraVehicles, IloNumVarType.Int));
        cplex.add(mipConversion.get(mipConversion.size() - 1));
    }
    
    /**
     * 
     * @param greater Set to "true" to return a greater or equal constraint. "false" for a less or equal constraint.
     * @return Set of constraints
     * @throws IloException
     */
    private IloRange[] getFeasabilityConstraints(boolean greater) throws IloException {
        int depotNodes = instance.isDuplicateOrigin() ? 2 : 1;
        IloRange[] contn = new IloRange[instance.getNbNodes() - depotNodes];

        for (int i = 0; i < instance.getNbNodes() - depotNodes; i++) {
            if (greater) {
                contn[i] = cplex.addRange(0, Double.MAX_VALUE);
            } else {
                contn[i] = cplex.addRange(-Double.MAX_VALUE, 0);
            }
        }

        return contn;
	}

    /**
     * @param relaxed
     * @return Set of constraints
     * @throws IloException
     */
    private IloRange[] getNodesConstraints(boolean relaxed) throws IloException {
        int depotNodes = instance.isDuplicateOrigin() ? 2 : 1;
        IloRange[] contn = new IloRange[instance.getNbNodes() - depotNodes];

        for (int i = 0; i < instance.getNbNodes() - depotNodes; i++) {
            if (relaxed) {
                contn[i] = cplex.addRange(1, Double.MAX_VALUE);
            } else {
                contn[i] = cplex.addRange(1, 1);
            }
        }

        return contn;
    }

    /**
     * Get the sum of x variables
     *
     * @param x
     * @return
     * @throws UnknownObjectException
     * @throws IloException
     */
    private double getSum(ArrayList<IloNumVar> x) throws UnknownObjectException, IloException {
        double sum = 0;
        for (IloNumVar xi : x) {
            sum += cplex.getValue(xi);
        }
        return sum;
    }

    /**
     * Writes the generated labels on a file and returns the list of labels in the solution set
     *
     * @param x
     * @param writeColumns
     * @return
     * @throws UnknownObjectException
     * @throws IloException
     * @throws IOException
     */
    private ArrayList<Label> getSolutionSet(ArrayList<IloNumVar> x, boolean writeColumns)
            throws UnknownObjectException, IloException, IOException {

        ArrayList<Label> solutionSet = new ArrayList<Label>();

        File file = writeColumns ? getWritableFile("columns") : null;

        writeColumnTitles(file);

        for (int index = 0; index < x.size(); index++) {
            IloNumVar xi = x.get(index);

            Label generatedRoute = routeColumns.get(index);
            double xValue = cplex.getValue(xi);

            if (xValue > 0) {
                solutionSet.add(generatedRoute);
            }

            writeColumn(file, xValue, generatedRoute);
        }

        return solutionSet;
    }

    /**
     * @param folderName
     * @return
     * @throws IOException
     */
    private File getWritableFile(String folderName) throws IOException {
        // Create folder
        int depotNodes = instance.isDuplicateOrigin() ? 2 : 1;
        int nbClients = instance.getNbNodes() - depotNodes;
        folderName = folderName + "-" + nbClients;
        File folder = new File(folderName);
        folder.mkdirs();

        // Create file
        File file = new File(folderName + File.separator + instance.getName() + "_" + nbClients + ".csv");
        file.createNewFile();

        return file;
    }

    /**
     * @param writer
     * @param contn
     * @throws UnknownObjectException
     * @throws IloException
     * @throws IOException
     */
    private void writeDualValues(FileWriter writer, IloRange[] contn) throws UnknownObjectException, IloException, IOException {
        if (writer != null) {
            String line = "";

            for (IloRange constraint : contn) {
                line += cplex.getDual(constraint) + "\t";
            }

            writer.write(line + "\n");
            writer.flush();
        }
    }

    /**
     * @param file
     * @param xValue
     * @param generatedRoute
     * @throws IOException
     */
    private void writeColumn(File file, double xValue, Label generatedRoute) throws IOException {
        if (file != null) {
            FileWriter writer = new FileWriter(file, true);

            writer.write(xValue + "\t");

            writer.write(generatedRoute.getRouteDistance(instance) + "\t");
            writer.write(generatedRoute.getCost() + "\t");
            writer.write(generatedRoute.getNbVisitedNodes() + "\t");
            writer.write(generatedRoute.getRoute() + "\n");

            writer.flush();
            writer.close();
        }
    }

    /**
     * @param file
     * @throws IOException
     */
    private void writeColumnTitles(File file) throws IOException {
        if (file != null) {
            FileWriter writer = new FileWriter(file);

            writer.write("Factor" + "\t" +
                    "Cost" + "\t" +
                    "Reduced Cost" + "\t" +
                    "N. Visited" + "\t" +
                    "Route" + "\n");
            writer.flush();
            writer.close();
        }
    }

    /**
     * Add column to the relaxed master problem
     *
     * @param routes
     * @param x
     * @param obj
     * @param contn
     * @param contc
     * @throws IloException
     */
    private void addRouteColumns(ArrayList<Label> routes, ArrayList<IloNumVar> x, IloObjective obj)
            throws IloException {
        int depotNodes = instance.isDuplicateOrigin() ? 2 : 1;
        for (Label route : routes) {
            IloColumn col = cplex.column(obj, route.getRouteDistance(instance));
            
            // Visit and feasability constraints
            for (int node = 0; node < instance.getNbNodes() - depotNodes; node++) {
            	// Visits
                int visit = route.isVisited(node + 1) ? 1 : 0;
                col = col.and(cplex.column(nodeRoutesConstraints[node], visit));
                
                // Stability
                double timeVisit = route.getVisitTime(node);
                col = col.and(cplex.column(stabilityConstraints[node], timeVisit));
                                
                // Precedence
                double startingTime = route.getStartingTime(instance);
                col = col.and(cplex.column(precedenceConstraints[node], startingTime));

            }
            
            // Vehicle capacity constraint
            col = col.and(cplex.column(vehiclesConstraint, 1));
            x.add(cplex.numVar(col, 0, 1, "x_" + x.size()));
            
            // To keep trace
            routeColumns.add(route);
        }
    }
    
    private void addScheduleColumns(ArrayList<Label> schedules, ArrayList<IloNumVar> y, IloObjective objective) 
    		throws IloException {
    	int depotNodes = productionInstance.isDuplicateOrigin() ? 2 : 1;
    	for (Label schedule : schedules) {
    		IloColumn col = cplex.column(objective, schedule.getRouteDistance(productionInstance));

    		// Visit and feasability constraints
    		for (int node = 0; node < productionInstance.getNbNodes() - depotNodes; node++) {
    			// Visits
    			int visit = schedule.isVisited(node + 1) ? 1 : 0;
    			col = col.and(cplex.column(nodeMachinesConstraints[node], visit));

    			// Stability
    			double timeVisit = schedule.getVisitTime(node) + productionInstance.getNode(node).getStabilityTime();
    			col = col.and(cplex.column(stabilityConstraints[node], timeVisit));

    			// Precedence
    			double startingTime = schedule.getStartingTime(instance);
    			col = col.and(cplex.column(precedenceConstraints[node], startingTime));

    		}

    		// Vehicle capacity constraint
    		col = col.and(cplex.column(machinesConstraint, 1));
    		y.add(cplex.numVar(col, 0, 1, "y_" + y.size()));

    		// To keep trace
    		scheduleColumns.add(schedule);
    	}
    }

    /**
     * Get all the negative cost routes
     *
     * @param timeLimit
     * @param labelLimit
     * @return
     */
    private ArrayList<Label> getNewColumns(int timeLimit, int labelLimit) {
        LabellingSolver solver = new LabellingSolver(instance);

        if (labelLimit == 0) {
            System.out.println("Solving exact method");
        }
        ArrayList<Label>[] nodeLabels = solver.genFeasibleRoutes(timeLimit, labelLimit);

        int depotIndex = instance.isDuplicateOrigin() ? nodeLabels.length - 1 : 0;

        // Get solution information
        ArrayList<Label> depotLabels = nodeLabels[depotIndex];

        ArrayList<Label> negCostRoutes = new ArrayList<Label>();
        for (Label currentLabel : depotLabels) {
            if (currentLabel.getCost() < 0) {
                negCostRoutes.add(currentLabel);
            }
        }

        Collections.sort(negCostRoutes);

        if (negCostRoutes.isEmpty()) {
            negCostRoutes.add(depotLabels.get(0));
        }

        return negCostRoutes;
    }
    
    /**
     * 
     * @param timeLimit
     * @param labelLimit
     * @return
     */
    private ArrayList<Label> getNewScheduleColumns(int timeLimit, int labelLimit) {
        LabellingSolver solver = new LabellingSolver(productionInstance);

        if (labelLimit == 0) {
            System.out.println("Solving exact method");
        }
        ArrayList<Label>[] nodeLabels = solver.genFeasibleRoutes(timeLimit, labelLimit);

        int depotIndex = instance.isDuplicateOrigin() ? nodeLabels.length - 1 : 0;

        // Get solution information
        ArrayList<Label> depotLabels = nodeLabels[depotIndex];

        ArrayList<Label> negCostRoutes = new ArrayList<Label>();
        for (Label currentLabel : depotLabels) {
            if (currentLabel.getCost() < 0) {
                negCostRoutes.add(currentLabel);
            }
        }

        Collections.sort(negCostRoutes);

        if (negCostRoutes.isEmpty()) {
            negCostRoutes.add(depotLabels.get(0));
        }

        return negCostRoutes;
    }

    /**
     * Generate columns folowing the start time order until there is no more capacity
     *
     * @return
     */
    private ArrayList<Label> getInitialCols(EspprcInstance subproblem) {

        ArrayList<Customer> nodes = new ArrayList<Customer>(Arrays.asList(subproblem.getNodes()));
        nodes.remove(0);
        Collections.sort(nodes);

        boolean[] coveredNodes = new boolean[subproblem.getNbNodes()];
        int nbCoveredNodes = subproblem.isDuplicateOrigin() ? 2 : 1; // Origin and Depot

        ArrayList<Label> result = new ArrayList<Label>();

        while (nbCoveredNodes < subproblem.getNbNodes()) {
            Label path = new Label(subproblem);
            Customer node = path.getCurrent();
            ArrayList<Customer> successors = new ArrayList<Customer>(subproblem.getSuccessors()[node.getId()]);
            while (path.getNbUnreachableNodes() < subproblem.getNbNodes()) {
                Collections.sort(successors);
                Customer nextNode = getNextReachable(path, coveredNodes, successors);

                path = path.extendLabel(nextNode, subproblem);

                if (!nextNode.isDepot() && nextNode.getId() != 0) {
                    nbCoveredNodes++;
                    coveredNodes[nextNode.getId()] = true;
                } else {
                    break;
                }

                successors = subproblem.getSuccessors()[nextNode.getId()];
                node = nextNode;
            }
            result.add(path);
        }
        return result;
    }

    /**
     * Get the next reachable node from the given path that has not visited before
     *
     * @param path
     * @param coveredNodes
     * @param successors
     * @return
     */
    private Customer getNextReachable(Label path, boolean[] coveredNodes, ArrayList<Customer> successors) {
        int index = 0;
        if (!instance.isDuplicateOrigin() && successors.size() > 1 && path.getCurrent().getId() != 0) {
            index++;
        }

        Customer nextNode = successors.get(index);
        while (!path.isReachable(nextNode) || coveredNodes[nextNode.getId()]) {
            index++;
            if (index == successors.size()) {
                index = 0;
            }
            nextNode = successors.get(index);
        }
        return nextNode;
    }

    /**
     * Generate default columns where one vehicle visits one node
     *
     * @return
     */
    @SuppressWarnings("unused")
    @Deprecated
    private ArrayList<Label> getDefaultCols() {
        ArrayList<Label> result = new ArrayList<Label>();

        ArrayList<Customer> nodes = new ArrayList<Customer>(Arrays.asList(instance.getNodes()));

        nodes.remove(0);
        nodes.remove(nodes.size() - 1);
        Customer depot = instance.getNode(instance.getNbNodes() - 1);

        for (Customer node : nodes) {
            Label label = new Label(instance);
            label = label.extendLabel(node, instance);
            label = label.extendLabel(depot, instance);

            result.add(label);
        }

        return result;
    }
}