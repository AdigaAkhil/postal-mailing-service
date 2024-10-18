# Postal Mailing Service

## Overview

This project implements a postal mailing service that integrates with the Google Places Autocomplete API to facilitate the input of address details including Country, State, City, Street Address, and Zip Code.

## Features

- Interactive command-line interface for address input
- Integration with Google Places Autocomplete API for address suggestion and validation
- Supports input of Country, State, City, Street Address, and Zip Code
- Efficient address completion and correction

## Prerequisites

Before running the application, ensure you have the following:

- Java Development Kit (JDK) 11 or later
- Maven or Gradle for dependency management (if applicable)
- A Google API key for accessing the Google Places Autocomplete API

## Installation

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/postal-mailing-service.git
   cd postal-mailing-service
   ```

2. Build the project (if using Maven):
   ```
   mvn clean package
   ```

## Setting Up the Google API Key

To run the application, you need to set the Google API key as an environment variable named `GOOGLE_API_KEY`. This can be done in your IDE or via the command line.

### Using the Command Line

Set the environment variable and run the JAR file using the following command:

```bash
GOOGLE_API_KEY=your_actual_api_key java -jar target/postal-mailing-service.jar
```

Replace `your_actual_api_key` with your Google API key and adjust the JAR file name if necessary.

### Using an IDE

1. Go to Run > Edit Configurations in your IDE.
2. Select your run configuration or create a new one.
3. In the Environment Variables section, add a new variable:
    - Name: `GOOGLE_API_KEY`
    - Value: Your actual Google API key
4. Save the configuration and run the application.

## Usage

After starting the application, follow the on-screen prompts to input address details. The application will suggest completions based on your input using the Google Places Autocomplete API.


## Acknowledgments

- Google Places Autocomplete API for providing address suggestions