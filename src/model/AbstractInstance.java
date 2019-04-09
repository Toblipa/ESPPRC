package model;

public abstract class AbstractInstance {

	private AbstractNode[] nodes;

    private int[][] adjacency;
    
    public AbstractInstance() {
    }
    public AbstractInstance(AbstractNode[] nodes, int[][] adjacency) {
		this.nodes = nodes;
		this.adjacency = adjacency;
	}

	public AbstractNode[] getNodes() {
		return nodes;
	}

	public void setNodes(AbstractNode[] nodes) {
		this.nodes = nodes;
	}

	public int[][] getAdjacency() {
		return adjacency;
	}

	public void setAdjacency(int[][] adjacency) {
		this.adjacency = adjacency;
	}

}
