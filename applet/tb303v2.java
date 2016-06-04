import processing.core.*; 
import processing.xml.*; 

import javax.sound.sampled.AudioFormat; 
import javax.sound.sampled.AudioSystem; 
import javax.sound.sampled.DataLine; 
import javax.sound.sampled.LineUnavailableException; 
import javax.sound.sampled.SourceDataLine; 

import java.applet.*; 
import java.awt.Dimension; 
import java.awt.Frame; 
import java.awt.event.MouseEvent; 
import java.awt.event.KeyEvent; 
import java.awt.event.FocusEvent; 
import java.awt.Image; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class tb303v2 extends PApplet {

SquareWavetable squareTable;
TriangleWavetable triangleTable;

Gauss gaussian;

Fader [] faderStepFrequency;
Fader filterFrequency;
Fader lfo;
Fader resonance;
Fader volume;

AudioThread audioThread;
Sequencer sequence;
Envelope envelope;
Filter butterworth;
float [] triangle;
float [] triangleFiltered;
float [] square;
float [] squareFiltered;
boolean next=false;

float readHead;
float readHeadEnv=0;

float rate=9;

float f;
int N=44100;

float read=0;
float [] stepFrequency;
int steps=16;


public void setup() {
  smooth();

  size(900, 500);

  squareTable=new SquareWavetable();
  triangleTable=new TriangleWavetable();
  faderStepFrequency=new Fader[steps];

  gaussian=new Gauss();

  // initialize the step faders
  int a=0;
  if (steps<=8) {
    for (int i=0; i<steps; i++) {
      faderStepFrequency[i]=new Fader(width/10+a, height/4, 50, 220, "STEP "+(i+1));
      a+=100;
    }
  }
  if (steps>8) {
    for (int i=0; i<steps/2; i++) {
      faderStepFrequency[i]=new Fader(width/10+a, height/4, 50, 220, "STEP "+(i+1));
      a+=100;
    }

    a=0;
    for (int i=steps/2; i<steps; i++) {
      faderStepFrequency[i]=new Fader(width/10+a, height/4+100, 50, 220, "STEP "+(i+1));
      a+=100;
    }
  }
  // initialize filter faders
  filterFrequency=new Fader(width/10, 250+height/4, 50, 200, " FREQ ");
  resonance=new Fader(100+width/10, 250+height/4, 50, sqrt(2), "  RES");
  lfo=new Fader(200+width/10, 250+height/4, 50, 10, "  LFO");

  // initialize volume control
  volume=new Fader(500+width/10, 250+height/4, 50, 1, "  VOL");

  // arrays for the wavetables
  triangle= new float[N];
  triangleFiltered=new float[N];
  square=new float[N];
  squareFiltered=new float[N];

  // initalizes the Sequencer object
  stepFrequency=new float[(int)steps];
  sequence=new Sequencer();

  // initialize Envelope object
  envelope=new Envelope(N);
  butterworth=new Filter();

  // Create the AudioThread object, which will connect to the audio 
  // interface and get it ready to use
  audioThread = new AudioThread();
  // Start the audioThread, which will cause it to continually call 'getAudioOut' (see below)
  audioThread.start();

  // create the wavetables
  squareTable.createSquareWavetable();
  triangleTable.createTriangleWavetable();
}


public void draw() {
  smooth();

  // filter the triangle wave
  butterworth.butterworthFilterTriangle();
  // filter the square wave
  butterworth.butterworthFilterSquare();
  // lfo
  butterworth.lfo();

  background(180);
  // each stepFrequency is one frequency determined by the correspondent fader
  for (int i=0; i<steps; i++) { 
    stepFrequency[i]=faderStepFrequency[i].faderValue();
    // call the functions related with the step frequency faders
    faderStepFrequency[i].draw();
    faderStepFrequency[i].move();
    faderStepFrequency[i].colors();
  }
  // faders related with the filter
  filterFrequency.draw();
  filterFrequency.move();
  filterFrequency.colors();

  lfo.draw();
  lfo.move();
  lfo.colors();

  resonance.draw();
  resonance.move();
  resonance.colors();

  // fader controling the volume
  volume.draw();
  volume.move();
  volume.colors();
}

// this function gets called when you press the escape key in the sketch
public void stop() {
  // tell the audio to stop
  audioThread.quit();
  // call the version of stop defined in our parent class, in case it does anything vital
  super.stop();
}

// this gets called by the audio thread when it wants some audio
// we should fill the sent buffer with the audio we want to send to the 
// audio output
public void generateAudioOut(float[] buffer) {


  for (int i=0;i<buffer.length; i++) {
    // go to the next step in the sequencer
    envelope.nextStep();
    // play the right frequency
    sequence.play();
    // change the step following a gaussian distribution. the envelope is not sync
//    gaussian.playGauss();

    // the filtered wave is multiplied by the envelope and the volume control
    buffer[i] =volume.faderValue()*envelope.env()*triangleFiltered[(int)readHead];

    readHead=(readHead+f) % N;
  }
}


/*
 *  This file has been adapted from 
 * https://github.com/mhroth/jvsthost/blob/master/src/com/synthbot/audioio/vst/JVstAudioThread.java
 *
 *  which contains the following license:
 *
 * Copyright 2007 - 2009 Martin Roth (mhroth@gmail.com)
 *                        Matthew Yee-King
 * 
 *  JVstHost is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  JVstHost is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JVstHost.  If not, see <http://www.gnu.org/licenses/>.
 *
 */







class AudioThread extends Thread {
  // buffer to store the audio data coming in
  // it's a 2D array as we may have more than one channel
  private  float[][] fInputs;
  // buffer to store the audio data going out
  private  float[][] fOutputs;
  // raw binary buffer which is used to actually send data to the sound card as bytes
  private  byte[] bOutput;
  // how many samples to process per cycle? 
  private int blockSize;
  // how many audio outputs/ inputs
  private int numOutputs;
  private int numInputs;
  // samples per second
  private int sampleRate;
  private int bitDepth;
  // the type of audio we are going to generate (PCM/compressed etc? )
  // see http://download.oracle.com/javase/1.5.0/docs/api/javax/sound/sampled/AudioFormat.html
  private AudioFormat audioFormat;
  //  used to access the audio system so we can send data to and from it
  private SourceDataLine sourceDataLine;
  // are we running?
  private boolean running;
  
  // we pull this value into a variable for speed (avoids repeating the field access and cast operation)
  private static final float ShortMaxValueAsFloat = (float) Short.MAX_VALUE;

  // constructor attempts to initialise the audio device
  AudioThread (){
    running = false;
   // mono
     numOutputs = 1;
     numInputs = 1;
     // block size 4096 samples, lower it if you want lower latency
    blockSize = 4096;
    sampleRate = 44100;
    bitDepth = 16;
    // initialise audio buffers
    fInputs = new float[numInputs][blockSize];
    fOutputs = new float[numOutputs][blockSize];
    bOutput = new byte[numOutputs * blockSize * 2];
  // set up the audio format, 
    audioFormat = new AudioFormat(sampleRate, bitDepth, numOutputs, true, false);
    DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);

    sourceDataLine = null;
  // here we try to initialise the audio system. try catch is exception handling, i.e. 
  // dealing with things not working as expected
    try {
      sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
      sourceDataLine.open(audioFormat, bOutput.length);
      sourceDataLine.start();
      running = true;
    } catch (LineUnavailableException lue) {
      // it went wrong!
      lue.printStackTrace(System.err);
      System.out.println("Could not initialise audio. check above stack trace for more info");
      //System.exit(1);
    }
  }
  // we are ovverriding the run method from the Thread class
  // run gets called when the thread starts
  public @Override
  // We must implement run, this gets triggered by start()
  void run () {
    while (running) {
      // generate the float buffer
      generateAudioOut(fOutputs[0]);
      // convert to bytes and send it to the card
      sourceDataLine.write(floatsToBytes(fOutputs, bOutput), 0, bOutput.length);
    }
  }

  // returns the current contents of the audio buffer
  public float[] getAudio(){
    return fOutputs[0];
  }
  
  // Our method that quits the thread
  // taken from http://wiki.processing.org/w/Threading
  public void quit() {
    System.out.println("Quitting audio thread."); 
    running = false;  // Setting running to false ends the loop in run()
    sourceDataLine.drain();
    sourceDataLine.close();  
    // IUn case the thread is waiting. . .
    // note that the interrupt method is defined in the Thread class which we are extending
    interrupt();
  }
  
   /**
   * Converts a float audio array [-1,1] to an interleaved array of 16-bit samples
   * in little-endian (low-byte, high-byte) format.
   */
  private byte[] floatsToBytes(float[][] fData, byte[] bData) {
    int index = 0;
    for (int i = 0; i < blockSize; i++) {
      for (int j = 0; j < numOutputs; j++) {
        short sval = (short) (fData[j][i] * ShortMaxValueAsFloat);
        bData[index++] = (byte) (sval & 0x00FF);
        bData[index++] = (byte) ((sval & 0xFF00) >> 8);
      }
    }
    return bData;
  }
  
  
}
class Envelope {
  float [] envelopeShape=new float[N]; 
  float [] env=new float[N];
  
  int i;

  Envelope(float N) {
    // create the envelope shape. It is the possitive part of a sine function 
    // it goes from 0 to 0.
    
    for(int n=0; n<N; n++) {
      envelopeShape[n]=sin(n*PI/N);
    }
  }
  
  public float env() {
    env[i]=envelopeShape[(int)readHeadEnv];
    i=(i+1)%env.length;
    return env[i];
  }
  
  public void nextStep() {
    
    readHeadEnv=(readHeadEnv+rate)%env.length; 
    if(readHeadEnv==N-rate) {
      next=true;
    }
    else {
      next=false;
    }
  }
}


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
  public void draw() {

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
      text(nf(faderValue(),0,1),-14.4f,4);
      }
      if(faderValue()>=100){
      text(nf(faderValue(),0,1),-18.5f,4);
      }
    popMatrix();
  }

// move the little line. calculate angular component, radial is constant=faderSize
  public void move() {
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
  public void colors() {
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
  public float faderValue() {
    if(mousePressed && mouseXTranslated<0) {
      faderValue=range*0.5f*(angle-PI/2-PI/6)/(3*PI/2-PI/2-PI/6);
      
    }
    if(mousePressed && mouseXTranslated>0) {
      faderValue=range*(0.5f+0.5f*(angle+PI/2)/(3*PI/2-PI/2-PI/6));
    }
    
    return faderValue;
  }
}

class Filter {
  float F, rF, a1, a2, a3, b1, b2, cF, targetF, frequency, w;
  Filter() {
  }

  public void butterworthFilterSquare() {
    for (int i=2; i<square.length; i++) {
      squareFiltered [i] = a1*(square[i])+a2*(square[i-1])+
        a3*(square[i-2])-b1*(squareFiltered[i-1])-b2*(squareFiltered[i-2]);

      cF=1/tan(PI*frequency/44100);
      a1 = 1.0f / ( 1.0f + rF * cF + cF * cF);
      a2 = 2*a1;
      a3 = a1;
      b1 = 2.0f * ( 1.0f - cF*cF) * a1;
      b2 = ( 1.0f - rF * cF + cF * cF) * a1;
      rF=resonance.faderValue();
    }
  }

  public void butterworthFilterTriangle() {
    for (int i=2; i<triangle.length; i++) {
      triangleFiltered [i] = a1*(triangle[i])+a2*(triangle[i-1])+
        a3*(triangle[i-2])-b1*(triangleFiltered[i-1])-b2*(triangleFiltered[i-2]);

      cF=1.0f/tan(PI*frequency/44100);
      a1 = 1.0f / ( 1.0f + rF * cF + cF * cF);
      a2 = 2*a1;
      a3 = a1;
      b1 = 2.0f * ( 1.0f - cF*cF) * a1;
      b2 = ( 1.0f - rF * cF + cF * cF) * a1;
      rF=resonance.faderValue();
    }
  }

  public void lfo() {
    frequency=10+filterFrequency.faderValue()
      +filterFrequency.faderValue()*sin(w);

    w+=lfo.faderValue()/10;
  }
}

class Gauss {
    // output numbers
  
  // numbers for deciding if output or not 
 
  // parameters of gauss bell
  float sigma=2;
  float mu=0;
  Gauss(){
  }

public void playGauss(){
   
   float x=(int)random(8, 10);
   float gr=random(1);
  // the gaussian distribution gives the probability of printing a "x" value
  // the next if statement "aplicates" that probability
  if (gr<gauss(x, sigma, mu)) {
    f=stepFrequency[(int)read];
    
      read=(read+1) % stepFrequency.length;
    
    println("Probability % = "+100*gauss(x,sigma,mu));

   println("value = "+x);
  }
}

  public float gauss(float x, float s, float u) {
    float d;
    d=exp(-(pow((x-u), 2))/(2*pow(s, 2)))/(sqrt(2*PI)*s);
    return d;
  }
}

class Sequencer {
  
  Sequencer() {
  }
  
// play the notes. f is the frequency.
  public void play() {
    f=stepFrequency[(int)read];
    if(next==true) {
      read=(read+1) % stepFrequency.length;
    }
  }
}

class SquareWavetable {
  SquareWavetable(){
  }
  public void createSquareWavetable(){

for(int j=0; j<N/2; j++) {

    square[j]=-1;
  }
  for(int j=N/2;j<N;j++) {

    square[j]=1;
  }
  }
}
class TriangleWavetable {
  TriangleWavetable() {
  }
  public void createTriangleWavetable() {

    for (int j=0; j<N/2; j++) {

      triangle[j]=(1.0f/N/2)*j;
    }
    for (int j=N/2; j<N; j++) {

      triangle[j]=1+(-1.0f/N)*j;
    }
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "--bgcolor=#ffffff", "tb303v2" });
  }
}
