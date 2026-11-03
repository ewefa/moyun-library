#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

if [ ! -f target/library-system-1.0.0.jar ]; then
  echo "[ERROR] 未找到 target/library-system-1.0.0.jar，请先执行: mvn -DskipTests package"
  exit 1
fi

echo "正在启动 墨韵图书馆 ... 访问地址: http://localhost:8080/"
exec java -Dfile.encoding=UTF-8 -jar target/library-system-1.0.0.jar
