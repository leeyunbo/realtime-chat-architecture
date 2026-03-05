#!/bin/bash

# 인프라(Postgres, Redis, MinIO) 실행 및 준비 대기
docker compose up -d
echo "인프라 시작 대기 중..."
sleep 3

# MinIO 버킷 생성 (없을 때만)
docker exec chat-minio mc alias set local http://localhost:9000 minioadmin minioadmin 2>/dev/null
docker exec chat-minio mc mb --ignore-existing local/chat-files 2>/dev/null

# Spring Boot 백그라운드 실행
echo "Spring Boot 시작..."
nohup ./gradlew bootRun > backend.log 2>&1 &
echo "백엔드 PID: $! (로그: backend.log)"
