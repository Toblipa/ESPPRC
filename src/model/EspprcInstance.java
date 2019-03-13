package model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class EspprcInstance {
	
	private Customer[] nodes;
	
	// the cost to go from node_i to node_j
	private double[][] cost;
	
	// the quantity of vehicles
	private int vehicles;
	
	// the capacity of the uniform float of vehicles
	private double capacity;
	
	public EspprcInstance() {
		super();
	}
	
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
	
	// Corresponds to the algorithm described in (Feillet D, 2014) section 4.4
	public List<Label>[] genFeasibleRoutes() {
		
		// Initialization
		@SuppressWarnings("unchecked")
		List<Label>[] labels = new List[this.getNodes().length];
		
		// Origin node
		labels[0] = new ArrayList<Label>();
		labels[0].add(new Label());
		
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
			Customer[] nodeSuccessors = this.getSuccessors(currentNode);
			
			for(int n = 0; n < nodeSuccessors.length ; n++) {
				// NOTE: n is NOT the index of the node
				// instead, use getCustomerId() function
				Customer currentSuccessor = nodeSuccessors[n];
				
				// Set of labels extended from i to j
				ArrayList<Label> extendedLabels = new ArrayList<Label>();
				
				// We extend all currentNode labels
				for(int l = 0; l < labels[currentNode.getCustomerId()].size(); l++) {
					Label currentLabel = labels[currentNode.getCustomerId()].get(l);
					int[] unreachableVector = currentLabel.getUnreachableNodes();
					
					if(unreachableVector[currentSuccessor.getCustomerId()] == 0) {
						extendedLabels.add(this.extendLabel(currentLabel, currentSuccessor));
					}
				}
				
				// TODO refactor the EEF method
				// The following code corresponds to the EEF method presented in (Feillet D, 2014)
				List<Label> successorLabels = labels[currentSuccessor.getCustomerId()];
				
				// Flag to see if the successor labels have changed
				boolean hasChanged = false;				
				
				// Check if labels we have by far are dominated
				// by the extended labels and viceversa
				int removedLabels = 0;
				for(int l=0; l < successorLabels.size(); l++) {
					int removedExtendedLabels = 0;
					for(int f=0; f < extendedLabels.size(); f++) {
						if( successorLabels.get(l-removedLabels).dominates(extendedLabels.get(f-removedExtendedLabels)) ) {
							// if the extended label is dominated by the current label
							// we remove the extended label
							extendedLabels.remove(f-removedExtendedLabels);
							removedExtendedLabels++;
						}
						else if( extendedLabels.get(f-removedExtendedLabels).dominates(successorLabels.get(l-removedLabels)) ){
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
				int removed = 0;
				for(int f=0; f < extendedLabels.size(); f++) {
					for(int g=f-removed+1; g < extendedLabels.size(); g++) {
						if( extendedLabels.get(f-removed).dominates(extendedLabels.get(g-removed)) ) {
							extendedLabels.remove(g-removed);
							removed++;
						}
						else if( extendedLabels.get(g-removed).dominates(extendedLabels.get(f-removed)) ) {
							extendedLabels.remove(f-removed);
							removed++;
							break;
						}
					}
				}
				// We merge label and extended label lists
				if(!extendedLabels.isEmpty()) {
					hasChanged = true;
					successorLabels.addAll(extendedLabels);
					labels[currentSuccessor.getCustomerId()] = successorLabels;
				}				
				// End EFF
				
				if(hasChanged) {
					E.add(currentSuccessor);
				}
			}
			
			// Reduction of E
			E.remove(currentNode);
			
		}while( !E.isEmpty() );
		
		return labels;
		
	}

	private ArrayList<Label> getNonDominatedLabels(List<Label> labels, ArrayList<Label> extendedLabels) {
		// TODO Auto-generated method stub
		return null;
	}

	private Label extendLabel(Label currentLabel, Customer currentSuccessor) {
		Customer currentNode = currentLabel.getCurrent();
		double arcCost = this.cost[currentNode.getCustomerId()][currentSuccessor.getCustomerId()];
		
		Label extendedLabel = new Label();
		
		extendedLabel.setPreviousLabel( currentLabel );
		extendedLabel.setCurrent( currentSuccessor );
		extendedLabel.setResources( currentLabel.getResources() + currentSuccessor.getDemand() );
		extendedLabel.setCost( currentLabel.getCost() + arcCost + currentSuccessor.getServiceTime() );
		
		int[] extendedVisitationVector = currentLabel.getVisitationVector();
		extendedVisitationVector[currentSuccessor.getCustomerId()] = 1;
		extendedLabel.setVisitationVector( extendedVisitationVector );
		extendedLabel.setNbVisitedNodes( currentLabel.getNbVisitedNodes() + 1 );
		
		int[] extendedUnreachableNodes = currentLabel.getUnreachableNodes();
		extendedLabel.setUnreachableNodes( extendedUnreachableNodes );
		extendedLabel.setNbUnreachableNodes( IntStream.of(extendedUnreachableNodes).sum() );
		
		return extendedLabel;
	}

	private Customer[] getSuccessors(Customer node) {
		// TODO Auto-generated method stub
		return new Customer[1];
	}	
}
