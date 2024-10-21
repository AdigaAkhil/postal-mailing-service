package org.demo;

import java.util.List;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class LocationProgramReloaded implements AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(LocationProgramReloaded.class);
  private static final String BASE_URL =
      "https://maps.googleapis.com/maps/api/place/autocomplete/json";
  private static final String GEOCODING_URL = "https://maps.googleapis.com/maps/api/geocode/json";
  private static final String GEOLOCATION_URL =
      "https://www.googleapis.com/geolocation/v1/geolocate";
  private static final int EXIT_CHOICE = 8;
  private static final int CLEAR_CHOICE = 7;
  private static final int USE_CURRENT_LOCATION = 6;

  private final String apiKey;
  private final Map<String, String> locationInfo = new HashMap<>();
  private final Scanner scanner;
  private final CloseableHttpClient httpClient;

  public LocationProgramReloaded() throws IOException {
    this.apiKey = System.getenv("GOOGLE_API_KEY");
    if (this.apiKey == null || this.apiKey.isEmpty()) {
      throw new IllegalStateException("GOOGLE_API_KEY environment variable is not set");
    }
    this.scanner = new Scanner(System.in);
    this.httpClient = HttpClients.createDefault();
  }

  public void run() {
    try {
      while (true) {
        displayMenu();
        int choice = getIntInput("Enter your choice");
        if (choice == EXIT_CHOICE) {
          logger.info("Exiting the program...");
          break;
        }
        processChoice(choice);
        displayLocationInfo();
      }
    } catch (Exception e) {
      logger.error("An unexpected error occurred", e);
    }
  }

  private void displayMenu() {
    System.out.println("\nPlease enter the index of your choice:");
    System.out.println("1. Country");
    System.out.println("2. State");
    System.out.println("3. City");
    System.out.println("4. Address");
    System.out.println("5. PinCode");
    System.out.println("6. Use Current Location");
    System.out.println("7. Clear");
    System.out.println("8. Exit");
  }

  private void processChoice(int choice) throws IOException {
    switch (choice) {
      case 1 -> handleLocationInput("country", "country");
      case 2 -> handleLocationInput("state", "administrative_area_level_1");
      case 3 -> handleLocationInput("city", "locality");
      case 4 -> handleLocationInput("address", "address");
      case 5 -> handleLocationInput("pinCode", "postal_code");
      case USE_CURRENT_LOCATION -> useCurrentLocation();
      case CLEAR_CHOICE -> clearLocationInfo();
      case EXIT_CHOICE -> {
        /* Do nothing, handled in run() */
      }
      default -> System.out.println("Invalid choice. Please try again.");
    }
  }

  private void useCurrentLocation() {
    try {
      System.out.println("Fetching your current location...");
      JSONObject geoLocation = getGeolocation();
      double latitude = geoLocation.getJSONObject("location").getDouble("lat");
      double longitude = geoLocation.getJSONObject("location").getDouble("lng");

      System.out.printf("Coordinates: %.6f, %.6f%n", latitude, longitude);

      String geocodingData = getGeocodingData(latitude, longitude);
      parseAndSetAddress(geocodingData);

      System.out.println("Location information updated based on your current location.");
    } catch (Exception e) {
      logger.error("Error fetching current location", e);
      System.out.println("Failed to fetch current location. Please try again or use manual input.");
    }
  }

  private void parseAndSetAddress(String jsonResponse) {
    JSONObject root = new JSONObject(jsonResponse);
    JSONArray results = root.getJSONArray("results");

    if (!results.isEmpty()) {
      JSONArray addressComponents = results.getJSONObject(0).getJSONArray("address_components");

      clearLocationInfo();

      for (int i = 0; i < addressComponents.length(); i++) {
        JSONObject component = addressComponents.getJSONObject(i);
        JSONArray types = component.getJSONArray("types");
        String longName = component.getString("long_name");

        updateLocationInfo(types, longName);
      }

      // Trim the address if it was set
      locationInfo.computeIfPresent("address", (k, v) -> v.trim());
    } else {
      System.out.println("No results found for the current location");
    }
  }

  private void updateLocationInfo(JSONArray types, String longName) {
    List<Object> typesList = types.toList();
    if (typesList.contains("country")) {
      locationInfo.put("country", longName);
    } else if (typesList.contains("administrative_area_level_1")) {
      locationInfo.put("state", longName);
    } else if (typesList.contains("locality")) {
      System.out.println("CAME HERE ??");
      locationInfo.put("city", longName);
    } else if (typesList.contains("postal_code")) {
      locationInfo.put("pinCode", longName);
    } else if ((typesList.contains("street_address")
        || typesList.contains("route")
        || typesList.contains("neighborhood"))
        || typesList.contains("sublocality")) {
      locationInfo.merge("address", longName, (old, newVal) -> old + " ," + newVal);
      System.out.println("AFTER : " + locationInfo.get("address"));
    }
  }

  private JSONObject getGeolocation() throws IOException, ParseException {
    HttpPost httpPost = new HttpPost(GEOLOCATION_URL + "?key=" + apiKey);
    httpPost.setHeader("Content-Type", "application/json");
    httpPost.setEntity(new StringEntity("{}"));

    try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
      String responseBody = EntityUtils.toString(response.getEntity());
      return new JSONObject(responseBody);
    }
  }

  private String getGeocodingData(double latitude, double longitude) throws IOException {
    String url =
        String.format("%s?latlng=%s,%s&key=%s", GEOCODING_URL, latitude, longitude, apiKey);
    return makeApiCall(url);
  }

  private void handleLocationInput(String key, String type) throws IOException {
    String input = getStringInput("Please enter the " + key);
    logger.info("Received {} {} from user", key, input);
    processApiRequest(input, type, key);
  }

  private void processApiRequest(String input, String type, String key) throws IOException {
    String url = buildApiUrl(input, type);
    String jsonResponse = makeApiCall(url);
    JSONObject response = new JSONObject(jsonResponse);

    if ("ZERO_RESULTS".equals(response.getString("status"))) {
      logger.info(
          "Received empty result from API response for input '{}' and type '{}'", input, type);
      locationInfo.put(key, input);
      return;
    }

    JSONArray predictions = response.getJSONArray("predictions");
    displayOptions(predictions);

    handleUserSelection(predictions, type, key);
  }

  private void handleUserSelection(JSONArray predictions, String type, String key)
      throws IOException {
    while (true) {
      String userInput = getStringInput("Enter index or continue searching");

      if (userInput.matches("\\d+")) {
        int index = Integer.parseInt(userInput);
        if (handleIndexInput(predictions, index, type, key)) {
          break;
        } else if (type.equalsIgnoreCase("postal_code")) {
          predictions = searchAgain(userInput, type);
          if (predictions.isEmpty()) {
            locationInfo.put(key, userInput);
            break;
          }
          displayOptions(predictions);
        }
      } else {
        predictions = searchAgain(userInput, type);
        if (predictions.isEmpty()) {
          locationInfo.put(key, userInput);
          break;
        }
        displayOptions(predictions);
      }
    }
  }

  private boolean handleIndexInput(JSONArray predictions, int index, String type, String key)
      throws IOException {
    if (type.equalsIgnoreCase("postal_code")) {
      if (index <= 5) {
        String confirmation = getStringInput("Is this an index value? (y/n)");
        if (confirmation.equalsIgnoreCase("y")) {
          return selectPrediction(predictions, index - 1, key);
        }
      }
      return false;
    } else if (index > 0 && index <= predictions.length()) {
      return selectPrediction(predictions, index - 1, key);
    }
    return false;
  }

  private boolean selectPrediction(JSONArray predictions, int index, String key)
      throws IOException {
    JSONObject prediction = predictions.getJSONObject(index);
    String mainText = prediction.getJSONObject("structured_formatting").getString("main_text");
    locationInfo.put(key, mainText);
    updateRelatedFields(prediction, key);
    return true;
  }

  private JSONArray searchAgain(String input, String type) throws IOException {
    String url = buildApiUrl(input, type);
    String jsonResponse = makeApiCall(url);
    JSONObject response = new JSONObject(jsonResponse);
    return response.getJSONArray("predictions");
  }

  private String buildApiUrl(String input, String type) {
    String encodedInput = URLEncoder.encode(input, StandardCharsets.UTF_8);
    return BASE_URL + "?input=" + encodedInput + "&key=" + apiKey + "&type=" + type;
  }

  private String makeApiCall(String url) throws IOException {
    HttpGet request = new HttpGet(URI.create(url));
    try (CloseableHttpResponse response = httpClient.execute(request)) {
      return EntityUtils.toString(response.getEntity());
    } catch (ParseException e) {
      throw new IOException("Error parsing API response", e);
    }
  }

  private void displayOptions(JSONArray predictions) {
    for (int i = 0; i < predictions.length(); i++) {
      JSONObject prediction = predictions.getJSONObject(i);
      String mainText = prediction.getString("description");
      System.out.println((i + 1) + ". " + mainText);
    }
  }

  private void updateRelatedFields(JSONObject prediction, String key) throws IOException {
    JSONArray terms = prediction.getJSONArray("terms");

    switch (key) {
      case "state" -> validateAndUpdateLocation(terms, "country");
      case "city" -> {
        validateAndUpdateLocation(terms, "country");
        validateAndUpdateLocation(terms, "administrative_area_level_1");
      }
      case "address", "pinCode" -> {
        validateAndUpdateLocation(terms, "country");
        validateAndUpdateLocation(terms, "administrative_area_level_1");
        validateAndUpdateLocation(terms, "locality");
      }
    }
  }

  private void validateAndUpdateLocation(JSONArray terms, String type) throws IOException {
    for (int i = terms.length() - 1; i >= 0; i--) {
      String termValue = terms.getJSONObject(i).getString("value");
      if (isValidLocation(termValue, type)) {
        String locationKey = getLocationKey(type);
        locationInfo.put(locationKey, termValue);
        terms.remove(i);
        break;
      }
    }
  }

  private boolean isValidLocation(String input, String type) throws IOException {
    String url = buildApiUrl(input, type);
    String jsonResponse = makeApiCall(url);
    JSONObject response = new JSONObject(jsonResponse);

    if ("OK".equals(response.getString("status"))) {
      JSONArray predictions = response.getJSONArray("predictions");
      if (!predictions.isEmpty()) {
        String mainText =
            predictions
                .getJSONObject(0)
                .getJSONObject("structured_formatting")
                .getString("main_text");
        return mainText.toLowerCase().contains(input.toLowerCase());
      }
    }
    return false;
  }

  private String getLocationKey(String type) {
    return switch (type) {
      case "country" -> "country";
      case "administrative_area_level_1" -> "state";
      case "locality" -> "city";
      default -> null;
    };
  }

  private void clearLocationInfo() {
    locationInfo.clear();
    logger.info("Cleared all location information");
    System.out.println("All location information has been cleared.");
  }

  private void displayLocationInfo() {
    System.out.println("\nCurrent Location Information:");
    System.out.println("Country: " + locationInfo.getOrDefault("country", "Not set"));
    System.out.println("State: " + locationInfo.getOrDefault("state", "Not set"));
    System.out.println("City: " + locationInfo.getOrDefault("city", "Not set"));
    System.out.println("Address: " + locationInfo.getOrDefault("address", "Not set"));
    System.out.println("PinCode: " + locationInfo.getOrDefault("pinCode", "Not set"));
  }

  private String getStringInput(String prompt) {
    System.out.print(prompt + ": ");
    return scanner.nextLine().trim();
  }

  private int getIntInput(String prompt) {
    while (true) {
      try {
        System.out.print(prompt + ": ");
        return Integer.parseInt(scanner.nextLine().trim());
      } catch (NumberFormatException e) {
        System.out.println("Invalid input. Please enter a number.");
      }
    }
  }

  @Override
  public void close() {
    try {
      scanner.close();
      httpClient.close();
    } catch (IOException e) {
      logger.error("Error closing resources", e);
    }
  }

  public static void main(String[] args) {
    try (LocationProgramReloaded program = new LocationProgramReloaded()) {
      program.run();
    } catch (IOException e) {
      logger.error("Failed to initialize LocationProgramReloaded", e);
    }
  }
}