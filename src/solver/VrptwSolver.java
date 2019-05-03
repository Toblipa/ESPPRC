package solver;

import java.util.ArrayList;

import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import model.EspprcInstance;
import model.Label;

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
	 * Initialize the solver with an ESPPRC instance
	 * @param instance
	 */
	public VrptwSolver(EspprcInstance instance) {
		this.instance = instance;
	}
	
	/**
	 * Corresponds to the algorithm described in (Feillet D, 2004) section 4.4
	 * @param timeLimit
	 */
	public void startColumnGeneration(int timeLimit, int labelLimit) {
        try {
            IloCplex cplex = new IloCplex();
            // Starting LP
            // max 1.5 x1 + x2
            // s.t. 2*x1 + x2 <= 5   (c1)
            //      x1 + x2 <= 6   (c2)
            //      x1 >= 0
            // 		x2 >= 0
            
            // Define the objective fucntion (empty)
            IloObjective obj = cplex.addMaximize();
            
            // Define constraints
            IloRange cont1 = cplex.addRange(-Double.MAX_VALUE, 5); // (c1)
            IloRange cont2 = cplex.addRange(-Double.MAX_VALUE, 6); // (C2)
            
            // Add initial columns : x1 and x2
            IloColumn col = cplex.column(obj, 1.5); // x1 has coeff 1.5 in the objective function
            col = col.and(cplex.column(cont1, 2)); // x1 has coeff 2 in constraint (c1)
            col = col.and(cplex.column(cont2, 1)); // x1 has coeff 1 in constraint (c2)
            IloNumVar x1 = cplex.numVar(col, 0, Double.MAX_VALUE); // define variable x1 as a column
            
            col = cplex.column(obj, 1);// x2 has coeff 1 in the objective function
            col = col.and(cplex.column(cont1, 1)); // x2 has coeff 1 in constraint (c1)
            col = col.and(cplex.column(cont2, 1)); // x2 has coeff 1 in constraint (c2)
            IloNumVar x2 = cplex.numVar(col, 0, Double.MAX_VALUE); // define variable x2 as a column
            
            // First solve
            cplex.solve();
            System.out.println("Obj : " + cplex.getObjValue()); // 5
            System.out.println("x1 : " + cplex.getValue(x1)); // 0
            System.out.println("x2 : " + cplex.getValue(x2)); // 5
            
            // Get dual values
            System.out.println("dual of constraint 1 : "+cplex.getDual(cont1)); // 1
            System.out.println("dual of constraint 1 : "+cplex.getDual(cont2)); // 0

            // Add a new column x3 such that the LP is
            // max 1.5 x1 + x2 + 3 x3
            // s.t. 2*x1 + x2 + 1.5*x3 <= 5   (c1)
            //      x1 + x2 + 2*x3 <= 6   (c2)
            //      x1 >= 0
            // 		x2 >= 0
            //              x3 >= 0
            col = cplex.column(obj, 3);
            col = col.and(cplex.column(cont1, 1.5));
            col = col.and(cplex.column(cont2, 2));
            IloNumVar x3 = cplex.numVar(col, 0, Double.MAX_VALUE);
            cplex.solve();
            System.out.println("Obj : " + cplex.getObjValue()); // 9
            System.out.println("x1 : " + cplex.getValue(x1)); // 0.4
            System.out.println("x2 : " + cplex.getValue(x2)); // 0
            System.out.println("x3 : " + cplex.getValue(x3)); // 2.8
            
            // Get dual values
            System.out.println("dual of constraint 1 : "+cplex.getDual(cont1)); // 0
            System.out.println("dual of constraint 1 : "+cplex.getDual(cont2)); // 1.5
            
        } catch (IloException e) {
            System.err.println("Concert exception caught: " + e);
        }

    }
}