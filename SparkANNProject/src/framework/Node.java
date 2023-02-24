package framework;

import java.io.Serializable;
import java.util.Comparator;

public class Node implements Serializable, Comparable<Node> {
	private int m_intNodeId;
	private Double m_doubLatitude;
	private Double m_doubLongitude;
	private Double m_distance;

	public Node(int nodeId, double longitude, double latitude) {
		// TODO Auto-generated constructor stub
		this.setM_intNodeId(nodeId);
		this.m_doubLatitude = longitude;
		this.m_doubLongitude = latitude;
	}

	public Node(int nodeId, double distance) {
		this.setM_intNodeId(nodeId);
		this.m_distance = distance;
	}

	public Node() {

	}

	public Double getM_distance() {
		return this.m_distance;
	}

	public int getNodeId() {
		return getM_intNodeId();
	}

	public void setNodeId(int strNodeId) {
		this.setM_intNodeId(strNodeId);
	}

	public Double getLongitude() {
		return m_doubLongitude;
	}

	public void setLongitude(Double doubLongitude) {
		this.m_doubLongitude = doubLongitude;
	}

	public Double getLatitude() {
		return m_doubLatitude;
	}

	public void setLatitude(Double doubLatitude) {
		this.m_doubLatitude = doubLatitude;
	}

	@Override
	public String toString() {
		return "Node [NodeId=" + getM_intNodeId() + ", " + "Latitude=" + m_doubLatitude + " Longitude="
				+ m_doubLongitude + "]";
	}

	@Override
	public int compareTo(Node o) {
		return Double.compare(m_distance, o.m_distance);
	}

	public int getM_intNodeId() {
		return m_intNodeId;
	}

	public void setM_intNodeId(int m_intNodeId) {
		this.m_intNodeId = m_intNodeId;
	}

}
