# PositionCoordinates
This Android app was developed in the context of a masters dissertation, with the objective of creating a navigation and localization system for a museum. Its purpose was to test the viablibility of the planned system.

The user's position is estimated in two different ways:
- Based on the trajectory, by using the sensors' readings which are collect and interpreted, so as to calculate the user's path. Three main elements are used in the trajectory calculation:
  - Pedometer, which looks for the step signature in the accelerometer's measures.
  - Turn detection, which looks for the turn signature and respective direction in the gyroscope's measures.
  - Compass, which determines the user's orientation (azimuth), based on sensor fusion (adapted from Paul Lawitzki's work in http://plaw.info/articles/sensorfusion/).
 - Based on Fingerprinting (incomplete), by using the measured beacons' RSSI value, the sensors' readings and the estimated azimuth to create a fingerprint that is sent to a remote server, which in turn returns its location prediction. This is used to correct the odometry error.
