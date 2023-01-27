package main;

import algorithm.BinarySearchTree;
import algorithm.BinarySearchTree.TreeNode;

import java.util.List;

public class BSTCheck {
	public static void main(String[] args) {
		int[] array = { 5, 3, 7, 1, 4, 6, 8, 33, 67, 90, 76, 87, 99, 132, 145, 890 };

		BinarySearchTree bst = new BinarySearchTree();

		bst.storeArray(array);

		// bst.printInOrderRec();
		if (bst.search(501)) {
			System.out.println("the value was found");
		} else {
			System.out.println("no such value");
		}

		TreeNode node = bst.searchToPrint(501);
		if (node != null) {
			System.out.println(node.getData());
		}

		System.out.println("The bst is printing");

//		RTree<Integer, Point> tree = RTree.create();
//
//		tree = tree.add(1, Geometries.point(1.0, 2.0));
//		tree = tree.add(1, Geometries.point(1.0, 2.0));
//		tree = tree.add(1, Geometries.point(1.0, 2.0));
//
//		// Search for all the entries in the RTree
//		Rectangle bounds = Geometries.rectangle(0, 0, 180, 360);
//		List<Entry<Integer, Point>> entries = tree.search(bounds).toList().toBlocking().single();
//
//		// Print the entries
//		for (Entry<Integer, Point> entry : entries) {
//			System.out.println("Edge ID: " + entry.value() + ", Line: " + entry.geometry());
//		}

	}

}
