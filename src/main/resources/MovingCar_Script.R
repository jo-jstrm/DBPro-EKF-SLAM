MyData <- read.csv(file="C:/Users/Bene/DBPro-EKF-SLAM/src/main/resources/testcsv.csv", header=FALSE, sep=",")

x <- MyData$V2
y <- MyData$V3

plot(x, y,
     main="MovingCarBiatch",
     ylab = "",
     type = "l",
     col="blue")


gpsData <- read.csv(file="C:/Users/Bene/DBPro-EKF-SLAM/src/main/resources/time_lat_lon_aa3_gpsx.csv", header=FALSE, sep=",")
str(gpsData)

x1 <- gpsData$V2
y1 <- gpsData$V3

plot(x1, y1,
     main="MovingCarBiatch",
     ylab = "",
     type = "l",
     col="red")

odoData <- read.csv(file="C:/Users/Bene/DBPro-EKF-SLAM/src/main/resources/testcsvODO.csv", header=FALSE, sep=",")

plot(odoData$V2, odoData$V3,
     main="Schnauze",
     type="l",
     col="black")


gpsData <- read.csv(file="C:/Users/Bene/DBpro-EKF-SLAM/src/main/resources/testcsvODO.csv", header = FALSE, sep=",")

plot(gpsData$V2, gpsData$V3,
       main = "actual tracking of the vehicle",
       type="l",
       col="red")

