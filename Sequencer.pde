class Sequencer {
  
  Sequencer() {
  }
  
// play the notes. f is the frequency.
  void play() {
    f=stepFrequency[(int)read];
    if(next==true) {
      read=(read+1) % stepFrequency.length;
    }
  }
}

