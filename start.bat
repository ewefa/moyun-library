@echo off
chcp 65001 >nul
cd /d %~dp0

if not exist "target\library-system-1.0.0.jar" (
    echo [ERROR] 未找到 target\library-system-1.0.0.jar
    echo 请先执行: mvn -DskipTests package
    pause
    exit /b 1
)

echo 正在启动 墨韵图书馆 ...
echo 访问地址: http://localhost:8080/
java -Dfile.encoding=UTF-8 -jar target\library-system-1.0.0.jar
pause
