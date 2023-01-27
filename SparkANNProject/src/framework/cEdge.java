package framework;

import java.util.Comparator;
import scala.Serializable;

public class cEdge implements Serializable {

	private int m_intEdgeId;
	private int m_intStartNodeId;
	private int m_intEndNodeId;
	private double m_doubLength;

	public int getEdgeId() {
		return m_intEdgeId;
	}

	public void setEdgeId(int intEdgeId) {
		if (intEdgeId < 0) {
			System.err.println("Error!! Cannot be null");
		} else {
			this.m_intEdgeId = intEdgeId;
		}
	}

	public int getStartNodeId() {
		return m_intStartNodeId;
	}

	public void setStartNodeId(int intSourceId) {
		this.m_intStartNodeId = intSourceId;
	}

	public int getEndNodeId() {
		return m_intEndNodeId;
	}

	public void setEndNodeId(int m_intDestinationId) {
		this.m_intEndNodeId = m_intDestinationId;
	}

	public double getLength() {
		return m_doubLength;
	}

	public void setLength(double doubDistance) {
		if (doubDistance > 0) {
			this.m_doubLength = doubDistance;
		}

	}

	@Override
	public String toString() {
		return "Edge [EdgeId=" + m_intEdgeId + ", Start Node=" + m_intStartNodeId + ", End Node=" + m_intEndNodeId
				+ ", Length=" + m_doubLength + "]";
	}

	public static Comparator<cEdge> DistanceComparator = new Comparator<cEdge>() {
//This is descending order.
		public int compare(cEdge edge1, cEdge edge2) {
			double distanceDiff = edge1.getLength() - edge2.getLength();

			if (distanceDiff > 0)
				return -1;
			if (distanceDiff < 0)
				return 1;
			return 0;
			// return (int) Math.round(distanceDiff);
		}
	};

}
