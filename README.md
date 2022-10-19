# IMU Visualization
<br/>


In this project, I made a 3D visualization of my android phone's orientation.

![Architecture](https://github.com/SivadineshPonrajan/IMU-Visualization/blob/main/Images/Architecture.PNG)
<br/>
- I created an android application with *Android Studio* to fetch and display the raw sensor values from the phone's accelerometer, gyrometer and the magnetometer sensors.
- This app can stream the sensor data to other devices as well throught the UDP sockets.
- You can find the source code of the android application in the ["SensViz"](https://github.com/SivadineshPonrajan/IMU-Visualization/tree/main/SensViz) directory.
- The exported apk file can be found as ["SensViz.apk"](https://github.com/SivadineshPonrajan/IMU-Visualization/blob/main/SensViz.apk) which can be used to install the app directly to any android phones.
- Used the [Processing](https://processing.org/) software to build the visualizer for displaying the visualization.
- The Processing code can be found inside the ["visualizer"](https://github.com/SivadineshPonrajan/IMU-Visualization/tree/main/visualizer) folder.
- I used the Processing 3.5.4 Version here. (There may be few changes in the code if you use the latest version of the Processing).

> Make sure that you are connected to the same network when you execute the code. I connected to my mobile's hotspot.

<br/>

### Android Application

![App's UI](https://github.com/SivadineshPonrajan/IMU-Visualization/blob/main/Images/appInterface.PNG?raw=true)

### Configure IP Address
- As you can see in the above image, the app has settings menu.
- Here you can configure your IP Address of your PC and port number for your socket connection.

### Visualization

![Processing's UI](https://github.com/SivadineshPonrajan/IMU-Visualization/blob/main/Images/processing.png?raw=true)

- In the Processing, the raw sensor values are received through the UDP socket stream from the mobile app.
- The values are received as a single string everytime which are seperated by the commas.
- Then the values of the Euler angles are calculated from the received sensor data.
- Based on the Euler angle values, the solid is rotated in the environment.

<br />

> The [powerpoint presentation](https://github.com/SivadineshPonrajan/IMU-Visualization/blob/main/Presentation.pptx) is there as well. "Gesture controlled Quadcopter" part of the project is uploadded in different repository and you can find it [here](https://github.com/SivadineshPonrajan/Tello-Gyro).
