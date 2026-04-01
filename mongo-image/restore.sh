#!/bin/bash
set -e

echo "Restoring cookigramdb from dump..."

mongorestore --drop --nsInclude="cookigramdb.*" /dump

echo "Restore completed."