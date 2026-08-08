#This script will initalize the database with schemas and mock data.

$env:PGPASSWORD = "postgres"
psql -U postgres -p 5433 -c "CREATE DATABASE zuzdog;" 2>$null
psql -U postgres -p 5433 -d zuzdog -c "TRUNCATE messages, matches, swipes, dogs, users RESTART IDENTITY CASCADE;"
psql -U postgres -p 5433 -d zuzdog -f backend\zuzdog-backend\src\main\resources\db\schemas\init-postgresql.sql
psql -U postgres -p 5433 -d zuzdog -f backend\zuzdog-backend\src\main\resources\db\schemas\mock-data.sql
psql -U postgres -p 5433 -d zuzdog -c "\dt"
psql -U postgres -p 5433 -d zuzdog -c "SELECT * FROM users;"
psql -U postgres -p 5433 -d zuzdog -c "SELECT * FROM dogs;"
psql -U postgres -p 5433 -d zuzdog -c "SELECT * FROM swipes;"
psql -U postgres -p 5433 -d zuzdog -c "SELECT * FROM matches;"
psql -U postgres -p 5433 -d zuzdog -c "SELECT * FROM messages ORDER BY message_id ASC LIMIT 5;"

# -----------------------------------------------------------------------------
# Step 1.6 DoD checks for the notifications table.
# Runs the same SQL the NotificationDao runs (insert / findForUser / countUnread / markAllRead)
# so we can see the full lifecycle at the psql level after loading the schema + mock data.
# -----------------------------------------------------------------------------
Write-Host "================ Step 1.6: notifications DoD checks ================"
# 1. show the seeded notifications for user 1, ordered newest first (findForUser behavior)
psql -U postgres -p 5433 -d zuzdog -c "SELECT notification_id, user_id, type, reference_id, title, created_at, read_at FROM notifications WHERE user_id = 1 ORDER BY created_at DESC;"
# 2. countUnread for user 1 (mock seeds 2 unread + 1 read, so this should print 2)
psql -U postgres -p 5433 -d zuzdog -c "SELECT COUNT(*) AS unread_for_user_1 FROM notifications WHERE user_id = 1 AND read_at IS NULL;"
# 3. insert a fresh unread notification (insert behavior) for user 1
psql -U postgres -p 5433 -d zuzdog -c "INSERT INTO notifications (user_id, type, reference_id, title, body) VALUES (1, 'MESSAGE', 2, 'DoD probe', 'checking insert + unread');"
# 4. recount — should now be 3
psql -U postgres -p 5433 -d zuzdog -c "SELECT COUNT(*) AS unread_for_user_1_after_insert FROM notifications WHERE user_id = 1 AND read_at IS NULL;"
# 5. markAllRead — flip every unread notification for user 1, returns the affected row count
psql -U postgres -p 5433 -d zuzdog -c "UPDATE notifications SET read_at = NOW() WHERE user_id = 1 AND read_at IS NULL RETURNING notification_id;"
# 6. recount — should now be 0
psql -U postgres -p 5433 -d zuzdog -c "SELECT COUNT(*) AS unread_for_user_1_after_markallread FROM notifications WHERE user_id = 1 AND read_at IS NULL;"
# 7. confirm findForUser still returns rows (only read_at changed, rows must persist) — newest first
psql -U postgres -p 5433 -d zuzdog -c "SELECT notification_id, type, title, read_at IS NOT NULL AS is_read FROM notifications WHERE user_id = 1 ORDER BY created_at DESC;"
Write-Host "====================================================================="







