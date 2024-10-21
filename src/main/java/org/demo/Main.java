package org.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Main class serves as the entry point for the application. */
public class Main {
  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  /**
   * The main method initializes the LocationProgram and starts the application.
   *
   * @param args Command-line arguments (not used).
   */
  public static void main(String[] args) {
    try (LocationProgram locationProgram = new LocationProgram()) {
      locationProgram.run();
    } catch (Exception e) {
      logger.error("An error occurred while running the program", e);
    }
  }
}
