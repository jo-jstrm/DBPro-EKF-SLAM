#Plots movement data and landmarks.

library(stringr)

MyData <- read.csv(file="C:/Users/Bene/DBPro-EKF-SLAM/src/main/resources/positionWithLmrks.csv", header=FALSE, sep=";")

str(MyData)

x <- MyData$V2
y <- MyData$V3
lmrks <- MyData$V4

plot(0,0,main="Movement with landmarks",
     xlim=c(-300,300),
     ylim=c(-450,150))

for(i in 1 : nrow(MyData)){
  x <- MyData[i, "V2"]
  y <- MyData[i, "V3"]
  lmrks <- toString(MyData[i, "V4"])
  splitted <- as.list(unlist(strsplit(lmrks,"[,]")))
  points(x,y,col="red")

  for(j in seq(from=1,to=length(splitted)-1,by=2)){
    lmrkx = splitted[[j]]
    lmrky = splitted[[j+1]]
    
    options(digits=18)
    lmrkx = as.double(gsub("[]()[]", "", lmrkx))
    lmrky = as.double(gsub("[]()[]", "", lmrky)) 
    
    points(lmrkx,lmrky,col="blue")
  }
  }

plot(x,y,type="l",col="red")

strtoi(c("0xff", "077", "123"))

