#!/bin/bash

# Load environment variables from .env file
export $(grep -v '^#' .env | xargs)

# Run the backend with local profile
./mvnw spring-boot:run -Dspring.profiles.active=local 