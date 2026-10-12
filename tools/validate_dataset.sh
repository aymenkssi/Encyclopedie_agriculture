#!/usr/bin/env sh
# Validates the seed dataset (schema, references, honesty rules) and runs the engine tests.
set -e
cd "$(dirname "$0")/.."
./gradlew :core:test
