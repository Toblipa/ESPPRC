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
	 * List of columns generated during the resolution of the VRPTW
	 */
	private ArrayList<Label> columns;
	
	/**
	 * Initialize the solver with an ESPPRC instance
	 * @param instance
	 */
	public VrptwSolver(EspprcInstance instance) {
		this.instance = instance;
		this.columns = new ArrayList<Label>();
	}
	
	/**
	 * 
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
            // A large number
			double M  = 1000;
			// If we relax the elementary contraints
			boolean relax = true;
			
            
            // min sum_{r_k \in \Omega} {c_k * x_k} + e * M
            // s.t. sum_{r_k \in \Omega} {a_{ik} * x_k} = 1, (\forall v_i \in  V) (cn)
            //		sum_{r_k \in \Omega} {x_k} - e <= U, (cc1)
            //		e >= 0. (cc2)
			
    		// Decision variables
			ArrayList<IloNumVar> x = new ArrayList<IloNumVar>();
    		IloNumVar extraVehicles = cplex.numVar( 0, Double.MAX_VALUE, "extraVehicles");

            // > Objective
			IloLinearNumExpr expression = cplex.linearNumExpr();
			expression.addTerm(M, extraVehicles);

            IloObjective objective = cplex.addMinimize(expression);
            
            // > Constraints
            // Node constraints
    		IloRange[] nodeConstraints = getNodesConstraints( relax ); 
    		
    		// Capacity constraint
    		expression = cplex.linearNumExpr();
    		expression.addTerm(-1, extraVehicles);
    		IloRange capConstraint = cplex.addRange(-Double.MAX_VALUE, expression, U);
            
    		// > Add initial columns
    		ArrayList<Label> initialCols = getInitialCols();
    		addColumns(initialCols, x, objective, nodeConstraints, capConstraint);
    		
    		// Write dual values		
			FileWriter writer = getDualValuesFile(writeDuals);
            
            // Time measure
    		long endTime = System.currentTimeMillis() + timeLimit*1000;
            
            // Pricing problem parameters
    		int maxLabels = labelLimit;
    		int SPTimeLimit = timeLimit;
    		Label minCostRoute = initialCols.get(0);
    		
    		// > Start column generation loop
    		int iteration = 0;
    		boolean finished = false;
            do {
            	iteration++;
                // ======================== Solve Relaxed Master Problem ==============================
	            cplex.solve();
	            System.out.println("Objective: " + cplex.getObjValue());
	            
	            // Get dual values        
	            instance.updateDualValues( cplex.getDuals(nodeConstraints) );
	            
	            // Write down dual values
	            writeDualValues(writer, nodeConstraints);
            
                // ======================== Solve Subproblem ==============================
        		
	            // Update maximum label quantity for the pricing problem
        		if ( iteration > 1 && minCostRoute.getCost() > costGap) {
        			if(maxLabels  > 0) {
        				maxLabels = 0;
        			}
        			else {
        				finished = true;
        			}
        		}
        		else if( maxLabels == 0 ) {
        			maxLabels = labelLimit;
        		}
	            
	            ArrayList<Label> newRoutes = getNewColumns(SPTimeLimit, maxLabels);
	            minCostRoute = newRoutes.get(0);
	            
	            // Add columns
    			addColumns(newRoutes, x, objective, nodeConstraints, capConstraint);
        		
    			System.out.println("Iteration nº " + iteration);
    			System.out.println("Generated route " + minCostRoute.getRoute());
    			System.out.println("With reduced cost " + minCostRoute.getCost());
    			
            }while( !finished && System.currentTimeMillis() < endTime );
            
            if(writeDuals) {
            	writer.close();
            }

            // Relaxed solution information
			cplex.solve();
			
            ArrayList<Label> relaxedSolution = getSolutionSet( x, writeColumns );
            double xSum = getSum( x );
            double lowerBound = cplex.getObjValue();
    		
            VRPTWResult result = new VRPTWResult(relaxedSolution,
            		lowerBound,
            		xSum,
            		initialCols.size(),
            		columns.size(),
            		iteration,
            		minCostRoute.getCost(),
            		finished
            		);
            
            // ======================== Solve Integer Master Problem ==============================
            
            mipConversion( x, extraVehicles );
            
			// We limit time for integer problem
			cplex.setParam( IloCplex.DoubleParam.TiLim, 90 );
			cplex.solve();
			
			double upperBound = cplex.getObjValue();
            System.out.println("Upper bound: " + upperBound);
            System.out.println("Lower bound: " + lowerBound);
            System.out.println("Relative gap: "+(upperBound - cplex.getBestObjValue())/cplex.getBestObjValue());
            
            result.setUpperBound( upperBound );
    		result.setGap( (upperBound-lowerBound)/lowerBound );
    		result.setMipGap( (upperBound - cplex.getBestObjValue())/cplex.getBestObjValue() );
            result.setIntegerSolution( getSolutionSet( x, false ) );
    		
            return result;
            
        } catch (IloException | IOException e) {
            System.err.println("Concert exception caught: " + e);
        }
        
        return null;
    }
	
	/**
	 * 
	 * @param x
	 * @param extraVehicles
	 * @throws IloException
	 */
	private void mipConversion(ArrayList<IloNumVar> x, IloNumVar extraVehicles) throws IloException {
        System.out.println("Converting to MIP...");

        List<IloConversion> mipConversion = new ArrayList<IloConversion>();
        for (IloNumVar decisionVar : x) {
			mipConversion.add( cplex.conversion(decisionVar, IloNumVarType.Bool) ) ;
			cplex.add( mipConversion.get(mipConversion.size()-1) );
        }
        
		mipConversion.add(cplex.conversion(extraVehicles, IloNumVarType.Int)) ;
		cplex.add(mipConversion.get(mipConversion.size()-1));
	}
	
	/**
	 * 
	 * @param relaxed
	 * @return
	 * @throws IloException
	 */
	private IloRange[] getNodesConstraints(boolean relaxed) throws IloException {
		IloRange[] contn = new IloRange[instance.getNbNodes()-2];
		
		for(int  i = 0; i < instance.getNbNodes()-2; i++) {
			if(relaxed) {
				contn[i] = cplex.addRange(1, Double.MAX_VALUE);
			}else {
				contn[i] = cplex.addRange(1, 1);
			}
		}
		
		return contn;
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
	 * @param writeColumns
	 * @return
	 * @throws UnknownObjectException
	 * @throws IloException
	 * @throws IOException
	 */
	private ArrayList<Label> getSolutionSet( ArrayList<IloNumVar> x, boolean writeColumns )
			throws UnknownObjectException, IloException, IOException {
		
		ArrayList<Label> solutionSet = new ArrayList<Label>();		
		
		File file = getColumnFile( writeColumns );

		writeColumnTitles( file );
		
		for(int index = 0; index < x.size(); index++) {
			IloNumVar xi = x.get(index);

			Label generatedRoute = columns.get(index);
			double xValue = cplex.getValue(xi);
			
			if( xValue > 0 ) {
				solutionSet.add( generatedRoute );
			}
			
			writeColumn( file, xValue, generatedRoute );
		}

		return solutionSet;
	}
	
	/**
	 * 
	 * @param writeDualValues 
	 * @return
	 * @throws IOException
	 */
	private FileWriter getDualValuesFile(boolean writeDualValues) throws IOException {
		// Create folder
		if(writeDualValues) {
			int nbClients = instance.getNbNodes() - 2;
			String folderName = "dualValues";
			folderName = folderName + "-" + nbClients;
			File folder = new File( folderName );
			folder.mkdirs();
			
			// Create file
			File file = new File(folderName + File.separator + instance.getName()+"_"+nbClients+".csv");
			file.createNewFile();			
			FileWriter writer = new FileWriter(file);
			return writer;
		}
		return null;
	}
	
	/**
	 * 
	 * @param writer
	 * @param contn
	 * @throws UnknownObjectException
	 * @throws IloException
	 * @throws IOException
	 */
	private void writeDualValues(FileWriter writer, IloRange[] contn) throws UnknownObjectException, IloException, IOException {
		if(writer != null) {
			String line = "";
	        
			for(IloRange constraint : contn) {
	        	line += cplex.getDual(constraint) + "\t" ;
	        }
	        
			writer.write( line + "\n" );
			writer.flush();
		}
	}
	
	/**
	 * 
	 * @param file
	 * @param xValue
	 * @param generatedRoute
	 * @throws IOException
	 */
	private void writeColumn(File file, double xValue, Label generatedRoute) throws IOException {
		if( file != null ) {
			FileWriter writer = new FileWriter(file, true);

			writer.write( xValue + "\t" );

			writer.write( generatedRoute.getRouteDistance(instance) + "\t" );
			writer.write( generatedRoute.getCost() + "\t" );
			writer.write( generatedRoute.getNbVisitedNodes() + "\t" );
			writer.write( generatedRoute.getRoute() + "\n" );
			
			writer.flush();
			writer.close();
		}		
	}
	
	/**
	 * 
	 * @param file
	 * @throws IOException
	 */
	private void writeColumnTitles(File file) throws IOException {
		if( file != null ) {
			FileWriter writer = new FileWriter(file);
			
			writer.write( "Factor" + "\t" +
					"Cost" + "\t" +
					"Reduced Cost" + "\t" +
					"N. Visited" + "\t" +
					"Route" + "\n" );
			writer.flush();
			writer.close();
		}
	}
	
	/**
	 * 
	 * @param writeColumns
	 * @return
	 * @throws IOException
	 */
	private File getColumnFile(boolean writeColumns) throws IOException {
		if( !writeColumns ) {
			return null;
		}
		
		// Create folder
		int nbClients = instance.getNbNodes() - 2;
		String folderName = "columns";
		folderName = folderName + "-" + nbClients;
		File folder = new File( folderName );
		folder.mkdirs();
		
		// Create file
		File file = new File(folderName + File.separator + instance.getName()+"_"+nbClients+".csv");
		file.createNewFile();
		
		return file;
	}
	
	/**
	 * Add column to the relaxed master problem
	 * @param routes
	 * @param x
	 * @param obj
	 * @param contn
	 * @param contc
	 * @throws IloException
	 */
	private void addColumns(ArrayList<Label> routes, ArrayList<IloNumVar> x, IloObjective obj, IloRange[] contn, IloRange contc)
			throws IloException {
		for(Label route : routes) {
			IloColumn col = cplex.column( obj, route.getRouteDistance(instance) );
			
			for(int node = 0; node < instance.getNbNodes()-2; node++) {
				int visit = route.isVisited( node + 1) ? 1:0;
				col = col.and( cplex.column(contn[node], visit) );
			}
			
			col = col.and( cplex.column(contc, 1) );
			x.add( cplex.numVar( col, 0,  1, "x_"+x.size()) );
			
			columns.add(route);
		}
	}
	
	/**
	 * Get all the negative cost routes
	 * @param timeLimit
	 * @param labelLimit
	 * @return
	 */
	private ArrayList<Label> getNewColumns(int timeLimit, int labelLimit) {
		LabellingSolver solver = new LabellingSolver(instance);
		
		if(labelLimit == 0) {
			System.out.println("Solving exact method");
		}
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
		
		if( negCostRoutes.isEmpty() ) {
			negCostRoutes.add(depotLabels.get(0));
		}
		
		return negCostRoutes;
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
	@SuppressWarnings("unused")
	@Deprecated
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
}