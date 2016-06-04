class SquareWavetable {
  SquareWavetable(){
  }
  void createSquareWavetable(){

for(int j=0; j<N/2; j++) {

    square[j]=-1;
  }
  for(int j=N/2;j<N;j++) {

    square[j]=1;
  }
  }
}
