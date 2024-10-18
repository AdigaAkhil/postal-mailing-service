package org.demo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocationProgramReloaded {

  private static final Logger logger = LoggerFactory.getLogger(LocationInfoProgram.class);
  private static final String CONFIG_FILE = "config.properties";
  private static final String BASE_URL =
      "https://maps.googleapis.com/maps/api/place/autocomplete/json";
  private static final int EXIT_CHOICE = 7;
  private static final int CLEAR_CHOICE = 6;

  private final String apiKey;
  private final Map<String, String> locationInfo = new HashMap<>();
  private final Scanner scanner;
  private final CloseableHttpClient httpClient;

  public LocationProgramReloaded() throws IOException {
    this.apiKey = loadApiKey();
    this.scanner = new Scanner(System.in);
    this.httpClient = HttpClients.createDefault();
  }

  private String loadApiKey() throws IOException {
    Properties properties = new Properties();
    try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
      if (input == null) {
        throw new IOException("Unable to find " + CONFIG_FILE);
      }
      properties.load(input);
      return properties.getProperty("api.key");
    }
  }

  public void run() {
    try {
      while (true) {
        displayMenu();
        int choice = getIntInput("Enter your choice: ");
        if (choice == EXIT_CHOICE) {
          logger.info("Exiting the program...");
          break;
        }
        processChoice(choice);
        displayLocationInfo();
      }
    } catch (Exception e) {
      logger.error("An unexpected error occurred", e);
    } finally {
      closeResources();
    }
  }

  private void displayMenu() {
    System.out.println("\nPlease enter the Below index :");
    System.out.println("1. Country");
    System.out.println("2. State");
    System.out.println("3. City");
    System.out.println("4. Address");
    System.out.println("5. PinCode");
    System.out.println("6. Clear");
    System.out.println("7. Exit");
  }

  private void processChoice(int choice) throws IOException {
    switch (choice) {
      case 1 -> handleLocationInput("country", "country");
      case 2 -> handleLocationInput("state", "administrative_area_level_1");
      case 3 -> handleLocationInput("city", "locality");
      case 4 -> handleLocationInput("address", "address");
      case 5 -> handleLocationInput("pinCode", "postal_code");
      case CLEAR_CHOICE -> clearLocationInfo();
      default -> System.out.println("Invalid choice. Please try again.");
    }
  }

  private void handleLocationInput(String key, String type) throws IOException {
    String input = getStringInput("Please Enter the " + key);
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
          // If it's a postal code and the number is greater than 5, treat it as a new search
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
      // If index > 5 for postal code, we'll return false and handle it in handleUserSelection
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
    System.out.println("Country - " + locationInfo.getOrDefault("country", "Not set"));
    System.out.println("State - " + locationInfo.getOrDefault("state", "Not set"));
    System.out.println("City - " + locationInfo.getOrDefault("city", "Not set"));
    System.out.println("Address - " + locationInfo.getOrDefault("address", "Not set"));
    System.out.println("PinCode - " + locationInfo.getOrDefault("pinCode", "Not set"));
  }

  private String getStringInput(String prompt) {
    System.out.print(prompt + ": ");
    return scanner.nextLine().trim();
  }

  private int getIntInput(String prompt) {
    while (true) {
      try {
        System.out.print(prompt);
        return Integer.parseInt(scanner.nextLine().trim());
      } catch (NumberFormatException e) {
        System.out.println("Invalid input. Please enter a number.");
      }
    }
  }

  private void closeResources() {
    try {
      scanner.close();
      httpClient.close();
    } catch (IOException e) {
      logger.error("Error closing resources", e);
    }
  }

  public static void main(String[] args) {
    try {
      new LocationProgramReloaded().run();
    } catch (IOException e) {
      logger.error("Failed to initialize LocationInfoProgram", e);
    }
  }
}
