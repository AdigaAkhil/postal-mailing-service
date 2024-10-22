
# Postal Mailing Service

## Overview
This project implements a postal mailing service that integrates with Google APIs to facilitate the input of address details including **Country**, **State**, **City**, **Street Address**, and **Zip Code**.

### Features
- Interactive command-line interface for address input.
- Integration with Google APIs for address suggestion and validation.
   - **Google Places Autocomplete API**
   - **Google Geolocation API**
   - **Google Geocoding API**
- Supports input of **Country**, **State**, **City**, **Street Address**, and **Zip Code**.
- Efficient address completion and correction.

## Prerequisites
Before running the application, ensure you have the following:
- **Java Development Kit (JDK) 11** or later.
- **Maven** or **Gradle** for dependency management (if applicable).
- A **Google API key** with access to the following APIs:
   - **Google Places API**
   - **Google Geolocation API**
   - **Google Geocoding API**

## Installation

1. Clone the repository:

    ```bash
    git clone https://github.com/AdigaAkhil/postal-mailing-service.git
    cd postal-mailing-service
    ```

2. Build the project (if using Maven):

    ```bash
    mvn clean package
    ```

## Setting Up the Google API Key
To run the application, you need to set the Google API key as an environment variable named `GOOGLE_API_KEY`. Additionally, ensure that the API key has the necessary access permissions and API restrictions set up.

### API Access Permissions
Your Google API key must have access to the following APIs:
- **Google Places API**
- **Google Geolocation API**
- **Google Geocoding API**

### Setting Up API Restrictions (Optional but Recommended)
For enhanced security, it's recommended to set up API restrictions on your API key:

1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Select your API key from the list of credentials.
3. Under the "API restrictions" section, select **Restrict key**.
4. Enable the following APIs:
   - **Places API**
   - **Geolocation API**
   - **Geocoding API**
5. Save the changes.

## Running the Application

### Using the Command Line
Set the environment variable and run the JAR file using the following command:

```bash
GOOGLE_API_KEY=<key> java -jar target/postal-mailing-service.jar
```

- Replace `<key>` with your actual Google API key.
- Adjust the JAR file name if necessary.

### Using an IDE
1. Go to **Run > Edit Configurations** in your IDE.
2. Select your run configuration or create a new one.
3. In the **Environment Variables** section, add a new variable:
   - **Name**: `GOOGLE_API_KEY`
   - **Value**: Google API key
4. Save the configuration and run the application.

## Usage
After starting the application, follow the on-screen prompts to input address details. The application will suggest completions based on your input using the integrated Google APIs.

## Acknowledgments
- **Google APIs** for providing address suggestions and geolocation services.
