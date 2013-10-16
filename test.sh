#!/bin/bash

echo "Copying data to Hadoop"

set +e
sudo docker run -privileged -v $(pwd):/tmp/MovementVectorJob teco/cdh3-hadoop-command --ip=$@ "dfs -rm sensoroutput.txt sensoroutput.txt"
sudo docker run -privileged -v $(pwd):/tmp/MovementVectorJob teco/cdh3-hadoop-command --ip=$@ "dfs -copyFromLocal /tmp/MovementVectorJob/data/sensoroutput.txt data/sensoroutput.txt"
set -e

echo "Running on test dataset"
sudo docker run -privileged -v $(pwd):/tmp/MovementVectorJob teco/cdh3-hadoop-submit-job --ip=$@ --job=/tmp/MovementVectorJob/teco-motionvector-0.8.jar "vector -i data/sensoroutput.txt -o vectortest"
echo "Creating training and holdout set with a random 80-20 split of the generated vector dataset"
sudo docker run -privileged -v $(pwd):/tmp/MovementVectorJob teco/cdh3-hadoop-submit-job --ip=$@ --job=/tmp/MovementVectorJob/mahout-0.8/mahout-examples-0.8-job.jar "org.apache.mahout.driver.MahoutDriver split \
    -i vectortest \
    --trainingOutput vectortest-train \
    --testOutput vectortest-test  \
    --randomSelectionPct 40 --overwrite --sequenceFiles -xm sequential"

echo "Training Naive Bayes model"
sudo docker run -privileged -v $(pwd):/tmp/MovementVectorJob teco/cdh3-hadoop-submit-job --ip=$@ --job=/tmp/MovementVectorJob/mahout-0.8/mahout-examples-0.8-job.jar "org.apache.mahout.driver.MahoutDriver trainnb \
    -i vectortest-train -el \
    -o vectortest-model \
    -li vectortest-labelindex \
    -ow"

echo "Self testing on training set"

sudo docker run -privileged -v $(pwd):/tmp/MovementVectorJob teco/cdh3-hadoop-submit-job --ip=$@ --job=/tmp/MovementVectorJob/mahout-0.8/mahout-examples-0.8-job.jar "org.apache.mahout.driver.MahoutDriver testnb \
    -i vectortest-train\
    -m vectortest-model \
    -l vectortest-labelindex \
    -ow -o vectortest-testing"

echo "Testing on holdout set"

sudo docker run -privileged -v $(pwd):/tmp/MovementVectorJob teco/cdh3-hadoop-submit-job --ip=$@ --job=/tmp/MovementVectorJob/mahout-0.8/mahout-examples-0.8-job.jar "org.apache.mahout.driver.MahoutDriver testnb \
    -i vectortest-test\
    -m vectortest-model \
    -l vectortest-labelindex \
    -ow -o vectortest-testing"