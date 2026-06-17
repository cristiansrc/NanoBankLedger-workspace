#!/bin/bash
set -e

echo "⚠️  ATENCIÓN: Esto eliminará TODOS los datos de la base de datos."
read -p "¿Continuar? (s/N): " confirm

if [ "$confirm" != "s" ] && [ "$confirm" != "S" ]; then
  echo "Operación cancelada."
  exit 0
fi

echo "🔄 Eliminando base de datos..."
PGPASSWORD=nanobank_dev psql -h localhost -U nanobank -d postgres -c "DROP DATABASE IF EXISTS nanobank_ledger;"
PGPASSWORD=nanobank_dev psql -h localhost -U nanobank -d postgres -c "CREATE DATABASE nanobank_ledger OWNER nanobank;"

echo "✅ Base de datos recreada. Ejecutando migraciones..."
cd /home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/NanoBankLedger-backend
./gradlew flywayMigrate -i

echo "✅ Base de datos reinicializada correctamente."
