@echo off
echo Testing bgai-service distributed transaction functionality...

echo.
echo 1. Testing basic chat request...
curl -X POST http://localhost:8080/api/chat/completions ^
  -H "Content-Type: application/json" ^
  -d "{\"messages\":[{\"role\":\"user\",\"content\":\"Hello, this is a test message\"}],\"model\":\"deepseek-chat\",\"userId\":\"test-user-001\",\"multiTurn\":false}"

echo.
echo.
echo 2. Testing multi-turn conversation...
curl -X POST http://localhost:8080/api/chat/completions ^
  -H "Content-Type: application/json" ^
  -d "{\"messages\":[{\"role\":\"user\",\"content\":\"What is the weather like today?\"}],\"model\":\"deepseek-chat\",\"userId\":\"test-user-002\",\"multiTurn\":true}"

echo.
echo.
echo 3. Testing transaction status check...
curl -X GET http://localhost:8080/api/transaction/status/test-user-001

echo.
echo.
echo 4. Testing error handling with invalid request...
curl -X POST http://localhost:8080/api/chat/completions ^
  -H "Content-Type: application/json" ^
  -d "{\"messages\":[],\"model\":\"deepseek-chat\",\"userId\":\"test-user-003\",\"multiTurn\":false}"

echo.
echo.
echo Test completed. Check the application logs for transaction details.
pause
