#!/bin/bash

echo "Copying data to Hadoop"

set +e
sudo docker run -privileged -v $(pwd):/tmp/MovementVectorJob teco/cdh3-hadoop-command --ip=$@ "dfs -rm data/KDDTrain+.arff"
sudo docker run -privileged -v $(pwd):/tmp/MovementVectorJob teco/cdh3-hadoop-command --ip=$@ "dfs -rm data/KDDTest+.arff"
sudo docker run -privileged -v $(pwd):/tmp/MovementVectorJob teco/cdh3-hadoop-command --ip=$@ "dfs -rm data/KDDTrain+.info"
sudo docker run -privileged -v $(pwd):/tmp/MovementVectorJob teco/cdh3-hadoop-command --ip=$@ "dfs -rmr nsl-forest"
sudo docker run -privileged -v $(pwd):/tmp/MovementVectorJob teco/cdh3-hadoop-command --ip=$@ "dfs -copyFromLocal /tmp/MovementVectorJob/data/KDDTrain+.arff data/KDDTrain+.arff"
sudo docker run -privileged -v $(pwd):/tmp/MovementVectorJob teco/cdh3-hadoop-command --ip=$@ "dfs -copyFromLocal /tmp/MovementVectorJob/data/KDDTest+.arff data/KDDTest+.arff"
set -e

echo "Generating a file descriptor for the dataset"
sudo docker run -privileged -v $(pwd):/tmp/MovementVectorJob teco/cdh3-hadoop-command --ip=$@ "jar /tmp/MovementVectorJob/mahout-0.8/mahout-examples-0.8-job.jar org.apache.mahout.classifier.df.tools.Describe -p data/KDDTrain+.arff -f data/KDDTrain+.info -d N 3 C 2 N C 4 N C 8 N 2 C 19 N L"

echo "Running example"
sudo docker run -privileged -v $(pwd):/tmp/MovementVectorJob teco/cdh3-hadoop-command --ip=$@ "jar /tmp/MovementVectorJob/mahout-0.8/mahout-examples-0.8-job.jar org.apache.mahout.classifier.df.mapreduce.BuildForest -Dmapred.max.split.size=1874231 -d data/KDDTrain+.arff -ds data/KDDTrain+.info -sl 5 -p -t 100 -o nsl-forest"

echo "Using the Decision Forest to Classify new data"
sudo docker run -privileged -h tecopc-hadoop -v $(pwd):/tmp/MovementVectorJob teco/cdh3-hadoop-command --ip=$@ "jar /tmp/MovementVectorJob/mahout-0.8/mahout-examples-0.8-job.jar org.apache.mahout.classifier.df.mapreduce.TestForest -i data/KDDTest+.arff -ds data/KDDTrain+.info -m nsl-forest -a -mr -o predictions"

