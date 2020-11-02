import java.lang.Math.*;
import java.util.HashMap;
import java.util.ArrayList;

// Biquad filter for use within other audio components.
// Supports lowpass, highpass, shelving bandpass, and peaking bandpass.
class BiquadComponent {
  // Constants
  public static final int LOWPASS = 0;
  public static final int HIGHPASS = 1;
  public static final int BANDPASS = 2;
  public static final int PEAK = 3;
  public static final int NOTCH = 4;
  public static final int LOWSHELF = 5;
  public static final int HIGHSHELF = 6;
  
  // Internal variables
  private int type;
  private float sampleRate;
  private float frequency, gain, s;
  private HashMap<Object, ArrayList<Double>> memory;
  private double a0, a1, a2;
  private double b0, b1, b2;
  
  public BiquadComponent(int type, float sampleRate, float frequency, float gain, float s){
    // check range
    if(0 > type || 6 < type){
      throw new IllegalArgumentException("Biquad filter type out of range: "+type);
    }
    this.type = type;
    this.sampleRate = sampleRate;
    this.frequency = frequency;
    this.gain = gain;
    this.s = s;
    this.memory = new HashMap<Object, ArrayList<Double>>();
    calculateCoeffs();
  }
  
  
  public float calculate(float x, Object source){
    ArrayList<Double> history = memory.get(source);
    double y = (1 / a0) * ((b0 * x) + (b1 * history.get(0)) + (b2 * history.get(1)) - (a1 * history.get(2)) - (a2 * history.get(3)));
    history.set(1, history.get(0));
    history.set(0, (double)x);
    history.set(3, history.get(2));
    history.set(2, y);
    return (float)y;
  }
  
  public BiquadComponent register(Object accessor){
    ArrayList<Double> list = new ArrayList<Double>();
    for( int i = 0; i < 4; i++){
      list.add(0.0d);
    }
    memory.put(accessor, list);
    return this;
  }
  
  public void unregister(Object accessor){
    memory.remove(accessor);
  }
  
  // Q, BW, S
  
  // Dynamic slope-like parameter.
  public void setS(float s){
    this.s = s;
    calculateCoeffs();
  }
  
  public void setFrequency(float frequency){
    this.frequency = frequency;
    calculateCoeffs();
  }
  
  // Used for peaking and shelving filters only
  public void setGain(float gain){
    this.gain = gain;
    calculateCoeffs();
  }
  
  private void calculateCoeffs(){
    double a;
    if(type == LOWSHELF || type == HIGHSHELF || type == PEAK){
      a = gain / 4.0;
    }else{
      a = (float)Math.sqrt(gain / 2);
    }
    
    double w0 = 2 * Math.PI * frequency / sampleRate;
    
    double alpha;
    if(type == LOWSHELF || type == HIGHSHELF){
      // Slope
      alpha = (Math.sin(w0) / 2) * Math.sqrt((a + (1/a)) * ((1/s)-1)+2);
    }else if(type == BANDPASS || type == PEAK){
      // Bandwidth
      alpha = Math.sin(w0) * Math.sinh((Math.log(2) / 2) * s * (w0 / Math.sin(w0)));
    }else{
      // Q
      alpha = Math.sin(w0) / (2 * s);
    }
    
    // Calculate the actual coefficients.
    switch (type) {
      case 0: // Low pass filter
        b0 = (1-Math.cos(w0)) / 2;
        b1 = 1 - Math.cos(w0);
        b2 = b0;
        a0 = 1 + alpha;
        a1 = -2 * Math.cos(w0);
        a2 = 1 - alpha;
        break;
      case 1: // High pass filter
        b0 = (1 + Math.cos(w0)) / 2;
        b1 = -(1 + Math.cos(w0));
        b2 = (1 + Math.cos(w0)) / 2;
        a0 = 1 + alpha;
        a1 = -2 * Math.cos(w0);
        a2 = 1 - alpha;
        break;
      case 2: // Band pass filter
        b0 = alpha;
        b1 = 0;
        b2 = -alpha;
        a0 = 1 + alpha;
        a1 = -2 * Math.cos(w0);
        a2 = 1 - alpha;
        break;
      case 3: // Peak
        b0 = 1 + alpha * a;
        b1 = -2 * Math.cos(w0);
        b2 = 1 - (alpha * a);
        a0 = 1 + (alpha / a);
        a1 = -2 * Math.cos(w0);
        a2 = 1 - (alpha / a);
        break;
      case 4: // Notch
        b0 = 1;
        b1 = -2 * Math.cos(w0);
        b2 = 1;
        a0 = 1 + alpha;
        a1 = -2 * Math.cos(w0);
        a2 = 1 - alpha;
        break;
      case 5: // Lowshelf
        b0 = a * ((a + 1) - (a - 1) * Math.cos(w0) + 2 * Math.sqrt(a) * alpha);
        b1 = 2 * a * ((a - 1) - (a + 1) * Math.cos(w0));
        b2 = a * ((a + 1) - (a - 1) * Math.cos(w0) - 2 * Math.sqrt(a) * alpha);
        a0 = (a + 1) + (a - 1) * Math.cos(w0) + 2 * Math.sqrt(a) * alpha;
        a1 = -2 * ((a - 1) + (a + 1) * Math.cos(w0));
        a2 = (a + 1) + (a - 1) * Math.cos(w0) - 2 * Math.sqrt(a) * alpha;
        break;
      case 6: // Highshelf
        b0 = a * ((a + 1) + (a - 1) * Math.cos(w0) + 2 * Math.sqrt(a) * alpha);
        b1 = -2 * a * ((a - 1) + (a + 1) * Math.cos(w0));
        b2 = a * ((a + 1) + (a - 1) * Math.cos(w0) - 2 * Math.sqrt(a) * alpha);
        a0 = (a + 1) - (a - 1) * Math.cos(w0) + 2 * Math.sqrt(a) * alpha;
        a1 = 2 * ((a - 1) - (a + 1) * Math.cos(w0));
        a2 = (a + 1) - (a - 1) * Math.cos(w0) - 2 * Math.sqrt(a) * alpha;
        break;
    }
    
    
    
  }
}
