#Plot of one of our cleaned data sets.
#Cols: timestamp, latitude, longitude, laser-array
gpsData <- read.csv(file="C:/Users/Bene/DBPro-EKF-SLAM/src/main/resources/time_lat_lon_aa3_gpsx.csv", header=FALSE, sep=",")
str(gpsData)

x1 <- gpsData$V2
y1 <- gpsData$V3

plot(x1, y1,
     main="MovingCar",
     ylab = "",
     type = "l",
     col="red")

#Plots the actual movement of the vehicle, according to the data.
gpsData <- read.csv(file="C:/Users/Bene/DBpro-EKF-SLAM/src/main/resources/testcsvODO.csv", header = FALSE, sep=",")

plot(gpsData$V2, gpsData$V3,
       main = "Actual tracking of the vehicle",
       type="l",
       col="red")

