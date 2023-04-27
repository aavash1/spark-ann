This is an implementation of Apache spark based processing of All-Nearest Neighbor Queries in a Road Network. The centralized version of this work is already my repository named as SQORN.

The required external JAR file is saved as "MESSIF.jar" and It helps to find a shortest path between two vertices. Includes YEN algorithm, huge thanks to the author. Original ksp.jar source can be found in "https://github.com/bsmock/k-shortest-paths".

To run the program; the two java files in the main package must be exported as "Runnable Jar file"

DistributedINE.class is the javafile to run the Incremental Network Expansion code
GraphNetworkSCLAlgorithm.class is the javafile to run SCl algorithm

Once the JAR file is created it must be moved to the main driver of the cluster. And run using spark-submit and pass the arguments
"spark-submit --master yarn --deploy-mode cluster --conf "spark.yarn.am.waitTime=500" --class main.ClassName --num-executors #  jarFileLocation args[0] args[1] args[2] args[3] args[4] args[5] args[6] args[7]" 
 The arguments are:
 args[0] Dataset Name
 args[1] Node Dataset File
 args[2] Edge Dataset File
 args[3] Partition File
 args[4] Partition Value
 args[5] Query object size
 args[6] Data object size
 args[7] distribtuion value
 
 Datasets freely available at: https://users.cs.utah.edu/~lifeifei/SpatialDataset.htm
