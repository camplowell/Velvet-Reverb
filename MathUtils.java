import java.lang.Math;


class MathUtils {
  public static float constrain(float value, float min, float max) {
    return Math.min(Math.max(value, min), max);
  }
  
  public static float lerp(float a, float b, float fac){
    return (float)((a * (1.0 - fac)) + (b * fac));
  }
}
