package solver;

import ilog.concert.*;
import ilog.cplex.*;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import model.EspprcInstance;
import model.Label;

public class Solver {
	EspprcInstance instance;

	public Solver(EspprcInstance instance) {
		this.instance = instance;
	}

	public void solveVRPTW() {
		try {
			IloCplex cplex = new IloCplex();

			// Introduction
			System.out.println("Solving the instance for "+this.instance.getNodes().length+" nodes");

			ArrayList<Label>[] nodeLabels = this.instance.genFeasibleRoutes();

			ArrayList<Label> depotLabels = nodeLabels[nodeLabels.length - 1];
			int nbFeasibleRoutes = depotLabels.size();

			// We create the necessary parameters and variables
			Label[] feasibleRoutes = new Label[nbFeasibleRoutes];
			IloNumVar[] x = new IloNumVar[nbFeasibleRoutes];

			// Decision variables
			for(int l=0; l < depotLabels.size(); l++) {
				feasibleRoutes[l] = depotLabels.get(l);
				x[l] = cplex.boolVar("x_"+l);
			}

			// Objective
			this.addObjective(cplex, x, feasibleRoutes);

			// Constraints
			this.addElementaryPathConstraints(cplex, x, feasibleRoutes);
//			this.addMaxVehiclesConstraints(cplex, x, maxVehicles);

			// We export the model to a file
			cplex.exportModel("EssprcModel.lp");

			// Solve
			cplex.solve();

			// Display results
			this.displayResults(cplex, x, feasibleRoutes);

		} catch (IloException ex) {	
			Logger.getLogger(Solver.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Diplay the chosen labels that minimizes the objective
	 * 
	 * @param cplex
	 * @param x
	 * @param feasibleRoutes
	 * @throws UnknownObjectException
	 * @throws IloException
	 */
	private void displayResults(IloCplex cplex, IloNumVar[] x, Label[] feasibleRoutes) throws UnknownObjectException, IloException {
		System.out.println("Solution:");

		double totalCost = 0.0;
		for (int r = 0; r < x.length; r++) {
			if( (int) cplex.getValue( x[r] ) != 0 ) {
				totalCost += feasibleRoutes[r].getCost();
				System.out.println(feasibleRoutes[r]);
				System.out.println(feasibleRoutes[r].getRoute());
				System.out.println("");
			}
		}

		System.out.println("Total cost: " + totalCost);
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

	private void addElementaryPathConstraints(IloCplex cplex, IloNumVar[] x, Label[] feasibleRoutes) throws IloException {

		// Matrix a[i][k] equals to
		// 1 if route k visits customer i
		// 0 if not

		int stop = this.instance.getNodes().length;
		if( this.instance.isDuplicateOrigin() ) { stop -=1; }

		for (int i = 1; i < stop; i++) {
			IloLinearNumExpr expression = cplex.linearNumExpr();
			for (int k = 0; k < feasibleRoutes.length; k++) {
				expression.addTerm(feasibleRoutes[k].getVisitationVector()[i], x[k]);
			}
			cplex.addEq(expression, 1.0);
		}
	}

	private void addObjective(IloCplex cplex, IloNumVar[] x, Label[] feasibleRoutes) throws IloException {
		IloLinearNumExpr obj = cplex.linearNumExpr();
		for(int i=0; i < x.length; i++) {
			obj.addTerm(x[i], feasibleRoutes[i].getCost());
		}
		cplex.addMinimize(obj);
	}
}
