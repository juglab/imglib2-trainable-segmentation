
package net.imglib2.trainable_segmentation.gpu.random_forest;

import net.imglib2.trainable_segmentation.random_forest.TransparentRandomTree;
import weka.core.Instance;

public class GpuRandomTreePrediction {

	final int numberOfNodes;
	final int numberOfLeafs;
	private int nodeCount = 0;
	private int leafCount = 0;
	final int[] attributeIndicies;
	final double[] threshold;
	final int[] smallerChild;
	final int[] biggerChild;
	final double[][] classProbabilities;

	public GpuRandomTreePrediction(TransparentRandomTree tree) {
		if (tree.isLeaf()) {
			this.numberOfLeafs = 1;
			this.numberOfNodes = 1;
			this.attributeIndicies = new int[] { 0 };
			this.threshold = new double[] { 0.0 };
			this.smallerChild = new int[] { Short.MIN_VALUE };
			this.biggerChild = new int[] { Short.MIN_VALUE };
			this.classProbabilities = new double[][] { tree.classProbabilities() };
		}
		else {
			this.numberOfLeafs = countLeafs(tree);
			this.numberOfNodes = countNodes(tree);
			this.attributeIndicies = new int[numberOfNodes];
			this.threshold = new double[numberOfNodes];
			this.smallerChild = new int[numberOfNodes];
			this.biggerChild = new int[numberOfNodes];
			this.classProbabilities = new double[numberOfLeafs][];
			addTree(tree);
		}
	}

	private int countNodes(TransparentRandomTree node) {
		return node.isLeaf() ? 0 : 1 + countNodes(node.smallerChild()) + countNodes(node
			.biggerChild());
	}

	private int countLeafs(TransparentRandomTree node) {
		return node.isLeaf() ? 1 : countLeafs(node.smallerChild()) + countLeafs(node
			.biggerChild());
	}

	int addTree(TransparentRandomTree node) {
		return node.isLeaf() ? addLeaf(node) : addNode(node);
	}

	private int addNode(TransparentRandomTree node) {
		int i = nodeCount++;
		attributeIndicies[i] = node.attributeIndex();
		threshold[i] = node.threshold();
		smallerChild[i] = addTree(node.smallerChild());
		biggerChild[i] = addTree(node.biggerChild());
		return i;
	}

	private int addLeaf(TransparentRandomTree node) {
		int i = leafCount++;
		if (i >= classProbabilities.length)
			throw new AssertionError();
		classProbabilities[i] = node.classProbabilities();
		return i + Short.MIN_VALUE;
	}

	public double[] distributionForInstance(Instance instance) {
		int nodeIndex = 0;
		while (nodeIndex >= 0) {
			int attributeIndex = attributeIndicies[nodeIndex];
			double attributeValue = instance.value(attributeIndex);
			nodeIndex = (attributeValue < threshold[nodeIndex]) ? smallerChild[nodeIndex]
				: biggerChild[nodeIndex];
		}
		int leafIndex = nodeIndex - Short.MIN_VALUE;
		return classProbabilities[leafIndex];
	}
}
