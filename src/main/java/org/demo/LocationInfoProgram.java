package org.demo;

import java.io.FileNotFoundException;
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

public class LocationInfoProgram {

  private static final Logger logger = LoggerFactory.getLogger(LocationInfoProgram.class);
  private static String API_KEY;
  private static final String BASE_URL =
      "https://maps.googleapis.com/maps/api/place/autocomplete/json";
  private static final Scanner scanner = new Scanner(System.in);
  private static final Map<String, String> locationInfo = new HashMap<>();
  private static final CloseableHttpClient client = HttpClients.createDefault();

  // Static block to load properties
  static {
    Properties properties = new Properties();
    try (InputStream input = LocationInfoProgram.class.getClassLoader().getResourceAsStream("config.properties")) {
      if (input == null) {
        logger.error("Sorry, unable to find config.properties");
        API_KEY = null;  // Handle the case where the file is not found
      } else {
        properties.load(input);
        API_KEY = properties.getProperty("api.key");  // Read the API key
      }
    } catch (IOException ex) {
      logger.error("Error loading properties file", ex);
      API_KEY = null;  // Handle IO exception
    }
  }



  public static void main(String[] args) {
    while (true) {
      displayMenu();
      int choice = getIntInput("Enter your choice: ");
      if (choice == 7) {
        logger.info("Exiting the program...");
        break;
      }
      processChoice(choice);
      displayLocationInfo();
    }
  }

  private static void displayMenu() {
    System.out.println("\nPlease enter the Below index :");
    System.out.println("1. Country");
    System.out.println("2. State");
    System.out.println("3. City");
    System.out.println("4. Address");
    System.out.println("5. PinCode");
    System.out.println("6. Clear");
    System.out.println("7. Exit");
  }

  private static void processChoice(int choice) {
    switch (choice) {
      case 1:
        handleCountryInput();
        break;
      case 2:
        handleStateInput();
        break;
      case 3:
        handleCityInput();
        break;
      case 4:
        handleAddressInput();
        break;
      case 5:
        handlePinCodeInput();
        break;
      case 6:
        clearLocationInfo();
        break;
      default:
        System.out.println("Invalid choice. Please try again.");
    }
  }

  private static void handleCountryInput() {
    String input = getStringInput("Please Enter the Country");
    logger.info("Received Country {} from user", input);
    String type = "country";
    processApiRequest(input, type, "country");
  }

  private static void handleStateInput() {
    String input = getStringInput("Please Enter the State");
    logger.info("Received State {} from user", input);
    String type = "administrative_area_level_1";
    processApiRequest(input, type, "state");
  }

  private static void handleCityInput() {
    String input = getStringInput("Please Enter the City");
    logger.info("Received City {} from user", input);
    String type = "locality";
    processApiRequest(input, type, "city");
  }

  private static void handleAddressInput() {
    String input = getStringInput("Please Enter the Address");
    logger.info("Received Address {} from user", input);
    String type = "address";
    processApiRequest(input, type, "address");
  }

  private static void handlePinCodeInput() {
    String input = getStringInput("Please Enter the PinCode");
    logger.info("Received PinCode {} from user", input);
    String type = "postal_code";
    processApiRequest(input, type, "pinCode");
  }

  private static void clearLocationInfo() {
    locationInfo.clear();
    logger.info("Cleared all location information");
    System.out.println("All location information has been cleared.");
  }


  private static void processApiRequest(String input, String type, String key) {
    try {
      String url = buildApiUrl(input, type);
      String jsonResponse = makeApiCall(url);
      JSONObject response = new JSONObject(jsonResponse);

      if ("ZERO_RESULTS".equals(response.getString("status"))) {
        logger.info("Received empty result from API response for input '{}' and type '{}'", input, type);
        locationInfo.put(key, input);
        return;
      }

      JSONArray predictions = response.getJSONArray("predictions");
      displayOptions(predictions);

      while (true) {
        String userInput = getStringInput("Enter index or continue searching");

        if (userInput.matches("\\d+")) {
          int index = Integer.parseInt(userInput);

          if (type.equalsIgnoreCase("postal_code")) {

            if (index <= 5) {
              String confirmation = getStringInput("Is this an index value? (y/n)");

              if (confirmation.equalsIgnoreCase("y")) {

                // If it's an index, select the prediction based on index
                if (index > 0 && index <= predictions.length()) {

                  String mainText =
                      predictions
                          .getJSONObject(index - 1)
                          .getJSONObject("structured_formatting")
                          .getString("main_text");
                  locationInfo.put(key, mainText);
                  updateRelatedFields(predictions.getJSONObject(index - 1), key);
                  break;
                } else {
                  System.out.println("Invalid index. Please try again.");
                }
              } else {
                // If the user says it's not an index, treat it as new input for an API call
                input = userInput;
                url = buildApiUrl(input, type);
                jsonResponse = makeApiCall(url);
                response = new JSONObject(jsonResponse);
                predictions = response.getJSONArray("predictions");
                displayOptions(predictions);
              }
            } else {
              // If the number is greater than 5, treat it as a new input for API call

              input = userInput;
              url = buildApiUrl(input, type);
              jsonResponse = makeApiCall(url);
              response = new JSONObject(jsonResponse);
              predictions = response.getJSONArray("predictions");
              displayOptions(predictions);
            }

          } else {
            if (index >= 0 && index <= predictions.length()) {

              String mainText =
                  predictions
                      .getJSONObject(index - 1)
                      .getJSONObject("structured_formatting")
                      .getString("main_text");
              locationInfo.put(key, mainText);
              updateRelatedFields(predictions.getJSONObject(index - 1), key);
              break;
            }
          }
        } else {
          input = userInput;
          url = buildApiUrl(input, type);
          jsonResponse = makeApiCall(url);
          response = new JSONObject(jsonResponse);
          predictions = response.getJSONArray("predictions");

          if (predictions.isEmpty()) {
            locationInfo.put(key, userInput);
            break;
          }

          displayOptions(predictions);
        }
      }
    } catch (IOException e) {
      System.out.println("Error occurred while making API call: " + e.getMessage());
    }
  }

  private static String buildApiUrl(String input, String type) throws IOException {
    String encodedInput = URLEncoder.encode(input, StandardCharsets.UTF_8);
    return BASE_URL + "?input=" + encodedInput + "&key=" + API_KEY + "&type=" + type;
  }

  private static String makeApiCall(String url) throws IOException {
    HttpGet request = new HttpGet(URI.create(url));

    try (CloseableHttpResponse response = client.execute(request)) {
      return EntityUtils.toString(response.getEntity());
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  private static void displayOptions(JSONArray predictions) {
    for (int i = 0; i < predictions.length(); i++) {
      JSONObject prediction = predictions.getJSONObject(i);
      String mainText = prediction.getString("description");
      System.out.println((i + 1) + ". " + mainText);
    }
  }

  private static void updateRelatedFields(JSONObject prediction, String key) throws IOException {
    JSONArray terms = prediction.getJSONArray("terms");

    switch (key) {
      case "state":
        if (!locationInfo.containsKey("country")) {
          validateAndUpdateLocation(terms, "country");
        }
        break;
      case "city":
        if (!locationInfo.containsKey("country")) {
          validateAndUpdateLocation(terms, "country");
        }
        if (!locationInfo.containsKey("state")) {
          validateAndUpdateLocation(terms, "administrative_area_level_1"); // For state
        }
        break;
      case "address":
      case "pinCode":
        if (!locationInfo.containsKey("country")) {
          validateAndUpdateLocation(terms, "country");
        }
        if (!locationInfo.containsKey("state")) {
          validateAndUpdateLocation(terms, "administrative_area_level_1"); // For state
        }
        if (!locationInfo.containsKey("city")) {
          validateAndUpdateLocation(terms, "locality"); // For city
        }
        break;
    }
  }

  private static void validateAndUpdateLocation(JSONArray terms, String type) throws IOException {
    // Loop through the terms array from the last element to the first

    for (int i = terms.length() - 1; i >= 0; i--) {
      String termValue = terms.getJSONObject(i).getString("value");
      if (isValidLocation(termValue, type)) {
        // Map the type to locationInfo key
        String locationKey = getLocationKey(type);
        locationInfo.put(locationKey, termValue);
        terms.remove(i);
        break; // Exit loop once valid location is found
      }
    }
  }

  private static boolean isValidLocation(String input, String type) throws IOException {
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

  private static String getLocationKey(String type) {
    return switch (type) {
      case "country" -> "country";
      case "administrative_area_level_1" -> // State type in Google API
          "state";
      case "locality" -> // City type in Google API
          "city";
      default -> null;
    };
  }

  private static void displayLocationInfo() {
    System.out.println("\nCurrent Location Information:");
    System.out.println("Country - " + locationInfo.getOrDefault("country", "Not set"));
    System.out.println("State - " + locationInfo.getOrDefault("state", "Not set"));
    System.out.println("City - " + locationInfo.getOrDefault("city", "Not set"));
    System.out.println("Address - " + locationInfo.getOrDefault("address", "Not set"));
    System.out.println("PinCode - " + locationInfo.getOrDefault("pinCode", "Not set"));
  }

  private static String getStringInput(String prompt) {
    System.out.print(prompt + ": ");
    return scanner.nextLine().trim();
  }

  private static int getIntInput(String prompt) {
    while (true) {
      try {
        System.out.print(prompt + " ");
        return Integer.parseInt(scanner.nextLine().trim());
      } catch (NumberFormatException e) {
        System.out.println("Invalid input. Please enter a number.");
      }
    }
  }
}
