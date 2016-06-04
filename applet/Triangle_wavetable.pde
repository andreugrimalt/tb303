class TriangleWavetable {
  TriangleWavetable() {
  }
  void createTriangleWavetable() {

    for (int j=0; j<N/2; j++) {

      triangle[j]=(1.0/N/2)*j;
    }
    for (int j=N/2; j<N; j++) {

      triangle[j]=1+(-1.0/N)*j;
    }
  }
}

