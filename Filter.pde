class Filter {
  float F, rF, a1, a2, a3, b1, b2, cF, targetF, frequency, w;
  Filter() {
  }

  void butterworthFilterSquare() {
    for (int i=2; i<square.length; i++) {
      squareFiltered [i] = a1*(square[i])+a2*(square[i-1])+
        a3*(square[i-2])-b1*(squareFiltered[i-1])-b2*(squareFiltered[i-2]);

      cF=1/tan(PI*frequency/44100);
      a1 = 1.0 / ( 1.0 + rF * cF + cF * cF);
      a2 = 2*a1;
      a3 = a1;
      b1 = 2.0 * ( 1.0 - cF*cF) * a1;
      b2 = ( 1.0 - rF * cF + cF * cF) * a1;
      rF=resonance.faderValue();
    }
  }

  void butterworthFilterTriangle() {
    for (int i=2; i<triangle.length; i++) {
      triangleFiltered [i] = a1*(triangle[i])+a2*(triangle[i-1])+
        a3*(triangle[i-2])-b1*(triangleFiltered[i-1])-b2*(triangleFiltered[i-2]);

      cF=1.0/tan(PI*frequency/44100);
      a1 = 1.0 / ( 1.0 + rF * cF + cF * cF);
      a2 = 2*a1;
      a3 = a1;
      b1 = 2.0 * ( 1.0 - cF*cF) * a1;
      b2 = ( 1.0 - rF * cF + cF * cF) * a1;
      rF=resonance.faderValue();
    }
  }

  void lfo() {
    frequency=10+filterFrequency.faderValue()
      +filterFrequency.faderValue()*sin(w);

    w+=lfo.faderValue()/10;
  }
}

