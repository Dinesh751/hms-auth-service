#!/bin/bash

# HMS Auth Service API Testing Script
BASE_URL="http://localhost:8080"

echo "🔥 HMS Auth Service API Testing"
echo "================================"

# 1. Health Check
echo "1️⃣ Testing Health Check..."
curl -s "$BASE_URL/actuator/health" | jq '.'
echo -e "\n"

# 2. Register Admin User
echo "2️⃣ Registering Admin User..."
ADMIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/v1/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@hospital.com",
    "password": "Admin123!",
    "firstName": "System",
    "lastName": "Administrator",
    "role": "ADMIN"
  }')
echo $ADMIN_RESPONSE | jq '.'
echo -e "\n"

# 3. Login Admin User
echo "3️⃣ Logging in Admin User..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/v1/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@hospital.com",
    "password": "Admin123!"
  }')
echo $LOGIN_RESPONSE | jq '.'

# Extract JWT token
JWT_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.accessToken')
echo "JWT Token: $JWT_TOKEN"
echo -e "\n"

# 4. Access Admin Dashboard
echo "4️⃣ Accessing Admin Dashboard..."
curl -s -X GET "$BASE_URL/api/admin/dashboard" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq '.'
echo -e "\n"

# 5. Register Doctor User
echo "5️⃣ Registering Doctor User..."
curl -s -X POST "$BASE_URL/api/auth/v1/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "doctor@hospital.com",
    "password": "Doctor123!",
    "firstName": "Dr. John",
    "lastName": "Smith",
    "role": "DOCTOR"
  }' | jq '.'
echo -e "\n"

# 6. Login Doctor and Test Dashboard
echo "6️⃣ Logging in Doctor User..."
DOCTOR_LOGIN=$(curl -s -X POST "$BASE_URL/api/auth/v1/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "doctor@hospital.com",
    "password": "Doctor123!"
  }')
DOCTOR_TOKEN=$(echo $DOCTOR_LOGIN | jq -r '.data.accessToken')

echo "7️⃣ Accessing Doctor Dashboard..."
curl -s -X GET "$BASE_URL/api/doctor/dashboard" \
  -H "Authorization: Bearer $DOCTOR_TOKEN" | jq '.'
echo -e "\n"

# 8. Test Profile Endpoint
echo "8️⃣ Accessing User Profile..."
curl -s -X GET "$BASE_URL/api/profile" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq '.'
echo -e "\n"

echo "✅ API Testing Complete!"
