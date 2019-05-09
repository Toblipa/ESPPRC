package solver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;
import model.Customer;
import model.EspprcInstance;
import model.Label;
import model.VRPTWResult;

/**
 * 
 * @author pablo
 *
 */
public class VrptwSolver {
	/**
	 * An instance containing the graph and the necessary information
	 */
	private EspprcInstance instance;
	
	/**
	 * 
	 */
	private IloCplex cplex;
	
	/**
	 * Initialize the solver with an ESPPRC instance
	 * @param instance
	 */
	public VrptwSolver(EspprcInstance instance) {
		this.instance = instance;
	}
	
	/**
	 * 
	 * @param timeLimit
	 * @param labelLimit
	 * @param writeResult
	 * @return
	 */
	public VRPTWResult startColumnGeneration(int timeLimit, int labelLimit, boolean writeResult) {
        try {
            cplex = new IloCplex();
            
            double gap = -1e-8;
            // Vehicle float
            int U = instance.getVehicles();
            // A large number
			double M  = 1000;
            
            // min sum_{r_k \in \Omega} {c_k * x_k} + e * M
            // s.t. sum_{r_k \in \Omega} {a_{ik} * x_k} = 1, (\forall v_i \in  V) (cn)
            //		sum_{r_k \in \Omega} {x_k} - e <= U, (cc1)
            //		e >= 0. (cc2)
			
    		// We create the necessary parameters and variables
			ArrayList<IloNumVar> x = new ArrayList<IloNumVar>();
    		IloNumVar epsilon = cplex.numVar( 0, Double.MAX_VALUE, "epsilon");

            // > Objective
			IloLinearNumExpr expression = cplex.linearNumExpr();
			expression.addTerm(M, epsilon);

            IloObjective obj = cplex.addMinimize(expression);
            
            // > Constraints
    		IloRange[] contn = new IloRange[instance.getNbNodes()];
    		contn[0] = cplex.addRange(1, Double.MAX_VALUE);
    		contn[instance.getNbNodes()-1] = cplex.addRange(1, Double.MAX_VALUE);
    		for(int  i = 1; i < instance.getNbNodes()-1; i++) {
    			contn[i] = cplex.addRange(1, 1);
//    			contn[i] = cplex.addRange(1, Double.MAX_VALUE);
    		}
    		 
    		expression = cplex.linearNumExpr();
    		expression.addTerm(-1, epsilon);
    		IloRange contc = cplex.addRange(-Double.MAX_VALUE, expression, U);
            
    		ArrayList<Label> allColumns = new ArrayList<Label>();
    		
    		// > Add initial columns
    		ArrayList<Label> initialCols = getInitialCols();
    		ArrayList<Label> defaultCols = getDefaultCols();
    		initialCols.addAll(defaultCols);
          
    		for(Label route : initialCols) {
    			addColumn(route, x, obj, contn, contc);
    		}
    		
            allColumns.addAll(initialCols);
            
            Label lastAddedRoute = null;
            int lastIteration = 0;
            
    		long startTime = System.currentTimeMillis();
    		long endTime = startTime + timeLimit*1000;
    		boolean inTime = true;
            for(int iteration = 0; iteration < 100; iteration++) {
            	
                // ======================== Solve Master Problem ==============================
	            cplex.solve();
	            System.out.println("Obj : " + cplex.getObjValue());
	            
	            // Get dual values            
	            double[] pi = cplex.getDuals(contn);
	            instance.updateDualValues(pi);
            
                // ======================== Solve Subproblem ==============================
	            ArrayList<Label> newRoutes = getNewColumns(timeLimit, labelLimit);
	            Label minCostRoute = newRoutes.get(0);
	            
				if( timeLimit > 0 ) {
					inTime = System.currentTimeMillis() < endTime;
				}
				
                // ======================== Add column ==============================
        		if( minCostRoute.getCost() > gap || !inTime ) {
        			System.out.println("Stopped at iteration " + iteration);
        			System.out.println("With generated route " + minCostRoute.getRoute());
        			System.out.println(minCostRoute);
        			
        			lastIteration = iteration;
            		lastAddedRoute = minCostRoute;
        			
            		break;
        		}
        		
//        		if( labelLimit > 0 && minCostRoute.getCost() > -5 ) {
//        			labelLimit = 0;
//        		}
        		
    			System.out.println("Iteration nº " + iteration);
    			System.out.println("Generated route " + minCostRoute.getRoute());
    			System.out.println(minCostRoute);
        		        		
        		for(Label route : newRoutes) {
        			allColumns.add(route);
        			addColumn(route, x, obj, contn, contc);
        		}
        		
        		lastAddedRoute = minCostRoute;
            }
            
            ArrayList<Label> solution = null;
            double xSum = 0;
			try {
				solution = getSolution( x, allColumns, writeResult );
				xSum = getSum( x );
			} catch (IOException e) {
				e.printStackTrace();
			}
    		
            VRPTWResult result = new VRPTWResult(solution,
            		cplex.getObjValue(),
            		xSum,
            		initialCols.size(),
            		allColumns.size(),
            		lastIteration,
            		lastAddedRoute.getCost()
            		);
            
//    		cplex.conversion((IloNumVar[]) x.toArray(), IloNumVarType.Bool);
//    		cplex.conversion(epsilon, IloNumVarType.Int);
//    		cplex.solve();
    		
            return result;
            
        } catch (IloException e) {
            System.err.println("Concert exception caught: " + e);
        }
        
        
        return null;
    }
	
	/**
	 * Get the sum of x variables
	 * @param x
	 * @return
	 * @throws UnknownObjectException
	 * @throws IloException
	 */
	private double getSum(ArrayList<IloNumVar> x) throws UnknownObjectException, IloException {
		double sum = 0;
		for(IloNumVar xi : x) {
			sum += cplex.getValue(xi);
		}
		return sum;
	}

	/**
	 * Writes the generated labels on a file and returns the list of labels in the solution set
	 * @param x
	 * @param allColumns
	 * @param write
	 * @return
	 * @throws UnknownObjectException
	 * @throws IloException
	 * @throws IOException
	 */
	private ArrayList<Label> getSolution( ArrayList<IloNumVar> x, ArrayList<Label> allColumns, boolean write )
			throws UnknownObjectException, IloException, IOException {
		
		ArrayList<Label> solutionSet = new ArrayList<Label>();
		File file = null;
		
		if( write ) {
			int nbClients = instance.getNbNodes() - 2;
			file = new File("columns_"+instance.getName()+"_"+nbClients+".csv");
			file.createNewFile();
			
			FileWriter writer = new FileWriter(file);
			
			writer.write( "Factor" + "\t" +
					"Cost" + "\t" +
					"Reduced Cost" + "\t" +
					"N. Visited" + "\t" +
					"Route" + "\n" );
			
			writer.close();
		}
		
		System.out.println( "\nSolution:" );
		for(int index = 0; index < x.size(); index++) {
			IloNumVar xi = x.get(index);

			Label generatedRoute = allColumns.get(index);
			double xValue = cplex.getValue(xi);
			
			if( xValue > 0 ) {
				solutionSet.add( generatedRoute );
				System.out.println(xi.getName() + ": " + xValue);
				System.out.println( generatedRoute );
				System.out.println( generatedRoute.getRoute() );	
			}

			if( write ) {
				FileWriter writer = new FileWriter(file, true);

				writer.write( xValue + "\t" );

				writer.write( generatedRoute.getRouteDistance(instance) + "\t" );
				writer.write( generatedRoute.getCost() + "\t" );
				writer.write( generatedRoute.getNbVisitedNodes() + "\t" );
				writer.write( generatedRoute.getRoute() + "\n" );

				writer.close();
			}
		}

		return solutionSet;
	}
	
	/**
	 * Add column to the relaxed master problem
	 * @param route
	 * @param x
	 * @param obj
	 * @param contn
	 * @param contc
	 * @throws IloException
	 */
	private void addColumn(Label route, ArrayList<IloNumVar> x, IloObjective obj, IloRange[] contn, IloRange contc)
			throws IloException {
		
		IloColumn col = cplex.column( obj, route.getRouteDistance(instance) );
		
		for(int node = 0; node < instance.getNbNodes(); node++) {
			int visit = route.isVisited( node ) ? 1:0;
			col = col.and( cplex.column(contn[node], visit) );
		}
		
		col = col.and( cplex.column(contc, 1) );
		x.add( cplex.numVar( col, 0,  1, "x_"+x.size()) );
	}
	
	/**
	 * Get all the negative cost routes
	 * @param timeLimit
	 * @param labelLimit
	 * @return
	 */
	private ArrayList<Label> getNewColumns(int timeLimit, int labelLimit) {
		LabellingSolver solver = new LabellingSolver(instance);
		
        ArrayList<Label>[] nodeLabels = solver.genFeasibleRoutes(timeLimit, labelLimit);
        
		// Get solution information
    	ArrayList<Label> depotLabels = nodeLabels[nodeLabels.length - 1];
    	
    	ArrayList<Label> negCostRoutes = new ArrayList<Label>();
		for ( Label currentLabel : depotLabels ) {
    		if(currentLabel.getCost() < 0) {
    			negCostRoutes.add(currentLabel);
    		}
    	}
		
		Collections.sort(negCostRoutes);
		
		return negCostRoutes;
	}
	
	/**
	 * Get the minimum cost route of the subproblem
	 * @param timeLimit
	 * @param labelLimit
	 * @return
	 */
	@SuppressWarnings("unused")
	private ArrayList<Label> getNewColumn(int timeLimit, int labelLimit) {
        LabellingSolver solver = new LabellingSolver(instance);
		
        ArrayList<Label>[] nodeLabels = solver.genFeasibleRoutes(timeLimit, labelLimit);
        
		// Get solution information
    	ArrayList<Label> depotLabels = nodeLabels[nodeLabels.length - 1];
    	
    	Label minCostRoute = depotLabels.get(0);
		for ( Label currentLabel : depotLabels ) {
    		if(currentLabel.getCost() < minCostRoute.getCost()) {
    			minCostRoute = currentLabel;
    		}
    	}
		
		return (ArrayList<Label>) Arrays.asList(minCostRoute);
	}
	
	/**
	 * Generate columns folowing the start time order until there is no more capacity
	 * @return
	 */
	private ArrayList<Label> getInitialCols() {
		ArrayList<Label> result = new ArrayList<Label>();
		
		ArrayList<Customer> nodes = new ArrayList<Customer>(Arrays.asList(instance.getNodes()));
		nodes.remove(0);
		boolean[] coveredNodes = new boolean[instance.getNbNodes()];
		int nbCoveredNodes = 2; // Origin and Depot
		
		Collections.sort(nodes);
		
		while(nbCoveredNodes < instance.getNbNodes()) {
			Label origin = new Label(instance);
			int index = 0;
			Customer nextNode = nodes.get(index);
			while(coveredNodes[nextNode.getId()] && index < instance.getNbNodes()-2) {
				index++;
				nextNode = nodes.get(index);
			}
			
			while( !nextNode.isDepot() ) {
				Label path = origin.extendLabel(nextNode, instance);
				coveredNodes[nextNode.getId()] = true;
				nbCoveredNodes++;
				origin = path;
				
				ArrayList<Customer> successors = instance.getSuccessors()[nextNode.getId()];
				Collections.sort(successors);
				index = 0;
				nextNode = successors.get(index);
				
				while((!origin.isReachable(nextNode) || coveredNodes[nextNode.getId()]) && index < successors.size()-1) {
					index++;
					nextNode = successors.get(index);
				}
			}
			
			Label path = origin.extendLabel(nextNode, instance);
			coveredNodes[nextNode.getId()] = true;
			
			result.add(path);
		}
		
		return result;
	}
	
	/**
	 * Generate default columns where one vehicle visits one node 
	 * @return
	 */
	private ArrayList<Label> getDefaultCols() {
		ArrayList<Label> result = new ArrayList<Label>();
		
		ArrayList<Customer> nodes = new ArrayList<Customer>(Arrays.asList(instance.getNodes()));
		
		nodes.remove(0);
		nodes.remove(nodes.size()-1);
		Customer depot = instance.getNode(instance.getNbNodes()-1);
		
		for (Customer node : nodes) {
			Label label = new Label(instance);
			label = label.extendLabel(node, instance);
			label = label.extendLabel(depot, instance);
			
			result.add( label );
		}
		
		return result;
	}
	
	/**
	 * 
	 * @param contn
	 * @param contc
	 * @throws UnknownObjectException
	 * @throws IloException
	 */
	@SuppressWarnings("unused")
	private void printDualValues(IloRange[] contn, IloRange contc) throws UnknownObjectException, IloException {
        System.out.println("dual of capacity constraint: "+cplex.getDual(contc));
        
        for(IloRange constraint : contn) {
            System.out.println("dual of path constraint: "+cplex.getDual(constraint));
        }
	}
	
	/**
	 * 
	 * @param epsilon
	 * @param x
	 * @throws UnknownObjectException
	 * @throws IloException
	 */
	@SuppressWarnings("unused")
	private void printVariables(IloNumVar epsilon, ArrayList<IloNumVar> x) throws UnknownObjectException, IloException {
        System.out.println("e : " + cplex.getValue(epsilon));
        for(IloNumVar xi : x) {
            System.out.println(xi.getName() + ": " + cplex.getValue(xi));
        }
	}
}