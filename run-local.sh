#!/bin/bash

# Load environment variables from .env file
set -a
source .env
set +a

# Set Flyway locations to point to the correct migrations directory
export FLYWAY_LOCATIONS="filesystem:../financial-tracker-db/migrations"

# Clean and compile the project first
echo "Cleaning and compiling the project..."
./mvnw clean compile

# Run the backend with local profile
echo "Starting the backend with local profile..."
./mvnw spring-boot:run -Dspring-boot.run.profiles=local 