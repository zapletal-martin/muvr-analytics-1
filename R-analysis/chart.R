########################################
# Knobs to turn
########################################

# Parent directory of the training data
rootdir <- "../../../../../../muvr-training-data/labelled"

lengthPerChannel <- 400


########################################
# Load data from CSV files
########################################

# Retrieve all the CSV file paths from the root directory to use them as examples
relativeFiles <- list.files(rootdir, recursive = TRUE, pattern = "*.csv")
csvFiles <- sapply(relativeFiles, function(p){file.path(rootdir, p)})

examples <- lapply(csvFiles, function(path)
{
  print(path)
  
  # Load and stack the data as a row vector
  movement_L <- read.csv(file = path, header = FALSE)
  movement <- data.frame(movement_L)
  
  return(list(
    label=levels(movement[1, 2]), 
    x=movement[,ncol(movement)-2], 
    y=movement[,ncol(movement)-1], 
    z=movement[,ncol(movement)]))
})

########################################
# Data cleansing
########################################

minLength <- lengthPerChannel

exampleLengths <- function(examples) {
  return(sapply(examples, function(e){length(e$x)}))
}

# Remove examples that are not annotated properly (indicated by an "x")
examples <- examples[which(sapply(examples, function(e){ e$label}) != "x")]

# Remove examples with a length that is to short to extract the necessary amount of features
examples <- examples[which(exampleLengths(examples) >= minLength)]

########################################
# Data visualisation
########################################

table(exampleLengths(examples))

# Statistics over the training examples
print("Length distribution")
print(table(exampleLengths(examples)))
hist(exampleLengths(examples))

# Statistics over label distributions
print("Label distribution")
labels <- sapply(examples, function(x){x$label})
cat("Number of labels ", length(c(table(labels))), "\n")
print(table(labels))

labelDist <- sort(table(labels), decreasing = TRUE)
end_point <- 0.5 + length(labelDist) + length(labelDist)-1 #this is the line which does the trick (together with barplot "space = 1" parameter)
op <- par(mar = c(7,4,4,2) + 0.1) ## lots of extra space in the margin for side 1
barplot(labelDist,las=2, 
        space = 1, 
        axisnames = FALSE, 
        main = "Number of examples per exercise", 
        ylab = "Number of examples", 
        col = terrain.colors(2, alpha = 1))
text(seq(1.5,end_point,by=2), par("usr")[3]-0.25, 
     srt = 60, adj= 1, xpd = TRUE,
     labels = paste(rownames(labelDist)), cex=0.65)
par(op) ## reset

# Plot all the examples for a given exercise
op <- par(mar = c(2,4,2,2) + 0.1)
plotExercise <- function(examples, exerciseName, maxExamples=5){
  filtered <- examples[which(sapply(examples, function(x){x$label == exerciseName}))]
  if(length(filtered) > maxExamples){
    filtered <- sample(filtered, size=maxExamples)
  }
  maxX <- max(exampleLengths(filtered))
  cat("Visualizing", exerciseName, ". Found", length(filtered), "examples\n")
  par(mfrow=c(3,1))
  colors <- terrain.colors(length(filtered), alpha = 1)
  for(dimension in c('x', 'y', 'z')){
    smoothedData <- sapply(filtered, function(x){ smooth(x[[dimension]]) }, simplify = TRUE)
    maxY <- max(sapply(smoothedData, max))
    minY <- min(sapply(smoothedData, min))
    plot(c(), 
         xlim = c(0, maxX), 
         ylim=c(minY, maxY),
         xlab = "Time",
         ylab = paste(dimension, "-acceleration", sep = ""))

    for(i in (1:length(filtered))){
      lines(smoothedData[[i]], col = colors[[i]])  
    }
  }
  title(main=paste("Training examples for '", exerciseName, "' (max=",maxExamples,")", sep = ""), outer = TRUE)
}

plotExercise(examples, 'dumbbell-press', maxExamples = 1000)
plotExercise(examples, 'triceps-dips')
plotExercise(examples, 'barbell-biceps-curl')
plotExercise(examples, 'dumbbell-chest-press')
plotExercise(examples, 'dumbbell-chest-fly')

par(op) ## reset

########################################
# Input data extraction
########################################
windowStepSize = 5
train_test_ratio = 0.8

augmentExample <- function(e){
  label <- e$label
  minIdx = floor(length(e$x) * 0.2)
  maxIdx = ceiling(length(e$x) * 0.8)
  if (maxIdx - minIdx < lengthPerChannel) {
    # No window augmentation since the example is to short
    start <- (length(e$x) - lengthPerChannel) / 2
    idxs <- start:(start + lengthPerChannel - 1)
    x <- matrix(e$x[idxs], nrow=1, ncol=lengthPerChannel, dimnames = list(c(1), 1:lengthPerChannel))
    y <- matrix(e$y[idxs], nrow=1, ncol=lengthPerChannel, dimnames = list(c(1), 1:lengthPerChannel))
    z <- matrix(e$z[idxs], nrow=1, ncol=lengthPerChannel, dimnames = list(c(1), 1:lengthPerChannel))
    return(data.frame(label=label, X=x, Y=y, Z=z))
  } else {
    N <- floor((maxIdx-lengthPerChannel-minIdx) / windowStepSize) + 1
    cat("Augmenting with ", N, "examples\n")
    x <- matrix(nrow=N, ncol=lengthPerChannel, dimnames = list(c(1:N), 1:lengthPerChannel))
    y <- matrix(nrow=N, ncol=lengthPerChannel, dimnames = list(c(1:N), 1:lengthPerChannel))
    z <- matrix(nrow=N, ncol=lengthPerChannel, dimnames = list(c(1:N), 1:lengthPerChannel))
    for(i in seq(minIdx, (maxIdx-lengthPerChannel),windowStepSize)){
      idx <- (i-minIdx) / windowStepSize + 1
      x[idx,] <- e$x[i:(i + lengthPerChannel - 1)]
      y[idx,] <- e$y[i:(i + lengthPerChannel - 1)]
      z[idx,] <- e$z[i:(i + lengthPerChannel - 1)]
    }
    return(data.frame(label=label, X=x, Y=y, Z=z))
  }
}

augmentExamples <- function(examples){
  emptyDF <- data.frame(label=rep(NA, 0), X=matrix(nrow=0, ncol=lengthPerChannel), Y=matrix(nrow=0, ncol=lengthPerChannel), Z=matrix(nrow=0, ncol=lengthPerChannel))
  dfs <- list()
  i = 1
  for(e in examples){
    dfs[[i]] <- augmentExample(e)
    i <- i + 1
  }
  return(do.call(rbind, dfs))
}

keepOnlyCommonLabels <- function(examples, keepFilter){
  return(Filter(function(e){ e$label %in% keepFilter }, examples))
}

scaleFeatures <- function(ds){
  return((ds + 4000) / 8000)
}

# Filter out labels that are not common enough
mostCommonLabels <- names(labelDist)

# Split into test and train (we will ignore validation for now)
filteredExamples <- keepOnlyCommonLabels(examples, mostCommonLabels)

trainIdx <- sample(length(filteredExamples), length(filteredExamples) * train_test_ratio)
trainExamples <- filteredExamples[trainIdx] 
testExamples <- filteredExamples[-trainIdx] 

# Augment and write the training data
augmentedTrainExamples <- augmentExamples(trainExamples)

train <- data.frame(labels = factor(augmentedTrainExamples$label, levels=mostCommonLabels, ordered = TRUE), 
                    numeric_labels = rep(NA, nrow(augmentedTrainExamples)), 
                    scaleFeatures(augmentedTrainExamples[2:ncol(augmentedTrainExamples)]))

train['numeric_labels'] <- sapply(train$label, as.numeric)
train$labels <- NULL
train <- train[sample(nrow(train)),]

write.csv(train, file = paste("~/data/labeled_exercise_data_f", lengthPerChannel,"_TRAIN.csv", sep = ""), row.names=FALSE)

# Process and write test data
augmentedTestExamples <- augmentExamples(testExamples)

test <- data.frame(labels = factor(augmentedTestExamples$label, levels=mostCommonLabels, ordered = TRUE), 
                    numeric_labels = rep(NA, nrow(augmentedTestExamples)), 
                   scaleFeatures(augmentedTestExamples[2:ncol(augmentedTestExamples)]))

test['numeric_labels'] <- sapply(test$label, as.numeric)
test <- test[sample(nrow(test)),]

write.csv(levels(test$labels), file = paste("~/data/labeled_exercise_data_f", lengthPerChannel,"_LABELS.csv", sep = ""))

test$labels <- NULL
write.csv(test, file = paste("~/data/labeled_exercise_data_f", lengthPerChannel,"_TEST.csv", sep = ""), row.names=FALSE)

