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
  
  float env() {
    env[i]=envelopeShape[(int)readHeadEnv];
    i=(i+1)%env.length;
    return env[i];
  }
  
  void nextStep() {
    
    readHeadEnv=(readHeadEnv+rate)%env.length; 
    if(readHeadEnv==N-rate) {
      next=true;
    }
    else {
      next=false;
    }
  }
}


