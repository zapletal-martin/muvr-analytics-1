########################################
# Load data from CSV files
########################################
readGymTrainingData <- function(rootdir){
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
}