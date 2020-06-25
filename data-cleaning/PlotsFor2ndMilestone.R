#position of landmarks
land_x <- c(1,2,2.5)
land_y <- c(1.5,1.5,0.7)


#situation in t-1: position, landmarks, and distance between pos and landmarks
plot(0,0,
     main = "state t-1",
     col="red",
     type = "p",
     xlab="x",
     ylab="y",
     xlim = c(-0.5,3),
     ylim = c(-0.5,2),
     pch=19,
     cex = 2,
     cex.main =3)

lines(c(0,1), c(0,1.5), col="red")
lines(c(0,2), c(0,1.5), col="red")
lines(c(0,2.5), c(0,0.7), col="red")
points(land_x,land_y,col="blue", pch=19, cex=3)

#prediction step, position changes, landmarks stay, no distances between pos and landmarks
plot(c(0,1),c(0,0.5),
     main = "precition step",
     col="red",
     type = "p",
     xlab="x",
     ylab="y",
     xlim = c(-0.5,3),
     ylim = c(-0.5,2),
     pch=19,
     cex = 2,
     cex.main = 3)
arrows(0,0,1,0.5,
       length=0.2,
       adj=1,
       col="black",
       lwd=3)

points(land_x,land_y,col="blue", pch=19, cex=3)

#update step: measure distances between new pos and landmarks, use for updates in algorithm

plot(c(0,1),c(0,0.5),
     main = "update step",
     col="red",
     type = "p",
     xlab="x",
     ylab="y",
     xlim = c(-0.5,3),
     ylim = c(-0.5,2),
     pch=19,
     cex = 2,
     cex.main = 3)
arrows(0,0,1,0.5,
       length=0.2,
       adj=1,
       col="black",
       lwd=3)
lines(c(1,1), c(0.5,1.5), col="red")
lines(c(1,2), c(0.5,1.5), col="red")
lines(c(1,2.5), c(0.5,0.7), col="red")
points(land_x,land_y,col="blue", pch=19, cex=3)

