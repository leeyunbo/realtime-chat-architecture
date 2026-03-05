#!/bin/bash

PID=$(lsof -ti:8085)
if [ -n "$PID" ]; then
  kill $PID
  echo "Spring Boot 종료 (PID: $PID)"
else
  echo "8085 포트에 실행 중인 프로세스 없음"
fi

docker compose down
echo "인프라 종료 완료"
