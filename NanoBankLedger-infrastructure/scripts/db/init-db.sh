#!/bin/bash
set -e

echo "⏳ Esperando a que PostgreSQL esté disponible..."
until pg_isready -h localhost -p 5432 -U nanobank; do
  sleep 1
done

echo "✅ PostgreSQL disponible. Ejecutando migraciones Flyway..."
cd /home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/NanoBankLedger-backend
./gradlew flywayMigrate -i

echo "✅ Migraciones completadas. Verificando tablas..."
PGPASSWORD=nanobank_dev psql -h localhost -U nanobank -d nanobank_ledger -c "\dt"

echo "✅ Base de datos inicializada correctamente."
