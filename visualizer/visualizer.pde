import hypermedia.net.*;

UDP udp;  // define the UDP object
float [] Euler = new float [3]; 
float [] Acc = new float [3];
PFont font;
final int VIEW_SIZE_X = 1024, VIEW_SIZE_Y = 720;
float calib=0;
float magX = 0;
float magY = 0;
float magZ = 0;
float psihat = 0.0;
float now = millis();
float new_yaw = 0.0;
float new_pitch = 0.0;
int flag = 0;
float yaw_error = 0.0;
FloatList pitcher = new FloatList();
FloatList roller = new FloatList();
FloatList yawer = new FloatList();
int threshold = 3;


void setup() 
{
  udp = new UDP( this, 5555 );
  udp.listen( true );
  size(1024, 720, P3D);
  font = loadFont("CourierNew36.vlw"); 

}



void buildBoxShape() {
  //box(60, 10, 40);
  noStroke();
  beginShape(QUADS);
  
  //Z+ (to the drawing area) // Green
  fill(#00ff00);
  vertex(-20, -5, 30);
  vertex(20, -5, 30);
  vertex(20, 5, 30);
  vertex(-20, 5, 30);
  
  //Z- // Blue
  fill(#0000ff);
  vertex(-20, -5, -30);
  vertex(20, -5, -30);
  vertex(20, 5, -30);
  vertex(-20, 5, -30);
  
  //X- //Red
  fill(#ff0000);
  vertex(-20, -5, -30);
  vertex(-20, -5, 30);
  vertex(-20, 5, 30);
  vertex(-20, 5, -30);
  
  //X+ // Yellow
  fill(#ffff00);
  vertex(20, -5, -30);
  vertex(20, -5, 30);
  vertex(20, 5, 30);
  vertex(20, 5, -30);
  
  //Y- // Pink
  fill(#ff00ff);
  vertex(-20, -5, -30);
  vertex(20, -5, -30);
  vertex(20, -5, 30);
  vertex(-20, -5, 30);
  
  //Y+ // Blue Green
  fill(#00ffff);
  vertex(-20, 5, -30);
  vertex(20, 5, -30);
  vertex(20, 5, 30);
  vertex(-20, 5, 30);
  
  endShape();
}


void drawCube() {  
  pushMatrix();
    translate(VIEW_SIZE_X/2, VIEW_SIZE_Y/2 + 50, 0);
    scale(5,5,5);
    rotateZ(-Euler[1]*3.14/180);
    rotateX(-Euler[2]*3.14/180);
    rotateY(-Euler[0]*3.14/180+calib*3.14/180);
    
    buildBoxShape();
    
  popMatrix();
}


void draw() {
  background(#000000);
  fill(#ffffff);

  
  textFont(font, 23);
  textAlign(LEFT, TOP);
  text("Accerlometer:\n" + Acc[0] + "\n" + Acc[1] + "\n" + Acc[2] , 20, 20);
  text("Euler Angles:\nYaw (psi)  : " + Euler[0] + "\nPitch (theta): " + Euler[1] + "\nRoll (phi)  : " + Euler[2], 200, 20);
  
  drawCube(); 
}

void mousePressed() { 
calib=Euler[0];
}

void receive( byte[] data, String ip, int port ) {  // <-- extended handler
  
  
  // get the "real" message =
  // forget the ";\n" at the end <-- !!! only for a communication with Pd !!!
  float dt = millis() - now;
  float alpha = 0.5; // constant alpha
  data = subset(data, 0, data.length);
  String message = new String( data );
  String[] list = split(message, ',');
  Acc[0]=Float.parseFloat(list[0]);
  Acc[1]=Float.parseFloat(list[1]);
  Acc[2]=Float.parseFloat(list[2]);
  //float GY=Float.parseFloat(list[4]);
  float GZ=Float.parseFloat(list[5]);
  magX = Float.parseFloat(list[6]);
  magY = Float.parseFloat(list[7]);
  magZ = Float.parseFloat(list[8]);
  now = millis();
  
  Euler[1]= asin(Acc[0]/9.81)*180/PI; // Pitch
  Euler[2]= atan2(Acc[1], Acc[2])*180/PI; // Roll
  
  //float Yh = (magY * cos(Euler[2])) - (magZ * sin(Euler[2]));
  //float Xh = (magX * cos(Euler[1]))+(magY * sin(Euler[2])*sin(Euler[1])) + (magZ * cos(Euler[2]) * sin(Euler[1]));
  
  //float MagYaw = -atan2(Yh, Xh)*180/PI; // Yaw
  
  
  
  Euler[0] = Acc[0];
  float roll = 0.0;
  float pitch = 0.0;
  float numerator = cos(Euler[2])*magY - sin(Euler[2])*magZ;
  float denominator = cos(Euler[1])*magX + sin(Euler[1])*sin(Euler[2])*magY + sin(Euler[1])*cos(Euler[2])*magZ;
  float MagYaw = -atan2(numerator,denominator)*180/PI;
  
  // Complementary Filter
  //new_yaw = alpha*(new_yaw+GZ*dt) + (1-alpha)*yaw;
  //yaw = new_yaw * 180/PI;
  
  float yaw = 0.0;
  psihat = psihat + GZ * dt;
  float finalyaw = -psihat*PI/180*90/25;
  println(GZ+ " tme - "+ dt);
  
  
  if(flag<threshold)
  {
    pitcher.append(0);
    roller.append(0);
    yawer.append(0);
    pitch = 0;
    roll = 0;
    yaw = 0;
  }else{
    pitcher.remove(0);
    roller.remove(0);
    yawer.remove(0);
    pitcher.append(Euler[1]);
    roller.append(Euler[2]);
    yawer.append(finalyaw);
    roll = 0.0;
    pitch = 0.0;
    yaw = 0.0;
    for(int i=0;i<threshold;i++){
     pitch = pitch + pitcher.get(i);
     roll = roll + roller.get(i);
     yaw = yaw + yawer.get(i);
    }
    pitch = float(nf(pitch/threshold,0,2));
    roll = float(nf(roll/threshold,0,2));
    yaw = float(nf(yaw/threshold,0,2));
  }
  //pitch = pitcher;
  if(flag<20){
    //yaw_error = -yaw;
    flag = flag + 1;
  }
  //yaw = yaw + yaw_error;
  Euler[1] = pitch;
  Euler[2] = roll;
  
    Euler[0] = yaw%360;
  // print the result
  println( "receive: \""+calib+ "\" message: \"" +message+"\" from "+ip+" on port "+port + " Yaw: "+ Euler[0] + " Check: " + GZ);
}
