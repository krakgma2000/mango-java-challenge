#!/bin/bash
#
# Benchmark of the products API, adapted from the script given with the challenge.
#
# Changed for new API requirements (token sent after login, new endpoints, product id read with jq,...)
#
# Proposals (marked as PROPOSAL in the code):
#   1. Wait for the API every 0.5 s, with a time limit.
#   2. Check the status code and the value of every functional request.
#   3. Load tests with a limited number of parallel requests.
#   4. Load tests report the success rate and the successful requests per second.
#
# Environment variables:
#   API_PASSWORD   password of the API user (required, it is not in the repository)
#   API_USERNAME   default mango_test
#   BASE_URL       default http://product-api:8080
#   CONCURRENCY    parallel requests in the load tests, default 50
#   MAX_WAIT       seconds to wait for the API to be ready, default 180

BASE_URL="${BASE_URL:-http://product-api:8080}"
HEALTH_ENDPOINT="$BASE_URL/actuator/health"
API_USERNAME="${API_USERNAME:-mango_test}"
CONCURRENCY="${CONCURRENCY:-50}"

if [ -z "$API_PASSWORD" ]; then
  echo "Error: API_PASSWORD is not set. Run it as: API_PASSWORD=... docker compose up"
  exit 1
fi

# PROPOSAL 1
MAX_WAIT="${MAX_WAIT:-180}"
echo "Esperando a que la API esté lista en $HEALTH_ENDPOINT..."
WAIT_START=$(date +%s.%N)
until curl -s "$HEALTH_ENDPOINT" | grep UP > /dev/null; do
  if [ "$(echo "$(date +%s.%N) - $WAIT_START > $MAX_WAIT" | bc)" -eq 1 ]; then
    echo "Error: the API was not ready after $MAX_WAIT seconds"
    exit 1
  fi
  sleep 0.5
done
WAIT_END=$(date +%s.%N)
echo "API ready after waiting $(echo "$WAIT_END - $WAIT_START" | bc) seconds"
echo -e "\n"

# Login
echo "Logging in as $API_USERNAME..."
TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$API_USERNAME\",\"password\":\"$API_PASSWORD\"}" | jq -r '.accessToken // empty')

if [ -z "$TOKEN" ]; then
  echo "Error: login failed for user $API_USERNAME"
  exit 1
fi
AUTH="Authorization: Bearer $TOKEN"
echo "Logged in"
echo -e "\n"

# PROPOSAL 2
FAILURES=0

check() {
  if [ "$2" != "$3" ] || [ "$4" != "$5" ]; then
    echo "  FAILED: $1 (expected $2 $4, got $3 $5)"
    FAILURES=$((FAILURES + 1))
  else
    echo "  OK: $1"
  fi
}

request() {
  if [ -n "$3" ]; then
    curl -s -w '\n%{http_code}' -X "$1" "$2" -H "$AUTH" -H "Content-Type: application/json" -d "$3"
  else
    curl -s -w '\n%{http_code}' -X "$1" "$2" -H "$AUTH"
  fi
}
body() { sed '$d' <<< "$1"; }
status() { tail -n 1 <<< "$1"; }

# Create a product
echo "Creating product..."
RESPONSE=$(request POST "$BASE_URL/products" '{"name":"Zapatillas deportivas","description":"Modelo 2025 edición limitada"}')
PRODUCT_ID=$(body "$RESPONSE" | jq -r '.id // empty')
check "create product" 201 "$(status "$RESPONSE")"

if [ -z "$PRODUCT_ID" ]; then
  echo "Error: Could not extract product ID from response"
  echo "Response: $(body "$RESPONSE")"
  exit 1
fi

echo "Product created with ID: $PRODUCT_ID"
echo -e "\n"

PRICES_URL="$BASE_URL/products/prices?productId=$PRODUCT_ID"

# Add first price
echo "Adding first price..."
RESPONSE=$(request POST "$PRICES_URL" '{"value":99.99,"initDate":"2024-01-01","endDate":"2024-06-30"}')
body "$RESPONSE"
check "first price" 201 "$(status "$RESPONSE")"
echo -e "\n"

# Add second price
echo "Adding second price..."
RESPONSE=$(request POST "$PRICES_URL" '{"value":129.99,"initDate":"2024-07-01","endDate":"2024-12-31"}')
body "$RESPONSE"
check "second price" 201 "$(status "$RESPONSE")"
echo -e "\n"

# Add third price
echo "Adding third price..."
RESPONSE=$(request POST "$PRICES_URL" '{"value":199.99,"initDate":"2025-01-01","endDate":null}')
body "$RESPONSE"
check "third price" 201 "$(status "$RESPONSE")"
echo -e "\n"

# PROPOSAL 2: a price that overlaps the open one must be rejected
echo "Adding an overlapping price..."
RESPONSE=$(request POST "$PRICES_URL" '{"value":1,"initDate":"2026-01-01"}')
check "overlapping price is rejected" 409 "$(status "$RESPONSE")"
echo -e "\n"

get_price() {
  echo "Getting price on date $1..."
  RESPONSE=$(request GET "$PRICES_URL&date=$1")
  body "$RESPONSE"
  check "price on $1" 200 "$(status "$RESPONSE")" "$2" "$(body "$RESPONSE" | jq -r '.value')"
  echo -e "\n"
}

# Get the price on a specific date
get_price "2024-04-15" "99.99"

# Get another price on a different date
get_price "2024-08-15" "129.99"

# Get current price
get_price "2025-03-01" "199.99"

# PROPOSAL 2: a date without price
echo "Getting price on a date without price..."
RESPONSE=$(request GET "$PRICES_URL&date=2020-01-01")
check "no price on 2020-01-01" 204 "$(status "$RESPONSE")"
echo -e "\n"

# Get full price history
echo "Getting full price history..."
RESPONSE=$(request GET "$BASE_URL/products/prices/history?productId=$PRODUCT_ID")
body "$RESPONSE"
check "price history" 200 "$(status "$RESPONSE")" 3 "$(body "$RESPONSE" | jq '.prices | length')"
echo -e "\n"

if [ "$FAILURES" -gt 0 ]; then
  echo "$FAILURES functional checks failed: the load tests are not run"
  exit 1
fi

echo "===================="
echo "PERFORMANCE TESTING"
echo "===================="
echo "Concurrency: $CONCURRENCY parallel requests"
echo -e "\n"

# PROPOSAL 3 and 4
load_test() {
  local label=$1 total=$2 method=$3 url=$4 data=$5
  local codes
  codes=$(mktemp)

  local start end duration ok
  start=$(date +%s.%N)
  if [ -n "$data" ]; then
    seq 1 "$total" | xargs -P "$CONCURRENCY" -I@ \
      curl -s -o /dev/null -w '%{http_code}\n' -X "$method" "$url" \
      -H "$AUTH" -H "Content-Type: application/json" -d "$data" >> "$codes"
  else
    seq 1 "$total" | xargs -P "$CONCURRENCY" -I@ \
      curl -s -o /dev/null -w '%{http_code}\n' -X "$method" "$url" -H "$AUTH" >> "$codes"
  fi
  end=$(date +%s.%N)

  duration=$(echo "$end - $start" | bc)
  ok=$(grep -c '^2' "$codes")
  echo "$total $label took: $duration seconds"
  echo "  successful: $ok/$total ($(echo "scale=1; 100 * $ok / $total" | bc)%)," \
    "$(echo "scale=1; $ok / $duration" | bc) successful requests/second"
  echo "  status codes: $(sort "$codes" | uniq -c | awk '{printf "%s x %s  ", $1, $2}')"
  rm -f "$codes"
}

# Test concurrent product creation
echo "Testing concurrent product creation..."
load_test "concurrent product creations" 1000 POST "$BASE_URL/products" \
  '{"name":"Producto Test @","description":"Descripción del producto @"}'
echo -e "\n"

# Test concurrent price queries
echo "Testing concurrent price queries..."
load_test "concurrent price queries" 20000 GET "$PRICES_URL&date=2024-04-15"
echo -e "\n"

# Test concurrent price history requests
echo "Testing concurrent price history requests..."
load_test "concurrent price history requests" 15000 GET "$BASE_URL/products/prices/history?productId=$PRODUCT_ID"
echo -e "\n"

echo "Benchmark completed successfully!"