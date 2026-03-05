#!/bin/bash

PID=$(lsof -ti:3000)
if [ -n "$PID" ]; then
  kill $PID
  echo "프론트엔드 종료 (PID: $PID)"
else
  echo "3000 포트에 실행 중인 프로세스 없음"
fi
