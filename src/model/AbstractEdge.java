package model;

public abstract class AbstractEdge {
	
	private AbstractNode start;
	
	private AbstractNode end;
	
	public AbstractNode getStart() {
		return start;
	}

	public void setStart(AbstractNode start) {
		this.start = start;
	}

	public AbstractNode getEnd() {
		return end;
	}

	public void setEnd(AbstractNode end) {
		this.end = end;
	}
}
