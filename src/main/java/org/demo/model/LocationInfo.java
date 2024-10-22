package org.demo.model;

import java.util.Optional;

/**
 * The LocationInfo class holds information about the user's location, including country, state,
 * city, address, and pin code.
 */
public class LocationInfo {

  private String country;
  private String state;
  private String city;
  private String address;
  private String pinCode;

  /** Clears all location information. */
  public void clear() {
    this.country = null;
    this.state = null;
    this.city = null;
    this.address = null;
    this.pinCode = null;
  }

  // Getters and Setters

  public Optional<String> getCountry() {
    return Optional.ofNullable(country);
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public Optional<String> getState() {
    return Optional.ofNullable(state);
  }

  public void setState(String state) {
    this.state = state;
  }

  public Optional<String> getCity() {
    return Optional.ofNullable(city);
  }

  public void setCity(String city) {
    this.city = city;
  }

  public Optional<String> getAddress() {
    return Optional.ofNullable(address);
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public void appendAddress(String additionalAddress) {
    if (this.address == null || this.address.isEmpty()) {
      this.address = additionalAddress;
    } else {
      this.address += ", " + additionalAddress;
    }
  }

  public Optional<String> getPinCode() {
    return Optional.ofNullable(pinCode);
  }

  public void setPinCode(String pinCode) {
    this.pinCode = pinCode;
  }

  /**
   * Sets the value of a location field based on the key.
   *
   * @param key The key representing the location field.
   * @param value The value to set.
   */
  public void setValueByKey(String key, String value) {
    switch (key) {
      case "country" -> setCountry(value);
      case "state" -> setState(value);
      case "city" -> setCity(value);
      case "address" -> setAddress(value);
      case "pinCode" -> setPinCode(value);
      default -> {}
    }
  }
}
