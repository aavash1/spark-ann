package main;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import java.lang.instrument.Instrumentation;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.apache.spark.SparkConf;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;

import org.apache.spark.api.java.function.Function3;
import org.apache.spark.api.java.function.PairFlatMapFunction;

import org.apache.spark.api.java.function.VoidFunction;

import algorithm.ANNClusteredOptimizedWithHeuristic;

import algorithm.ClusteringNodes;
import algorithm.ClusteringRoadObjects;
import algorithm.NearestNeighbor;

import algorithm.RandomObjectGenerator;
import algorithm.RangeQueryAlgorithm;

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

public class GraphNetworkSCLAlgorithm {

	static List<Tuple3<Integer, Integer, Double>> ForComparison = new ArrayList<>();
	static List<Tuple3<Integer, Integer, Double>> ResultFromInitialMerging = new ArrayList<>();

	private static Instrumentation instrumentation;

	public static void premain(String args, Instrumentation inst) {
		instrumentation = inst;
	}

	public static void main(String[] args) throws Exception {
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");

		/**
		 * 1 Pass the path for loading the datasets 1.1 Dataset for graph containing
		 * nodes and edges
		 */
		// String nodeDatasetFile = "Dataset/TinygraphNodes.txt";
		// String edgeDataSetFile = "Dataset/TinyGraphEdge.txt";

		String datasetName = args[0];
		String nodeDatasetFile = args[1];
		String edgeDataSetFile = args[2];
		String metisPartitionOutputFile = args[3];
		String partitionValue = args[4];
		String queryObjectSize = args[5];
		String dataObjectSize = args[6];
		String distributionType = args[7];

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

		double SDValue = 0.0;
		int datasetScaleFactor = 0;
		if (datasetName.equalsIgnoreCase("cal")) {
			SDValue = 2;
			datasetScaleFactor = 1;
		} else if (datasetName.equalsIgnoreCase("nw")) {
			SDValue = 2;
			datasetScaleFactor = 3000;
		} else if (datasetName.equalsIgnoreCase("olden")) {
			SDValue = 2;
			datasetScaleFactor = 1000;
		} else if (datasetName.equalsIgnoreCase("sanj")) {
			SDValue = 2;
			datasetScaleFactor = 1990;
		} else {
			// Handle invalid input
			System.err.println("Invalid dataset name: " + datasetName);
			System.exit(1);
		}

		switch (distributionValue) {
		case 1:
			// Both Q and Q are uniformly distributed
			RandomObjectGenerator.generateUniformRandomObjectsOnMap(cGraph, querySize, dataSize);
			break;
		case 2:
			// <U,C> distribution for Q and D
			RandomObjectGenerator.zgenerateCUUCDistribution(cGraph, SDValue, datasetScaleFactor, querySize, dataSize,
					false);
			break;

		case 3:
			// <C,U> distribution for Q and D
			RandomObjectGenerator.zgenerateCUUCDistribution(cGraph, SDValue, datasetScaleFactor, querySize, dataSize,
					true);
			break;

		case 4:
			// Centroid for both Q and D
			RandomObjectGenerator.zgenerateCCDistribution(cGraph, SDValue, datasetScaleFactor, querySize, dataSize);

			break;
		}

//		RandomObjectGenerator.zgenerateCCDistribution(cGraph, 2, 1, Integer.parseInt(queryobjectSize),
//				Integer.parseInt(dataobjectSize));
		// RandomObjectGenerator.generateUniformRandomObjectsOnMap(cGraph, 100, 500);

		// String PCManualObject = "Dataset/manualobject/ManualObjectOnTinyGraph.txt";

		// String PCManualObject = "Dataset/manualobject/ManualObjectsOnRoad.txt";
		// UtilsManagement.readRoadObjectTxtFile1(cGraph, PCManualObject);

		// cGraph.printEdgesInfo();
		// cGraph.getObjectsWithInfo();

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
		Map<Integer, Integer> vertexIdPartitionIndexMap = new HashMap<Integer, Integer>();

		// Map<Integer, Integer> vertexIdPartitionIndexMap = new HashMap<Integer,
		// Integer>();
		for (int i = 0; i < graphPartitionIndex.size(); i++) {
			// System.out.println(i + " " + Long.valueOf(graphPartitionIndex.get(i)));
			vertexIdPartitionIndex.put(partitionIndexKey[i], Long.valueOf(graphPartitionIndex.get(i)));
			vertexIdPartitionIndexMap.put(partitionIndexKey[i], graphPartitionIndex.get(i));
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

		long preCompTimeStart = System.currentTimeMillis();
		Map<Integer, ArrayList<holder>> DARTTableMap = getNearestNeighborForBoundaryNodes(boundaryVerticesList,
				vertexIdPartitionIndexMap, cGraph);
		long preCompTimeEnd = System.currentTimeMillis();
		long preCompTotalTime = preCompTimeEnd - preCompTimeStart;
		double preCompTotalTimeInSecond = (double) preCompTotalTime / 1000;
		System.out.println("Precomputation Time: " + preCompTotalTime + " milli-seconds, " + preCompTotalTimeInSecond
				+ " seconds");

		long size = estimateSize(DARTTableMap);

		System.out.println("Estimated size of map in bytes: " + size);

//		for (Integer borderNode : DARTTableMap.keySet()) {
//			for (holder result : DARTTableMap.get(borderNode)) {
//
//				System.out.println("Border Vertex: " + borderNode + " Nearest Data object is: "
//						+ DARTTableMap.get(borderNode).toString());
//
//			}
//
//		}

		/**
		 * Load Spark Necessary Items
		 */

//		SparkConf config = new SparkConf().setAppName("ANN-SCL-FIN").set("spark.locality.wait", "0")
//				.set("spark.submit.deployMode", "cluster").set("spark.driver.maxResultSize", "6g")
//				.set("spark.executor.memory", "6g").setMaster("spark://35.194.176.13:8188").set("spark.cores.max", "8")
//				.set("spark.blockManager.port", "10025").set("spark.driver.blockManager.port", "10026")
//				.set("spark.driver.port", "10027").set("spark.shuffle.service.enabled", "false")
//				.set("spark.dynamicAllocation.enabled", "false")
//				.set("spark.metrics.conf.*.sink.console.class", "org.apache.spark.metrics.sink.ConsoleSink");

		// SparkConf config = new
		// SparkConf().setMaster("local[*]").setAppName("ANNCLUSTERED");

		// SparkConf config = new
		// SparkConf().setAppName("ANN-SCL-FIN").setMaster("local[*]");

		// For the cluster we can use any config from the below 2 config:
		// BELOW, I repeat DOWN BELOW.

//		SparkConf config = new SparkConf().setAppName("ANN-SCL-FIN").set("spark.submit.deployMode", "cluster")
		// .set("spark.driver.maxResultSize", "4g").set("spark.executor.memory",
		// "4g").set("spark.cores.max", "8");

		SparkConf config = new SparkConf().setAppName("ANN-SCL-FIN").set("spark.submit.deployMode", "cluster")
				.setMaster("yarn").set("spark.driver.maxResultSize", "4g").set("spark.executor.memory", "4g")
				.set("spark.cores.max", "8");

		JavaSparkContext jscontext = new JavaSparkContext(config);

		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");

		JavaRDD<List<Tuple3<Integer, Integer, Double>>> pathRDD = jscontext.parallelize(shortestPathList)
				.map(new Function<List<Path>, List<Tuple3<Integer, Integer, Double>>>() {

					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public List<Tuple3<Integer, Integer, Double>> call(List<Path> shortestPathList) throws Exception {

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

		// CoreGraph embeddedGraph = createAugmentedNetwork(cGraph, pathRDD,
		// BoundaryEdge, boundaryPairVertices);
		// embeddedGraph.printEdgesInfo();

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

		// final LongAccumulator totalTimeAccumulator =
		// jscontext.sc().longAccumulator("totalTime");

		long executorsStartTime = System.currentTimeMillis();

		toCreateSubgraphRDD.foreachPartition(
				new VoidFunction<Iterator<Tuple2<Object, Iterable<Tuple4<Object, Object, Double, ArrayList<RoadObject>>>>>>() {

					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					List<Tuple3<Integer, Integer, Double>> nearestNeighborList = new ArrayList<Tuple3<Integer, Integer, Double>>();

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
							// long startTime = System.currentTimeMillis();
							nearestNeighborList = can.call(subGraph0, true, nodeCluster);
							// long endTime = System.currentTimeMillis();
							// double duration = endTime - startTime;
							// ForComparison = nearestNeighborList;
							ForComparison.addAll(nearestNeighborList);

							// totalTimeAccumulator.add((long) duration);
							// System.out.println("Time taken to run algorithm: " + duration + "
							// milli-seconds");
						}

					}
				});

		long executorsEndTime = System.currentTimeMillis();
		double jobExecutionTime = executorsEndTime - executorsStartTime;
		System.out.println("The " + CustomPartitionSize + " executors took: " + jobExecutionTime + " milli-seconds");

		jscontext.close();

//		long TotalTime = totalTimeAccumulator.value();
//		System.out.println("Total Time Taken by the accumulators to run SCL: " + TotalTime);

		long mergeStepStart = System.currentTimeMillis();
		List<Tuple3<Integer, Integer, Double>> mergeStep1 = initialMerging(cGraph, vertexIdPartitionIndexMap,
				boundaryVerticesList, ForComparison, DARTTableMap);
		ResultFromInitialMerging = mergeStep1;
		List<Tuple3<Integer, Integer, Double>> mergeStep2 = finalMerging(ResultFromInitialMerging, ForComparison,
				DARTTableMap);

		long mergeStepEnd = System.currentTimeMillis();
		long totalMergingTime = mergeStepEnd - mergeStepStart;
		double MergingDurationTotal = (double) totalMergingTime;
		System.out.println("Final Global ANN Merging Time: " + MergingDurationTotal + "milli-second");

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

	/**
	 * This method looks for the nearest data, object towards the different
	 * partitions. This method will be used to find the NN for the boundary vertex.
	 */

	public static Map<Integer, ArrayList<holder>> getNearestNeighborForBoundaryNodes(
			LinkedList<String> boundaryVerticesList, Map<Integer, Integer> vertexIdPartitionIndexMap,
			CoreGraph cGraph) {
		Map<Integer, ArrayList<holder>> DARTTableMap = new HashMap<>();
		String differentPartition = "dp";

		for (String bVertex : boundaryVerticesList) {

			int nodeId = Integer.parseInt(bVertex);
			int dataObjectFound = 0;

			PriorityQueue<Node> pq = new PriorityQueue<>();
			boolean visited[] = new boolean[cGraph.getNodesWithInfo().size() + 1];
			pq.offer(new Node(nodeId, 0.0));
			visited[nodeId] = true;

			ArrayList<holder> NNLists = new ArrayList<>();

			while (!pq.isEmpty()) {
				Node currentNode = pq.poll();

				for (int adjacentNodes : cGraph.getAdjNodeIds(currentNode.getM_intNodeId())) {
					if (!visited[adjacentNodes]) {
						visited[adjacentNodes] = true;
						double distance = currentNode.getM_distance();
						int edgeId = cGraph.getEdgeId(currentNode.getM_intNodeId(), adjacentNodes);
						List<RoadObject> objects = cGraph.getObjectsOnEdges().get(edgeId);

						if (objects != null) {
							for (RoadObject rObj : objects) {
								if ((rObj.getType() == false) && (dataObjectFound < 2)) {
									distance += rObj.getDistanceFromStartNode();

									if (vertexIdPartitionIndexMap.get(currentNode
											.getM_intNodeId()) != vertexIdPartitionIndexMap.get(adjacentNodes)) {
										holder hold = new holder(rObj.getObjectId(), differentPartition, distance,
												rObj.getType());

										NNLists.add(hold);
										DARTTableMap.put(currentNode.getM_intNodeId(), NNLists);
										dataObjectFound += 1;
									} else {
										continue;
									}

								}
							}

						} else {
							distance += cGraph.getEdgeDistance(currentNode.getM_intNodeId(), adjacentNodes);
							pq.offer(new Node(adjacentNodes, distance));
						}
					}
				}

			}

		}

		return DARTTableMap;

	}

	public static Map<Integer, ArrayList<holder>> findNNForEmbeddedGraph(LinkedList<String> boundaryVerticesList,
			Map<Integer, Integer> vertexIdPartitionIndexMap, CoreGraph cGraph) {

		Map<Integer, ArrayList<holder>> DARTTableMap = new HashMap<>();

		String differentPartition = "dp";
		String samePartition = "sp";

		for (String bVertex : boundaryVerticesList) {

			int nodeId = Integer.parseInt(bVertex);
			int dataObjectFound = 0;

			boolean dpFlag = true;

			// List<Tuple3<String, Integer, Double>> nnList = new ArrayList<>();

			PriorityQueue<Node> pq = new PriorityQueue<>();
			boolean visited[] = new boolean[cGraph.getNodesWithInfo().size() + 1];
			pq.offer(new Node(nodeId, 0.0));
			visited[nodeId] = true;

			ArrayList<holder> NNLists = new ArrayList<>();

			while (!pq.isEmpty()) {
				Node currentNode = pq.poll();

				for (int adjacentNodes : cGraph.getAdjNodeIds(currentNode.getM_intNodeId())) {
					if (!visited[adjacentNodes]) {
						visited[adjacentNodes] = true;
						double distance = currentNode.getM_distance();
						int edgeId = cGraph.getEdgeId(currentNode.getM_intNodeId(), adjacentNodes);
						List<RoadObject> objects = cGraph.getObjectsOnEdges().get(edgeId);

						if (objects != null) {
							for (RoadObject rObj : objects) {
								if ((rObj.getType() == false) && (dataObjectFound < 2)) {
									distance += rObj.getDistanceFromStartNode();

									// Condition 1: If the currentNode and adjacentNodes are in different partitions
									if ((dpFlag == true) && (vertexIdPartitionIndexMap.get(currentNode
											.getM_intNodeId()) != vertexIdPartitionIndexMap.get(adjacentNodes))) {
										holder hold = new holder(rObj.getObjectId(), differentPartition, distance,
												rObj.getType());

										NNLists.add(hold);
										DARTTableMap.put(currentNode.getM_intNodeId(), NNLists);
										dataObjectFound += 1;
										dpFlag = false;

									}
									// Condition 2: If the currentNode and adjacentNodes are in boundaryVertex
									else if (boundaryVerticesList.contains(currentNode.getM_intNodeId())
											&& boundaryVerticesList.contains(adjacentNodes)) {
										holder hold = new holder(rObj.getObjectId(), samePartition, distance,
												rObj.getType());

										NNLists.add(hold);
										DARTTableMap.put(currentNode.getM_intNodeId(), NNLists);

										dataObjectFound += 1;

									}
									// Otherwise, continue searching
									else {
										continue;
									}
								}

							}
						} else {
							distance += cGraph.getEdgeDistance(currentNode.getM_intNodeId(), adjacentNodes);
							pq.offer(new Node(adjacentNodes, distance));
						}
					}
				}
			}

		}

		return DARTTableMap;
	}

	public static List<Tuple3<Integer, Integer, Double>> initialMerging(CoreGraph cGraph,
			Map<Integer, Integer> vertexIdPartitionIndexMap, LinkedList<String> boundaryVerticesList,
			List<Tuple3<Integer, Integer, Double>> FromExecutors, Map<Integer, ArrayList<holder>> DARTTable) {
		/**
		 * Calculates the intial merges step.
		 * 
		 * @param cGraph                  takes the whole graph
		 * @param vertexPartitionIndexMap takes the vertex partition info
		 * @param boundaryVerticesList    for running range query on each vertex id
		 * @param fromExecutors
		 * @param DARTTable
		 * @return the List of Tuple3 consisting the pruned query objects.
		 */

		RangeQueryAlgorithm rngQ = new RangeQueryAlgorithm(cGraph, vertexIdPartitionIndexMap);
		List<Tuple3<Integer, Integer, Double>> allQueriesTobeCompared = new ArrayList<>();
		/**
		 * Find the Query objects that are within the range for each boundarynodes
		 */

		for (Integer borderNodeId : DARTTable.keySet()) {

			Double heuristicDistance = rngQ.getHeuristicRange(cGraph, borderNodeId,
					DARTTable.get(borderNodeId).get(0).getDistance());
			List<Tuple3<Integer, Integer, Double>> queriesTobeCompared = rngQ.returnQueryWithinRange(borderNodeId,
					heuristicDistance);
			allQueriesTobeCompared.addAll(queriesTobeCompared);

//			for (Tuple3<Integer, Integer, Double> tuple : queriesTobeCompared) {
//				System.out.println(tuple._1() + " " + tuple._2() + " " + tuple._3());
//			}

		}
		return allQueriesTobeCompared;
	}

	public static List<Tuple3<Integer, Integer, Double>> finalMerging(
			List<Tuple3<Integer, Integer, Double>> fromMergeStep1, List<Tuple3<Integer, Integer, Double>> fromExecutors,
			Map<Integer, ArrayList<holder>> DARTTable) {

		/**
		 * Calculates the final merges step.
		 * 
		 * @param fromMergeStep1 the first Tuple3 from the mergestep1
		 * @param fromExecutors  the ANN result form executors
		 * @param DARTTable      embedded graph
		 * @return the List of Tuple3 with updated result set.
		 */
		List<Tuple3<Integer, Integer, Double>> finalResult = new ArrayList<>();

		for (Tuple3<Integer, Integer, Double> mergeStepInit : fromMergeStep1) {
			int borderNode = mergeStepInit._1();
			int queryObject = mergeStepInit._2();
			double distanceToQueryObjectFromBorderNode = mergeStepInit._3();

			if (fromExecutors != null) {
				for (Tuple3<Integer, Integer, Double> fromWorkerNodes : fromExecutors) {
					int queryObjectFromWorker = fromWorkerNodes._1();
					int nearestDObjFromQObj = fromWorkerNodes._2();
					double nnDistance = fromWorkerNodes._3();

					// if (queryObject == queryObjectFromWorker) {
					for (Integer borderVertex : DARTTable.keySet()) {
						int boundaryNode = borderVertex;
						if ((borderNode == boundaryNode) && (queryObject == queryObjectFromWorker)) {
							ArrayList<holder> nearestDataObject = DARTTable.get(boundaryNode);
							for (int i = 0; i < nearestDataObject.size(); i++) {
								double distanceToDataObjectFromBoundary = nearestDataObject.get(i).getDistance();

								if (nnDistance > distanceToQueryObjectFromBorderNode
										+ distanceToDataObjectFromBoundary) {
									int qObj = queryObject;
									int dObj = nearestDataObject.get(i).getObjectId();
									double updatedDistance = distanceToQueryObjectFromBorderNode
											+ distanceToDataObjectFromBoundary;
									finalResult.add(new Tuple3<Integer, Integer, Double>(qObj, dObj, updatedDistance));

								}

							}
						}
						finalResult.add(new Tuple3<Integer, Integer, Double>(queryObjectFromWorker, nearestDObjFromQObj,
								nnDistance));

					}

				}
			}

		}
		return finalResult;

	}

	public static long estimateSize(Map<Integer, ArrayList<holder>> map) {
		long size = 0;

		// Estimate the size of the map object
		size += getObjectSize(map);

		// Estimate the size of the map's elements
		for (Map.Entry<Integer, ArrayList<holder>> entry : map.entrySet()) {
			// Add the size of the key (an Integer object)
			size += getObjectSize(entry.getKey());

			// Add the size of the value (an ArrayList object)
			size += getObjectSize(entry.getValue());

			// Add the size of each element in the ArrayList (a Holder object)
			for (holder holder : entry.getValue()) {
				size += getObjectSize(holder);
			}
		}

		return size;
	}

	public static long getObjectSize(Object object) {
		if (instrumentation == null) {
			throw new IllegalStateException("Instrumentation is not initialized");
		}
		return instrumentation.getObjectSize(object);
	}

//To compare the Distances
//	static class NodeDistance implements Comparable<NodeDistance> {
//		int nodeId;
//		double distance;
//
//		public NodeDistance(int nodeId, double distance) {this
//			this.nodeId = nodeId;
//			this.distance = distance;
//		}
//
//		@Override
//		public int compareTo(NodeDistance o) {
//			return Double.compare(distance, o.distance);
//		}
//	}

	static class holder implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1766669831160333164L;
		private Integer objectId;
		private String side;
		private double distance;
		private boolean type;

		public holder(int objectId, String side, Double distance, boolean type) {
			this.objectId = objectId;
			if ((side.equalsIgnoreCase("dp")) || (side.equalsIgnoreCase("sp"))) {
				this.side = side;
			} else {
				throw new IllegalArgumentException("Invalid Input");
			}

			this.distance = distance;
			this.type = type;

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
			if ((side.equalsIgnoreCase("sp")) || (side.equalsIgnoreCase("dp"))) {
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

		public boolean getType() {
			return type;
		}

		@Override
		public String toString() {
			return "Object Details: {" + "objectId=" + objectId + ", side='" + side + '\'' + ", distance=" + distance
					+ ", type=" + type + '}';
		}

	}

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

									for (int k = 1; k < m_objectIdClusters.get(objectClusterIndex).size() - 1; k++) {

										for (RoadObject rO : nearestFalseObjectForRequiredQueryObject.keySet()) {

											answerList.add(new Tuple3<Integer, Integer, Double>(
													m_objectIdClusters.get(objectClusterIndex).get(k), rO.getObjectId(),
													Math.abs(nearestFalseObjectForRequiredQueryObject.get(rO))));

										}

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

									for (int k = 1; k < m_objectIdClusters.get(objectClusterIndex).size() - 1; k++) {

										for (RoadObject rO : nearestFalseObjectForRequiredQueryObject.keySet()) {

											answerList.add(new Tuple3<Integer, Integer, Double>(
													m_objectIdClusters.get(objectClusterIndex).get(k), rO.getObjectId(),
													Math.abs(nearestFalseObjectForRequiredQueryObject.get(rO))));

										}

									}

									for (int k = m_objectIdClusters.get(objectClusterIndex).size(); k <= 1; k--) {

										for (RoadObject rO : nearestFalseObjectForRequiredQueryObject.keySet()) {

											answerList.add(new Tuple3<Integer, Integer, Double>(
													m_objectIdClusters.get(objectClusterIndex).get(k), rO.getObjectId(),
													Math.abs(nearestFalseObjectForRequiredQueryObject.get(rO))));

										}

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

									} else {
										answerList.add(new Tuple3<Integer, Integer, Double>(currentTrueObject,
												nearestFalseObjIdForBoundaryStart,
												Math.abs(distanceFromCurrentObjectToBoundaryStartNearestFalseObject)));
									}

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
