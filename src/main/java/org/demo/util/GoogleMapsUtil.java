package org.demo.util;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The GoogleMapsUtil class provides utility methods related to Google Maps, such as opening a
 * location in the default web browser.
 */
public class GoogleMapsUtil {

  private static final Logger logger = LoggerFactory.getLogger(GoogleMapsUtil.class);

  /**
   * Opens the specified URL in the default web browser.
   *
   * @param url The URL to open.
   * @throws IOException If an I/O error occurs.
   * @throws URISyntaxException If the URL is malformed.
   */
  public static void openInBrowser(String url) throws IOException, URISyntaxException {
    if (Desktop.isDesktopSupported()) {
      Desktop desktop = Desktop.getDesktop();
      desktop.browse(new URI(url));
      logger.info("Opened URL in browser: {}", url);
    } else {
      logger.error("Desktop API is not supported on this system.");
      throw new UnsupportedOperationException("Desktop API is not supported on this system.");
    }
  }
}
