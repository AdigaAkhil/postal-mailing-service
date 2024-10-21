package org.demo.exception;

/** A custom exception class for Google API errors. */
public class GoogleApiException extends Exception {

  private final String statusCode;

  /**
   * Constructs a new GoogleApiException with the specified detail message and status code.
   *
   * @param statusCode The status code returned by the API.
   * @param message The detail message.
   */
  public GoogleApiException(String statusCode, String message) {
    super(message);
    this.statusCode = statusCode;
  }

  /**
   * Gets the status code returned by the API.
   *
   * @return The status code.
   */
  public String getStatusCode() {
    return statusCode;
  }
}
