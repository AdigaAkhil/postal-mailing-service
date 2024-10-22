package demo.util;

import static org.junit.jupiter.api.Assertions.*;

import org.demo.util.InputValidator;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the InputValidator class.
 */
public class InputValidatorTest {

  @Test
  public void testIsInteger_withValidIntegers() {
    assertTrue(InputValidator.isInteger("123"));
    assertTrue(InputValidator.isInteger("-456"));
    assertTrue(InputValidator.isInteger("0"));
  }

  @Test
  public void testIsInteger_withInvalidIntegers() {
    assertFalse(InputValidator.isInteger("12.34"));
    assertFalse(InputValidator.isInteger("abc"));
    assertFalse(InputValidator.isInteger(""));
    assertFalse(InputValidator.isInteger(null));
  }
}
