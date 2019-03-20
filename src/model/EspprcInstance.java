package model;

import java.util.ArrayList;
import java.util.stream.IntStream;

public class EspprcInstance {
	
	// the nodes
	private Customer[] nodes;
	
	// the successors to every node
	private ArrayList<Customer>[] successors;
	
	// the cost to go from node_i to node_j
	private double[][] cost;
	
	// the quantity of vehicles
	private int vehicles;
	
	// the capacity of the uniform float of vehicles
	private double capacity;
	
	// if we duplicate de origin node
	private boolean duplicateOrigin;
	
	// default constructor
	public EspprcInstance() {
		super();
	}
	
	// ===== PREPROCESSING NODES =====
	
	public void buildCosts() {
		// We introduce a cost factor to model time costs
		double costFactor = 1;
        
		int nbNodes = this.getNodes().length;
        this.cost = new double[nbNodes][nbNodes];
        for(int i=0; i < nbNodes; i++) {
            for(int j=0; j < nbNodes; j++) {
                this.cost[i][j] = this.getNodes()[i].distance(this.getNodes()[j]) * costFactor;
            }
        }
	}
	
	@SuppressWarnings("unchecked")
	public void buildSuccessors() {
		this.successors = new ArrayList[nodes.length];
		
		for(int i = 0; i < this.getNodes().length; i++) {
			Customer node = this.getNodes()[i];
			this.successors[i] = new ArrayList<Customer>();
		
			// We check every node to see if it is a valid successor
			for(int n = 1; n < this.getNodes().length; n++) {
				Customer nextCustomer = this.getNodes()[n];
				
				// We compute the time needed to reach the node which corresponds to
				// the minimal time to complete the service in the current node + the time needed to get to the next node
				double timeToReach = node.getStart() + node.getServiceTime() + this.cost[node.getCustomerId()][nextCustomer.getCustomerId()];
				
				// Check if it is possible
				if( nextCustomer.getEnd() >= timeToReach ) {
					this.successors[i].add(nextCustomer);
				}
			}
		}
		
		if(this.duplicateOrigin) {
			this.successors[this.successors.length-1] = new ArrayList<Customer>();
		}
	}
	
	// ===== GETTERS & SETTERS =====
	
	public Customer[] getNodes() {
		return nodes;
	}

	public void setNodes(Customer[] nodes) {
		this.nodes = nodes;
	}
	
	public int getVehicles() {
		return vehicles;
	}

	public void setVehicles(int vehicles) {
		this.vehicles = vehicles;
	}

	public double[][] getCost() {
		return cost;
	}

	public void setCost(double[][] cost) {
		this.cost = cost;
	}

	public double getCapacity() {
		return capacity;
	}

	public void setCapacity(double capacity) {
		this.capacity = capacity;
	}
	
	public ArrayList<Customer>[] getSuccessors() {
		return successors;
	}

	public void setSuccessors(ArrayList<Customer>[] successors) {
		this.successors = successors;
	}
	
	public boolean isDuplicateOrigin() {
		return duplicateOrigin;
	}

	public void setDuplicateOrigin(boolean duplicateOrigin) {
		this.duplicateOrigin = duplicateOrigin;
	}	
	
	/**
	 *  Corresponds to the algorithm described in (Feillet D, 2004) section 4.4
	 * @return
	 */
	public ArrayList<Label>[] genFeasibleRoutes() {
		
		// Initialization
		@SuppressWarnings("unchecked")
		ArrayList<Label>[] labels = new ArrayList[this.getNodes().length];
		
		// Origin node
		Label originLabel = new Label( this.getNodes()[0]);
		
		int[] originUnreachableVector = new int[this.getNodes().length];
		originUnreachableVector[0] = 1;
		originLabel.setUnreachableNodes(originUnreachableVector);
		
		int[] originVisitationVector = new int[this.getNodes().length];
		originVisitationVector[0] = 1;
		originLabel.setVisitationVector( originVisitationVector );
		
		originLabel.addResource("Time");
		originLabel.setResource("Capacity", this.capacity);
		
		labels[0] = new ArrayList<Label>();
		labels[0].add( originLabel );
		
		// Customer labels
		for(int i = 1; i < this.getNodes().length; i++) {
			labels[i] = new ArrayList<Label>();
		}
		
		// Customers waiting to be treated
		ArrayList<Customer> E = new ArrayList<Customer>();
		E.add(this.getNodes()[0]);
		do {
			// Exploration of the successors of a node
			Customer currentNode = E.get(0);
//			System.out.println("DEBUG: current node: "+currentNode.getCustomerId()+" "+currentNode);
			
			ArrayList<Customer> nodeSuccessors = this.successors[currentNode.getCustomerId()];
			for(int n = 0; n < nodeSuccessors.size() ; n++) {
				// NOTE: n is NOT the index of the node
				// instead, use getCustomerId() function
				Customer currentSuccessor = nodeSuccessors.get(n);
				
				// Set of labels extended from i to j
				ArrayList<Label> extendedLabels = new ArrayList<Label>();
				
				// We extend all currentNode labels
				for(int l = 0; l < labels[currentNode.getCustomerId()].size(); l++) {
					Label currentLabel = labels[currentNode.getCustomerId()].get(l);
					
					int[] unreachableVector = currentLabel.getUnreachableNodes();
					if(unreachableVector[currentSuccessor.getCustomerId()] == 0) {
						Label ext = this.extendLabel(currentLabel, currentSuccessor);
						extendedLabels.add(ext);
					}
				}
				
				//
				ArrayList<Label> successorLabels = labels[currentSuccessor.getCustomerId()];
				
				EFF resultEFF = null;
				if(this.duplicateOrigin && currentSuccessor.getCustomerId() == this.nodes.length-1) {
					successorLabels.addAll(extendedLabels);
					resultEFF = new EFF(successorLabels, false);
				}
				else {
//					resultEFF = this.dummyEFF(successorLabels, extendedLabels);
					
//					resultEFF = this.EFF3(successorLabels, extendedLabels);
	
					resultEFF = this.EFF2(successorLabels, extendedLabels);
					
//					resultEFF = this.EFF1(successorLabels, extendedLabels);
				}
				
				labels[currentSuccessor.getCustomerId()] = resultEFF.getLabels();
				
				// End EFF
				if(resultEFF.isHasChanged() && !E.contains(currentSuccessor)) {
					E.add(currentSuccessor);
				}
			}
						
			// Reduction of E
			E.remove(currentNode);
			
		}while( !E.isEmpty() );
		
		return labels;
		
	}
	
	/*
	 * EFF class to return the results of the dominance check procedure
	 */
	private class EFF {
		ArrayList<Label> labels;
		boolean hasChanged;
		
		public EFF(ArrayList<Label> labels, boolean hasChanged) {
			this.labels = labels;
			this.hasChanged = hasChanged;
		}
		
		public ArrayList<Label> getLabels() {
			return this.labels;
		}
		
		public boolean isHasChanged() {
			return this.hasChanged;
		}	
	}
	
	/** The following function corresponds to the EEF method presented in (Feillet D, 2004)
	 * 
	 * @param successorLabels
	 * @param extendedLabels
	 * @return
	 */
	@SuppressWarnings("unused")
	private EFF EFF1(ArrayList<Label> successorLabels, ArrayList<Label> extendedLabels) {

		// Flag to see if the successor labels have changed
		boolean hasChanged = false;
		
		// Check if labels we have by far are dominated
		// by the extended labels and viceversa
		for(int l=0; l < successorLabels.size(); l++) {
			for(int f=0; f < extendedLabels.size(); f++) {
				successorLabels.get(l).checkDominance(extendedLabels.get(f));
			}
		}
		// Check dominance among extended labels
		for(int f=0; f < extendedLabels.size(); f++) {
			for(int g=f+1; g < extendedLabels.size(); g++) {
				extendedLabels.get(f).checkDominance(extendedLabels.get(g));
			}
		}
		
		// We merge label and extended label lists
		ArrayList<Label> newLabels = new ArrayList<Label>();
		for(int l=0; l < successorLabels.size(); l++) {
			if(successorLabels.get(l).isDominated()) {
				hasChanged = true;
			}
			else {
				newLabels.add(successorLabels.get(l));
			}
		}
		
		for(int f=0; f < extendedLabels.size(); f++) {
			if(!extendedLabels.get(f).isDominated()) {
				hasChanged = true;
				newLabels.add(extendedLabels.get(f));
			}
		}
		
		return new EFF(newLabels, hasChanged);
		
	}
	
	/**
	 * Another implementation of the EFF procedure
	 * 
	 * @param successorLabels
	 * @param extendedLabels
	 * @return
	 */
	public EFF EFF2(ArrayList<Label> successorLabels, ArrayList<Label> extendedLabels) {
		// Flag to see if the successor labels have changed
		boolean hasChanged = false;
		
		// Check if labels we have by far are dominated
		// by the extended labels and viceversa
		int removedLabels = 0;
		for(int l=0; l < successorLabels.size(); l++) {
			int removedExtendedLabels = 0;
			
			for(int f=0; f < extendedLabels.size(); f++) {
				successorLabels.get(l-removedLabels).checkDominance(extendedLabels.get(f-removedExtendedLabels));
				if( extendedLabels.get(f-removedExtendedLabels).isDominated() ) {
					// if the extended label is dominated by the current label
					// we remove the extended label
					extendedLabels.remove(f-removedExtendedLabels);
					removedExtendedLabels++;
				}
				if( successorLabels.get(l-removedLabels).isDominated() ){
					// if the current label is dominated by an extended label
					// we remove the current label and break the inner for statement
					successorLabels.remove(l-removedLabels);
					removedLabels++;
					hasChanged = true;
					break;
				}
			}
		}
		
		// Check dominance among extended labels
		int removedBefore = 0;
		for(int f=0; f < extendedLabels.size(); f++) {
			int removedAfter = 0;
			
			for(int g=f-removedBefore+1; g < extendedLabels.size(); g++) {
				extendedLabels.get(f-removedBefore).checkDominance(extendedLabels.get(g-removedAfter));
				if( extendedLabels.get(g-removedAfter).isDominated() ) {
					extendedLabels.remove(g-removedAfter);
					removedAfter++;
				}
				if( extendedLabels.get(g-removedAfter).isDominated() ) {
					extendedLabels.remove(f-removedBefore);
					removedBefore++;
					removedAfter++;
					break;
				}
			}
		}
		
		if(!extendedLabels.isEmpty()) {
			hasChanged = true;
			successorLabels.addAll(extendedLabels);
		}
		
		return new EFF(successorLabels, hasChanged);
	}
	/**
	 * 
	 * @param successorLabels
	 * @param extendedLabels
	 * @return
	 */
	@SuppressWarnings("unused")
	private EFF EFF3(ArrayList<Label> successorLabels, ArrayList<Label> extendedLabels) {
		boolean hasChanged = false;
		int pivot = successorLabels.size();
		
		successorLabels.addAll(extendedLabels);
		
		int removedBefore = 0;
		for(int i = 0; i < successorLabels.size(); i++) {
			int removedAfter = 0;
			for(int j = i-removedBefore+1; j < successorLabels.size(); j++) {
				if ( successorLabels.get(i-removedBefore).checkDominance(successorLabels.get(j-removedAfter)) ) {
					if( !hasChanged && j-removedAfter < pivot ) {
						hasChanged = true;
					}
					
					successorLabels.remove(j-removedAfter);
					removedAfter++;
				}
				if( successorLabels.get(i-removedBefore).isDominated() ) {
					if( !hasChanged && i-removedBefore < pivot ) {
						hasChanged = true;
					}

					successorLabels.remove(i-removedBefore);
					removedAfter++;
					removedBefore++;
				}
			}
		}
		
		if(!hasChanged && successorLabels.size() != pivot) {
			hasChanged = true;
		}
		
		return new EFF(successorLabels, hasChanged);
	}
	
	@SuppressWarnings("unused")
	private EFF dummyEFF(ArrayList<Label> successorLabels, ArrayList<Label> extendedLabels) {
		boolean hasChanged = false;
		
		successorLabels.addAll(extendedLabels);
		
		if(!extendedLabels.isEmpty()) {
			hasChanged = true;
		}
		
		return new EFF(successorLabels, hasChanged);
	}

	/**
	 * Extends the current label to an adjacent node
	 * @param currentLabel
	 * @param currentSuccessor
	 * @return
	 */
	private Label extendLabel(Label currentLabel, Customer currentSuccessor) {
		
		Customer currentNode = currentLabel.getCurrent();
		double arcCost = this.cost[currentNode.getCustomerId()][currentSuccessor.getCustomerId()];
		
		// We create a new label on the node successor
		Label extendedLabel = new Label( currentSuccessor );
		
		// We stock the previous label
		extendedLabel.setPreviousLabel( currentLabel );
		
		// We add the cost
		extendedLabel.setCost( currentLabel.getCost() + arcCost );
		
		// We add the resources of the label, considering we cannot visit the customer before the start time
		if( currentSuccessor.getStart() > currentLabel.getResources() + arcCost ) {
			extendedLabel.setResources( currentSuccessor.getStart() + currentSuccessor.getServiceTime() );
			extendedLabel.setResource( "Time", currentSuccessor.getStart() + currentSuccessor.getServiceTime() );
		}
		else {
			extendedLabel.setResources( currentLabel.getResources() + arcCost + currentSuccessor.getServiceTime() );
			extendedLabel.setResource( "Time", currentLabel.getResources() + arcCost + currentSuccessor.getServiceTime() );
		}
		extendedLabel.setResource( "Capacity", currentLabel.getResource("Capacity") - currentSuccessor.getDemand());
		
		// We update the visitation vector
		int[] extendedVisitationVector = currentLabel.getVisitationVector().clone();
		extendedVisitationVector[currentSuccessor.getCustomerId()] = 1;
		extendedLabel.setVisitationVector( extendedVisitationVector );
		extendedLabel.setNbVisitedNodes( currentLabel.getNbVisitedNodes() + 1 );
		
		// We update unreachable nodes
		int[] extendedUnreachableNodes = currentLabel.getUnreachableNodes().clone();
		extendedUnreachableNodes[currentSuccessor.getCustomerId()] = 1;
		
		for(int i = 0; i < this.nodes.length; i++ ) {
			if ( extendedUnreachableNodes[i] != 1 &&
					this.nodes[i].getEnd() < extendedLabel.getResources() + this.cost[currentSuccessor.getCustomerId()][i] ) {
				extendedUnreachableNodes[i] = 1;
			}
		}
		
		
		extendedLabel.setUnreachableNodes( extendedUnreachableNodes );
		extendedLabel.setNbUnreachableNodes( IntStream.of(extendedUnreachableNodes).sum() );
		
//		System.out.println("Total unreachable nodes "+extendedLabel.getNbUnreachableNodes());
		
		return extendedLabel;
	}
}
