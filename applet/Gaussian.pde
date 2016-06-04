class Gauss {
    // output numbers
  
  // numbers for deciding if output or not 
 
  // parameters of gauss bell
  float sigma=2;
  float mu=0;
  Gauss(){
  }

void playGauss(){
   
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

  float gauss(float x, float s, float u) {
    float d;
    d=exp(-(pow((x-u), 2))/(2*pow(s, 2)))/(sqrt(2*PI)*s);
    return d;
  }
}

