# Start Infrastructure Containers (PowerShell)
Write-Host "Starting CS Scholar AI Infrastructure (PostgreSQL with pgvector, Redis, MinIO)..." -ForegroundColor Cyan
docker compose up -d

Write-Host "Waiting for services to become healthy..." -ForegroundColor Yellow
Start-Sleep -Seconds 5
docker compose ps

