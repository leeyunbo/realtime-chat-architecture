#!/bin/bash

cd frontend

# 의존성 설치 (node_modules 없을 때만)
if [ ! -d "node_modules" ]; then
  npm install
fi

# Vite 개발 서버 백그라운드 실행
nohup npm run dev > ../frontend.log 2>&1 &
echo "프론트엔드 PID: $! (로그: frontend.log)"
