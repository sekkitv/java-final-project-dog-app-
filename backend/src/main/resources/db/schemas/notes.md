# Database setup

## One-time setup
1. Install PostgreSQL (the script assumes port 5433 and user `postgres` with password `postgres`).
2. You need instance of postgres 18.

## To see the tables
1. run the script .scripts/reset-db.ps1 or see the commands inside the script and put into cmd.

## Reset and seed the database
From the project root:
```powershell
.scripts/reset-db.ps1
```

This script will:
1. Run `init-postgresql.sql` (5 tables + indexes)
2. Run `mock-data.sql` (10 Israeli users, dogs, swipes, matches, messages)

