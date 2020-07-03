# DBPro-EKF-SLAM

This is our project on implementing an Extended Kalman Filter for Simultaneous Localization and Mapping of a moving car through a distributed streaming engine, in this case Apache Flink. It took place in DIMA Group's Winterterm 2019 Database Project class at TU Berlin.

![alt text](https://raw.githubusercontent.com/jo-jstrm/DBPro-EKF-SLAM/master/report/tex/EKF-output.png)

## :round_pushpin: Description
The Extended Kalman Filter (EKF) is a state space model (SSM), widely used for tracking objects from noisy observations, such as signals generated by airplanes, vessels, robots, etc [1,2,3]. In SLAM: Simultaneous Localization and Mapping the location of a robot and the map of the environment are estimated at the same time [4]. The EKF algorithm is suitable for such scenario due to its ability to fuse nonlinear system kinematics, given a set of noisy meas-urements.  SLAM and Kalman Filtering have been studied and implemented in various toolsfor a long time but mainly applied to single machines and relatively small sets of batch data. Several traditional tools get short on processing the big amounts of stream data that can be generated nowadays or simply they are not capable of processing stream data. Therefore some researchers have started to consider the possibility to use and implement these techniques in large scale distributed processing systems, for example in [5] the EKF is implemented using the MapReduce frame-work, and in [6] the EKF is implemented for Vessels’ position tracking using Apache Flink.

## :books: Education Objective
Study  the  posibilities  of  implementing  EKF  SLAM  for  real  time  processing  of  stream  data  using  modern  dis-tributed stream processing systems such as Apache Flink.  The starting point of this project is a version of the Kalman Filter already implemented in Apache Flink.

## :file_folder: Repo Structure
This repository consists of the source code as well as the report (German only) on the usage of the Extended Kalman Filter in distributed streaming.
