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


void setup() {
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


void draw() {
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
void stop() {
  // tell the audio to stop
  audioThread.quit();
  // call the version of stop defined in our parent class, in case it does anything vital
  super.stop();
}

// this gets called by the audio thread when it wants some audio
// we should fill the sent buffer with the audio we want to send to the 
// audio output
void generateAudioOut(float[] buffer) {


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

