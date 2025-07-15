#!/bin/bash

# Load environment variables from .env file
set -a
source .env
set +a

# Set Flyway locations to point to the correct migrations directory
export FLYWAY_LOCATIONS="filesystem:../financial-tracker-db/migrations"

# Run the backend with local profile
./mvnw spring-boot:run -Dspring.profiles.active=local 