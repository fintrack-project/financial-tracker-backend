#!/bin/bash

# Load environment variables from .env file
set -a
source .env
set +a

# Run the backend with local profile
./mvnw spring-boot:run -Dspring.profiles.active=local 