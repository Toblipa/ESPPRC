package solver;

import ilog.concert.*;
import ilog.cplex.*;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import model.EspprcInstance;
import model.Label;

public class Solver {
	EspprcInstance instance;
	
	public Solver(EspprcInstance instance) {
		this.instance = instance;
	}
	
	public void solveVRP() {
        try {
            IloCplex cplex = new IloCplex();
            
            List<Label>[] nodeLabels = this.instance.genFeasibleRoutes();
            Label[] feasibleRoutes = (Label[]) nodeLabels[nodeLabels.length - 1].toArray();
            
            // Decision variables
            IloNumVar[] x = new IloNumVar[feasibleRoutes.length];
            for(int i=0; i < x.length; i++) {
            	x[i] = cplex.boolVar("x_"+i);
            }
            
            // Objective
            this.addObjective(cplex, x, feasibleRoutes);
            
            // Constraints
            this.addElementaryPathConstraints(cplex, x, feasibleRoutes);
//            this.addElementaryPathRelaxationConstraints(cplex, x, feasibleRoutes);
//            this.addMaxVehiclesConstraints(cplex, x, maxVehicles);
            
            cplex.exportModel("EssprcModel.lp");

            // Solve
//            cplex.solve();

            // Display results
            
        } catch (IloException ex) {	
            Logger.getLogger(Solver.class.getName()).log(Level.SEVERE, null, ex);
        }
	}

	private void addMaxVehiclesConstraints(IloCplex cplex, IloNumVar[] x) throws IloException{
		
        IloLinearNumExpr expr = cplex.linearNumExpr();
        
        for (int i=1; i < x.length; i++) {
        	expr.addTerm(x[i], 1.0);
        }
        
        cplex.addLe(expr, this.instance.getVehicles());
		
	}

	private void addElementaryPathRelaxationConstraints(IloCplex cplex, IloNumVar[] x, Label[] feasibleRoutes) throws IloException {
		
		// Matrix a[i][k] equals to the times
		// customer i is visited in route r
		int[][] a  = new int[this.instance.getNodes().length][feasibleRoutes.length];
		
		for (int i = 0; i < this.instance.getNodes().length; i++) {
			IloLinearNumExpr expression = cplex.linearNumExpr();
			for (int k = 0; k < feasibleRoutes.length; k++) {
				expression.addTerm(a[i][k], x[i]);
			}
			cplex.addGe(expression, 1.0);
		}
		
	}

	private void addElementaryPathConstraints(IloCplex cplex, IloNumVar[] x, Label[] feasibleRoutes) throws IloException {

		// Matrix a[i][k] equals to
		// 1 if route k visits customer i
		// 0 if not
		int[][] a  = new int[this.instance.getNodes().length][feasibleRoutes.length];
		
		for (int i = 0; i < this.instance.getNodes().length; i++) {
			IloLinearNumExpr expression = cplex.linearNumExpr();
			for (int k = 0; k < feasibleRoutes.length; k++) {
				expression.addTerm(a[i][k], x[i]);
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
