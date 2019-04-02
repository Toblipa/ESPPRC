package solver;

import ilog.concert.*;
import ilog.cplex.*;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.util.logging.Level;
import java.util.logging.Logger;

import model.Customer;
import model.ESPPRCResult;
import model.EspprcInstance;

public class EspprcSolver {
	
	// The instance to solve
	EspprcInstance instance;
	
	/**
	 * Solver constructor
	 * 
	 * @param instance
	 */
	public EspprcSolver(EspprcInstance instance) {
		this.instance = instance;
	}
	
	/**
	 * Cplex linear model to solve an ESPPRC
	 * 
	 * @return
	 */
	public ESPPRCResult solveESPPRC(int timeLimit) {
		try {
			IloCplex cplex = new IloCplex();
			cplex.setParam(IloCplex.DoubleParam.TiLim, timeLimit);
			
			// Decision variables
			IloNumVar[][] x = new IloNumVar[this.instance.getNodes().length][this.instance.getNodes().length];

			for(int i=0; i < x.length; i++) {
				for(int j=0; j < x[i].length; j++) {
					x[i][j] = cplex.boolVar("x_"+i+"_"+j);
				}
			}
			
			// Objective
			this.addESPPRCObjective(cplex, x);

			// Constraints
			this.addOneTourConstraints(cplex, x);

			this.addFluxConstraints(cplex, x);

			this.addCapacityConstraints(cplex, x);

			this.addTimeWindowsConstraints(cplex, x);

			// Export the model into a file
			cplex.exportModel("ESPPRCModel.lp");

			// Solve
			long startTime = System.nanoTime();

			cplex.solve();
			
			long endTime = System.nanoTime();
			
			long timeElapsed = endTime - startTime;

			// Display results
			Route route = this.displayRoutesResult(cplex, x);
			
			return new ESPPRCResult(route.getPath(), cplex.getObjValue(), timeElapsed/1000000, route.getNbVisitedNodes());
			
		} catch (IloException ex) {	
			Logger.getLogger(EspprcSolver.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		return null;
	}
	
	/**
	 * Returns a string containing the route found by cplex
	 * 
	 * @param cplex
	 * @param x
	 * @throws UnknownObjectException
	 * @throws IloException
	 */
	private Route displayRoutesResult(IloCplex cplex, IloNumVar[][] x) throws UnknownObjectException, IloException {		
		String out = "";
    	
		int currentNode = 0;
    	int nbVisitedNodes = 0;
    	
		boolean finished = false;
    	while ( !finished ) {
    		for(int i = 1; i < x[currentNode].length; i++) {
    			if( cplex.getValue(x[currentNode][i]) > 0 ) {
    				nbVisitedNodes++;
    				out += currentNode + ", ";
    				currentNode = i;
    				
    				if(i == x[currentNode].length - 1) {
    					finished = true;
    					out += "Depot";
    				}
    				
    				break;
    			}
    			
    			if(i == x[currentNode].length - 1) {
    				finished = true;
    			}
    		}
    	}
    	
    	return new Route(out, nbVisitedNodes);
	}
	
	public class Route {
		String path;
		int nbVisitedNodes;
		
		public Route(String path, int nbVisitedNodes) {
			this.path = path;
			this.nbVisitedNodes = nbVisitedNodes;
		}
		
		public String getPath() {
			return this.path;
		}
		
		public int getNbVisitedNodes() {
			return this.nbVisitedNodes;
		}
	}
	
	/**
	 * Showing results in form of a matrix
	 * 
	 * @param cplex
	 * @param x
	 * @throws UnknownObjectException
	 * @throws IloException
	 */
	@SuppressWarnings("unused")
	private void displayMatrixResult(IloCplex cplex, IloNumVar[][] x) throws UnknownObjectException, IloException {
		System.out.println("Solution:");

		for (int i = 0; i < x.length; i++) {
			for(int j = 0; j < x[i].length; j++) {
				if(i!=j) {
					System.out.print( (int) Math.abs( cplex.getValue( x[i][j]) ) + " ");
				}
				else {
					System.out.print( "0" + " ");
				}
			}
			System.out.println("");
		}

	}
	
	/**
	 * Add time windows constraints corresponding to
	 * s_i + serv_i + d_{ij} - s_j + Mx_{ij} <= M (for all i in V\{p})
	 * where p is the origin, V the ensemble of nodes, M a large number,
	 * s_i the time when node i is served & d_{ij} the distance of arc (v_i,v_j)
	 * 
	 * @param cplex
	 * @param x
	 * @throws IloException
	 */
	private void addTimeWindowsConstraints(IloCplex cplex, IloNumVar[][] x) throws IloException {
		// A large number
		double M = 10000;

		// The time s_i where customer i is served
		IloNumVar[] s = new IloNumVar[instance.getNodes().length];
		for(int i=0; i < s.length; i++) {
			Customer currentNode = this.instance.getNodes()[i];
			s[i] = cplex.numVar(currentNode.getStart(), currentNode.getEnd(), "s_"+i);
		}

		// Time windows when visiting from node i to node j
		for(int i=0; i < s.length; i++) {
			Customer currentNode = this.instance.getNodes()[i];

			for(int j=1; j < s.length; j++) {
				IloLinearNumExpr expression = cplex.linearNumExpr();

				expression.addTerm(1.0, s[i]);
				expression.addTerm(-1.0, s[j]);
				expression.addTerm(M, x[i][j]);

				cplex.addLe(expression, M - currentNode.getServiceTime() - this.instance.getDistance()[i][j]);	
			}
		}
	}
	
	/**
	 * Add elementary flux constraints corresponding to
	 * sum_{j=0}{ x_{ij} } - sum_{j=0}{ x_{ij} } = { 1 if i = p ; -1 if i = t ; 0 else }
	 * 
	 * @param cplex
	 * @param x
	 * @throws IloException
	 */
	private void addFluxConstraints(IloCplex cplex, IloNumVar[][] x) throws IloException {
		for (int i = 0; i < x.length; i++) {
			IloLinearNumExpr expression = cplex.linearNumExpr();
			
			for(int j = 0; j < x[i].length; j++) {
				expression.addTerm(1.0, x[i][j]);
			}
			
			for(int j = 0; j < x[i].length; j++) {
				expression.addTerm(-1.0, x[j][i]);
			}
			
			if (i == 0) {
				cplex.addEq(expression, 1.0);
			}
			else if(i == x.length-1) {
				cplex.addEq(expression, -1.0);
			}
			else {
				cplex.addEq(expression, 0.0);
			}
		}
	}
	
	/**
	 * Add constraints to avoid multiple tours
	 * sum{j = 0}{ x_{0j} } = 1 & sum{j = 0}{ x_{jt} } = 1
	 * 
	 * @param cplex
	 * @param x
	 * @throws IloException
	 */
	private void addOneTourConstraints(IloCplex cplex, IloNumVar[][] x) throws IloException {
		IloLinearNumExpr expression = cplex.linearNumExpr();
		
		for(int j = 0; j < x[0].length; j++) {
			expression.addTerm(1.0, x[j][x.length-1]);
		}
		
		cplex.addLe(expression, 1.0);
		
		IloLinearNumExpr expr = cplex.linearNumExpr();
		
		for(int j = 0; j < x.length; j++) {
			expr.addTerm(1.0, x[0][j]);
		}
		
		cplex.addLe(expr, 1.0);
		
	}
	
	/**
	 * Add constraints to take into account capacity constraints
	 * sum_{(v_i, v_j) in E}{d_i x_{ij}} <= Q
	 * 
	 * @param cplex
	 * @param x
	 * @throws IloException
	 */
	private void addCapacityConstraints(IloCplex cplex, IloNumVar[][] x) throws IloException {
		IloLinearNumExpr expression = cplex.linearNumExpr();

		for(int i = 1; i < x.length; i++) {
			for(int j = 1; j < x[i].length; j++) {
				expression.addTerm(this.instance.getNodes()[i].getDemand(), x[i][j]);
			}
		}

		cplex.addLe(expression, this.instance.getCapacity());

	}

	/**
	 * Add the the maximum number of routes (vehicles) we may use
	 * 
	 * @param cplex
	 * @param x
	 * @throws IloException
	 */
	@SuppressWarnings("unused")
	private void addMaxVehiclesConstraints(IloCplex cplex, IloNumVar[] x) throws IloException{

		IloLinearNumExpr expr = cplex.linearNumExpr();

		for (int i=1; i < x.length; i++) {
			expr.addTerm(x[i], 1.0);
		}

		cplex.addLe(expr, this.instance.getVehicles());

	}
	
	/**
	 * 
	 * @param cplex
	 * @param x
	 * @throws IloException
	 */
	private void addESPPRCObjective(IloCplex cplex, IloNumVar[][] x) throws IloException {
		IloLinearNumExpr obj = cplex.linearNumExpr();

		for(int i=0; i < x.length; i++) {
			for(int j=0; j < x[i].length; j++) {
				if(i != j) {
					obj.addTerm(x[i][j], this.instance.getCost()[i][j]);
				}
			}
		}


		cplex.addMinimize(obj);
	}
	
}
