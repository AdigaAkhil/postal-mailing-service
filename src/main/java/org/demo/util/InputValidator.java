package org.demo.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The InputValidator class provides utility methods for validating user inputs. */
public class InputValidator {

  private static final Logger logger = LoggerFactory.getLogger(InputValidator.class);

  /**
   * Validates if the given string is an integer.
   *
   * @param input The input string to validate.
   * @return True if the input is a valid integer, false otherwise.
   */
  public static boolean isInteger(String input) {
    try {
      Integer.parseInt(input);
      return true;
    } catch (NumberFormatException e) {
      logger.warn("Invalid integer input: {}", input);
      return false;
    }
  }
}
