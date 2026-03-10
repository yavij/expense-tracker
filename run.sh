#!/bin/bash
# Expense Tracker - Run script
# Backend uses port 7001 (7000 often used by macOS AirPlay)

cd "$(dirname "$0")"

# Use Java 17 if available
if [ -d "/Library/Java/JavaVirtualMachines/jdk-17.0.2.jdk" ]; then
  export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-17.0.2.jdk/Contents/Home"
fi

echo "=== Expense Tracker ==="
echo ""
echo "Starting backend (port 7001)..."
echo "Starting frontend (port 3000)..."
echo ""

# Run both; exit when either exits
# DEV_PHONE_OTP=123456 enables phone login without Firebase (dev mode)
# FIREBASE_PROJECT_ID: set to your Firebase project ID for server-side token verification
# RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET: Get test keys from https://dashboard.razorpay.com/app/keys
export RAZORPAY_KEY_ID="${RAZORPAY_KEY_ID:-rzp_test_REPLACE_ME}"
export RAZORPAY_KEY_SECRET="${RAZORPAY_KEY_SECRET:-REPLACE_ME_SECRET}"
export FIREBASE_PROJECT_ID="${FIREBASE_PROJECT_ID:-app-debug-21407}"

(cd backend && PORT=7001 DEV_PHONE_OTP=123456 FIREBASE_PROJECT_ID="$FIREBASE_PROJECT_ID" RAZORPAY_KEY_ID="$RAZORPAY_KEY_ID" RAZORPAY_KEY_SECRET="$RAZORPAY_KEY_SECRET" mvn exec:java -Dexec.mainClass="expensetracker.Main") &
B=$!
(cd frontend && npm run dev) &
F=$!

trap "kill $B $F 2>/dev/null; exit 0" INT TERM
wait
