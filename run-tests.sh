#!/bin/bash

# FinTrack Backend Test Runner Script
# Usage:
#   ./run-tests.sh                    # Run all tests
#   ./run-tests.sh -d A               # Run all tests under directory A
#   ./run-tests.sh -d "A B"           # Run all tests under directories A and B
#   ./run-tests.sh -d "B B-1 B-1-2"  # Run all tests under B-1-2 directory
#   ./run-tests.sh -p "com.fintrack.service"        # Run tests in specific package
#   ./run-tests.sh -f "BaseTest"      # Run specific test file (BaseTest.java)
#   ./run-tests.sh -f "SimpleTest ApplicationTest"  # Run multiple test files

set -e  # Exit on any error

# Load environment variables from .env file if it exists
load_env_variables() {
    if [ -f ".env" ]; then
        print_info "Loading environment variables from .env file..."
        
        # Read .env file and export variables safely
        while IFS= read -r line; do
            # Skip comments and empty lines
            if [[ "$line" =~ ^[[:space:]]*# ]] || [[ -z "${line// }" ]]; then
                continue
            fi
            
            # Check if line contains an assignment
            if [[ "$line" =~ ^[[:space:]]*[A-Za-z_][A-Za-z0-9_]*= ]]; then
                # Extract variable name and value
                var_name=$(echo "$line" | cut -d'=' -f1 | xargs)
                var_value=$(echo "$line" | cut -d'=' -f2- | xargs)
                
                # Remove quotes if present
                var_value=$(echo "$var_value" | sed 's/^"//;s/"$//;s/^'\''//;s/'\''$//')
                
                # Export the variable
                export "$var_name=$var_value"
            fi
        done < ".env"
        
        print_success "Environment variables loaded successfully"
    else
        print_warning "No .env file found. Using system environment variables."
    fi
}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
TEST_DIRECTORIES=""
TEST_PACKAGE=""
TEST_FILES=""
VERBOSE=false
SKIP_TESTS=false
USE_ENV_VARS=true

# Function to print usage
print_usage() {
    echo -e "${BLUE}FinTrack Backend Test Runner${NC}"
    echo ""
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -d, --directories DIRS      Run tests in specific directories (space-separated)"
    echo "  -p, --package PACKAGE       Run tests in specific package"
    echo "  -f, --files FILES           Run specific test files (space-separated)"
    echo "  -v, --verbose               Enable verbose output"
    echo "  -s, --skip-tests            Skip tests (just compile)"
    echo "  -e, --no-env                Don't load .env file"
    echo "  -h, --help                  Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                          # Run all tests"
    echo "  $0 -d A                     # Run all tests under directory A"
    echo "  $0 -d 'A B'                 # Run all tests under directories A and B"
    echo "  $0 -d 'B B-1 B-1-2'        # Run all tests under B-1-2 directory"
    echo "  $0 -p 'com.fintrack.service'        # Run tests in service package"
    echo "  $0 -f 'BaseTest'            # Run BaseTest.java"
    echo "  $0 -f 'SimpleTest ApplicationTest'  # Run multiple test files"
    echo "  $0 -v -d A                  # Run tests in directory A with verbose output"
    echo "  $0 -e                       # Run tests without loading .env file"
    echo ""
    echo "Environment Variables:"
    echo "  The script will automatically load environment variables from .env file"
    echo "  Available variables: POSTGRES_USER, DATABASE_NAME, JWT_SECRET, etc."
    echo ""
    echo "Directory Structure Example:"
    echo "  src/test/java/com/fintrack/"
    echo "  ├── A/"
    echo "  │   ├── A1Test.java"
    echo "  │   └── A2Test.java"
    echo "  ├── B/"
    echo "  │   ├── B1/"
    echo "  │   │   ├── B11Test.java"
    echo "  │   │   └── B12Test.java"
    echo "  │   └── B2Test.java"
    echo ""
}

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to show environment variables
show_env_variables() {
    print_info "Current environment variables:"
    echo ""
    
    # Show key environment variables
    local env_vars=(
        "POSTGRES_USER" "DATABASE_NAME" "DATABASE_HOST" "DATABASE_HOST_PORT"
        "JWT_SECRET" "JWT_EXPIRATION"
        "STRIPE_TEST_SECRET_KEY" "STRIPE_TEST_PUBLISHABLE_KEY"
        "SMTP_HOST" "SMTP_PORT" "MAIL_FROM_ADDRESS"
        "KAFKA_BROKER" "VALKEY_HOST" "VALKEY_PORT"
        "RAPIDAPI_KEY" "TWELVE_DATA_API_KEY"
    )
    
    for var in "${env_vars[@]}"; do
        if [ -n "${!var}" ]; then
            # Mask sensitive values
            local value="${!var}"
            if [[ "$var" == *"SECRET"* ]] || [[ "$var" == *"KEY"* ]] || [[ "$var" == *"PASSWORD"* ]]; then
                value="${value:0:8}..."
            fi
            echo "  - $var: $value"
        fi
    done
    echo ""
}

# Function to find test classes in directories
find_test_classes_in_directories() {
    local directories="$1"
    local test_classes=""
    
    # Convert space-separated directories to array
    IFS=' ' read -ra DIR_ARRAY <<< "$directories"
    
    for dir in "${DIR_ARRAY[@]}"; do
        # Find all test files in the directory
        local test_files=$(find "src/test/java" -path "*/$dir/*" -name "*Test.java" -type f 2>/dev/null || true)
        
        if [ -n "$test_files" ]; then
            for file in $test_files; do
                # Extract class name from file path
                local class_name=$(basename "$file" .java)
                local package_path=$(dirname "$file" | sed 's|src/test/java/||' | tr '/' '.')
                local full_class_name="$package_path.$class_name"
                
                # Add to test classes list
                if [ -n "$test_classes" ]; then
                    test_classes="$test_classes,$full_class_name"
                else
                    test_classes="$full_class_name"
                fi
            done
        else
            print_warning "No test files found in directory: $dir"
        fi
    done
    
    echo "$test_classes"
}

# Function to find test classes by file names
find_test_classes_by_files() {
    local files="$1"
    local test_classes=""
    
    # Convert space-separated files to array
    IFS=' ' read -ra FILE_ARRAY <<< "$files"
    
    for file_name in "${FILE_ARRAY[@]}"; do
        # Find the test file by name
        local test_file=$(find "src/test/java" -name "${file_name}.java" -type f 2>/dev/null || true)
        
        if [ -n "$test_file" ]; then
            # Extract class name from file path
            local class_name=$(basename "$test_file" .java)
            local package_path=$(dirname "$test_file" | sed 's|src/test/java/||' | tr '/' '.')
            local full_class_name="$package_path.$class_name"
            
            # Add to test classes list
            if [ -n "$test_classes" ]; then
                test_classes="$test_classes,$full_class_name"
            else
                test_classes="$full_class_name"
            fi
        else
            print_warning "Test file not found: ${file_name}.java"
        fi
    done
    
    echo "$test_classes"
}

# Function to print simple test results summary
print_test_summary() {
    echo ""
    echo "=========================================="
    echo "  📊 FinTrack Backend Test Summary"
    echo "=========================================="
    echo ""
    
    # Extract test results from Maven output
    local total_tests=$(grep "Tests run:" target/surefire-reports/*.txt 2>/dev/null | tail -1 | grep -o "Tests run: [0-9]*" | grep -o "[0-9]*" || echo "0")
    local failures=$(grep "Failures:" target/surefire-reports/*.txt 2>/dev/null | tail -1 | grep -o "Failures: [0-9]*" | grep -o "[0-9]*" || echo "0")
    local errors=$(grep "Errors:" target/surefire-reports/*.txt 2>/dev/null | tail -1 | grep -o "Errors: [0-9]*" | grep -o "[0-9]*" || echo "0")
    local skipped=$(grep "Skipped:" target/surefire-reports/*.txt 2>/dev/null | tail -1 | grep -o "Skipped: [0-9]*" | grep -o "[0-9]*" || echo "0")
    
    # Get individual test class results
    local test_classes=$(find target/surefire-reports -name "*.txt" -exec basename {} .txt \; 2>/dev/null)
    
    echo "📈 Overall Results:"
    echo "  Total Tests: $total_tests"
    echo "  Passed: $((total_tests - failures - errors - skipped))"
    echo "  Failed: $failures"
    echo "  Errors: $errors"
    echo "  Skipped: $skipped"
    echo ""
    
    echo "🧪 Test Classes:"
    for test_class in $test_classes; do
        local class_file="target/surefire-reports/${test_class}.txt"
        if [ -f "$class_file" ]; then
            local class_tests=$(grep "Tests run:" "$class_file" | grep -o "Tests run: [0-9]*" | grep -o "[0-9]*" || echo "0")
            local class_failures=$(grep "Failures:" "$class_file" | grep -o "Failures: [0-9]*" | grep -o "[0-9]*" || echo "0")
            local class_errors=$(grep "Errors:" "$class_file" | grep -o "Errors: [0-9]*" | grep -o "[0-9]*" || echo "0")
            
            if [ "$class_failures" -eq 0 ] && [ "$class_errors" -eq 0 ]; then
                echo "  ✅ $test_class ($class_tests tests)"
            else
                echo "  ❌ $test_class ($class_tests tests, $class_failures failures, $class_errors errors)"
            fi
        fi
    done
    
    echo ""
    if [ "$failures" -eq 0 ] && [ "$errors" -eq 0 ]; then
        echo "🎉 All tests passed successfully!"
    else
        echo "⚠️  Some tests failed. Check the output above for details."
    fi
    echo "=========================================="
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -d|--directories)
            TEST_DIRECTORIES="$2"
            shift 2
            ;;
        -p|--package)
            TEST_PACKAGE="$2"
            shift 2
            ;;
        -f|--files)
            TEST_FILES="$2"
            shift 2
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -s|--skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        -e|--no-env)
            USE_ENV_VARS=false
            shift
            ;;
        -h|--help)
            print_usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            print_usage
            exit 1
            ;;
    esac
done

# Function to build Maven command
build_maven_command() {
    local cmd="./mvnw clean compile"
    
    if [ "$SKIP_TESTS" = true ]; then
        echo "$cmd"
        return
    fi
    
    cmd="$cmd test"
    
    # Add test directory filter if specified
    if [ -n "$TEST_DIRECTORIES" ]; then
        local test_classes=$(find_test_classes_in_directories "$TEST_DIRECTORIES")
        if [ -n "$test_classes" ]; then
            cmd="$cmd -Dtest=\"$test_classes\""
            print_info "Found test classes: $test_classes"
        else
            print_error "No test classes found in specified directories: $TEST_DIRECTORIES"
            exit 1
        fi
    fi
    
    # Add package filter if specified
    if [ -n "$TEST_PACKAGE" ]; then
        cmd="$cmd -Dtest=\"$TEST_PACKAGE.*\""
    fi
    
    # Add test file filter if specified
    if [ -n "$TEST_FILES" ]; then
        local test_classes=$(find_test_classes_by_files "$TEST_FILES")
        if [ -n "$test_classes" ]; then
            cmd="$cmd -Dtest=\"$test_classes\""
            print_info "Found test classes: $test_classes"
        else
            print_error "No test classes found for specified files: $TEST_FILES"
            exit 1
        fi
    fi
    
    # Add verbose flag if requested
    if [ "$VERBOSE" = true ]; then
        cmd="$cmd -X"
    fi
    
    echo "$cmd"
}

# Function to run tests
run_tests() {
    print_info "Starting FinTrack Backend Tests..."
    echo ""
    
    # Check if we're in the right directory
    if [ ! -f "pom.xml" ]; then
        print_error "pom.xml not found. Please run this script from the backend directory."
        exit 1
    fi
    
    # Load environment variables if enabled
    if [ "$USE_ENV_VARS" = true ]; then
        load_env_variables
        show_env_variables
    else
        print_info "Skipping .env file loading (--no-env flag used)"
    fi
    
    # Build the Maven command
    local maven_cmd=$(build_maven_command)
    
    print_info "Executing: $maven_cmd"
    echo ""
    
    # Execute the command with proper error handling
    if eval "$maven_cmd"; then
        echo ""
        print_success "All tests completed successfully! 🎉"
        
        # Show test summary if available
        if [ -d "target/surefire-reports" ]; then
            print_info "Test reports available in target/surefire-reports/"
            print_test_summary
        fi
    else
        echo ""
        print_error "Tests failed! ❌"
        exit 1
    fi
}

# Function to show available test directories
show_available_test_directories() {
    print_info "Available test directories:"
    echo ""
    
    # Find all test directories
    local test_dirs=$(find src/test/java -type d -name "*" 2>/dev/null | sed 's|src/test/java/||' | grep -v "^$" || true)
    
    if [ -z "$test_dirs" ]; then
        print_warning "No test directories found in src/test/java/"
        return
    fi
    
    echo "$test_dirs" | sort | while read -r dir; do
        if [ -n "$dir" ]; then
            # Count test files in this directory
            local test_count=$(find "src/test/java/$dir" -name "*Test.java" -type f 2>/dev/null | wc -l)
            if [ "$test_count" -gt 0 ]; then
                echo "  - $dir ($test_count test files)"
            fi
        fi
    done
    echo ""
}

# Function to show available test files
show_available_test_files() {
    print_info "Available test files:"
    echo ""
    
    # Find all test files
    local test_files=$(find src/test/java -name "*Test.java" -type f 2>/dev/null | sed 's|src/test/java/||' | sed 's|.java$||' | tr '/' '.' || true)
    
    if [ -z "$test_files" ]; then
        print_warning "No test files found in src/test/java/"
        return
    fi
    
    echo "$test_files" | sort | while read -r file; do
        if [ -n "$file" ]; then
            # Extract just the class name
            local class_name=$(echo "$file" | sed 's/.*\.//')
            echo "  - $class_name"
        fi
    done
    echo ""
}

# Main execution
main() {
    echo -e "${BLUE}================================${NC}"
    echo -e "${BLUE}  FinTrack Backend Test Runner${NC}"
    echo -e "${BLUE}================================${NC}"
    echo ""
    
    # Show available test directories and files if no specific tests are requested
    if [ -z "$TEST_DIRECTORIES" ] && [ -z "$TEST_PACKAGE" ] && [ -z "$TEST_FILES" ] && [ "$SKIP_TESTS" = false ]; then
        print_info "No specific tests specified. Running all tests."
        show_available_test_directories
        show_available_test_files
    fi
    
    # Run the tests
    run_tests
}

# Run main function
main "$@" 