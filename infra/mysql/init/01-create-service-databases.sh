#!/bin/bash
set -eu

create_service_database() {
    local service="$1"
    local password="$2"
    local name="sqljudge_${service}"

    mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" <<-SQL
        CREATE DATABASE IF NOT EXISTS \`${name}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
        CREATE USER IF NOT EXISTS '${name}'@'%' IDENTIFIED BY '${password}';
        GRANT ALL PRIVILEGES ON \`${name}\`.* TO '${name}'@'%';
SQL
}

create_service_database auth "${MYSQL_AUTH_PASSWORD}"
create_service_database problem "${MYSQL_PROBLEM_PASSWORD}"

mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" -e "FLUSH PRIVILEGES;"
