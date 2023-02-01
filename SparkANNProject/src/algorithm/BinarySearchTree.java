package algorithm;

import java.io.Serializable;

import scala.collection.immutable.NewRedBlackTree.Tree;

public class BinarySearchTree implements Serializable {

	public static class TreeNode {
		private int data;
		TreeNode left;
		TreeNode right;

		TreeNode(int data) {
			// TODO Auto-generated constructor stub
			this.setData(data);
			this.left = null;
			this.right = null;
		}

		public int getData() {
			return data;
		}

		public void setData(int data) {
			this.data = data;
		}
	}

	private TreeNode root;

	public BinarySearchTree() {
		this.root = null;
	}

	public void insert(int data) {
		root = insert(root, data);
	}

	private TreeNode insert(TreeNode node, int data) {
		// TODO Auto-generated method stub
		if (node == null) {
			return new TreeNode(data);
		}
		if (data < node.getData()) {
			node.left = insert(node.left, data);
		} else {
			node.right = insert(node.right, data);
		}
		return node;
	}

	public void storeArray(int[] array) {
		for (int element : array) {
			insert(element);
		}
	}

	public boolean search(int data) {
		return search(root, data);
	}

	private boolean search(TreeNode node, int data) {
		if (node == null) {
			return false;
		}
		if (node.getData() == data) {
			return true;
		}
		if (data < node.getData()) {
			return search(node.left, data);
		} else if (data > node.getData()) {
			return search(node.right, data);
		}
		// TODO Auto-generated method stub
		return false;
	}

	public TreeNode searchToPrint(int data) {
		return searchToPrint(root, data);
	}

	private TreeNode searchToPrint(TreeNode node, int data) {
		if (node == null) {
			return null;
		}
		if (node.getData() == data) {
			return node;
		}
		if (data < node.getData()) {
			return searchToPrint(node.left, data);
		} else {
			return searchToPrint(node.right, data);
		}
		// TODO Auto-generated method
	}

	public void delete(int data) {
		root = delete(root, data);
	}

	private TreeNode delete(TreeNode node, int data) {
		if (node == null) {
			return null;
		}
		if (data < node.getData()) {
			node.left = delete(node.left, data);
		} else if (data > node.getData()) {
			node.right = delete(node.right, data);
		} else {
			if (node.left == null) {
				return node.right;
			} else if (node.right == null) {
				return node.left;
			} else {
				TreeNode successor = findSuccessor(node.right);
				node.setData(successor.getData());

				node.right = delete(node.right, successor.getData());
			}
		}

		return node;
	}

	private TreeNode findSuccessor(TreeNode node) {
		// TODO Auto-generated method stub
		while (node.left != null) {
			node = node.left;
		}
		return node;
	}

	public void printInOrderRec() {
		printInOrderRec(root);
		System.out.println();
	}

	private void printInOrderRec(TreeNode node) {
		if (node == null) {
			return;
		}
		printInOrderRec(node.left);
		System.out.print(node.getData() + " ");
		printInOrderRec(node.right);
	}

}
