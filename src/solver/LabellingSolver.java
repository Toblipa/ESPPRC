package solver;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import model.Customer;
import model.EspprcInstance;
import model.Label;


public class LabellingSolver {
	
	/**
	 * An instance containing the graph and the necessary information
	 */
	private EspprcInstance instance;
	
	/**
	 * Initialize the solver with an ESPPRC instance
	 * @param instance
	 */
	public LabellingSolver(EspprcInstance instance) {
		this.instance = instance;
	}
	
	/**
	 * Corresponds to the algorithm described in (Feillet D, 2004) section 4.4
	 * @param timeLimit
	 * @return list containg the non dominated labels generated on each node of the graph
	 */
	public ArrayList<Label>[] genFeasibleRoutes(int timeLimit, int labelLimit) {
		
		// Initialization
		@SuppressWarnings("unchecked")
		ArrayList<Label>[] labels = new ArrayList[instance.getNbNodes()];
		
		// Origin node
		Label originLabel = new Label( instance );
		labels[0] = new ArrayList<Label>();
		labels[0].add( originLabel );
		
		// Intitialize customer labels
		for(int i = 1; i < instance.getNbNodes(); i++) {
			labels[i] = new ArrayList<Label>();
		}
		
		// Customers waiting to be treated
		Queue<Customer> E = new LinkedList<Customer>();	
		E.add( instance.getNode(0) );
		
		// To stop the algorithm at a certain time
		long startTime = System.currentTimeMillis();
		long endTime = startTime + timeLimit*1000;
		boolean inTime = true;
		// Repeat until E is empty
		do {
			// We choose a node in the waiting list
			Customer currentNode = E.poll();
			
			// Exploration of the successors of a node
			ArrayList<Customer> nodeSuccessors = this.instance.getSuccessors()[currentNode.getId()];
			for(Customer currentSuccessor : nodeSuccessors) {

				// Set of labels extended from currentNode to currentSuccessor
				ArrayList<Label> extendedLabels = new ArrayList<Label>();
				
				// We extend all currentNode labels
				int customerId = currentNode.getId();
				for(Label currentLabel : labels[customerId]) {
					if( !currentLabel.isExtended() && currentLabel.isReachable( currentSuccessor ) ) {
						Label extendedLabel = currentLabel.extendLabel( currentSuccessor, this.instance );
						extendedLabels.add(extendedLabel);
					}
				}
				
				ArrayList<Label> successorLabels = labels[currentSuccessor.getId()];
				
				boolean resultEFF = this.methodEFF2(successorLabels, extendedLabels, labelLimit);

				// End EFF
				if( resultEFF ) {
					E.remove( currentSuccessor );
					E.add( currentSuccessor );
				}
			}
			// Set labels to extended
			labels[currentNode.getId()].stream().forEach( label -> label.setExtended(true) );
			
			if( timeLimit > 0 ) {
				inTime = System.currentTimeMillis() < endTime;
			}
		}while( !E.isEmpty() && inTime );
		
		return labels;
	}
	
	/**
	 * The following function corresponds to the EEF method presented in (Feillet D, 2004)
	 * 
	 * @param successorLabels
	 * @param extendedLabels
	 * @return
	 */
	public boolean methodEFF2(ArrayList<Label> successorLabels, ArrayList<Label> extendedLabels, int labelLimit) {
		// Flag to see if the successor labels have changed
		boolean hasChanged = false;

		// Check dominance among extended labels
		for( Label extendedLabel : extendedLabels ) {
			int removed = 0;
			for( int i  = 0; i-removed < successorLabels.size(); i++ ) {
				Label label = successorLabels.get(i-removed);
				
				if( label.dominates(extendedLabel) ) {
					break;
				}
				
				if( extendedLabel.dominates(label) ) {
					successorLabels.remove(i-removed);
					hasChanged = true;
					removed++;
				}
			}
			
			if( !extendedLabel.isDominated() ) {
				hasChanged = true;
				addToList(successorLabels, extendedLabel);
			}
		}
				
		if( labelLimit > 0 && successorLabels.size() > labelLimit ) {
			for( int i=0 ; i < successorLabels.size() - labelLimit; i++ ) {
//				int removeIndex = 0;
//				int removeIndex = (int) successorLabels.size()/2;
				int removeIndex = successorLabels.size()-1;
				successorLabels.remove( removeIndex );
			}
		}

		return hasChanged;
	}
	
	/**
	 * Add label to list following comparison rules
	 * @param successorLabels
	 * @param extendedLabel
	 */
	private void addToList(ArrayList<Label> successorLabels, Label extendedLabel) {		
		for( int i=0; i < successorLabels.size(); i++ ) {
			int comparison = successorLabels.get(i).compareTo(extendedLabel);
			if(comparison  > 0) {
				successorLabels.add(i, extendedLabel);
				return;
			}
		}
		
		successorLabels.add(extendedLabel);
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
