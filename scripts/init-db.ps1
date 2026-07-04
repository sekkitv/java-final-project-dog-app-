#This script will initalize the database with schemas and mock data.

$env:PGPASSWORD = "postgres"
psql -U postgres -p 5433 -c "CREATE DATABASE zuzdog;" 2>$null
psql -U postgres -p 5433 -d zuzdog -c "TRUNCATE messages, matches, swipes, dogs, users RESTART IDENTITY CASCADE;"
psql -U postgres -p 5433 -d zuzdog -f backend\src\main\resources\db\schemas\init-postgresql.sql
psql -U postgres -p 5433 -d zuzdog -f backend\src\main\resources\db\schemas\mock-data.sql
psql -U postgres -p 5433 -d zuzdog -c "\dt"
psql -U postgres -p 5433 -d zuzdog -c "SELECT * FROM users;"
psql -U postgres -p 5433 -d zuzdog -c "SELECT * FROM dogs;"
psql -U postgres -p 5433 -d zuzdog -c "SELECT * FROM swipes;"
psql -U postgres -p 5433 -d zuzdog -c "SELECT * FROM matches;"
psql -U postgres -p 5433 -d zuzdog -c "SELECT * FROM messages ORDER BY message_id ASC LIMIT 5;"







