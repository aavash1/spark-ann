package main;

import java.util.ArrayList;
import java.util.Map;

import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Line;
import com.github.davidmoten.rtree.geometry.Point;

import framework.CoreGraph;
import framework.Node;
import framework.UtilsManagement;

public class RTreeVisualizer2D {

//	private static final int WIDTH = 800;
//	private static final int HEIGHT = 600;
//	private static final int MARGIN = 50;
//
//	public static void visualize(RTree<Integer, Line> rtree) {
//		JFrame frame = new JFrame("RTree Visualization");
//		JPanel panel = new JPanel() {
//			@Override
//			protected void paintComponent(java.awt.Graphics g) {
//				super.paintComponent(g);
//				Graphics2D g2d = (Graphics2D) g;
//				g2d.setColor(Color.BLACK);
//				g2d.setBackground(Color.WHITE);
//				rtree.entries().subscribe(entry -> {
//					Line line = entry.geometry();
//					g2d.draw(new Line2D.Double(line.x1(), line.y1(), line.x2(), line.y2()));
//				});
//			}
//		};
//		panel.setPreferredSize(new java.awt.Dimension(WIDTH, HEIGHT));
//		frame.add(panel);
//		frame.pack();
//		frame.setVisible(true);
//	}

	public static void main(String[] args) {

		String nodeDatasetFile = "GraphDatasets/California_Nodes.txt";
		String edgeDataSetFile = "GraphDatasets/California_Edges.txt";

		CoreGraph cGraph = UtilsManagement.readEdgeTxtFileReturnGraph(edgeDataSetFile);
		ArrayList<Node> nodesList = UtilsManagement.readTxtNodeFile(nodeDatasetFile);
		Map<Integer, Node> nodesMap = UtilsManagement.readTxtNodeFileReturnMap(nodeDatasetFile);
		cGraph.setNodesWithInfo(nodesList);
		cGraph.setNodesWithInfoMap(nodesMap);
		// Create an RTree with some test data

		RTree<Integer, Point> rtree = RTree.create();

		for (Node vertex : cGraph.getNodesWithInfo()) {
			rtree = rtree.add(vertex.getNodeId(), Geometries.point(vertex.getLongitude(), vertex.getLatitude()));
		}

		// Print the entries
		for (Entry<Integer, com.github.davidmoten.rtree.geometry.Point> entry : rtree.entries().toBlocking()
				.toIterable()) {
			// System.out.println("Vertex ID: " + entry.value() + ", Coordinates: X: " +
			// entry.geometry().x() + ", Y: "
			// + entry.geometry().y());
		}

		int size = rtree.size();
		// System.out.println("Number of entries in RTree: " + size);

		RTree<Integer, Line> rtree2 = RTree.create();

		for (int i = 0; i < cGraph.getEdgesWithInfo().size(); i++) {
			int id = cGraph.getEdgesWithInfo().get(i).getEdgeId();
			double x1 = cGraph.getNodeFromMap(cGraph.getStartNodeIdOfEdge(id)).getLongitude();
			double y1 = cGraph.getNodeFromMap(cGraph.getStartNodeIdOfEdge(id)).getLatitude();
			double x2 = cGraph.getNodeFromMap(cGraph.getEndNodeIdOfEdge(id)).getLongitude();
			double y2 = cGraph.getNodeFromMap(cGraph.getEndNodeIdOfEdge(id)).getLatitude();
			rtree2 = rtree2.add(id, Geometries.line(x1, y1, x2, y2));
		}

		// System.out.println(rtree2.size());

//		rtree2.visualize(4000, 4000).save("newTreeWithEdge.png");

		// rtree.visualize(400, 400).save("newViz.png");
	}
}
