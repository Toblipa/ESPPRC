package model;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Random;

import model.Label.DominanceResult;

public class EspprcInstance {
	
	// the nodes
	private Customer[] nodes;
	
	// the successors to every node
	private ArrayList<Customer>[] successors;
	
	// the cost to go from node_i to node_j
	private double[][] cost;
	
	// the time to go from node_i to node_j
	private double[][] distance;
	
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
	
	/**
	 * Funtion to stock the arc costs in a matrix
	 * It ramdomly generates negative costs for the arcs
	 */
	public void buildArcs() {
		int max = 20;
		int min = 0;
		Random rand = new Random(0);
		
		// We introduce a cost factor to model time costs
		double costFactor = 1;
		double timeFactor = 1;
        
		int nbNodes = this.getNodes().length;
        this.cost = new double[nbNodes][nbNodes];
        this.distance = new double[nbNodes][nbNodes];
        for(int i=0; i < nbNodes; i++) {
            for(int j=0; j < nbNodes; j++) {
            	if(i != j && i != nbNodes-1) {
            		double euclidianDistance = this.getNodes()[i].distance(this.getNodes()[j]);
            		int randomInt = rand.nextInt(max - min + 1) + min;
                	this.cost[i][j] = ( euclidianDistance * costFactor) - randomInt;
                    this.distance[i][j] = euclidianDistance * timeFactor;
            	}
            	else {
            		this.cost[i][j] = 0;
            	}
            }
        }
        
        this.cost[0][nbNodes-1] = 0;
	}
	
	/**
	 * Function to see which arcs are allowed or forbiden
	 */
	@SuppressWarnings("unchecked")
	public void buildSuccessors() {
		this.successors = new ArrayList[nodes.length];
		
		for(int i = 0; i < this.getNodes().length; i++) {
			Customer node = this.getNodes()[i];
			ArrayList<Customer> sux = new ArrayList<Customer>();

			this.successors[i] = new ArrayList<Customer>();
		
			// We check every node to see if it is a valid successor
			for(int n = 1; n < this.getNodes().length; n++) {
				Customer nextNode = this.getNodes()[n];
				
				// We compute the time needed to reach the node which corresponds to
				// the minimal time to complete the service in the current node + the time needed to get to the next node
				double timeToReach = node.getStart() + node.getServiceTime() + this.distance[node.getCustomerId()][nextNode.getCustomerId()];
				
				// Check if it is possible
				if( i != n && nextNode.getEnd() >= timeToReach ) {
					this.successors[i].add(nextNode);
					sux.add(nextNode);
				}
			}
		}
		
		if(this.duplicateOrigin) {
			this.successors[this.successors.length-1] = new ArrayList<Customer>();
			this.successors[0].remove( this.successors[0].size()-1 );
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

	public double[][] getDistance() {
		return distance;
	}

	public void setDistance(double[][] distance) {
		this.distance = distance;
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
	 *  
	 * @return
	 */
	public ArrayList<Label>[] genFeasibleRoutes() {
		
		// Initialization
		@SuppressWarnings("unchecked")
		ArrayList<Label>[] labels = new ArrayList[this.getNodes().length];
		
		// Origin node
		Label originLabel = new Label( this.getNodes()[0]);
		
		BitSet originUnreachableVector = new BitSet();
		originUnreachableVector.set(0);
		originLabel.setUnreachableNodes(originUnreachableVector);
		
		int[] originVisitationVector = new int[this.getNodes().length];
		originVisitationVector[0] = 1;
		originLabel.setVisitationVector( originVisitationVector );
		
//		originLabel.addResource("Time");
//		originLabel.addResource("Capacity");
		
		labels[0] = new ArrayList<Label>();
		labels[0].add( originLabel );
		
		// Customer labels
		for(int i = 1; i < this.getNodes().length; i++) {
			labels[i] = new ArrayList<Label>();
		}
		
		// Customers waiting to be treated
		ArrayList<Customer> E = new ArrayList<Customer>();
		E.add(this.getNodes()[0]);
//		int itNumber = 0;
		
		// TODO clean
		long timeSpentEFF = 0;
		long timeSpentExtending = 0;
		
		do {
			// Debug puposes
//			this.displayE(E, itNumber);
			
			// Exploration of the successors of a node
			Customer currentNode = E.get(0);
			
			ArrayList<Customer> nodeSuccessors = this.successors[currentNode.getCustomerId()];
			for(int n = 0; n < nodeSuccessors.size() ; n++) {
				// NOTE: n is NOT the index of the node
				// instead, use getCustomerId() function
				Customer currentSuccessor = nodeSuccessors.get(n);
				
				// Set of labels extended from i to j
				ArrayList<Label> extendedLabels = new ArrayList<Label>();
				
				// We extend all currentNode labels
				long startTimeExtending = System.nanoTime(); // TODO clean
				for(int l = 0; l < labels[currentNode.getCustomerId()].size(); l++) {
					Label currentLabel = labels[currentNode.getCustomerId()].get(l);
					
					BitSet unreachableVector = currentLabel.getUnreachableNodes();
					if( !unreachableVector.get(currentSuccessor.getCustomerId()) ) {
						Label ext = this.extendLabel(currentLabel, currentSuccessor);
						extendedLabels.add(ext);
					}
				}
				long endTimeExtending = System.nanoTime(); // TODO clean
				timeSpentExtending += (endTimeExtending - startTimeExtending); // TODO clean
				
				ArrayList<Label> successorLabels = labels[currentSuccessor.getCustomerId()];

//				EFF resultEFF = this.dummyEFF(successorLabels, extendedLabels);
				
				long startTimeEFF = System.nanoTime(); // TODO clean
//				EFF resultEFF = this.methodEFF(successorLabels, extendedLabels);
//				EFF resultEFF = this.methodEFF2(successorLabels, extendedLabels);
				EFF resultEFF = this.methodEFF3(successorLabels, extendedLabels);
				
				long endTimeEFF = System.nanoTime(); // TODO clean
				timeSpentEFF += (endTimeEFF - startTimeEFF); // TODO clean
				
				labels[currentSuccessor.getCustomerId()] = resultEFF.getLabels();

				// End EFF
				if(resultEFF.isHasChanged() && !E.contains(currentSuccessor)) {
					E.add(currentSuccessor);
				}
			}
			
			// Reduction of E
//			E.remove(currentNode);
			E.remove(0);
			
//			itNumber ++;
		}while( !E.isEmpty() );
		
		System.out.println("> Time Spent EFF: "+(timeSpentEFF/1000000));
		System.out.println("> Time Spent Extending Labels: "+(timeSpentExtending/1000000));

		return labels;
	}

	/*
	 * EFF class to return the results of the dominance check procedure
	 */
	private class EFF {
		private ArrayList<Label> labels;
		private boolean hasChanged;
		private long timeSpentExtended;
		private long timeSpentCurrent;
		
		public EFF(ArrayList<Label> labels, boolean hasChanged) {
			this.labels = labels;
			this.hasChanged = hasChanged;
		}
		
		public EFF(ArrayList<Label> labels, boolean hasChanged, long timeSpentExtended, long timeSpentCurrent) {
			this.labels = labels;
			this.hasChanged = hasChanged;
			this.timeSpentExtended = timeSpentExtended;
			this.timeSpentCurrent = timeSpentCurrent;
		}
		
		public ArrayList<Label> getLabels() {
			return this.labels;
		}
		
		public boolean isHasChanged() {
			return this.hasChanged;
		}
	}
	
	/**
	 * The following function corresponds to the EEF method presented in (Feillet D, 2004)
	 * 
	 * @param successorLabels
	 * @param extendedLabels
	 * @return
	 */
	public EFF methodEFF(ArrayList<Label> successorLabels, ArrayList<Label> extendedLabels) {
		// Flag to see if the successor labels have changed
		boolean hasChanged = false;
		
		// Check if labels we have by far are dominated
		// by the extended labels and viceversa
		int removedLabels = 0;
		
		long timeSpentComparing = 0;
		long timeReadingThrough = 0;
		
		for(int l=0; l < successorLabels.size(); l++) {
			int removedExtendedLabels = 0;
			
			for(int f=0; f < extendedLabels.size(); f++) {
				
				long startTimeComparing = System.nanoTime();
				DominanceResult response = successorLabels.get(l-removedLabels).checkDominance(extendedLabels.get(f-removedExtendedLabels));
				long endTimeComparing = System.nanoTime();
				
				timeSpentComparing += (endTimeComparing - startTimeComparing);
				
				boolean equals = response.getEquals();
				timeReadingThrough += response.getTimeElapsed();
				
				if( extendedLabels.get(f-removedExtendedLabels).isDominated() || equals) {
					// if the extended label is dominated by the current label
					// we remove the extended label
					extendedLabels.remove(f-removedExtendedLabels);
					removedExtendedLabels++;
				}
				else if( successorLabels.get(l-removedLabels).isDominated() ){
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
				long startTimeComparing = System.nanoTime();
				DominanceResult result = extendedLabels.get(f-removedBefore).checkDominance(extendedLabels.get(g-removedAfter));
				long endTimeComparing = System.nanoTime();
				timeSpentComparing += (endTimeComparing - startTimeComparing);
				
				timeReadingThrough += result.getTimeElapsed();

				if( extendedLabels.get(g-removedAfter).isDominated() ) {
					extendedLabels.remove(g-removedAfter);
					removedAfter++;
				}
				else if( extendedLabels.get(g-removedAfter).isDominated() ) {
					extendedLabels.remove(f-removedBefore);
					removedBefore++;
					removedAfter++;
					break;
				}
			}
		}
		
		// Check if successor labels have changed
		if(!extendedLabels.isEmpty()) {
			hasChanged = true;
			successorLabels.addAll(extendedLabels);
		}
		
		return new EFF(successorLabels, hasChanged, timeSpentComparing, timeReadingThrough);
	}
	
	/**
	 * The following function corresponds to the EEF method presented in (Feillet D, 2004)
	 * 
	 * @param successorLabels
	 * @param extendedLabels
	 * @return
	 */
	public EFF methodEFF2(ArrayList<Label> successorLabels, ArrayList<Label> extendedLabels) {
		// Flag to see if the successor labels have changed
		boolean hasChanged = false;
		
		ArrayList<Label> resultLabels = new ArrayList<Label>();
		
		// Check if labels we have by far are dominated
		// by the extended labels and viceversa		
		long timeSpentComparing = 0;
		long timeReadingThrough = 0;
		
		for(int l=0; l < successorLabels.size(); l++) {
			Label analyzingLabel = successorLabels.get(l);
			int removedLabels = 0;
			
			for(int f=0; f < extendedLabels.size(); f++) {
				Label extLabel = extendedLabels.get(f-removedLabels);
				if( !extLabel.isDominated() ) {
					
					long startTimeComparing = System.nanoTime();
					DominanceResult response = analyzingLabel.checkDominance(extLabel);
					long endTimeComparing = System.nanoTime();
					
					timeSpentComparing += (endTimeComparing - startTimeComparing);
					timeReadingThrough += response.getTimeElapsed();
					
					boolean equals = response.getEquals();
					
					if( equals ) {
						extendedLabels.remove(f-removedLabels);
					}
					else if(analyzingLabel.isDominated()) {
						break;
					}
				}
			}
			
			if(!analyzingLabel.isDominated()) {
				resultLabels.add(analyzingLabel);
			}
			else if(!hasChanged) {
				hasChanged = true;
			}
		}
		
		// Check dominance among extended labels		
		for(int f=0; f < extendedLabels.size(); f++) {			
			Label extLabel1 = extendedLabels.get(f);
			
			if( extLabel1.isDominated() ) { continue; }
			
			for(int g=f+1; g < extendedLabels.size(); g++) {
				Label extLabel2 = extendedLabels.get(g);
				
				if( !extLabel2.isDominated() ) {
					long startTimeComparing = System.nanoTime();
					DominanceResult response = extLabel1.checkDominance(extLabel2);
					long endTimeComparing = System.nanoTime();
					
					timeSpentComparing += (endTimeComparing - startTimeComparing);
					
					timeReadingThrough += response.getTimeElapsed();
				}else { continue; }
				
				if( extLabel1.isDominated() ) { break; }
			}
			
			if( !extLabel1.isDominated() ) {
				resultLabels.add(extLabel1);
				hasChanged = true;
			}
		}
		
		return new EFF(resultLabels, hasChanged, timeSpentComparing, timeReadingThrough);
	}
	
	private EFF methodEFF3(ArrayList<Label> successorLabels, ArrayList<Label> extendedLabels) {
		// Flag to see if the successor labels have changed
		boolean hasChanged = false;

		ArrayList<Label> resultLabels = new ArrayList<Label>();

		// Check if labels we have by far, are dominated
		// by the extended labels and viceversa		
		long timeSpentComparing = 0;
		long timeReadingThrough = 0;
		for(int l=0; l < successorLabels.size(); l++) {
			Label analyzingLabel = successorLabels.get(l);
			int removedLabels = 0;

			for(int f=0; f < extendedLabels.size(); f++) {
				Label extLabel = extendedLabels.get(f-removedLabels);
				
				// Check if it is not a duplicated label
				if( analyzingLabel.getPreviousLabel() == extLabel.getPreviousLabel() ) {
					extendedLabels.remove(f-removedLabels);
					removedLabels++;
					continue;
				}
				
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

		return new EFF(resultLabels, hasChanged, timeSpentComparing, timeReadingThrough);
	}
	
	/**
	 * Just a dummy function for debug purposes
	 * 
	 * @param successorLabels
	 * @param extendedLabels
	 * @return
	 */
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
	 * 
	 * @param currentLabel
	 * @param currentSuccessor
	 * @return
	 */
	private Label extendLabel(Label currentLabel, Customer currentSuccessor) {
		
		Customer currentNode = currentLabel.getCurrent();
		double arcCost = this.cost[currentNode.getCustomerId()][currentSuccessor.getCustomerId()];
		double arcDistance = this.distance[currentNode.getCustomerId()][currentSuccessor.getCustomerId()];
		
		// We create a new label on the node successor
		Label extendedLabel = new Label( currentSuccessor );
		
		// We stock the previous label
		extendedLabel.setPreviousLabel( currentLabel );
		
		// We add the cost
		extendedLabel.setCost( currentLabel.getCost() + arcCost );
		
		// We add the resources of the label, considering we cannot visit the customer before the start time
		if( currentSuccessor.getStart() > currentLabel.getResource(0) + currentNode.getServiceTime() + arcDistance ) {
			extendedLabel.setResource( 0, currentSuccessor.getStart() );
		}
		else {
			extendedLabel.setResource( 0, currentLabel.getResource(0) + currentNode.getServiceTime() + arcDistance );
		}
		// Add demand resource
		extendedLabel.setResource( 1, currentLabel.getResource(1) + currentSuccessor.getDemand());
		
		// We update the visitation vector
		int[] extendedVisitationVector = currentLabel.getVisitationVector().clone();
		extendedVisitationVector[currentSuccessor.getCustomerId()] = 1;
		extendedLabel.setVisitationVector( extendedVisitationVector );
		extendedLabel.setNbVisitedNodes( currentLabel.getNbVisitedNodes() + 1 );
		
		// We update unreachable nodes
		BitSet currentUnreachableNodes = currentLabel.getUnreachableNodes();
		BitSet extendedUnreachableNodes = new BitSet();
		extendedUnreachableNodes.set(currentSuccessor.getCustomerId());
		
		for(int i = 0; i < this.nodes.length; i++ ) {
			if( currentSuccessor.isDepot() ) {
				extendedUnreachableNodes.set(i);
				continue;
			}
			
			double timeToReach = extendedLabel.getResource(0) + currentSuccessor.getServiceTime() + this.distance[currentSuccessor.getCustomerId()][i];
			if ( currentUnreachableNodes.get(i) ||
					( this.nodes[i].getEnd() < timeToReach ||
							this.capacity < extendedLabel.getResource(1) + this.nodes[i].getDemand()) ) {
				extendedUnreachableNodes.set(i);
			}
		}
		
		extendedLabel.setUnreachableNodes( extendedUnreachableNodes );
		extendedLabel.setNbUnreachableNodes( extendedUnreachableNodes.cardinality() );
				
		return extendedLabel;
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
			System.out.print(E.get(e).getCustomerId()!=6? E.get(e).getCustomerId():"Depot");
			if(e != E.size()-1) System.out.print(", ");
		}
		System.out.println("}");		
	}
}
