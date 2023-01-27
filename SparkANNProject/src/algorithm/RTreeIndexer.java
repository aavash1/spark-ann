package algorithm;

import java.util.Map;

import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Line;
import com.github.davidmoten.rtree.geometry.Point;

import framework.CoreGraph;
import framework.Node;

public class RTreeIndexer {

	public static RTree<Integer, Line> createRTree(CoreGraph graph) {
		RTree<Integer, Line> rtree = RTree.create();

		for (int i = 0; i < graph.getEdgesWithInfo().size(); i++) {
			int id = graph.getEdgesWithInfo().get(i).getEdgeId();
			double x1 = graph.getNodeFromMap(graph.getStartNodeIdOfEdge(id)).getLongitude();
			double y1 = graph.getNodeFromMap(graph.getStartNodeIdOfEdge(id)).getLatitude();
			double x2 = graph.getNodeFromMap(graph.getEndNodeIdOfEdge(id)).getLongitude();
			double y2 = graph.getNodeFromMap(graph.getEndNodeIdOfEdge(id)).getLatitude();
			rtree = rtree.add(id, Geometries.line(x1, y1, x2, y2));
		}
		return rtree;

	}

	public static RTree<Integer, Point> createRTreePoints(CoreGraph graph) {
		RTree<Integer, Point> rtree = RTree.create();

		for (Node vertex : graph.getNodesWithInfo()) {
			rtree = rtree.add(vertex.getNodeId(), Geometries.point(vertex.getLongitude(), vertex.getLatitude()));
		}

		return rtree;
	}

}
