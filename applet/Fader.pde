class Fader {
  // inital angle
  float angle=PI/2+PI/6;
  //mouse coordinates translated
  float mouseXTranslated, mouseYTranslated;
  // relating to the buttons
  float xPos, yPos, faderSize, littleLine;
  float range;
  String faderName;
  float faderValue;
  boolean faderTouched=false;
  
  PFont label;

  
  Fader(float xPos, float yPos, float faderSize, float range,String faderName) {
    this.xPos=xPos;
    this.yPos=yPos;
    this.faderSize=faderSize;
    this.faderName=faderName;
    this.range=range;
    label = loadFont("CourierNewPSMT-14.vlw");
    
  }

// draws the fader
  void draw() {

    fill(180);
    // origin at the center of the button
    pushMatrix();
    translate(xPos,yPos);
    stroke(0);
    fill(180,0,0,0);
    arc(0,0,faderSize,faderSize,2*PI/3,2*PI+PI/3);
    stroke(0);
    // draw the little line, convert from polar to cartesian
    littleLine=faderSize/5+faderSize/2;
    line(faderSize/2*cos(angle),faderSize/2*sin(angle),(littleLine)
      *cos(angle),(littleLine)*sin(angle));
      
      // LABELS
      fill(0);
      text(faderName, -21, littleLine+15);
      if(faderValue()<10){
      text(nf(faderValue(),0,1),-10,4);
      }
      if(faderValue()>=10&&faderValue()<100){
      text(nf(faderValue(),0,1),-14.4,4);
      }
      if(faderValue()>=100){
      text(nf(faderValue(),0,1),-18.5,4);
      }
    popMatrix();
  }

// move the little line. calculate angular component, radial is constant=faderSize
  void move() {
    // origin at the center of the button
    pushMatrix();
    translate(xPos,yPos);
    // detect if the mouse is close to the button and has been clicked
    if(mouseX>xPos-littleLine-5&&mouseX<xPos+littleLine+5
      &&mouseY>yPos-littleLine-5&&mouseY<yPos+littleLine+5&&mousePressed) {
        // mouse coordinates' origin translated to the center of the button
      mouseXTranslated=(mouseX-xPos);
      mouseYTranslated=(mouseY-yPos);
      // the mouse is at the right semicircle
      if(mousePressed && mouseXTranslated>0) {
        // calculate angle with arctangent. arctangent [-PI/2, PI/2]!
        angle=atan(mouseYTranslated/mouseXTranslated);
        // contrain to the left semicircle
        angle=constrain(angle,-PI/2,PI/2-PI/6);
      }
      // same as above but now when the mouse is on the left semicircle
      if(mousePressed && mouseXTranslated<0) {
        // rotate the angular component PI radians
        angle=atan(mouseYTranslated/mouseXTranslated)+PI;
        angle=constrain(angle,PI/2+PI/6,3*PI/2);
        if(angle>PI/2+PI/6) {
          faderTouched=true;
        }
      }
      
    }
    popMatrix();
  }

// draw the yellow arc. 
  void colors() {
    strokeWeight(3);
    pushMatrix();
    translate(xPos,yPos);
    stroke(255,255,0);
    fill(180,0,0,0);
    // right side
    if(faderTouched&&angle>PI/2-PI/6) {
      arc(0,0,faderSize,faderSize,2*PI/3,angle);
    }
    // left side
    if(faderTouched&&angle>-PI/2&&angle<=PI/2-PI/6) {
      stroke(255,255,0);
      arc(0,0,faderSize,faderSize,-3*PI/2+PI/6,angle);
    }

    popMatrix();
  }

// get the value between 0 and range
  float faderValue() {
    if(mousePressed && mouseXTranslated<0) {
      faderValue=range*0.5*(angle-PI/2-PI/6)/(3*PI/2-PI/2-PI/6);
      
    }
    if(mousePressed && mouseXTranslated>0) {
      faderValue=range*(0.5+0.5*(angle+PI/2)/(3*PI/2-PI/2-PI/6));
    }
    
    return faderValue;
  }
}

