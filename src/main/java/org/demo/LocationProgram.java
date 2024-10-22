package org.demo;

import java.io.Closeable;
import java.io.IOException;
import java.util.Scanner;
import org.demo.exception.GoogleApiException;
import org.demo.model.LocationInfo;
import org.demo.service.GoogleApiService;
import org.demo.util.GoogleMapsUtil;
import org.demo.util.InputValidator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The LocationProgram class contains the main logic for interacting with the user and managing
 * location information.
 */
public class LocationProgram implements Closeable {

  private static final Logger logger = LoggerFactory.getLogger(LocationProgram.class);

  private static final int EXIT_CHOICE = 9;
  private static final int CLEAR_CHOICE = 7;
  private static final int USE_CURRENT_LOCATION = 6;

  private final String apiKey;
  private final LocationInfo locationInfo;
  private final Scanner scanner;
  private final GoogleApiService googleApiService;

  /** Constructs a new LocationProgram instance. */
  public LocationProgram() {
    this.apiKey = System.getenv("GOOGLE_API_KEY");
    if (this.apiKey == null || this.apiKey.isEmpty()) {
      throw new IllegalStateException("GOOGLE_API_KEY environment variable is not set");
    }
    this.scanner = new Scanner(System.in);
    this.googleApiService = new GoogleApiService(apiKey);
    this.locationInfo = new LocationInfo();
  }

  /** Starts the main loop of the application. */
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
      logger.info("An error occurred: {}", e.getMessage());
    }
  }

  /** Displays the main menu to the user. */
  private void displayMenu() {
    System.out.println("\nPlease enter the index of your choice: ");
    System.out.println("1. Country");
    System.out.println("2. State");
    System.out.println("3. City");
    System.out.println("4. Address");
    System.out.println("5. PinCode");
    System.out.println("6. Use Current Location");
    System.out.println("7. Clear");
    System.out.println("8. Pinpoint on Google Maps");
    System.out.println("9. Exit");
  }

  /**
   * Processes the user's menu choice.
   *
   * @param choice The user's choice from the menu.
   */
  private void processChoice(int choice) {
    try {
      switch (choice) {
        case 1 -> handleLocationInput("country", "country");
        case 2 -> handleLocationInput("state", "administrative_area_level_1");
        case 3 -> handleLocationInput("city", "locality");
        case 4 -> handleLocationInput("address", "address");
        case 5 -> handleLocationInput("pinCode", "postal_code");
        case USE_CURRENT_LOCATION -> useCurrentLocation();
        case CLEAR_CHOICE -> clearLocationInfo();
        case 8 -> pinpointOnGoogleMaps();
        default -> System.out.println("Invalid choice. Please try again.");
      }
    } catch (Exception e) {
      logger.error("Error processing choice", e);
      logger.info("An error occurred while processing your choice: {}", e.getMessage());
    }
  }

  /**
   * Handles location input from the user.
   *
   * @param key The key representing the location type (e.g., "country").
   * @param type The type parameter for the API call.
   * @throws IOException If an I/O error occurs.
   * @throws GoogleApiException If an error occurs related to the Google API.
   */
  private void handleLocationInput(String key, String type) throws IOException, GoogleApiException {
    String input = getStringInput("Please enter the " + key + ": ");
    logger.info("Received {} '{}' from user", key, input);
    processApiRequest(input, type, key);
  }

  /**
   * Processes the API request based on user input.
   *
   * @param input The user's input.
   * @param type The type parameter for the API call.
   * @param key The key representing the location type.
   * @throws IOException If an I/O error occurs.
   * @throws GoogleApiException If an error occurs related to the Google API.
   */
  private void processApiRequest(String input, String type, String key)
      throws IOException, GoogleApiException {
    JSONArray predictions = googleApiService.getPredictions(input, type);

    if (predictions.isEmpty()) {
      logger.info("No results found for input '{}' and type '{}'", input, type);
      locationInfo.setValueByKey(key, input);
      return;
    }

    displayOptions(predictions);
    handleUserSelection(predictions, type, key);
  }

  /**
   * Handles the user's selection from the list of predictions.
   *
   * @param predictions The array of predictions from the API.
   * @param type The type parameter for the API call.
   * @param key The key representing the location type.
   * @throws IOException If an I/O error occurs.
   * @throws GoogleApiException If an error occurs related to the Google API.
   */
  private void handleUserSelection(JSONArray predictions, String type, String key)
      throws IOException, GoogleApiException {
    while (true) {
      String userInput = getStringInput("Enter index or continue searching: ");

      if (InputValidator.isInteger(userInput)) {
        int index = Integer.parseInt(userInput);
        if (handleIndexInput(predictions, index, type, key)) {
          break;
        } else if (type.equalsIgnoreCase("postal_code")) {
          // Re-query with the user input
          predictions = googleApiService.getPredictions(userInput, type);
          if (predictions.isEmpty()) {
            locationInfo.setValueByKey(key, userInput);
            break;
          }
          displayOptions(predictions);
        } else {
          logger.info("Invalid index. Please try again.");
        }
      } else {
        predictions = googleApiService.getPredictions(userInput, type);
        if (predictions.isEmpty()) {
          locationInfo.setValueByKey(key, userInput);
          break;
        }
        displayOptions(predictions);
      }
    }
  }

  /**
   * Handles the index input from the user and selects the prediction if valid.
   *
   * @param predictions The array of predictions from the API.
   * @param index The index entered by the user.
   * @param type The type parameter for the API call.
   * @param key The key representing the location type.
   * @return True if the prediction was successfully selected, false otherwise.
   * @throws IOException If an I/O error occurs.
   * @throws GoogleApiException If an error occurs related to the Google API.
   */
  private boolean handleIndexInput(JSONArray predictions, int index, String type, String key)
      throws IOException, GoogleApiException {
    if (type.equalsIgnoreCase("postal_code")) {
      if (index <= 5) {
        String confirmation = getStringInput("Is this an index value? (y/n)");
        if (confirmation.equalsIgnoreCase("y")) {
          selectPrediction(predictions, index - 1, key);
          return true;
        }
      }
      return false;
    } else if (index > 0 && index <= predictions.length()) {
      selectPrediction(predictions, index - 1, key);
      return true;
    }
    return false;
  }

  /**
   * Selects a prediction based on the user's choice and updates location information.
   *
   * @param predictions The array of predictions from the API.
   * @param index The index of the selected prediction.
   * @param key The key representing the location type.
   * @throws IOException If an I/O error occurs.
   * @throws GoogleApiException If an error occurs related to the Google API.
   */
  private void selectPrediction(JSONArray predictions, int index, String key)
      throws IOException, GoogleApiException {
    JSONObject prediction = predictions.getJSONObject(index);
    String mainText = prediction.getJSONObject("structured_formatting").getString("main_text");
    locationInfo.setValueByKey(key, mainText);
    updateRelatedFields(prediction, key);
  }

  /**
   * Updates related fields in the location information based on the selected prediction.
   *
   * @param prediction The selected prediction.
   * @param key The key representing the location type.
   * @throws IOException If an I/O error occurs.
   * @throws GoogleApiException If an error occurs related to the Google API.
   */
  private void updateRelatedFields(JSONObject prediction, String key)
      throws IOException, GoogleApiException {
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
      default -> logger.debug("No additional fields to update for key: {}", key);
    }
  }

  /**
   * Validates and updates the location information based on the terms and type.
   *
   * @param terms The array of terms from the prediction.
   * @param type The type parameter for the API call.
   * @throws IOException If an I/O error occurs.
   * @throws GoogleApiException If an error occurs related to the Google API.
   */
  private void validateAndUpdateLocation(JSONArray terms, String type)
      throws IOException, GoogleApiException {
    for (int i = terms.length() - 1; i >= 0; i--) {
      String termValue = terms.getJSONObject(i).getString("value");
      if (isValidLocation(termValue, type)) {
        String locationKey = getLocationKey(type);
        if (locationKey != null) {
          locationInfo.setValueByKey(locationKey, termValue);
          terms.remove(i);
          break;
        }
      }
    }
  }

  /**
   * Checks if the input is a valid location of the specified type.
   *
   * @param input The input string to validate.
   * @param type The type parameter for the API call.
   * @return True if valid, false otherwise.
   * @throws IOException If an I/O error occurs.
   * @throws GoogleApiException If an error occurs related to the Google API.
   */
  private boolean isValidLocation(String input, String type)
      throws IOException, GoogleApiException {
    JSONArray predictions = googleApiService.getPredictions(input, type);
    if (!predictions.isEmpty()) {
      String mainText =
          predictions
              .getJSONObject(0)
              .getJSONObject("structured_formatting")
              .getString("main_text");

      return mainText.toLowerCase().contains(input.toLowerCase());
    }
    return false;
  }

  /**
   * Maps the Google API location type to the LocationInfo key.
   *
   * @param type The Google API location type.
   * @return The corresponding key in LocationInfo, or null if not applicable.
   */
  private String getLocationKey(String type) {
    return switch (type) {
      case "country" -> "country";
      case "administrative_area_level_1" -> "state";
      case "locality" -> "city";
      default -> null;
    };
  }

  /**
   * Displays the list of prediction options to the user.
   *
   * @param predictions The array of predictions from the API.
   */
  private void displayOptions(JSONArray predictions) {
    for (int i = 0; i < predictions.length(); i++) {
      JSONObject prediction = predictions.getJSONObject(i);
      String description = prediction.getString("description");
      System.out.println((i + 1) + ". " + description);
    }
  }

  /** Uses the current location to update location information. */
  private void useCurrentLocation() {
    try {
      logger.info("Fetching your current location...");
      JSONObject geoLocation = googleApiService.getGeolocation();
      double latitude = geoLocation.getJSONObject("location").getDouble("lat");
      double longitude = geoLocation.getJSONObject("location").getDouble("lng");

      logger.info("Coordinates: {}, {}", latitude, longitude);

      JSONObject geocodingData = googleApiService.getGeocodingData(latitude, longitude);
      parseAndSetAddress(geocodingData);

      logger.info("Location information updated based on your current location.");
    } catch (GoogleApiException e) {
      logger.error("Google API error: {}", e.getMessage());
      logger.info("An error occurred with the Google API: {}", e.getMessage());
    } catch (IOException e) {
      logger.error("Error fetching current location", e);
      logger.info("Failed to fetch current location. Please try again or use manual input.");
    }
  }

  /**
   * Parses the geocoding data and updates location information.
   *
   * @param geocodingData The JSONObject response from the geocoding API.
   */
  private void parseAndSetAddress(JSONObject geocodingData) {
    JSONArray results = geocodingData.getJSONArray("results");

    if (!results.isEmpty()) {
      JSONArray addressComponents = results.getJSONObject(0).getJSONArray("address_components");

      locationInfo.clear();

      for (int i = 0; i < addressComponents.length(); i++) {
        JSONObject component = addressComponents.getJSONObject(i);
        JSONArray types = component.getJSONArray("types");
        String longName = component.getString("long_name");

        updateLocationInfo(types, longName);
      }
    } else {
      logger.info("No results found for the current location.");
    }
  }

  /**
   * Updates the location information based on the types of address components.
   *
   * @param types The types of the address component.
   * @param longName The long name of the address component.
   */
  private void updateLocationInfo(JSONArray types, String longName) {
    for (Object typeObj : types) {
      String type = typeObj.toString();
      switch (type) {
        case "country" -> locationInfo.setCountry(longName);
        case "administrative_area_level_1" -> locationInfo.setState(longName);
        case "locality" -> locationInfo.setCity(longName);
        case "postal_code" -> locationInfo.setPinCode(longName);
        case "street_address", "route", "neighborhood", "sublocality", "street_number" ->
            locationInfo.appendAddress(longName);
        default -> {}
      }
    }
  }

  /** Opens the location in Google Maps. */
  private void pinpointOnGoogleMaps() {
    try {
      String address = locationInfo.getAddress().orElse("");
      String city = locationInfo.getCity().orElse("");
      String state = locationInfo.getState().orElse("");
      String country = locationInfo.getCountry().orElse("");

      if (!address.isEmpty() || !city.isEmpty() || !state.isEmpty() || !country.isEmpty()) {
        // Combine address components
        StringBuilder addressInput = new StringBuilder();
        if (!address.isEmpty()) {
          addressInput.append(address);
        }
        if (!city.isEmpty()) {
          if (!addressInput.isEmpty()) {
            addressInput.append(", ");
          }
          addressInput.append(city);
        }
        if (!state.isEmpty()) {
          if (!addressInput.isEmpty()) {
            addressInput.append(", ");
          }
          addressInput.append(state);
        }
        if (!country.isEmpty()) {
          if (!addressInput.isEmpty()) {
            addressInput.append(", ");
          }
          addressInput.append(country);
        }

        String completeAddress = googleApiService.getCompleteAddress(addressInput.toString());
        if (completeAddress != null) {
          String googleMapsUrl = googleApiService.constructGoogleMapsURL(completeAddress);
          GoogleMapsUtil.openInBrowser(googleMapsUrl);
        } else {
          logger.info("Error: Complete address could not be found. Please try again.");
        }
      } else {
        logger.info(
            "Error: Insufficient location data. Please provide at least one address component before using this feature.");
      }
    } catch (Exception e) {
      logger.error("Error pinning on Google Maps", e);
      logger.info("An error occurred while trying to open Google Maps: {}", e.getMessage());
    }
  }


  /** Clears all location information. */
  private void clearLocationInfo() {
    locationInfo.clear();
    logger.info("All location information has been cleared.");
  }

  /** Displays the current location information. */
  private void displayLocationInfo() {
    System.out.println("\nCurrent Location Information:");
    System.out.println("Country: " + locationInfo.getCountry().orElse("Not set"));
    System.out.println("State: " + locationInfo.getState().orElse("Not set"));
    System.out.println("City: " + locationInfo.getCity().orElse("Not set"));
    System.out.println("Address: " + locationInfo.getAddress().orElse("Not set"));
    System.out.println("PinCode: " + locationInfo.getPinCode().orElse("Not set"));
  }

  /**
   * Gets a string input from the user.
   *
   * @param prompt The prompt to display.
   * @return The user's input.
   */
  private String getStringInput(String prompt) {
    System.out.println(prompt);
    return scanner.nextLine().trim();
  }

  /**
   * Gets an integer input from the user.
   *
   * @param prompt The prompt to display.
   * @return The user's input as an integer.
   */
  private int getIntInput(String prompt) {
    while (true) {
      try {
        System.out.println(prompt);
        return Integer.parseInt(scanner.nextLine().trim());
      } catch (NumberFormatException e) {
        logger.warn("Invalid input for integer. Prompt: {}", prompt);
        logger.info("Invalid input. Please enter a number.");
      }
    }
  }

  /**
   * Closes resources used by the LocationProgram.
   *
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void close() throws IOException {
    scanner.close();
    googleApiService.close();
  }
}
