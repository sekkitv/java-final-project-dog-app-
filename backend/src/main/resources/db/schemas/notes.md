#To run the database on computer you need the following things
install postgres on your computer
run this command at terminal
psql -U postgres -p 5433 -d zuzdog -f backend\src\main\resources\db\schemas\init-postgresql.sql
it should initialize the dataset

