# Muvr

[muvr](http://www.muvr.io/) is a demonstration of an application that uses _wearable devices_ (Pebble)—in combination with a mobile app—to submit physical (i.e. accelerometer, compass) and biological (i.e. heart rate) information to a CQRS/ES cluster to be analysed.

#### muvr-analytics
`muvr-analytics` contains analytics pipelines external to the main application, including
* pipelines to suggest future exercise sessions
* pipelines to classify users to groups by attribute similarity
* pipelines to improve classification and other models
* pipelines to train models to recognize repetitions of exercises

This part of the project can be viewed as a data science playground. The models used in the application are trained using spark jobs written in either scala or python. Tasks that require more explorational analysis can be done using R.

#### Other components of the system
- [muvr-server](https://github.com/muvr/muvr-server) CQRS/ES cluster 
- [muvr-ios](https://github.com/muvr/muvr-ios)iOS application showcasing mobile machine learning and data collection
- [muvr-pebble](https://github.com/muvr/muvr-pebble) Pebble application, example implementation of a wearable device 
- [muvr-preclassification](https://github.com/muvr/muvr-preclassification) mobile data processing and classification

## Getting started
Basic information to get started is below. Please also have a look at the other components of the system to get a better understanding how everything fits together.

### Installation
Make sure your Java is up to date (>= 1.8.0) and you got [sbt](http://www.scala-sbt.org/) installed .
```
git clone git@github.com:muvr/muvr-analytics.git

# To deserialize the messages stored in the cassandra cluster 
# we need some libraries of the `muvr-server`
git clone git@github.com:muvr/muvr-server.git

cd muvr-analytics
```
There are two steps to train models that can be used by mobile clients. First the data needs to be prepared. This includes reading data from the cassandra cluser and grouping it in a way that a single group contains all examples the machine learning algorithm will be trained on. 
```
# To run the data preparation pipeline use the scala spark job
./run_spark.sh "basic.DatasetExtractionMain"
```
The second step is to train a machine learning model for each of this groups.
```
cd mlp
./run_spark.sh
```
This will train a Multi Layer Perceptron on the previously generated datasets.

### Issues

For any bugs or feature requests please:

1. Search the open and closed
   [issues list](https://github.com/muvr/muvr-analytics/issues) to see if we're
   already working on what you have uncovered.
2. Make sure the issue / feature gets filed in the relevant components (e.g. server, analytics, ios)
3. File a new [issue](https://github.com/muvr/muvr-analytics/issues) or contribute a 
  [pull request](https://github.com/muvr/muvr-analytics/pulls) 

## License
Please have a look at the [LICENSE](https://github.com/muvr/muvr-analytics/blob/develop/LICENSE) file.
