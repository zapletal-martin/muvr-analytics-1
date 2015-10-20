########################################
# Data cleansing
########################################

exampleLengths <- function(examples) {
  return(sapply(examples, function(e){length(e$x)}))
}

enforceMinLength <- function(examples, minLength){
  # Remove examples with a length that is to short to extract the necessary amount of features
  return(examples[which(exampleLengths(examples) >= minLength)])
}

enforceProperLabel <- function(examples){
  # Remove examples that are not annotated properly (indicated by an "x")
  return(examples[which(sapply(examples, function(e){ e$label}) != "x")])
}

