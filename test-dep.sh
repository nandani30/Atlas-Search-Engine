#!/bin/bash
check_dep() {
  echo "Checking $1:$2:$3"
  mvn dependency:get -Dartifact=$1:$2:$3 -DremoteRepositories=central::default::https://repo.maven.apache.org/maven2 -q -B 2>/dev/null
  if [ $? -eq 0 ]; then
    echo "Found: $1:$2:$3"
  fi
}
check_dep tech.libsql jdbc 0.0.1
check_dep tech.libsql libsql-jdbc 0.0.1
check_dep tech.turso jdbc 0.0.1
check_dep tech.turso libsql-jdbc 0.0.1
check_dep org.libsql jdbc 0.0.1
check_dep org.libsql libsql-jdbc 0.0.1
check_dep io.github.libsql jdbc 0.0.1
check_dep io.github.libsql libsql-jdbc 0.0.1
check_dep org.xerial sqlite-jdbc 3.45.2.0
