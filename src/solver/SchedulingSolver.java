package solver;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;
import model.Customer;
import model.ESPPRCResult;
import model.EspprcInstance;
import model.Schedule;

public class SchedulingSolver {
	
	/**
	 * The instance to solve
	 */
	EspprcInstance instance;
	
	/**
	 * Solver constructor
	 * 
	 * @param instance
	 */
	public SchedulingSolver(EspprcInstance instance) {
		this.instance = instance;
	}
	
	/**
	 * Cplex linear model to solve an ESPPRC
	 * 
	 * @return
	 */
	public Schedule solveSchedule(int timeLimit) {
		try {
			IloCplex cplex = new IloCplex();
			cplex.setParam(IloCplex.DoubleParam.TiLim, timeLimit);
			
			// >>> Decision variables
			IloNumVar[][] x = new IloNumVar[this.instance.getNodes().length][this.instance.getNodes().length];
			for(int i=0; i < x.length; i++) {
				for(int j=0; j < x[i].length; j++) {
					x[i][j] = cplex.boolVar("x_"+i+"_"+j);
				}
			}
			
			// The time s_i where customer i is served
			IloNumVar[] s = new IloNumVar[instance.getNodes().length];
			for(int i=0; i < s.length; i++) {
				Customer currentNode = this.instance.getNodes()[i];
				s[i] = cplex.numVar(currentNode.getStart(), currentNode.getEnd(), "s_"+i);
			}
			
			IloNumVar duration = cplex.numVar(0, Double.MAX_VALUE, "duration");
			
			// >>> Objective
			this.addScheduleObjective(cplex, x, s, duration);

			// >>> Constraints
			this.addOneTourConstraints(cplex, x);

			this.addFluxConstraints(cplex, x);

			this.addDurationConstraints(cplex, s, duration);
			
			this.addServiceTimeConstraints(cplex, x, s);

			this.addTimeWindowsConstraints(cplex, x, s);

			// Export the model into a file
			cplex.exportModel("SchedulingModel.lp");

			// Solve
			long startTime = System.nanoTime();

			cplex.solve();
			
			long endTime = System.nanoTime();
			
			long timeElapsed = endTime - startTime;

			// Display results
//			this.displayMatrixResult(cplex, x);
			Schedule route = this.displayRoutesResult( cplex, x, s );
			route.setCost( cplex.getObjValue() );
			
			return route;
			
//			return new ESPPRCResult(route.getPath(), cplex.getObjValue(), timeElapsed/1000000, route.getNbVisitedNodes());
			
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
	private Schedule displayRoutesResult(IloCplex cplex, IloNumVar[][] x, IloNumVar[] s) throws UnknownObjectException, IloException {		
		String out = "";
    	
		int currentNode = 0;
    	int nbVisitedNodes = 0;
    	Schedule result =  new Schedule(x.length);

		boolean finished = false;
    	while ( !finished ) {
    		for(int i = 1; i < x[currentNode].length; i++) {
    			if( cplex.getValue(x[currentNode][i]) > 0 ) {   				
    				if(i == x[currentNode].length - 1) {
    					finished = true;
    					out += "Depot";
    					break;
    				}
    				System.out.println( "Node "+i+" at "+cplex.getValue(s[i]) );
    				System.out.println( instance.getNode(i).getStart()+ " : " + instance.getNode(i).getEnd() );
    				result.addJob(instance.getNode(i));
    				nbVisitedNodes++;
    				out += i + ", ";
    				currentNode = i;
    				break;
    			}
    			
    			if(i == x[currentNode].length - 1) {
    				finished = true;
    			}
    		}
    	}
    	result.setPath(out);
    	result.setNbVisitedNodes(nbVisitedNodes);
    	return result;
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
//				if(i!=j) {
					System.out.print( (int) Math.abs( cplex.getValue( x[i][j]) ) + " ");
//				}
//				else {
//					System.out.print( "0" + " ");
//				}
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
	private void addTimeWindowsConstraints(IloCplex cplex, IloNumVar[][] x, IloNumVar[] s) throws IloException {
		// A large number
		double M = 1000;

		// Time windows when visiting from node i to node j
		for(int i=0; i < s.length; i++) {
			Customer currentNode = this.instance.getNodes()[i];

			for(int j=1; j < s.length; j++) {
				IloLinearNumExpr expression = cplex.linearNumExpr();

				expression.addTerm(1.0, s[i]);
				expression.addTerm(-1.0, s[j]);
				expression.addTerm(M, x[i][j]);

				cplex.addLe(expression, M - currentNode.getServiceTime() - this.instance.getDistanceMatrix()[i][j]);	
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
	private void addDurationConstraints(IloCplex cplex, IloNumVar[] s, IloNumVar duration) throws IloException {
		for(int i = 1; i < s.length; i++) {
			for(int j = 1; j < s.length; j++) {
				IloLinearNumExpr expression = cplex.linearNumExpr();
				expression.addTerm(duration, 1);
				expression.addTerm(s[i], -1);
				expression.addTerm(s[j], 1);
				cplex.addGe(expression, this.instance.getNode(i).getProductionTime());
			}
		}
	}

	/**
	 * Add the the maximum number of routes (vehicles) we may use
	 * 
	 * @param cplex
	 * @param x
	 * @throws IloException
	 */
	private void addServiceTimeConstraints(IloCplex cplex, IloNumVar[][] x, IloNumVar[] s) throws IloException{
		double M = instance.getNode(0).getEnd();
		IloLinearNumExpr expr = cplex.linearNumExpr();

		for (int i=1; i < x.length; i++) {
			expr.addTerm(s[i], 1);
			for (int j=1; j < x[i].length; j++) {
				expr.addTerm(x[i][j], -M);
			}
			cplex.addLe(expr, 0);

		}

	}
	
	/**
	 * 
	 * @param cplex
	 * @param x
	 * @throws IloException
	 */
	private void addScheduleObjective(IloCplex cplex, IloNumVar[][] x, IloNumVar[] s, IloNumVar duration) throws IloException {
		IloLinearNumExpr obj = cplex.linearNumExpr();
		
		obj.addTerm(duration, 1.0);
		
		for(int i=0; i < x.length; i++) {
			Customer current = instance.getNode(i);
			obj.addTerm(s[i], current.getPrecedenceDual() + current.getStabilityDual());
			for(int j=0; j < x[i].length; j++) {
				Customer next = instance.getNode(j);
				if(i != j) {
					obj.addTerm(x[i][j], next.getStabilityTime()*next.getStabilityDual() + instance.getCost(i, j));
				}
			}
		}


		cplex.addMinimize(obj);
	}
}
