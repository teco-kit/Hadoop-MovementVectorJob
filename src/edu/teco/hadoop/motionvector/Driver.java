package edu.teco.hadoop.motionvector;

import org.apache.hadoop.util.ProgramDriver;

/**
 * Driver class
 */
public class Driver {

  /**
   * Entry point.
   * @param argv command line arguments
   */
  public static void main(String argv[]) {
    int exitCode = -1;
    ProgramDriver pgd = new ProgramDriver();
    System.out.println("TecO MotionVector");
    try {
      pgd.addClass("vector", MotionVectorJob.class,
              "A map/reduce program that converts motion sensor data into Mahout vectors.");      
      pgd.driver(argv);

      // Success
      exitCode = 0;
    }
    catch(Throwable e){
      e.printStackTrace();
    }
    System.exit(exitCode);
  }
}
