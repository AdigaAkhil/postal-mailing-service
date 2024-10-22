package demo.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.demo.model.LocationInfo;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the LocationInfo class.
 */
public class LocationInfoTest {

  @Test
  public void testSetAndGetCountry() {
    LocationInfo locationInfo = new LocationInfo();
    locationInfo.setCountry("USA");
    assertEquals(Optional.of("USA"), locationInfo.getCountry());
  }

  @Test
  public void testSetAndGetState() {
    LocationInfo locationInfo = new LocationInfo();
    locationInfo.setState("California");
    assertEquals(Optional.of("California"), locationInfo.getState());
  }

  @Test
  public void testSetAndGetCity() {
    LocationInfo locationInfo = new LocationInfo();
    locationInfo.setCity("Los Angeles");
    assertEquals(Optional.of("Los Angeles"), locationInfo.getCity());
  }

  @Test
  public void testSetAndGetAddress() {
    LocationInfo locationInfo = new LocationInfo();
    locationInfo.setAddress("123 Main St");
    assertEquals(Optional.of("123 Main St"), locationInfo.getAddress());
  }

  @Test
  public void testAppendAddress() {
    LocationInfo locationInfo = new LocationInfo();
    locationInfo.appendAddress("123 Main St");
    locationInfo.appendAddress("Apt 4B");
    assertEquals(Optional.of("123 Main St, Apt 4B"), locationInfo.getAddress());
  }

  @Test
  public void testAppendAddress_whenAddressIsNull() {
    LocationInfo locationInfo = new LocationInfo();
    locationInfo.appendAddress("Suite 100");
    assertEquals(Optional.of("Suite 100"), locationInfo.getAddress());
  }

  @Test
  public void testSetAndGetPinCode() {
    LocationInfo locationInfo = new LocationInfo();
    locationInfo.setPinCode("90001");
    assertEquals(Optional.of("90001"), locationInfo.getPinCode());
  }

  @Test
  public void testClear() {
    LocationInfo locationInfo = new LocationInfo();
    locationInfo.setCountry("USA");
    locationInfo.setState("California");
    locationInfo.setCity("Los Angeles");
    locationInfo.setAddress("123 Main St");
    locationInfo.setPinCode("90001");

    locationInfo.clear();

    assertEquals(Optional.empty(), locationInfo.getCountry());
    assertEquals(Optional.empty(), locationInfo.getState());
    assertEquals(Optional.empty(), locationInfo.getCity());
    assertEquals(Optional.empty(), locationInfo.getAddress());
    assertEquals(Optional.empty(), locationInfo.getPinCode());
  }

  @Test
  public void testSetValueByKey_withValidKeys() {
    LocationInfo locationInfo = new LocationInfo();
    locationInfo.setValueByKey("country", "USA");
    locationInfo.setValueByKey("state", "California");
    locationInfo.setValueByKey("city", "Los Angeles");
    locationInfo.setValueByKey("address", "123 Main St");
    locationInfo.setValueByKey("pinCode", "90001");

    assertEquals(Optional.of("USA"), locationInfo.getCountry());
    assertEquals(Optional.of("California"), locationInfo.getState());
    assertEquals(Optional.of("Los Angeles"), locationInfo.getCity());
    assertEquals(Optional.of("123 Main St"), locationInfo.getAddress());
    assertEquals(Optional.of("90001"), locationInfo.getPinCode());
  }

  @Test
  public void testSetValueByKey_withInvalidKey() {
    LocationInfo locationInfo = new LocationInfo();
    locationInfo.setValueByKey("invalidKey", "Some Value");

    // No fields should be set
    assertEquals(Optional.empty(), locationInfo.getCountry());
    assertEquals(Optional.empty(), locationInfo.getState());
    assertEquals(Optional.empty(), locationInfo.getCity());
    assertEquals(Optional.empty(), locationInfo.getAddress());
    assertEquals(Optional.empty(), locationInfo.getPinCode());
  }

  @Test
  public void testGetters_whenFieldsAreNull() {
    LocationInfo locationInfo = new LocationInfo();

    assertEquals(Optional.empty(), locationInfo.getCountry());
    assertEquals(Optional.empty(), locationInfo.getState());
    assertEquals(Optional.empty(), locationInfo.getCity());
    assertEquals(Optional.empty(), locationInfo.getAddress());
    assertEquals(Optional.empty(), locationInfo.getPinCode());
  }

  @Test
  public void testAppendAddress_whenExistingAddressIsEmpty() {
    LocationInfo locationInfo = new LocationInfo();
    locationInfo.setAddress("");
    locationInfo.appendAddress("Apt 4B");

    assertEquals(Optional.of("Apt 4B"), locationInfo.getAddress());
  }
}
