package solver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import model.Customer;
import model.EspprcInstance;
import model.Label;


public class LabellingSolver {
	private EspprcInstance instance;
	
	/**
	 * Initialize the solver with an ESPPRC instance
	 * @param instance
	 */
	public LabellingSolver(EspprcInstance instance) {
		this.instance = instance;
	}
	
	/**
	 *  Corresponds to the algorithm described in (Feillet D, 2004) section 4.4
	 *  
	 * @return
	 */
	public ArrayList<Label>[] genFeasibleRoutes(int timeLimit) {
		
		Customer[] instanceNodes = this.instance.getNodes();
		
		// Initialization
		@SuppressWarnings("unchecked")
		ArrayList<Label>[] labels = new ArrayList[instanceNodes.length];
		
		// Origin node
		Label originLabel = Label.createOriginLabel(instance);
		
		labels[0] = new ArrayList<Label>();

		labels[0].add( originLabel );
		
		// Intitialize customer labels
		for(int i = 1; i < instanceNodes.length; i++) {
			labels[i] = new ArrayList<Label>();
		}
		
		// Customers waiting to be treated
		Queue<Customer> E = new LinkedList<Customer>();	
		E.add(instanceNodes[0]);
		
		// To stop the algorithm at a certain time
		long startTime = System.currentTimeMillis();
		long endTime = startTime + timeLimit*1000;
		
		// Repeat until E is empty
		do {
			// We choose a node in the waiting list
			Customer currentNode = E.poll();
			
			// Exploration of the successors of a node
			ArrayList<Customer> nodeSuccessors = this.instance.getSuccessors()[currentNode.getId()];
			for(Customer currentSuccessor : nodeSuccessors) {

				// Set of labels extended from i to j
				ArrayList<Label> extendedLabels = new ArrayList<Label>();
				
				// We extend all currentNode labels
				int customerId = currentNode.getId();
//				for(Label currentLabel : labels[customerId]) {
				for ( Iterator<Label> iterator = labels[customerId].iterator(); iterator.hasNext(); ) {
					Label currentLabel = iterator.next();
					if( !currentLabel.isExtended() && currentLabel.isReachable( currentSuccessor ) ) {
						Label ext = currentLabel.extendLabel( currentSuccessor, this.instance );
						extendedLabels.add(ext);
					}
				}
				
				ArrayList<Label> successorLabels = labels[currentSuccessor.getId()];
				
				boolean resultEFF = this.methodEFF2(successorLabels, extendedLabels);

				// End EFF
				if( resultEFF ) {
					E.remove( currentSuccessor );
					E.add( currentSuccessor );
				}
			}
			// Set labels to extended
			labels[currentNode.getId()].stream().forEach( label -> label.setExtended(true) );
		}while( !E.isEmpty() && System.currentTimeMillis() < endTime);
		
		return labels;
	}
	
	/**
	 * The following function corresponds to the EEF method presented in (Feillet D, 2004)
	 * 
	 * @param successorLabels
	 * @param extendedLabels
	 * @return
	 */
	public boolean methodEFF2(ArrayList<Label> successorLabels, ArrayList<Label> extendedLabels) {
		// Flag to see if the successor labels have changed
		boolean hasChanged = false;

		// Check dominance among extended labels
		for(Label extLabel : extendedLabels ) {
			int removed = 0;
			for(int i  = 0; i-removed < successorLabels.size(); i++) {
				Label label = successorLabels.get(i-removed);
				
				if( label.dominates(extLabel) ) {
					break;
				}
				
				if( extLabel.dominates(label) ) {
					successorLabels.remove(i-removed);
					hasChanged = true;
					removed++;
				}
			}
			
			if( !extLabel.isDominated() ) {
				hasChanged = true;
				successorLabels.add(extLabel);
			}
		}

		return hasChanged;
	}
	
	/**
	 * The following function corresponds to the EEF method presented in (Feillet D, 2004)
	 * 
	 * @param successorLabels
	 * @param extendedLabels
	 * @return
	 */
	public boolean methodEFF3(LinkedList<Label> successorLabels, ArrayList<Label> extendedLabels) {
		// Flag to see if the successor labels have changed
		boolean hasChanged = false;

		// Check dominance among extended labels
		for(Label extLabel : extendedLabels ) {
			for ( Iterator<Label> iter = successorLabels.iterator(); iter.hasNext(); ) {
				Label label = iter.next();
				if( label.dominates(extLabel) ) {
					break;
				}
				if( extLabel.dominates(label) ) {
					iter.remove();
					hasChanged = true;
				}
			}
			
			if( !extLabel.isDominated() ) {
				hasChanged = true;
				successorLabels.add(extLabel);
			}
		}

		return hasChanged;
	}
	
	/**
	 * The following function corresponds to the EEF method presented in (Feillet D, 2004)
	 * 
	 * @param successorLabels
	 * @param extendedLabels
	 * @return
	 */
	@SuppressWarnings("unused")
	private boolean methodEFF1(ArrayList<Label> successorLabels, ArrayList<Label> extendedLabels) {
		// Flag to see if the successor labels have changed
		boolean hasChanged = false;

		// Check dominance among extended labels
		for(Label extLabel : extendedLabels ) {
			int removed = 0;
			for(int i  = 0; i-removed < successorLabels.size(); i++) {
				Label label = successorLabels.get(i-removed);
				
				if( label.checkDominance(extLabel) ) {
					break;
				}
				
				if( label.isDominated() ) {
					successorLabels.remove(i-removed);
					hasChanged = true;
					removed++;
				}
			}
			
			if( !extLabel.isDominated() ) {
				hasChanged = true;
				successorLabels.add(extLabel);
			}
		}

		return hasChanged;
	}

	/**
	 * Print function for debug purposes
	 * 
	 * @param E
	 * @param itNumber
	 */
	@SuppressWarnings("unused")
	private void displayE(Queue<Customer> E, int itNumber) {
		System.out.print(itNumber+": {");
		for(Customer customer : E) {
			System.out.print(customer.getId());
			System.out.print(", ");
		}
		System.out.println("}");		
	}
}
