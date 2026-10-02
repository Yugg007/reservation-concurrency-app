#!/bin/bash

echo ""
echo "============================================================"
echo "            BOOKING CONCURRENCY TEST"
echo "============================================================"
echo ""

# ------------------------------------------------------------
# Check k6
# ------------------------------------------------------------

if ! command -v k6 &> /dev/null
then
    echo "ERROR: k6 is not installed or not available in PATH."
    echo ""
    echo "Please install k6 and try again."
    echo ""
    exit 1
fi

# ------------------------------------------------------------
# Configuration
# ------------------------------------------------------------

read -p "Enter Show ID: " SHOW_ID

if [ -z "$SHOW_ID" ]; then
    echo ""
    echo "ERROR: Show ID cannot be empty."
    exit 1
fi

read -p "Enter Base URL [http://localhost:8080]: " BASE_URL

if [ -z "$BASE_URL" ]; then
    BASE_URL="http://localhost:8080"
fi

read -p "Enter number of concurrent users [100]: " VUS

if [ -z "$VUS" ]; then
    VUS=100
fi

read -p "Enter seats to test (comma-separated) [A2]: " SEATS

if [ -z "$SEATS" ]; then
    SEATS="A2"
fi

# ------------------------------------------------------------
# Display configuration
# ------------------------------------------------------------

echo ""
echo "============================================================"
echo "TEST CONFIGURATION"
echo "============================================================"
echo ""
echo "Base URL          : $BASE_URL"
echo "Show ID           : $SHOW_ID"
echo "Seats             : $SEATS"
echo "Concurrent Users  : $VUS"
echo ""
echo "============================================================"
echo ""
echo "Starting test..."
echo ""

# ------------------------------------------------------------
# Run k6
# ------------------------------------------------------------

k6 run \
    -e BASE_URL="$BASE_URL" \
    -e SHOW_ID="$SHOW_ID" \
    -e VUS="$VUS" \
    -e SEATS="$SEATS" \
    booking-concurrency.js

echo ""
echo "============================================================"
echo "Test execution completed."
echo "============================================================"