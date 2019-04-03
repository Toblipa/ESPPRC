package solver;

import java.util.ArrayList;
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
		Label originLabel = new Label( instanceNodes[0] );
		
		boolean[] originUnreachableVector = new boolean[instanceNodes.length];
		originUnreachableVector[0] = true;
		originLabel.setUnreachableNodes(originUnreachableVector);
		
		boolean[] originVisitationVector = new boolean[instanceNodes.length];
		originVisitationVector[0] = true;
		originLabel.setVisitationVector( originVisitationVector );
		
		labels[0] = new ArrayList<Label>();
		labels[0].add( originLabel );
		
		// Customer labels
		for(int i = 1; i < instanceNodes.length; i++) {
			labels[i] = new ArrayList<Label>();
		}
		
		// Customers waiting to be treated
		Queue<Customer> E = new LinkedList<Customer>();		
		E.add(instanceNodes[0]);
		
		long startTime = System.currentTimeMillis();
		long endTime = startTime + timeLimit*1000;
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
				for(Label currentLabel : labels[customerId]) {
					if( currentLabel.isReachable( currentSuccessor ) && !currentLabel.isExtended()) {
						Label ext = currentLabel.extendLabel( currentSuccessor, this.instance );
						extendedLabels.add(ext);
					}
				}
				
				ArrayList<Label> successorLabels = labels[currentSuccessor.getId()];
				
//				EFF resultEFF = this.methodEFF1(successorLabels, extendedLabels);
				boolean resultEFF = this.methodEFF2(successorLabels, extendedLabels);
				
//				labels[currentSuccessor.getId()] = resultEFF.getLabels();

				// End EFF
				if( !E.contains(currentSuccessor) && resultEFF ) {
					E.add(currentSuccessor);
				}
			}
			
			labels[currentNode.getId()].stream().forEach(label -> label.setExtended(true));			
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
	public boolean methodEFF1(ArrayList<Label> successorLabels, ArrayList<Label> extendedLabels) {
		// Flag to see if the successor labels have changed
		boolean hasChanged = false;
		
		ArrayList<Label> resultLabels = new ArrayList<Label>();
		
		// Check if labels we have by far are dominated
		// by the extended labels and viceversa		
		for(int l=0; l < successorLabels.size(); l++) {
			Label analyzingLabel = successorLabels.get(l);
			
			for(int f=0; f < extendedLabels.size(); f++) {
				Label extLabel = extendedLabels.get(f);
				if( !extLabel.isDominated() ) {
					analyzingLabel.checkDominance(extLabel);					
					
					if( analyzingLabel.isDominated() ) {
						hasChanged = true;
						break;
					}
				}
			}
			
			if( !analyzingLabel.isDominated() ) {
				resultLabels.add(analyzingLabel);
			}
		}
		
		// Check dominance among extended labels		
		for(int f=0; f < extendedLabels.size(); f++) {
			Label extLabel1 = extendedLabels.get(f);
			
			if( extLabel1.isDominated() ) { continue; }
			
			for(int g=f+1; g < extendedLabels.size(); g++) {
				Label extLabel2 = extendedLabels.get(g);
				
				if( !extLabel2.isDominated() ) {
					extLabel1.checkDominance(extLabel2);
				}
				else {
					continue;
				}
				
				if( extLabel1.isDominated() ) {
					break;
				}
			}
			
			if( !extLabel1.isDominated() ) {
				resultLabels.add(extLabel1);
				hasChanged = true;
			}
		}
		
		successorLabels.clear();
		successorLabels.addAll(resultLabels);
		
		return hasChanged;
	}
	
	private boolean methodEFF2(ArrayList<Label> successorLabels, ArrayList<Label> extendedLabels) {
		// Flag to see if the successor labels have changed
		boolean hasChanged = false;

		ArrayList<Label> resultLabels = new ArrayList<Label>();

		// Check if labels we have by far, are dominated
		// by the extended labels and viceversa		
		for(Label analyzingLabel : successorLabels) {
			for(Label extLabel : extendedLabels) {
				// If the extended label has not been dominated yet
				if( !extLabel.isDominated() ) {
					boolean actualDominates = analyzingLabel.dominates(extLabel);
					
					// If the extended label has not been dominated, we check the opposite
					if( !actualDominates && extLabel.dominates(analyzingLabel)) {
						hasChanged = true;
						break;
					}
				}
			}
			// If no extended label dominates the current label, we add it
			if( !analyzingLabel.isDominated() ) {
				resultLabels.add(analyzingLabel);
			}
		}

		// Check dominance among extended labels
		for(int f=0; f < extendedLabels.size(); f++) {
			Label extLabel1 = extendedLabels.get(f);

			if( extLabel1.isDominated() ) { continue; }

			for(int g=f+1; g < extendedLabels.size(); g++) {
				Label extLabel2 = extendedLabels.get(g);

				if( !extLabel2.isDominated() ) {
					boolean firstDominance = extLabel1.dominates(extLabel2);
					
					if( !firstDominance && extLabel2.dominates(extLabel1)) {
						break;
					}
					
				}else { continue; }
			}

			if( !extLabel1.isDominated() ) {
				resultLabels.add(extLabel1);
				hasChanged = true;
			}
		}
		
		successorLabels.clear();
		successorLabels.addAll(resultLabels);
		
		return hasChanged;
	}
	
	/**
	 * Just a dummy function for debug purposes
	 * 
	 * @param successorLabels
	 * @param extendedLabels
	 * @return
	 */
	@SuppressWarnings("unused")
	private boolean dummyEFF(ArrayList<Label> successorLabels, ArrayList<Label> extendedLabels) {
		boolean hasChanged = false;
		
		successorLabels.addAll(extendedLabels);
		
		if(!extendedLabels.isEmpty()) {
			hasChanged = true;
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
	private void displayE(ArrayList<Customer> E, int itNumber) {
		System.out.print(itNumber+": {");
		for(int e = 0; e < E.size(); e++) {
			System.out.print(E.get(e).getId()!=6? E.get(e).getId():"Depot");
			if(e != E.size()-1) System.out.print(", ");
		}
		System.out.println("}");		
	}
}
