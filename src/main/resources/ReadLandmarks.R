MyData <- read.csv(file="C:/Users/Bene/DBPro-EKF-SLAM/src/main/resources/positionWithLmrks.csv", header=FALSE, sep=",")

MyData

x <- MyData$V2
y <- MyData$V3

#lmrksX = c(lm1x,lm2x)


for(e in MyData){
    n = ncol(e)
 }
