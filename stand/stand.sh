#!/usr/bin/env bash
# Управление стендом изнутри WSL. Вызывается из start-stand.ps1 / stop-stand.ps1.
cd "$(dirname "$0")" || exit 1
service docker start >/dev/null 2>&1
case "$1" in
  up)     docker compose up -d ;;
  down)   docker compose stop ;;
  status) docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" ;;
  logs)   docker compose logs --tail=40 espocrm ;;
  # Лог самого приложения EspoCRM - здесь видны причины отказов API
  # (валидация, дубли, кодировка). Маска *.log вместо вычисления даты:
  # лог именуется по дате ВНУТРИ контейнера, она может не совпасть с датой Windows.
  applog) docker exec espocrm bash -c 'tail -n 40 /var/www/html/data/logs/*.log' ;;
  *)      echo "использование: stand.sh {up|down|status|logs|applog}"; exit 1 ;;
esac
