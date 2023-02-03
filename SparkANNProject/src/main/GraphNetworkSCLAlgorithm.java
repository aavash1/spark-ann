package main;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;

import org.apache.spark.api.java.function.Function3;
import org.apache.spark.api.java.function.PairFlatMapFunction;

import org.apache.spark.api.java.function.VoidFunction;

import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Line;

import algorithm.ANNClusteredOptimizedWithHeuristic;
import algorithm.BinarySearchTree;
import algorithm.ClusteringNodes;
import algorithm.ClusteringRoadObjects;
import algorithm.NearestNeighbor;
import algorithm.RTreeIndexer;
import algorithm.RandomObjectGenerator;
import graph.CustomPartitioner;
import framework.CoreGraph;
import framework.Node;
import framework.RoadObject;
import framework.UtilitiesMgmt;
import framework.UtilsManagement;
import framework.cEdge;

import edu.ufl.cise.bsmock.graph.YenGraph;
import edu.ufl.cise.bsmock.graph.ksp.Yen;
import edu.ufl.cise.bsmock.graph.util.Path;
import scala.Tuple2;
import scala.Tuple3;
import scala.Tuple4;
import scala.Tuple5;
import scala.reflect.ClassTag;

public class GraphNetworkSCLAlgorithm {

	private static Map<Integer, Integer> vertexIdPartitionIndexMap = new HashMap<Integer, Integer>();
	private static Map<Integer, ArrayList<holder>> DARTTableMap = new HashMap<>();

	public static void main(String[] args) throws Exception {
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");

		// Defining tags

		ClassTag<Double> doubleTag = scala.reflect.ClassTag$.MODULE$.apply(Double.class);
		ClassTag<Node> nodeTag = scala.reflect.ClassTag$.MODULE$.apply(Node.class);

		/**
		 * 1 Pass the path for loading the datasets 1.1 Dataset for graph containing
		 * nodes and edges
		 */
		// String nodeDatasetFile = "Dataset/TinygraphNodes.txt";
		// String edgeDataSetFile = "Dataset/TinyGraphEdge.txt";

		String nodeDatasetFile = args[0];
		String edgeDataSetFile = args[1];
		String metisPartitionOutputFile = args[2];
		String partitionValue = args[3];
		String queryObjectSize = args[4];
		String dataObjectSize = args[5];
		String distributionType = args[6];

		if (args.length < 0) {
			System.err.println("No arguements are passed");
		}

//		String nodeDatasetFile = "/home/aavashbhandari/Datasets/California_Nodes.txt";
//		String edgeDataSetFile = "/home/aavashbhandari/Datasets/California_Edges.txt";
//
//		/**
//		 * 1.2 Dataset for METIS graph and Partition Output
//		 */
//		String metisInputGraph = "Metisgraph/ManualGraph.txt";
//		// String metisPartitionOutputFile = "PartitionDataset/tg_part.txt";
//		String metisPartitionOutputFile = "/home/aavashbhandari/Partitionfiles/California_part_4.txt";

		/**
		 * Load Graph using CoreGraph Framework, YenGraph for calculating shortest paths
		 */

		CoreGraph cGraph = UtilsManagement.readEdgeTxtFileReturnGraph(edgeDataSetFile);

		YenGraph yGraph = new YenGraph(edgeDataSetFile);

		/**
		 * Create Vertices List from the nodeDataset
		 */
		ArrayList<Node> nodesList = UtilsManagement.readTxtNodeFile(nodeDatasetFile);
		Map<Integer, Node> nodesMap = UtilsManagement.readTxtNodeFileReturnMap(nodeDatasetFile);
		cGraph.setNodesWithInfo(nodesList);
		cGraph.setNodesWithInfoMap(nodesMap);
		// cGraph.printEdgesInfo();

		/**
		 * Test the YenGraph for finding SPF between two vertex
		 */

		/**
		 * Generate Random Objects on Edge Data Object=100 Query Object=500 Manual Road
		 * Object is also used for testing
		 */

		int distributionValue = Integer.parseInt(distributionType);
		int querySize = Integer.parseInt(queryObjectSize);
		int dataSize = Integer.parseInt(dataObjectSize);
		switch (distributionValue) {
		case 1:
			// Both Q and Q are uniformly distributed
			RandomObjectGenerator.generateUniformRandomObjectsOnMap(cGraph, querySize, dataSize);
			break;
		case 2:
			// <U,C> distribution for Q and D
			RandomObjectGenerator.zgenerateCUUCDistribution(cGraph, 2, 1, querySize, dataSize, false);
			break;

		case 3:
			// <C,U> distribution for Q and D
			RandomObjectGenerator.zgenerateCUUCDistribution(cGraph, 2, 1, querySize, dataSize, true);
			break;

		case 4:
			// Centroid for both Q and D
			RandomObjectGenerator.zgenerateCCDistribution(cGraph, 2, 1, querySize, dataSize);

			break;
		}

//		RandomObjectGenerator.zgenerateCCDistribution(cGraph, 2, 1, Integer.parseInt(queryobjectSize),
//				Integer.parseInt(dataobjectSize));
		// RandomObjectGenerator.generateUniformRandomObjectsOnMap(cGraph, 100, 500);

		// String PCManualObject = "Dataset/manualobject/ManualObjectOnTinyGraph.txt";

		// String PCManualObject = "Dataset/manualobject/ManualObjectsOnRoad.txt";
		// UtilsManagement.readRoadObjectTxtFile1(cGraph, PCManualObject);

		// cGraph.printEdgesInfo();

		/**
		 * Read the output of METIS as partitionFile
		 */
		ArrayList<Integer> graphPartitionIndex = new ArrayList<Integer>();
		graphPartitionIndex = UtilitiesMgmt.readMETISPartition(metisPartitionOutputFile, graphPartitionIndex);
		// Map<Integer, Integer> partitionMap = new HashMap<Integer, Integer>();

		int[] partitionIndexKey = new int[graphPartitionIndex.size()];
		for (int i = 0; i < cGraph.getAdjancencyMap().size(); i++) {

			partitionIndexKey[i] = (int) cGraph.getAdjancencyMap().keySet().toArray()[i];

		}

		/**
		 * This vertexPartitionIndex holds the generic type of IntegerId and It's
		 * respective partiton value
		 * 
		 */
		Map<Object, Object> vertexIdPartitionIndex = new HashMap<Object, Object>();
		// Map<Integer, Integer> vertexIdPartitionIndexMap = new HashMap<Integer,
		// Integer>();
		for (int i = 0; i < graphPartitionIndex.size(); i++) {
			// System.out.println(i + " " + Long.valueOf(graphPartitionIndex.get(i)));
			vertexIdPartitionIndex.put(partitionIndexKey[i], Long.valueOf(graphPartitionIndex.get(i)));
			vertexIdPartitionIndexMap.put(i, graphPartitionIndex.get(i));
		}

		// Depending upon the size of cluster, CustomPartitionSize can be changed
		// int CustomPartitionSize = 4;
		int CustomPartitionSize = Integer.parseInt(partitionValue);
		// int CustomPartitionSize = 2;

		/**
		 * Selecting the Boundaries after graph partitions
		 * 
		 */

		Map<Object, Object> boundaryPairVertices = new HashMap<>();
		LinkedList<String> boundaryVerticesList = new LinkedList<>();
		ArrayList<cEdge> BoundaryEdge = new ArrayList<>();
		ArrayList<Object> BoundaryEdgeId = new ArrayList<>();

		for (cEdge selectedEdge : cGraph.getEdgesWithInfo()) {
			int SrcId = selectedEdge.getStartNodeId();
			int DestId = selectedEdge.getEndNodeId();

			if (vertexIdPartitionIndex.get(SrcId) == vertexIdPartitionIndex.get(DestId)) {

			} else {

				boundaryPairVertices.put(SrcId, vertexIdPartitionIndex.get(SrcId));
				boundaryPairVertices.put(DestId, vertexIdPartitionIndex.get(DestId));
				BoundaryEdge.add(selectedEdge);
				BoundaryEdgeId.add(selectedEdge.getEdgeId());

			}

		}

		/**
		 * This map holds partitionIndex as keys and ArrayList of Border vertices as
		 * values
		 **/

		Map<Object, ArrayList<Object>> boundaryVertexPairs = new HashMap<>();
		LinkedHashMap<Integer, ArrayList<String>> stringBoundaryVertices = new LinkedHashMap<>();

		for (Object BoundaryVertex : boundaryPairVertices.keySet()) {

			boundaryVerticesList.add(String.valueOf(BoundaryVertex));

			if (boundaryVertexPairs.isEmpty()) {
				ArrayList<Object> vertices = new ArrayList<Object>();
				ArrayList<String> strVertices = new ArrayList<String>();
				vertices.add(BoundaryVertex);
				strVertices.add(String.valueOf(BoundaryVertex));
				boundaryVertexPairs.put(boundaryPairVertices.get(BoundaryVertex), vertices);
				stringBoundaryVertices.put(Integer.parseInt(String.valueOf(boundaryPairVertices.get(BoundaryVertex))),
						strVertices);

			} else if (boundaryVertexPairs.containsKey(boundaryPairVertices.get(BoundaryVertex))) {
				boundaryVertexPairs.get(boundaryPairVertices.get(BoundaryVertex)).add(BoundaryVertex);
				stringBoundaryVertices.get(Integer.parseInt(String.valueOf(boundaryPairVertices.get(BoundaryVertex))))
						.add(String.valueOf(BoundaryVertex));
			} else if (!(boundaryVertexPairs.isEmpty())
					&& (!boundaryVertexPairs.containsKey(boundaryPairVertices.get(BoundaryVertex)))) {
				ArrayList<Object> vertices = new ArrayList<Object>();
				ArrayList<String> strVertices = new ArrayList<String>();
				vertices.add(BoundaryVertex);
				strVertices.add(String.valueOf(BoundaryVertex));
				boundaryVertexPairs.put(boundaryPairVertices.get(BoundaryVertex), vertices);
				stringBoundaryVertices.put(Integer.parseInt(String.valueOf(boundaryPairVertices.get(BoundaryVertex))),
						strVertices);

			}
		}

		// System.out.println(boundaryPairVertices);

//		for (cEdge boundaryVert : BoundaryEdge) {
//			System.out.println(boundaryVert);
//		}
//		System.out.println(BoundaryEdge);
//		BoundaryEdge.forEach(
//				x -> System.out.println(x.getEdgeId() + " src: " + x.getStartNodeId() + " dest: " + x.getEndNodeId()));
//		System.out.println(stringBoundaryVertices);

		/**
		 * |Partition_Index|Source_vertex|Destination_vertex|Edge_Length|ArrayList<Road_Object>|
		 */

		List<Tuple5<Object, Object, Object, Double, ArrayList<RoadObject>>> tuplesForSubgraphs = new ArrayList<>(
				graphPartitionIndex.size());

		for (int i = 0; i < graphPartitionIndex.size(); i++) {

			for (Integer dstIndex : cGraph.getAdjancencyMap().get(partitionIndexKey[i]).keySet()) {
				int adjEdgeId = cGraph.getEdgeId(partitionIndexKey[i], dstIndex);
				if (!BoundaryEdgeId.contains(cGraph.getEdgeId(partitionIndexKey[i], dstIndex))) {
					tuplesForSubgraphs.add(new Tuple5<Object, Object, Object, Double, ArrayList<RoadObject>>(
							Long.valueOf(graphPartitionIndex.get(i)), partitionIndexKey[i], dstIndex,
							cGraph.getEdgeDistance(partitionIndexKey[i], dstIndex),
							cGraph.getObjectsOnEdges().get(adjEdgeId)));

					Tuple4<Object, Object, Double, ArrayList<RoadObject>> nt = new Tuple4<Object, Object, Double, ArrayList<RoadObject>>(
							partitionIndexKey[i], dstIndex, cGraph.getEdgeDistance(partitionIndexKey[i], dstIndex),
							cGraph.getObjectsOnEdges().get(adjEdgeId));

				}

			}

		}

		/**
		 * Using the Boundaries hashmap <PartitionIndex, ArrayList<BorderVertices> to
		 * find the shortest path from one partition to another partition Storing all
		 * the vertex that are in shortest path list in a separate ArrayList
		 */
		ArrayList<List<Path>> shortestPathList = runSPFAlgo(yGraph, stringBoundaryVertices, CustomPartitionSize);

		System.out.println();

		/**
		 * From the given graph create Indexing scheme using RTrees
		 * 
		 */
		RTree<Integer, Line> rtree = RTreeIndexer.createRTree(cGraph);
		// int size = rtree.size();
		// System.out.println("Number of entries in RTree: " + size);

//		// Index the border nodes with binary search tree to prune the traversal.
//		BinarySearchTree bst = new BinarySearchTree();
//		int[] arrayOfBorderNodes = new int[boundaryVerticesList.size()];
//
//		for (int i = 0; i < boundaryVerticesList.size(); i++) {
//			arrayOfBorderNodes[i] = Integer.parseInt(boundaryVerticesList.get(i));
//		}
//
//		bst.storeArray(arrayOfBorderNodes);

		for (int i = 0; i < boundaryVerticesList.size(); i++) {
			int nodeId = Integer.parseInt(boundaryVerticesList.get(i));
			findNNForEmbeddedGraph(nodeId, cGraph);
		}

		System.out.println("DartTable: " + DARTTableMap.toString());

		/**
		 * Load Spark Necessary Items
		 */
		Logger.getLogger("org.apache").setLevel(Level.WARN);

//		SparkConf config = new SparkConf().setAppName("ANNCLUSTERD").set("spark.locality.wait", "0")
//				.set("spark.submit.deployMode", "cluster").set("spark.driver.maxResultSize", "6g")
//				.set("spark.executor.memory", "6g").setMaster("spark://34.80.87.222:7077").set("spark.cores.max", "8")
//				.set("spark.blockManager.port", "10025").set("spark.driver.blockManager.port", "10026")
//				.set("spark.driver.port", "10027").set("spark.shuffle.service.enabled", "false")
//				.set("spark.dynamicAllocation.enabled", "false");

		// SparkConf config = new
		// SparkConf().setMaster("local[*]").setAppName("ANNCLUSTERED");

		SparkConf config = new SparkConf().setAppName("ANN-SCL-FIN").setMaster("local[*]");

		try (JavaSparkContext jscontext = new JavaSparkContext(config)) {

			System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");

			JavaRDD<String> BoundaryVertexRDD = jscontext.parallelize(boundaryVerticesList);
			JavaRDD<cEdge> BoundaryEdgeRDD = jscontext.parallelize(BoundaryEdge);

//			BoundaryEdgeRDD.foreach(
//					x -> System.out.println(x.getStartNodeId() + "-->" + x.getEndNodeId() + " : " + x.getLength()));
//			System.out.println(" ");/SparkANN/convertedGraphs/California_Edges.txt

			JavaRDD<List<Tuple3<Integer, Integer, Double>>> pathRDD = jscontext.parallelize(shortestPathList)
					.map(new Function<List<Path>, List<Tuple3<Integer, Integer, Double>>>() {

						/**
						 * 
						 */
						private static final long serialVersionUID = 1L;

						@Override
						public List<Tuple3<Integer, Integer, Double>> call(List<Path> shortestPathList)
								throws Exception {

							List<Tuple3<Integer, Integer, Double>> edgesForEmbeddedNetwork = new ArrayList<>();

							for (Path p : shortestPathList) {
								try {
									int a = Integer.parseInt(p.getEdges().getFirst().getFromNode().toString());

									int b = Integer.parseInt(p.getEdges().getLast().getToNode().toString());

									double dist = Math.abs(p.getTotalCost());
									// System.out.println(a + "-->" + b + " : " + dist);

									edgesForEmbeddedNetwork.add(new Tuple3<Integer, Integer, Double>(a, b, dist));

								} catch (NumberFormatException e) {
								}

							}

							return edgesForEmbeddedNetwork;
						}

					});

			/**
			 * Creating Embedded Network Graph: 1) Initially create a Virtual Vertex 2)
			 * Create a Embedded graph connecting VIRTUAL NODE to every other boundary Nodes
			 * 3) Set the weights as ZERO 4) Run the traversal from VIRTUAL NODE to other
			 * BOUNDARY NODES 5) Calculate the distance to the nearest node and store it in
			 * a array Tuple2<Object,Map<Object,Double>> VirtualGraph
			 **/

			CoreGraph embeddedGraph = createAugmentedNetwork(cGraph, pathRDD, BoundaryEdge, boundaryPairVertices);
			embeddedGraph.printEdgesInfo();

			/**
			 * Once the graph is created: 1) Combine the GraphRDD with RoadObjectPairRDD,
			 * and BoundaryEdgeRDD. 2) Apply CustomPartitioner function on the newly created
			 * RDD 3)
			 */

			JavaRDD<Tuple5<Object, Object, Object, Double, ArrayList<RoadObject>>> tupleForSubgraphsRDD = jscontext
					.parallelize(tuplesForSubgraphs);

			JavaPairRDD<Object, Tuple4<Object, Object, Double, ArrayList<RoadObject>>> tupleForSubgraphsPairRDD = tupleForSubgraphsRDD
					.mapPartitionsToPair(
							new PairFlatMapFunction<Iterator<Tuple5<Object, Object, Object, Double, ArrayList<RoadObject>>>, Object, Tuple4<Object, Object, Double, ArrayList<RoadObject>>>() {

								/**
								 * 
								 */
								private static final long serialVersionUID = 1L;

								@Override
								public Iterator<Tuple2<Object, Tuple4<Object, Object, Double, ArrayList<RoadObject>>>> call(
										Iterator<Tuple5<Object, Object, Object, Double, ArrayList<RoadObject>>> rowTups)
										throws Exception {

									List<Tuple2<Object, Tuple4<Object, Object, Double, ArrayList<RoadObject>>>> infoListWithKey = new ArrayList<>();
									while (rowTups.hasNext()) {
										Tuple5<Object, Object, Object, Double, ArrayList<RoadObject>> element = rowTups
												.next();
										Object partitionIndex = element._1();
										Object sourceVertex = element._2();
										Object destVertex = element._3();
										Double edgeLength = element._4();
										ArrayList<RoadObject> listOfRoadObjects = element._5();

										Tuple4<Object, Object, Double, ArrayList<RoadObject>> t = new Tuple4<Object, Object, Double, ArrayList<RoadObject>>(
												sourceVertex, destVertex, edgeLength, listOfRoadObjects);
										infoListWithKey.add(
												new Tuple2<Object, Tuple4<Object, Object, Double, ArrayList<RoadObject>>>(
														partitionIndex, t));
									}

									return infoListWithKey.iterator();
								}

							});

			JavaPairRDD<Object, Iterable<Tuple4<Object, Object, Double, ArrayList<RoadObject>>>> toCreateSubgraphRDD = tupleForSubgraphsPairRDD
					.groupByKey().partitionBy(new CustomPartitioner(CustomPartitionSize));

			// toCreateSubgraphRDD.foreach(x -> System.out.println(x));

			toCreateSubgraphRDD.foreachPartition(
					new VoidFunction<Iterator<Tuple2<Object, Iterable<Tuple4<Object, Object, Double, ArrayList<RoadObject>>>>>>() {

						/**
						 * 
						 */
						private static final long serialVersionUID = 1L;
						// private List<Map<Integer, Integer>> NNListMap = new ArrayList<>();
						// private List<Tuple3<Integer, Integer, Double>> nnList = new ArrayList<>();
						List<Tuple3<Integer, Integer, Double>> nearestNeighborList = new ArrayList<Tuple3<Integer, Integer, Double>>();
						// CoreGraph subGraph1 = new CoreGraph();
						private long duration = 0l;

						@Override
						public void call(
								Iterator<Tuple2<Object, Iterable<Tuple4<Object, Object, Double, ArrayList<RoadObject>>>>> eachTuple)
								throws Exception {

							int sourceVertex;
							int destVertex;
							double edgeLength;

							int roadObjectId;
							boolean roadObjectType;
							double distanceFromStart;

							while (eachTuple.hasNext()) {
								CoreGraph subGraph0 = new CoreGraph();
								Tuple2<Object, Iterable<Tuple4<Object, Object, Double, ArrayList<RoadObject>>>> theTup = eachTuple
										.next();

								ArrayList<Integer> addedEdge = new ArrayList<Integer>();
								ArrayList<Integer> addedObjects = new ArrayList<Integer>();
								Iterator<Tuple4<Object, Object, Double, ArrayList<RoadObject>>> iter = theTup._2()
										.iterator();
								while (iter.hasNext()) {
									Tuple4<Object, Object, Double, ArrayList<RoadObject>> listInfo = iter.next();
									sourceVertex = Integer.parseInt(String.valueOf(listInfo._1()));
									destVertex = Integer.parseInt(String.valueOf(listInfo._2()));
									edgeLength = listInfo._3();

									ArrayList<RoadObject> roadObjList = listInfo._4();

									if (addedEdge.isEmpty()) {
										subGraph0.addEdge(sourceVertex, destVertex, edgeLength);
										int currentEdgeId = subGraph0.getEdgeId(sourceVertex, destVertex);
										addedEdge.add(currentEdgeId);

									} else if ((!addedEdge.isEmpty()
											&& (!addedEdge.contains(subGraph0.getEdgeId(sourceVertex, destVertex))))) {
										subGraph0.addEdge(sourceVertex, destVertex, edgeLength);
										int currentEdgeId = subGraph0.getEdgeId(sourceVertex, destVertex);
										addedEdge.add(currentEdgeId);
									}

									if (roadObjList != null) {
										for (int i = 0; i < roadObjList.size(); i++) {
											roadObjectId = roadObjList.get(i).getObjectId();
											roadObjectType = roadObjList.get(i).getType();
											distanceFromStart = roadObjList.get(i).getDistanceFromStartNode();

											RoadObject rn0 = new RoadObject();
											rn0.setObjId(roadObjectId);
											rn0.setType(roadObjectType);
											rn0.setDistanceFromStartNode(distanceFromStart);

											if (addedObjects.isEmpty()) {
												subGraph0.addObjectOnEdge(subGraph0.getEdgeId(sourceVertex, destVertex),
														rn0);
												addedObjects.add(rn0.getObjectId());
											} else if ((!addedObjects.isEmpty())
													&& (!addedObjects.contains(rn0.getObjectId()))) {
												subGraph0.addObjectOnEdge(subGraph0.getEdgeId(sourceVertex, destVertex),
														rn0);
												addedObjects.add(rn0.getObjectId());
											}

										}
									}

								}
								ClusteringNodes clusteringNodes = new ClusteringNodes();
								Map<Integer, LinkedList<Integer>> nodeCluster = clusteringNodes.cluster(subGraph0);

								clusteredANN can = new clusteredANN();
								long startTime = System.nanoTime();
								nearestNeighborList = can.call(subGraph0, true, nodeCluster);
								long endTime = System.nanoTime();
								duration = (endTime - startTime) / 10000000;
								// clusteredANN can = new clusteredANN();
								// nnList = can.call(subGraph0, true);

//								ANNNaive ann0 = new ANNNaive();
//								Map<Integer, Integer> result = ann0.compute(subGraph0, true);
//								NNListMap.add(result);
//								JavaRDD<Tuple3<Integer, Integer, Double>> NearestNeighborResult = jscontext
//										.parallelize(nnList);
//								NearestNeighborResult.saveAsTextFile("/SparkANN/Result");

							}
							System.out.println("Time taken to run algorithm: " + duration);

						}
					});

//			st.stop();
//
//			System.out.print("Elapsed Time in Minute: " + st.elapsedMillis());

			jscontext.close();

		}

	}

	public static List<Tuple2<Object, Map<Object, Double>>> createEmbeddedNetwork(JavaRDD<String> BoundaryVerticesRDD) {
		Object virtualVertex = Integer.MAX_VALUE;
		List<Tuple2<Object, Map<Object, Double>>> embeddedNetwork = new ArrayList<>();
		for (Object BoundaryVertex : BoundaryVerticesRDD.collect()) {
			Map<Object, Double> connectingVertex = new HashMap<Object, Double>();
			connectingVertex.put(BoundaryVertex, 0.0);
			embeddedNetwork.add(new Tuple2<Object, Map<Object, Double>>(virtualVertex, connectingVertex));

		}

		return embeddedNetwork;

	}

	@SuppressWarnings("unlikely-arg-type")
	public static CoreGraph createAugmentedNetwork(CoreGraph cGraph,
			JavaRDD<List<Tuple3<Integer, Integer, Double>>> pathRDD, ArrayList<cEdge> borderEdge,
			Map<Object, Object> borderPairVertex) {
		/*
		 * The embedded graph is the new graph created to construct graphs from the
		 * border nodes only
		 */
		CoreGraph embeddedGraph = new CoreGraph();

		/*
		 * augmentedGraph is the graph that is created to have a virtual vertex and its
		 * connection to all other graphs from embedded network with a direction and
		 * weight as zero
		 */

		int newEdgeId = cGraph.getEdgesWithInfo().size() + 1;

		for (cEdge edges : borderEdge) {
			embeddedGraph.addEdge(newEdgeId, edges.getStartNodeId(), edges.getEndNodeId(), edges.getLength());
			newEdgeId++;
		}

		for (List<Tuple3<Integer, Integer, Double>> pathList : pathRDD.collect()) {
			for (Tuple3<Integer, Integer, Double> path : pathList) {
				newEdgeId = newEdgeId - 2;
				int srcVertex = path._1();
				int destVertex = path._2();
				double edgeLength = path._3();

				if (!borderEdge.contains(cGraph.getEdgeId(srcVertex, destVertex))) {
					if (borderPairVertex.get(srcVertex) == borderPairVertex.get(destVertex)) {
						embeddedGraph.addEdge(newEdgeId, srcVertex, destVertex, edgeLength);
						newEdgeId++;
					}
				}

			}

		}

		/*
		 * Once the embedded graph is created Create one virtual vertex and create an
		 * augmented graph with it
		 */

		CoreGraph augmentedGraph = new CoreGraph();

		int virtualVertex = cGraph.getNodesWithInfo().size() + 1;

		for (Node embeddedNode : embeddedGraph.getNodesWithInfo()) {
			augmentedGraph.addEdge(newEdgeId, virtualVertex, embeddedNode.getNodeId(), 0);
			newEdgeId++;
		}

		return augmentedGraph;
	}

	public static ArrayList<List<Path>> runSPF(YenGraph yenG, HashMap<Integer, ArrayList<String>> boundaries,
			int partSize) {

		ArrayList<List<Path>> SPList = new ArrayList<List<Path>>();
		Yen yenAlgo = new Yen();

		for (Integer mapIndex : boundaries.keySet()) {
			for (int i = 0; i < boundaries.get(mapIndex).size(); i++) {
				String vertexSrc = boundaries.get(mapIndex).get(i);

				for (int j = mapIndex + 1; j < partSize; j++) {
					for (int k = 0; k < boundaries.get(j).size(); k++) {
						String vertexDest = boundaries.get(j).get(k);
						List<Path> pathList = new LinkedList<Path>();
						pathList = yenAlgo.ksp(yenG, vertexSrc, vertexDest, 1);
						SPList.add(pathList);
						// 09System.out.println(vertexSrc + " -> " + vertexDest);

					}
				}

			}
		}

		return SPList;
	}

	public static ArrayList<List<Path>> runSP(YenGraph yenG, LinkedList<String> boundaryVertices, int partSize) {
		ArrayList<List<Path>> SPList = new ArrayList<List<Path>>();
		Yen yenAlgo = new Yen();

		for (int i = 0; i < boundaryVertices.size(); i++) {
			String srcVertex = boundaryVertices.get(i);
			for (int j = i + 1; j < boundaryVertices.size(); j++) {
				String destVertex = boundaryVertices.get(j);
				List<Path> pathList = new LinkedList<Path>();
				pathList = yenAlgo.ksp(yenG, srcVertex, destVertex, 1);
				SPList.add(pathList);
				// System.out.println(srcVertex + " , " + destVertex);
			}
		}
		return SPList;

	}

	public static ArrayList<List<Path>> runSPFAlgo(YenGraph yenG, HashMap<Integer, ArrayList<String>> boundaries,
			int partSize) {

		ArrayList<List<Path>> SPList = new ArrayList<List<Path>>();
		Yen yenAlgo = new Yen();

		for (Integer mapIndex : boundaries.keySet()) {
			for (int i = 0; i < boundaries.get(mapIndex).size(); i++) {
				String vertexSrc = boundaries.get(mapIndex).get(i);
				for (int a = i + 1; a < boundaries.get(mapIndex).size(); a++) {
					String vertexDest = boundaries.get(mapIndex).get(a);
					List<Path> pathList = new LinkedList<Path>();
					pathList = yenAlgo.ksp(yenG, vertexSrc, vertexDest, 1);
					SPList.add(pathList);
				}

				for (int j = mapIndex + 1; j < partSize; j++) {
					for (int k = 0; k < boundaries.get(j).size(); k++) {
						String vertexDest = boundaries.get(j).get(k);
						List<Path> pathList = new LinkedList<Path>();
						pathList = yenAlgo.ksp(yenG, vertexSrc, vertexDest, 1);
						SPList.add(pathList);
						// 09System.out.println(vertexSrc + " -> " + vertexDest);

					}
				}

			}
		}

		return SPList;
	}

	public static List<Path> getSPFbetweenTwoNodes(YenGraph yg, String src, String dest, int partSize) {
		List<Path> spf = new ArrayList<Path>();
		Yen yalg = new Yen();

		spf = yalg.ksp(yg, src, dest, partSize);

		return spf;
	}

//	public static LinkedList<Integer> unifyAllShortestPaths(ArrayList<List<Path>> spfList) {
//		LinkedList<Integer> shortestpathsUnion = new LinkedList<Integer>();
//
//		for (List<Path> p : spfList) {
//			for (int i = 0; i < p.size(); i++) {
//				for (int j = 0; j < p.get(i).getNodes().size(); j++) {
//					int vertex = Integer.parseInt(p.get(i).getNodes().get(j));
//					if (!shortestpathsUnion.contains(vertex)) {
//
//						shortestpathsUnion.add(vertex);
//						// System.out.println("added " + vertex);
//					}
//
//				}
//				// System.out.println(" ");
//			}
//		}
//
//		return shortestpathsUnion;
//	}

	public static CoreGraph readSPFreturnGraph(ArrayList<List<Path>> shortestPathList) {

		CoreGraph embeddedGraph = new CoreGraph();

		for (List<Path> path : shortestPathList) {
			for (Path p : path) {
				int srcVertex = Integer.parseInt(p.getEdges().getFirst().getFromNode());
				int destinationVertex = Integer.parseInt(p.getEdges().getLast().getToNode());
				double edgeWeight = Double.valueOf(p.getTotalCost());
				embeddedGraph.addEdge(srcVertex, destinationVertex, edgeWeight);

			}
		}

		return embeddedGraph;

	}

	/*
	 * 
	 * public static void findNNForEmbeddedGraph(int nodeId, CoreGraph cGraph,
	 * ArrayList<String> boundaryVerticesList) {
	 * 
	 * Initially create a Binary search tree to index the boundaryVertices Take the
	 * nodeId and run the traversal: check if the adjacent node is in the BST or not
	 * If not check the edgeId with two nodes, check if there is any data object or
	 * not If yes create a holder class and add the details and add it as arraylist
	 * 
	 * 
	 * 
	 * Map<Integer, ArrayList<holder>> result = new HashMap<Integer,
	 * ArrayList<holder>>();
	 * 
	 * List<Tuple3<String, Integer, Double>> nnList = new ArrayList<>();
	 * ArrayList<holder> NNLists = new ArrayList<>();
	 * 
	 * String towardsAnotherBorder = "tb"; String towardsNonBorder = "ntb";
	 * 
	 * int[] arrayOfBorderNodes = new int[boundaryVerticesList.size()]; Set<Integer>
	 * boundaryVertices = new HashSet<>();
	 * 
	 * for (int i = 0; i < boundaryVerticesList.size(); i++) { int vertex =
	 * Integer.parseInt(boundaryVerticesList.get(i)); arrayOfBorderNodes[i] =
	 * vertex; boundaryVertices.add(vertex); }
	 * 
	 * PriorityQueue<NodeDistance> pq = new PriorityQueue<NodeDistance>();
	 * 
	 * boolean visited[] = new boolean[cGraph.getNodesWithInfo().size()];
	 * pq.offer(new NodeDistance(nodeId, 0.0)); // pq.add(nodeId); // set the
	 * distace as 0 // double nearestDistance = 0.0;
	 * 
	 * visited[nodeId] = true;
	 * 
	 * while (!pq.isEmpty()) { NodeDistance currentNode = pq.poll();
	 * 
	 * for (int adjacentNodes : cGraph.getAdjNodeIds(currentNode.nodeId)) { if
	 * (!visited[adjacentNodes]) { visited[adjacentNodes] = true; double distance =
	 * currentNode.distance; int edgeId = cGraph.getEdgeId(currentNode.nodeId,
	 * adjacentNodes); List<RoadObject> objects =
	 * cGraph.getObjectsOnEdges().get(edgeId);
	 * 
	 * if (objects != null) { for (RoadObject rObj : objects) { if (rObj.getType()
	 * == false) { distance += rObj.getDistanceFromStartNode(); // nnList.add(new
	 * Tuple3<>(towardsNonBorder, rObj.getObjectId(), distance)); holder hold = new
	 * holder(rObj.getObjectId(), towardsNonBorder, distance); NNLists.add(hold);
	 * DARTTableMap.put(currentNode.nodeId, NNLists); } else { distance +=
	 * cGraph.getEdgeDistance(currentNode.nodeId, adjacentNodes); } } } else {
	 * distance += cGraph.getEdgeDistance(currentNode.nodeId, adjacentNodes); }
	 * 
	 * if (boundaryVertices.contains(adjacentNodes)) { nnList.add(new
	 * Tuple3<>(towardsAnotherBorder, adjacentNodes, distance)); //holder hold = new
	 * holder(rObj.getObjectId(), towardsAnotherBorder, distance);
	 * NNLists.add(hold); DARTTableMap.put(currentNode.nodeId, NNLists); } else {
	 * pq.offer(new NodeDistance(adjacentNodes, distance)); } } } }
	 * 
	 * // while (pq.size() != 0) { // nodeId = pq.poll(); // // // Get all adjacent
	 * vertices of the dequeued // // vertex If a adjacent has not been visited, //
	 * // then mark it visited and enqueue it // // Iterator<Integer> i =
	 * cGraph.getAdjNodeIds(nodeId).listIterator(); // while (i.hasNext()) { // int
	 * adjacentNode = i.next(); // // if (!visited[adjacentNode]) { // // you get
	 * the nodes, now check if the node is in the border node or not // if
	 * ((vertexIdPartitionIndexMap.get(nodeId) ==
	 * vertexIdPartitionIndexMap.get(adjacentNode))) { // // check if the edge has
	 * any data object in it or not. // //
	 * cGraph.getObjectsOnEdges().get(cGraph.getEdgeId(nodeId, adjacentNode)); //
	 * for (RoadObject rObj :
	 * cGraph.getObjectsOnEdges().get(cGraph.getEdgeId(nodeId, adjacentNode))) { //
	 * if (rObj.getType() != true) { // nearestDistance = nearestDistance +
	 * rObj.getDistanceFromStartNode(); // // nnList.add(new Tuple3<String, Integer,
	 * Double>(towardsNonBorder, rObj.getObjectId(), // nearestDistance)); // // }
	 * // if there is no data object then simply add the edge // else //
	 * nearestDistance = nearestDistance + cGraph.getEdgeDistance(nodeId,
	 * adjacentNode); // continue; // } // // } else if
	 * ((vertexIdPartitionIndexMap.get(nodeId) !=
	 * vertexIdPartitionIndexMap.get(adjacentNode))) { // //
	 * cGraph.getObjectsOnEdges().get(cGraph.getEdgeId(nodeId, adjacentNode)); //
	 * for (RoadObject rObj :
	 * cGraph.getObjectsOnEdges().get(cGraph.getEdgeId(nodeId, adjacentNode))) { //
	 * if (rObj.getType() != true) { // nearestDistance = nearestDistance +
	 * rObj.getDistanceFromStartNode(); // // nnList.add(new Tuple3<String, Integer,
	 * Double>(towardsAnotherBorder, rObj.getObjectId(), // nearestDistance)); // //
	 * } else // nearestDistance = nearestDistance + cGraph.getEdgeDistance(nodeId,
	 * adjacentNode); // continue; // } // // } // visited[adjacentNode] = true; //
	 * pq.add(adjacentNode); // } // // } // }
	 * 
	 * }
	 */

	public static void findNNForEmbeddedGraph(int nodeId, CoreGraph cGraph) {

		List<Tuple3<String, Integer, Double>> nnList = new ArrayList<>();
		ArrayList<holder> NNLists = new ArrayList<>();

		String towardsAnotherBorder = "tb";
		String towardsNonBorder = "ntb";

//		int[] arrayOfBorderNodes = new int[boundaryVerticesList.size()];
//		Set<Integer> boundaryVertices = new HashSet<>();
//
//		for (int i = 0; i < boundaryVerticesList.size(); i++) {
//			int vertex = Integer.parseInt(boundaryVerticesList.get(i));
//			arrayOfBorderNodes[i] = vertex;
//			boundaryVertices.add(vertex);
//		}

		PriorityQueue<NodeDistance> pq = new PriorityQueue<>();
		boolean visited[] = new boolean[cGraph.getNodesWithInfo().size()];
		pq.offer(new NodeDistance(nodeId, 0.0));
		visited[nodeId] = true;

		while (!pq.isEmpty()) {
			NodeDistance currentNode = pq.poll();

			for (int adjacentNodes : cGraph.getAdjNodeIds(currentNode.nodeId)) {
				if (!visited[adjacentNodes]) {
					visited[adjacentNodes] = true;
					double distance = currentNode.distance;
					int edgeId = cGraph.getEdgeId(currentNode.nodeId, adjacentNodes);
					List<RoadObject> objects = cGraph.getObjectsOnEdges().get(edgeId);

					if (objects != null) {
						for (RoadObject rObj : objects) {
							if (rObj.getType() == false) {
								distance += rObj.getDistanceFromStartNode();

								if (vertexIdPartitionIndexMap.get(currentNode) == vertexIdPartitionIndexMap
										.get(adjacentNodes)) {
									holder hold = new holder(rObj.getObjectId(), towardsNonBorder, distance);
									NNLists.add(hold);
									DARTTableMap.put(currentNode.nodeId, NNLists);
								} else if (vertexIdPartitionIndexMap.get(currentNode) != vertexIdPartitionIndexMap
										.get(adjacentNodes)) {

									holder hold = new holder(rObj.getObjectId(), towardsAnotherBorder, distance);
									NNLists.add(hold);
									DARTTableMap.put(currentNode.nodeId, NNLists);
								}

								if (NNLists.size() == 2) {
									break;
								}

							} else {
								distance += cGraph.getEdgeDistance(currentNode.nodeId, adjacentNodes);
							}
						}
						if (NNLists.size() == 2) {
							break;
						}
					} else {
						distance += cGraph.getEdgeDistance(currentNode.nodeId, adjacentNodes);
					}

//					if (boundaryVertices.contains(adjacentNodes)) {
//						nnList.add(new Tuple3<>(towardsAnotherBorder, adjacentNodes, distance));
//						holder hold = new holder(adjacentNodes, towardsAnotherBorder, distance);
//						NNLists.add(hold);
//						DARTTableMap.put(currentNode.nodeId, NNLists);
//						if (NNLists.size() == 2)
//							break;
//					}
				}
			}
		}
	}

//To compare the Distances
	static class NodeDistance implements Comparable<NodeDistance> {
		int nodeId;
		double distance;

		public NodeDistance(int nodeId, double distance) {
			this.nodeId = nodeId;
			this.distance = distance;
		}

		@Override
		public int compareTo(NodeDistance o) {
			return Double.compare(distance, o.distance);
		}
	}

	static class holder implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1766669831160333164L;
		private Integer objectId;
		private String side;
		private double distance;

		public holder(int objectId, String side, Double distance) {
			this.objectId = objectId;
			if ((side.equalsIgnoreCase("tb")) && (side.equalsIgnoreCase("ntb"))) {
				this.side = side;
			} else {
				throw new IllegalArgumentException("Invalid Input");
			}

			this.distance = distance;

		}

		public Integer getObjectId() {
			return objectId;
		}

		public void setObjectId(Integer objectId) {
			this.objectId = objectId;
		}

		public String getSide() {
			return side;
		}

		public void setSide(String side) {
			if ((side.equalsIgnoreCase("tb")) && (side.equalsIgnoreCase("ntb"))) {
				this.side = side;
			} else {
				throw new IllegalArgumentException("Invalid Input");
			}

		}

		public double getDistance() {
			return distance;
		}

		public void setDistance(double distance) {
			this.distance = distance;
		}

	}

//	public static Tuple2<Integer, Integer> getNearestDataObject() {
//		Tuple2<Integer, Integer> NNs = null;
//
//		return NNs;
//	}

	static class clusteredANN extends ANNClusteredOptimizedWithHeuristic implements
			Function3<CoreGraph, Boolean, Map<Integer, LinkedList<Integer>>, List<Tuple3<Integer, Integer, Double>>>,
			Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = -2583115030602356667L;

		private Map<Integer, LinkedList<Integer>> m_objectIdClusters;
		private Map<Integer, LinkedList<Integer>> m_nodeIdClusters;
		private int sizeOfNodeClusters, sizeOfObjectClusters;
		private List<Tuple3<Integer, Integer, Double>> answerList = new ArrayList<>();

		CoreGraph m_graph;

		@Override
		public List<Tuple3<Integer, Integer, Double>> call(CoreGraph cg, Boolean queryType,
				Map<Integer, LinkedList<Integer>> nodeClusters) throws Exception {

			ClusteringRoadObjects clusteringObjects = new ClusteringRoadObjects();

			m_graph = cg;
			m_nodeIdClusters = nodeClusters;
			m_objectIdClusters = clusteringObjects.clusterWithIndex4(cg, m_nodeIdClusters, queryType);

			NearestNeighbor nn = new NearestNeighbor();
			int boundaryStartQueryObj, boundaryEndQueryObj;
			int objectCounter = 0;
			int queriedObjCounter = 0;
			int objectClusterCounter = 0;

			if (queryType) {
				// Iterate through boundary objects of object clusters
				for (Integer objectClusterIndex : m_objectIdClusters.keySet()) {
					objectClusterCounter++;
					if (!m_objectIdClusters.get(objectClusterIndex).isEmpty()) {
						objectCounter += m_objectIdClusters.get(objectClusterIndex).size();
						if (m_objectIdClusters.get(objectClusterIndex).size() == 1) {

							queriedObjCounter++;
							int queryObj = m_objectIdClusters.get(objectClusterIndex).getFirst();
							Map<RoadObject, Double> nearestFalseObjId = nn.getNearestFalseObjectToGivenObjOnMap(m_graph,
									queryObj);

							for (RoadObject rO : nearestFalseObjId.keySet()) {

								answerList.add(new Tuple3<Integer, Integer, Double>(queryObj, rO.getObjectId(),
										nearestFalseObjId.get(rO)));

							}

						} else if (m_objectIdClusters.get(objectClusterIndex).size() == 2) {

							int currentClusterIndex = clusteringObjects.getObjectClusterNodeClusterInfo()
									.get(objectClusterIndex);
							int endNode1 = m_nodeIdClusters.get(currentClusterIndex).getFirst();
							int endNode2 = m_nodeIdClusters.get(currentClusterIndex).getLast();
							// for heuristic method
							if (cg.isTerminalNode(endNode1)) {
								ArrayList<RoadObject> objectList = m_graph
										.returnAllObjectsOnNodeCluster(m_nodeIdClusters.get(currentClusterIndex));

								if (objectList.get(objectList.size() - 1).getType() == true) {
									queriedObjCounter++;
									int requiredQueryObject = m_objectIdClusters.get(objectClusterIndex).getLast();

									Map<RoadObject, Double> nearestFalseObjectForRequiredQueryObject = nn
											.getNearestFalseObjectToGivenObjOnMap(m_graph, requiredQueryObject);
									for (RoadObject rO : nearestFalseObjectForRequiredQueryObject.keySet()) {

										answerList.add(new Tuple3<Integer, Integer, Double>(requiredQueryObject,
												rO.getObjectId(),
												Math.abs(nearestFalseObjectForRequiredQueryObject.get(rO))));

									}
//									m_nearestNeighborSets.put(requiredQueryObject,
//											nearestFalseObjectForRequiredQueryObject);
//									m_nearestNeighborSets.put(objectClusters.get(objectClusterIndex).getFirst(),
//											nearestFalseObjectForRequiredQueryObject);
								} else {
									continue;
								}

							} else if (cg.isTerminalNode(endNode2)) {
								// to check if there is data object between the object cluster and terminal
								// node.
								ArrayList<RoadObject> objectList = m_graph
										.returnAllObjectsOnNodeCluster(m_nodeIdClusters.get(currentClusterIndex));

								if (objectList.get(objectList.size() - 1).getType() == true) {

									queriedObjCounter++;
									int requiredQueryObject = m_objectIdClusters.get(objectClusterIndex).getFirst();

									Map<RoadObject, Double> nearestFalseObjectForRequiredQueryObject = nn
											.getNearestFalseObjectToGivenObjOnMap(m_graph, requiredQueryObject);
									for (RoadObject rO : nearestFalseObjectForRequiredQueryObject.keySet()) {

										answerList.add(new Tuple3<Integer, Integer, Double>(requiredQueryObject,
												rO.getObjectId(),
												Math.abs(nearestFalseObjectForRequiredQueryObject.get(rO))));

									}

//									int nearestFalseObjectForRequiredQueryObject = nn
//											.getNearestFalseObjectIdToGivenObjOnMap(cg, requiredQueryObject);
//									m_nearestNeighborSets.put(requiredQueryObject,
//											nearestFalseObjectForRequiredQueryObject);
//									m_nearestNeighborSets.put(m_objectIdClusters.get(objectClusterIndex).getLast(),
//											nearestFalseObjectForRequiredQueryObject);
								} else {
									queriedObjCounter += 2;
									boundaryStartQueryObj = m_objectIdClusters.get(objectClusterIndex).getFirst();
									boundaryEndQueryObj = m_objectIdClusters.get(objectClusterIndex).getLast();

//									int nearestFalseObjForBeginingExit = nn
//											.getNearestFalseObjectIdToGivenObjOnMap(cg, boundaryStartQueryObj);
//									int nearestFalseObjForEndExit = nn.getNearestFalseObjectIdToGivenObjOnMap(cg,
//											boundaryEndQueryObj);

									Map<RoadObject, Double> nearestFalseObjForBeginingExit = nn
											.getNearestFalseObjectToGivenObjOnMap(m_graph, boundaryStartQueryObj);
									Map<RoadObject, Double> nearestFalseObjForEndExit = nn
											.getNearestFalseObjectToGivenObjOnMap(m_graph, boundaryEndQueryObj);

									for (RoadObject rO : nearestFalseObjForBeginingExit.keySet()) {

										answerList.add(new Tuple3<Integer, Integer, Double>(boundaryStartQueryObj,
												rO.getObjectId(), Math.abs(nearestFalseObjForBeginingExit.get(rO))));

									}

									for (RoadObject rO : nearestFalseObjForEndExit.keySet()) {

										answerList.add(new Tuple3<Integer, Integer, Double>(boundaryEndQueryObj,
												rO.getObjectId(), Math.abs(nearestFalseObjForEndExit.get(rO))));

									}

//									m_nearestNeighborSets.put(boundaryStartQueryObj, nearestFalseObjForBeginingExit);
//									m_nearestNeighborSets.put(boundaryEndQueryObj, nearestFalseObjForEndExit);
								}

							} else {
								queriedObjCounter += 2;
								boundaryStartQueryObj = m_objectIdClusters.get(objectClusterIndex).getFirst();
								boundaryEndQueryObj = m_objectIdClusters.get(objectClusterIndex).getLast();

								Map<RoadObject, Double> nearestFalseObjForBeginingExit = nn
										.getNearestFalseObjectToGivenObjOnMap(m_graph, boundaryStartQueryObj);
								Map<RoadObject, Double> nearestFalseObjForEndExit = nn
										.getNearestFalseObjectToGivenObjOnMap(m_graph, boundaryEndQueryObj);

								for (RoadObject rO : nearestFalseObjForBeginingExit.keySet()) {

									answerList.add(new Tuple3<Integer, Integer, Double>(boundaryStartQueryObj,
											rO.getObjectId(), Math.abs(nearestFalseObjForBeginingExit.get(rO))));

								}

								for (RoadObject rO : nearestFalseObjForEndExit.keySet()) {

									answerList.add(new Tuple3<Integer, Integer, Double>(boundaryEndQueryObj,
											rO.getObjectId(), Math.abs(nearestFalseObjForEndExit.get(rO))));

								}

							}
						}
						// when object cluster size is greater than 2
						else {
							// new codes start---
							int currentClusterIndex = clusteringObjects.getObjectClusterNodeClusterInfo()
									.get(objectClusterIndex);
							int currentClusterSize = m_nodeIdClusters.get(currentClusterIndex).size();

							int endNode1 = m_nodeIdClusters.get(currentClusterIndex).getFirst();
							int endNode2 = m_nodeIdClusters.get(currentClusterIndex).getLast();

							if (cg.isTerminalNode(endNode1)) {
								ArrayList<RoadObject> objectList = m_graph
										.returnAllObjectsOnNodeCluster(m_nodeIdClusters.get(currentClusterIndex));

								System.out.println("");
								if (objectList.get(objectList.size() - 1).getType() == true) {
									queriedObjCounter++;
									int requiredQueryObject = m_nodeIdClusters.get(objectClusterIndex).get(0);

									Map<RoadObject, Double> nearestFalseObjectForRequiredQueryObject = nn
											.getNearestFalseObjectToGivenObjOnMap(m_graph, requiredQueryObject);
									for (RoadObject rO : nearestFalseObjectForRequiredQueryObject.keySet()) {

										answerList.add(new Tuple3<Integer, Integer, Double>(requiredQueryObject,
												rO.getObjectId(),
												Math.abs(nearestFalseObjectForRequiredQueryObject.get(rO))));

									}

//									int nearestFalseObjectForRequiredQueryObject = nn
//											.getNearestFalseObjectIdToGivenObjOnMap(gr, requiredQueryObject);
//									m_nearestNeighborSets.put(requiredQueryObject,
//											nearestFalseObjectForRequiredQueryObject);

									for (int k = 1; k < m_objectIdClusters.get(objectClusterIndex).size() - 1; k++) {

										for (RoadObject rO : nearestFalseObjectForRequiredQueryObject.keySet()) {

											answerList.add(new Tuple3<Integer, Integer, Double>(
													m_objectIdClusters.get(objectClusterIndex).get(k), rO.getObjectId(),
													Math.abs(nearestFalseObjectForRequiredQueryObject.get(rO))));

										}

//										m_nearestNeighborSets.put(m_objectIdClusters.get(objectClusterIndex).get(k),
//												nearestFalseObjectForRequiredQueryObject);
									}
								} else {
									continue;
								}

							} else if (cg.isTerminalNode(endNode2)) {
								// System.out.println("Terminal: "+endNode2);
								ArrayList<RoadObject> objectList = m_graph
										.returnAllObjectsOnNodeCluster(m_nodeIdClusters.get(currentClusterIndex));

								if (objectList.get(objectList.size() - 1).getType() == true) {

									queriedObjCounter++;
									int requiredQueryObject = m_objectIdClusters.get(objectClusterIndex).getFirst();

									Map<RoadObject, Double> nearestFalseObjectForRequiredQueryObject = nn
											.getNearestFalseObjectToGivenObjOnMap(m_graph, requiredQueryObject);

									for (RoadObject rO : nearestFalseObjectForRequiredQueryObject.keySet()) {

										answerList.add(new Tuple3<Integer, Integer, Double>(requiredQueryObject,
												rO.getObjectId(),
												Math.abs(nearestFalseObjectForRequiredQueryObject.get(rO))));

									}

//									int nearestFalseObjectForRequiredQueryObject = nn
//											.getNearestFalseObjectIdToGivenObjOnMap(gr, requiredQueryObject);
//									m_nearestNeighborSets.put(requiredQueryObject,
//											nearestFalseObjectForRequiredQueryObject);

									for (int k = 1; k < m_objectIdClusters.get(objectClusterIndex).size() - 1; k++) {

										for (RoadObject rO : nearestFalseObjectForRequiredQueryObject.keySet()) {

											answerList.add(new Tuple3<Integer, Integer, Double>(
													m_objectIdClusters.get(objectClusterIndex).get(k), rO.getObjectId(),
													Math.abs(nearestFalseObjectForRequiredQueryObject.get(rO))));

										}

//										m_nearestNeighborSets.put(m_objectIdClusters.get(objectClusterIndex).get(k),
//												nearestFalseObjectForRequiredQueryObject);
									}

//									m_nearestNeighborSets.put(requiredQueryObject,
//											nearestFalseObjectForRequiredQueryObject);
									for (int k = m_objectIdClusters.get(objectClusterIndex).size(); k <= 1; k--) {

										for (RoadObject rO : nearestFalseObjectForRequiredQueryObject.keySet()) {

											answerList.add(new Tuple3<Integer, Integer, Double>(
													m_objectIdClusters.get(objectClusterIndex).get(k), rO.getObjectId(),
													Math.abs(nearestFalseObjectForRequiredQueryObject.get(rO))));

										}
//										m_nearestNeighborSets.put(m_objectIdClusters.get(objectClusterIndex).get(k),
//												nearestFalseObjectForRequiredQueryObject);
									}
								} else {
									queriedObjCounter += 2;
									boundaryStartQueryObj = m_objectIdClusters.get(objectClusterIndex).getFirst();
									boundaryEndQueryObj = m_objectIdClusters.get(objectClusterIndex).getLast();

									Map<RoadObject, Double> nearestFalseObjWithDistBoundaryStart = nn
											.getNearestFalseObjectToGivenObjOnMap(m_graph, boundaryStartQueryObj);
									Map<RoadObject, Double> nearestFalseObjWithDistBoundaryEnd = nn
											.getNearestFalseObjectToGivenObjOnMap(m_graph, boundaryEndQueryObj);

									RoadObject[] nearestFalseObjBoundaryStart = nearestFalseObjWithDistBoundaryStart
											.keySet().toArray(new RoadObject[0]);
									RoadObject[] nearestFalseObjBoundaryEnd = nearestFalseObjWithDistBoundaryEnd
											.keySet().toArray(new RoadObject[0]);

									int nearestFalseObjIdForBoundaryStart = nearestFalseObjBoundaryStart[0]
											.getObjectId();
									int nearestFalseObjIdForBoundaryEnd = nearestFalseObjBoundaryEnd[0].getObjectId();

									Double[] nearestFalseObjDistBoundaryStart = nearestFalseObjWithDistBoundaryStart
											.values().toArray(new Double[0]);
									Double[] nearestFalseObjDistBoundaryEnd = nearestFalseObjWithDistBoundaryEnd
											.values().toArray(new Double[0]);

									for (RoadObject rO : nearestFalseObjWithDistBoundaryStart.keySet()) {

										answerList.add(new Tuple3<Integer, Integer, Double>(boundaryStartQueryObj,
												rO.getObjectId(),
												Math.abs(nearestFalseObjWithDistBoundaryStart.get(rO))));

									}

									for (RoadObject rO : nearestFalseObjWithDistBoundaryEnd.keySet()) {

										answerList.add(new Tuple3<Integer, Integer, Double>(boundaryEndQueryObj,
												rO.getObjectId(),
												Math.abs(nearestFalseObjWithDistBoundaryEnd.get(rO))));

									}

									// System.out.println("index:" + objectClusterIndex);

									for (int i = m_objectIdClusters.get(objectClusterIndex)
											.indexOf(boundaryStartQueryObj)
											+ 1; i < m_objectIdClusters.get(objectClusterIndex).size() - 1; i++) {
										// System.out.println("i:" + i );
										int currentTrueObject = m_objectIdClusters.get(objectClusterIndex).get(i);
										LinkedList<Integer> currentObjCluster = new LinkedList<Integer>();
										currentObjCluster.addAll(m_objectIdClusters.get(objectClusterIndex));
										LinkedList<Integer> currentNodeCluster = new LinkedList<Integer>();

										currentNodeCluster.addAll(m_nodeIdClusters.get(clusteringObjects
												.getObjectClusterNodeClusterInfo().get(objectClusterIndex)));

										double distToBoundaryStartObj = m_graph
												.getDistanceBetweenBoundaryObjAndCurrentObj(currentNodeCluster,
														currentObjCluster, boundaryStartQueryObj, currentTrueObject);
										double distToBoundaryEndObj = m_graph
												.getDistanceBetweenBoundaryObjAndCurrentObj(currentNodeCluster,
														currentObjCluster, boundaryEndQueryObj, currentTrueObject);

										double distanceFromCurrentObjectToBoundaryStartNearestFalseObject = nearestFalseObjDistBoundaryStart[0]
												+ distToBoundaryStartObj;
										double distanceFromCurrentObjectToBoundaryEndNearestFalseObject = nearestFalseObjDistBoundaryEnd[0]
												+ distToBoundaryEndObj;

										if (distanceFromCurrentObjectToBoundaryStartNearestFalseObject > distanceFromCurrentObjectToBoundaryEndNearestFalseObject) {
											answerList.add(new Tuple3<Integer, Integer, Double>(currentTrueObject,
													nearestFalseObjIdForBoundaryEnd, Math.abs(
															distanceFromCurrentObjectToBoundaryEndNearestFalseObject)));
//											m_nearestNeighborSets.put(currentTrueObject,
//													nearestFalseObjIdForBoundaryEnd);
										} else {
											answerList.add(new Tuple3<Integer, Integer, Double>(currentTrueObject,
													nearestFalseObjIdForBoundaryStart, Math.abs(
															distanceFromCurrentObjectToBoundaryStartNearestFalseObject)));
										}

									}
								}

							}

							// new codes end---

							else {

								queriedObjCounter += 2;
								boundaryStartQueryObj = m_objectIdClusters.get(objectClusterIndex).getFirst();
								boundaryEndQueryObj = m_objectIdClusters.get(objectClusterIndex).getLast();

								Map<RoadObject, Double> nearestFalseObjWithDistBoundaryStart = nn
										.getNearestFalseObjectToGivenObjOnMap(m_graph, boundaryStartQueryObj);
								Map<RoadObject, Double> nearestFalseObjWithDistBoundaryEnd = nn
										.getNearestFalseObjectToGivenObjOnMap(m_graph, boundaryEndQueryObj);

								RoadObject[] nearestFalseObjBoundaryStart = nearestFalseObjWithDistBoundaryStart
										.keySet().toArray(new RoadObject[0]);
								RoadObject[] nearestFalseObjBoundaryEnd = nearestFalseObjWithDistBoundaryEnd.keySet()
										.toArray(new RoadObject[0]);

								int nearestFalseObjIdForBoundaryStart = nearestFalseObjBoundaryStart[0].getObjectId();
								int nearestFalseObjIdForBoundaryEnd = nearestFalseObjBoundaryEnd[0].getObjectId();

								Double[] nearestFalseObjDistBoundaryStart = nearestFalseObjWithDistBoundaryStart
										.values().toArray(new Double[0]);
								Double[] nearestFalseObjDistBoundaryEnd = nearestFalseObjWithDistBoundaryEnd.values()
										.toArray(new Double[0]);

//								m_nearestNeighborSets.put(boundaryStartQueryObj, nearestFalseObjIdForBoundaryStart);
//
//								m_nearestNeighborSets.put(boundaryEndQueryObj, nearestFalseObjIdForBoundaryEnd);

								for (RoadObject rO : nearestFalseObjWithDistBoundaryStart.keySet()) {

									answerList.add(new Tuple3<Integer, Integer, Double>(boundaryStartQueryObj,
											rO.getObjectId(), Math.abs(nearestFalseObjWithDistBoundaryStart.get(rO))));

								}

								for (RoadObject rO : nearestFalseObjWithDistBoundaryEnd.keySet()) {

									answerList.add(new Tuple3<Integer, Integer, Double>(boundaryEndQueryObj,
											rO.getObjectId(), Math.abs(nearestFalseObjWithDistBoundaryEnd.get(rO))));

								}

								for (int i = m_objectIdClusters.get(objectClusterIndex).indexOf(boundaryStartQueryObj)
										+ 1; i < m_objectIdClusters.get(objectClusterIndex).size() - 1; i++) {

									int currentTrueObject = m_objectIdClusters.get(objectClusterIndex).get(i);
									LinkedList<Integer> currentObjCluster = new LinkedList<Integer>();
									currentObjCluster.addAll(m_objectIdClusters.get(objectClusterIndex));
									LinkedList<Integer> currentNodeCluster = new LinkedList<Integer>();

									currentNodeCluster.addAll(m_nodeIdClusters.get(clusteringObjects
											.getObjectClusterNodeClusterInfo().get(objectClusterIndex)));

									double distToBoundaryStartObj = m_graph.getDistanceBetweenBoundaryObjAndCurrentObj(
											currentNodeCluster, currentObjCluster, boundaryStartQueryObj,
											currentTrueObject);
									double distToBoundaryEndObj = m_graph.getDistanceBetweenBoundaryObjAndCurrentObj(
											currentNodeCluster, currentObjCluster, boundaryEndQueryObj,
											currentTrueObject);

									double distanceFromCurrentObjectToBoundaryStartNearestFalseObject = nearestFalseObjDistBoundaryStart[0]
											+ distToBoundaryStartObj;
									double distanceFromCurrentObjectToBoundaryEndNearestFalseObject = nearestFalseObjDistBoundaryEnd[0]
											+ distToBoundaryEndObj;

									if (distanceFromCurrentObjectToBoundaryStartNearestFalseObject > distanceFromCurrentObjectToBoundaryEndNearestFalseObject) {
										answerList.add(new Tuple3<Integer, Integer, Double>(currentTrueObject,
												nearestFalseObjIdForBoundaryEnd,
												Math.abs(distanceFromCurrentObjectToBoundaryEndNearestFalseObject)));
//										m_nearestNeighborSets.put(currentTrueObject,
//												nearestFalseObjIdForBoundaryEnd);
									} else {
										answerList.add(new Tuple3<Integer, Integer, Double>(currentTrueObject,
												nearestFalseObjIdForBoundaryStart,
												Math.abs(distanceFromCurrentObjectToBoundaryStartNearestFalseObject)));
									}

//									if (distanceFromCurrentObjectToBoundaryStartNearestFalseObject > distanceFromCurrentObjectToBoundaryEndNearestFalseObject) {
//										m_nearestNeighborSets.put(currentTrueObject, nearestFalseObjIdForBoundaryEnd);
//									} else {
//										m_nearestNeighborSets.put(currentTrueObject, nearestFalseObjIdForBoundaryStart);
//									}

								}

							}
						}
					}
				}

			}

			// TODO Auto-generated method stub
			return answerList;
		}

	}

	/**
	 * ending bracket
	 * 
	 */
}
