package org.demo.service;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.demo.exception.GoogleApiException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The GoogleApiService class is responsible for making API calls to various Google services, such
 * as Places Autocomplete, Geolocation, and Geocoding APIs.
 */
public class GoogleApiService implements Closeable {

  private static final Logger logger = LoggerFactory.getLogger(GoogleApiService.class);

  private static final String AUTOCOMPLETE_URL =
      "https://maps.googleapis.com/maps/api/place/autocomplete/json";
  private static final String GEOCODING_URL = "https://maps.googleapis.com/maps/api/geocode/json";
  private static final String GEOLOCATION_URL =
      "https://www.googleapis.com/geolocation/v1/geolocate";

  private final String apiKey;
  private final CloseableHttpClient httpClient;

  /**
   * Constructs a new GoogleApiService with the specified API key.
   *
   * @param apiKey The Google API key.
   */
  public GoogleApiService(String apiKey) {
    this.apiKey = apiKey;
    this.httpClient = HttpClients.createDefault();
  }

  /**
   * Retrieves predictions from the Google Places Autocomplete API based on input and type.
   *
   * @param input The user's input.
   * @param type The type parameter for the API call.
   * @return A JSONArray of predictions.
   * @throws IOException If an I/O error occurs.
   * @throws GoogleApiException If the API returns an error status.
   */
  public JSONArray getPredictions(String input, String type)
      throws IOException, GoogleApiException {
    String encodedInput = URLEncoder.encode(input, StandardCharsets.UTF_8);
    String url = AUTOCOMPLETE_URL + "?input=" + encodedInput + "&key=" + apiKey + "&type=" + type;

    String jsonResponse = makeApiCall(url);
    JSONObject response = new JSONObject(jsonResponse);

    handleApiStatus(response);

    return response.getJSONArray("predictions");
  }

  /**
   * Retrieves geolocation data using the Google Geolocation API.
   *
   * @return A JSONObject containing the geolocation data.
   * @throws IOException If an I/O error occurs.
   */
  public JSONObject getGeolocation() throws IOException {
    String url = GEOLOCATION_URL + "?key=" + apiKey;
    HttpPost httpPost = new HttpPost(url);
    httpPost.setHeader("Content-Type", "application/json");
    httpPost.setEntity(new StringEntity("{}"));

    try (var response = httpClient.execute(httpPost)) {
      int statusCode = response.getCode();
      String responseBody = EntityUtils.toString(response.getEntity());
      JSONObject jsonResponse = new JSONObject(responseBody);

      if (statusCode != 200) {
        // The Geolocation API returns errors in an "error" object
        if (jsonResponse.has("error")) {
          JSONObject error = jsonResponse.getJSONObject("error");
          String message = error.optString("message", "Unknown error");
          logger.error("Error from Geolocation API: {}", message);
          throw new IOException("Error from Geolocation API: " + message);
        } else {
          logger.error("Unexpected response from Geolocation API: {}", responseBody);
          throw new IOException("Unexpected response from Geolocation API");
        }
      }
      return jsonResponse;
    } catch (ParseException e) {
      logger.error("Error parsing geolocation response", e);
      throw new IOException("Error parsing geolocation response", e);
    }
  }

  /**
   * Retrieves geocoding data based on latitude and longitude.
   *
   * @param latitude The latitude coordinate.
   * @param longitude The longitude coordinate.
   * @return A JSONObject containing the geocoding data.
   * @throws IOException If an I/O error occurs.
   * @throws GoogleApiException If the API returns an error status.
   */
  public JSONObject getGeocodingData(double latitude, double longitude)
      throws IOException, GoogleApiException {
    String url =
        String.format("%s?latlng=%s,%s&key=%s", GEOCODING_URL, latitude, longitude, apiKey);
    String jsonResponse = makeApiCall(url);
    JSONObject response = new JSONObject(jsonResponse);

    handleApiStatus(response);

    return response;
  }

  /**
   * Gets the complete address for a partial address using the Places Autocomplete API.
   *
   * @param partialAddress The partial address input.
   * @return The complete address as a string, or null if not found.
   * @throws IOException If an I/O error occurs.
   * @throws GoogleApiException If the API returns an error status.
   */
  public String getCompleteAddress(String partialAddress) throws IOException, GoogleApiException {
    JSONArray predictions = getPredictions(partialAddress, "address");
    if (!predictions.isEmpty()) {
      return predictions.getJSONObject(0).getString("description");
    } else {
      return null;
    }
  }

  /**
   * Constructs a Google Maps URL for a given address.
   *
   * @param address The address to map.
   * @return The Google Maps URL.
   */
  public String constructGoogleMapsURL(String address) {
    if (address != null) {
      String queryParam = address.replace(" ", "+");
      return String.format("https://www.google.com/maps/search/?api=1&query=%s", queryParam);
    } else {
      return null;
    }
  }

  /**
   * Makes an API call to the specified URL and returns the response as a string.
   *
   * @param url The URL to call.
   * @return The API response as a string.
   * @throws IOException If an I/O error occurs.
   */
  private String makeApiCall(String url) throws IOException {
    HttpGet request = new HttpGet(URI.create(url));
    try (var response = httpClient.execute(request)) {
      return EntityUtils.toString(response.getEntity());
    } catch (ParseException e) {
      logger.error("Error parsing API response from URL: {}", url, e);
      throw new IOException("Error parsing API response", e);
    }
  }

  /**
   * Handles the API response status. Throws an exception if the status is not OK.
   *
   * @param response The JSON response from the API.
   * @throws GoogleApiException If the API returns an error status.
   */
  private void handleApiStatus(JSONObject response) throws GoogleApiException {
    String status = response.optString("status", "UNKNOWN_ERROR");

    switch (status) {
      case "OK":
        // Do nothing; successful response
        break;
      case "ZERO_RESULTS":
        logger.info("No results found for the given input.");
        break;
      case "INVALID_REQUEST":
        logger.error("Invalid request sent to Google API.");
        throw new GoogleApiException(status, "Invalid request sent to Google API.");
      case "OVER_QUERY_LIMIT":
        logger.error("Over query limit. Check your API key and billing status.");
        throw new GoogleApiException(
            status, "Over query limit. Check your API key and billing status.");
      case "REQUEST_DENIED":
        logger.error("Request denied by Google API.");
        throw new GoogleApiException(status, "Request denied by Google API.");
      case "UNKNOWN_ERROR":
        logger.error("Unknown error from Google API.");
        throw new GoogleApiException(status, "Unknown error from Google API.");
      default:
        logger.error("Unhandled status code from Google API: {}", status);
        throw new GoogleApiException(status, "Unhandled status code from Google API.");
    }
  }

  /**
   * Closes the HTTP client.
   *
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void close() throws IOException {
    httpClient.close();
  }
}
