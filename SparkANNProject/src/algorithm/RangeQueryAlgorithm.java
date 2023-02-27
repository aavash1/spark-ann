package algorithm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import framework.CoreGraph;
import framework.RoadObject;
import framework.cEdge;

public class RangeQueryAlgorithm implements Serializable {

	private static CoreGraph cGraph;

	private static Map<Integer, Integer> vertexIdPartitionIndexMap;

	public RangeQueryAlgorithm(CoreGraph cGraph, Map<Integer, Integer> vertexIdPartitionIndexMap) {
		this.cGraph = cGraph;
		this.vertexIdPartitionIndexMap = vertexIdPartitionIndexMap;
	}

	public double getHeuristicRange(CoreGraph cGraph, int vertexId, double nearestDistance) {

		// Find adjacent neighboring edges for the input vertex
		List<Integer> adjacentEdges = cGraph.getAdjacencyEdgeIds(vertexId);

		// Find the maximum length out of all the adjacent edges
		double maxLength = 0;
		for (Integer edgeId : adjacentEdges) {
			double length = cGraph.getEdgeDistance(edgeId);
			if (length > maxLength) {
				maxLength = length;
			}
		}

		// Count the degree of the selected vertex
		int degree = adjacentEdges.size();

		// Divide the maximum length by the degree
		double heuristicValue = maxLength / degree;

		// Evaluate range query from the input vertex with the evaluated distance
		double rangeQueryDistance = heuristicValue + nearestDistance;
		// TODO: Implement range query evaluation

		return rangeQueryDistance;

	}

	public List<Integer> returnQueryWithinRange(int selectedVertex, double rangeDistance) {
		List<Integer> queryObjWithinRange = new ArrayList<>();
		Set<Integer> visited = new HashSet<>();
		Queue<Integer> queue = new LinkedList<>();

		visited.add(selectedVertex);
		queue.offer(selectedVertex);
		while (!queue.isEmpty()) {
			int currentNode = queue.poll();
			for (int adjacentNode : cGraph.getAdjNodeIds(currentNode)) {

				if (vertexIdPartitionIndexMap.get(currentNode) == vertexIdPartitionIndexMap.get(adjacentNode)) {
					double edgeLength = cGraph.getEdgeDistance(selectedVertex, adjacentNode);
					if (!visited.contains(adjacentNode) && edgeLength <= rangeDistance) {
						visited.add(adjacentNode);
						queue.offer(adjacentNode);
						if (edgeLength < rangeDistance) {
							RoadObject robj = cGraph.getRoadObjectOnEdge(selectedVertex, currentNode);
							if (robj != null) {
								if (robj.getType() != false) {
									queryObjWithinRange.add(robj.getObjectId());
								}
							}
						}

					}
				}

			}
		}

		return queryObjWithinRange;
	}

}
