package main;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.github.davidmoten.rtree.*;
import com.github.davidmoten.rtree.geometry.*;

public class RTreeIndexer {

	public static void visualize(RTree<Integer, Point> rtree) {
		final int WIDTH = 800;
		final int HEIGHT = 800;
		final int MARGIN = 50;
		JFrame frame = new JFrame("RTree Visualization");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel panel = new JPanel() {
			@Override
			protected void paintComponent(java.awt.Graphics g) {
				super.paintComponent(g);
				Graphics2D g2d = (Graphics2D) g;
				g2d.setColor(Color.BLACK);
				g2d.setBackground(Color.WHITE);
				rtree.entries().subscribe(entry -> {
					Point point = entry.geometry();
					g2d.draw(new Ellipse2D.Double(point.x() - 2, point.y() - 2, 4, 4));
				});
			}
		};
		panel.setPreferredSize(new java.awt.Dimension(WIDTH, HEIGHT));
		frame.add(panel);
		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args) throws Exception {
		// URL of the dataset
		String datasetUrl = "https://www.cs.utah.edu/~lifeifei/research/tpq/cal.cnode";

		// Create a new RTree
		RTree<Integer, Point> tree = RTree.create();

		// Read in the data from the URL
		List<String> data = new BufferedReader(new InputStreamReader(new URL(datasetUrl).openStream())).lines()
				.collect(Collectors.toList());

		// Iterate through the data and add each node to the R-tree
		for (int i = 0; i < data.size(); i++) {
			String[] parts = data.get(i).split(" ");
			int id = Integer.parseInt(parts[0]);
			double x = Double.parseDouble(parts[1]);
			double y = Double.parseDouble(parts[2]);
			tree = tree.add(id, Geometries.point(x, y));
		}

		// Print all the entries in the tree
//		for (Entry<Integer, Point> entry : tree.entries().toBlocking().toIterable()) {
//			System.out.println("Vertex ID: " + entry.value() + ", Coordinates: (" + entry.geometry().x() + ", "
//					+ entry.geometry().y() + ")");
//		}

		int size = tree.size();
		System.out.println("Number of entries in RTree: " + size);

		// Visualize the RTree
		// visualize(tree);

		tree.visualize(800, 800).save("myTree.png");

	}
}
