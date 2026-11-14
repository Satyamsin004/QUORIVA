#!/usr/bin/env bash
# Start CS Scholar AI Infrastructure Services
set -e

echo "Starting CS Scholar AI Infrastructure (PostgreSQL with pgvector, Redis, MinIO)..."
docker compose up -d

echo "Waiting for services to become healthy..."
sleep 5
docker compose ps

