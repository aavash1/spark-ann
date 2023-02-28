package algorithm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import framework.CoreGraph;
import framework.Node;
import framework.RoadObject;
import framework.cEdge;
import scala.Tuple2;
import scala.Tuple3;

public class RangeQueryAlgorithm implements Serializable {

	private static CoreGraph cGraph;

	private static Map<Integer, Integer> vertexIdPartitionIndexMap;

	public RangeQueryAlgorithm(CoreGraph cGraph, Map<Integer, Integer> vertexIdPartitionIndexMap) {
		this.cGraph = cGraph;
		this.vertexIdPartitionIndexMap = vertexIdPartitionIndexMap;
	}

	public double getHeuristicRange(CoreGraph cGraph, int vertexId, double nearestDistance) {

		// Find adjacent neighboring edges for the input vertex

		List<Integer> adjacentNodes = cGraph.getAdjNodeIds(vertexId);

		// Find the maximum length out of all the adjacent edges
		double maxLength = 0;

		for (Integer adjNodeId : adjacentNodes) {
			double length = cGraph.getEdgeDistance(vertexId, adjNodeId);
			if (length > maxLength) {
				maxLength = length;
			}
		}

		// Count the degree of the selected vertex
		int degree = adjacentNodes.size();

		// Divide the maximum length by the degree
		double heuristicValue = maxLength / degree;

		// Evaluate range query from the input vertex with the evaluated distance
		double rangeQueryDistance = heuristicValue + nearestDistance;
		// TODO: Implement range query evaluation

		return rangeQueryDistance;

	}

	public List<Tuple3<Integer, Integer, Double>> returnQueryWithinRange(int selectedVertex, double rangeDistance) {
		List<Tuple3<Integer, Integer, Double>> queryObjectsWithinRange = new ArrayList<>();
		Set<Integer> visited = new HashSet<>();
		Queue<Tuple2<Integer, Double>> queue = new LinkedList<>();

		visited.add(selectedVertex);
		queue.offer(new Tuple2<>(selectedVertex, 0.0));
		while (!queue.isEmpty()) {
			Tuple2<Integer, Double> current = queue.poll();
			int currentNode = current._1;
			double currentDistance = current._2;
			for (int adjacentNode : cGraph.getAdjNodeIds(currentNode)) {
				double edgeLength = cGraph.getEdgeDistance(currentNode, adjacentNode);
				if (edgeLength + currentDistance <= rangeDistance && !visited.contains(adjacentNode)) {
					visited.add(adjacentNode);
					if (edgeLength < rangeDistance) {
						RoadObject robj = cGraph.getRoadObjectOnEdge(currentNode, adjacentNode);
						if (robj != null && robj.getType() != false) {
							queryObjectsWithinRange
									.add(new Tuple3<>(currentNode, robj.getObjectId(), edgeLength + currentDistance));
						}
					}
					queue.offer(new Tuple2<>(adjacentNode, edgeLength + currentDistance));
				}
			}
		}
		return queryObjectsWithinRange;
	}

}
