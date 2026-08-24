#!/bin/sh
# Read-only smoke validation for a deployed Repository AIS instance.
# Usage: sh validate-repository-server.sh https://host.example/ais

set -eu

BASE=${1:-}
if [ -z "$BASE" ]; then
  echo "Usage: $0 https://host.example/ais" >&2
  exit 2
fi
BASE=${BASE%/}
TMP_ROOT=${TMPDIR:-/tmp}/ais-repository-validate-$$
mkdir -p "$TMP_ROOT"
trap 'rm -rf "$TMP_ROOT"' EXIT HUP INT TERM

PASS=0
FAIL=0

check_http() {
  name=$1
  url=$2
  output=$3
  code=$(curl -L -sS -o "$output" -w '%{http_code}' "$url" || true)
  case "$code" in
    2??) echo "PASS $name ($code)"; PASS=$((PASS + 1)) ;;
    *) echo "FAIL $name ($code): $url" >&2; FAIL=$((FAIL + 1)); return 1 ;;
  esac
}

check_xml() {
  name=$1
  file=$2
  if command -v xmllint >/dev/null 2>&1; then
    if xmllint --noout "$file" >/dev/null 2>&1; then
      echo "PASS $name XML well-formed"
      PASS=$((PASS + 1))
    else
      echo "FAIL $name XML is not well-formed" >&2
      FAIL=$((FAIL + 1))
    fi
  else
    echo "SKIP $name XML parser (xmllint is not installed)"
  fi
}

check_contains() {
  name=$1
  file=$2
  pattern=$3
  if grep -q "$pattern" "$file"; then
    echo "PASS $name"
    PASS=$((PASS + 1))
  else
    echo "FAIL $name: pattern '$pattern' not found" >&2
    FAIL=$((FAIL + 1))
  fi
}

check_http "Repository homepage" "$BASE/repository" "$TMP_ROOT/home.html" || true
check_http "robots.txt" "$BASE/robots.txt" "$TMP_ROOT/robots.txt" || true
check_http "sitemap.xml" "$BASE/sitemap.xml" "$TMP_ROOT/sitemap.xml" || true
check_xml "sitemap.xml" "$TMP_ROOT/sitemap.xml"

for verb in Identify ListMetadataFormats ListSets; do
  file="$TMP_ROOT/oai-$verb.xml"
  check_http "OAI $verb" "$BASE/oai?verb=$verb" "$file" || true
  check_xml "OAI $verb" "$file"
  check_contains "OAI $verb response" "$file" "<$verb>"
done

LIST="$TMP_ROOT/oai-ListIdentifiers.xml"
check_http "OAI ListIdentifiers" "$BASE/oai?verb=ListIdentifiers&metadataPrefix=oai_dc" "$LIST" || true
check_xml "OAI ListIdentifiers" "$LIST"
check_contains "OAI ListIdentifiers response" "$LIST" '<ListIdentifiers>'

IDENTIFIER=$(sed -n 's:.*<identifier>\([^<]*\)</identifier>.*:\1:p' "$LIST" | head -n 1)
if [ -n "$IDENTIFIER" ]; then
  ENCODED=$(printf '%s' "$IDENTIFIER" | sed 's/%/%25/g; s/:/%3A/g; s/ /%20/g')
  RECORD="$TMP_ROOT/oai-GetRecord.xml"
  check_http "OAI GetRecord" "$BASE/oai?verb=GetRecord&metadataPrefix=oai_dc&identifier=$ENCODED" "$RECORD" || true
  check_xml "OAI GetRecord" "$RECORD"
  check_contains "OAI GetRecord response" "$RECORD" '<GetRecord>'
else
  echo "SKIP OAI GetRecord (ListIdentifiers returned no identifier)"
fi

BAD="$TMP_ROOT/oai-bad-argument.xml"
check_http "OAI badArgument transport" "$BASE/oai?verb=Identify&metadataPrefix=oai_dc" "$BAD" || true
check_contains "OAI rejects illegal arguments" "$BAD" 'error code="badArgument"'

echo "Summary: PASS=$PASS FAIL=$FAIL"
if [ "$FAIL" -ne 0 ]; then exit 1; fi

