#!/usr/bin/env bash
# Резервное копирование и восстановление базы стенда.
# Вызывается из backup-db.ps1 / restore-db.ps1, но можно и напрямую:
#   bash db.sh backup seed
#   bash db.sh restore seed
#
# Пароль root берётся из переменной окружения ВНУТРИ контейнера,
# поэтому он нигде не светится в командной строке и в истории команд.

cd "$(dirname "$0")" || exit 1
service docker start >/dev/null 2>&1

NAME="${2:-seed}"
FILE="backup/espocrm-$NAME.sql.gz"
ERR=$(mktemp)

case "$1" in
  backup)
    mkdir -p backup
    docker exec espocrm-mysql sh -c \
      'exec mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers --no-tablespaces espocrm' \
      2>"$ERR" | gzip > "$FILE"
    # предупреждение про пароль в командной строке - штатное, его отфильтровываем
    real=$(grep -vi "Using a password" "$ERR")
    if [ -n "$real" ]; then echo "ОШИБКА: $real"; rm -f "$ERR"; exit 1; fi
    size=$(stat -c %s "$FILE" 2>/dev/null)
    if [ "${size:-0}" -lt 1000 ]; then echo "ОШИБКА: дамп подозрительно мал ($size байт)"; rm -f "$ERR"; exit 1; fi
    echo "Дамп сохранён: $FILE ($size байт)"
    echo "Таблиц в дампе: $(zcat "$FILE" | grep -c '^CREATE TABLE')"
    ;;
  restore)
    if [ ! -f "$FILE" ]; then echo "ОШИБКА: файл $FILE не найден"; rm -f "$ERR"; exit 1; fi
    zcat "$FILE" | docker exec -i espocrm-mysql sh -c \
      'exec mysql -u root -p"$MYSQL_ROOT_PASSWORD" espocrm' 2>"$ERR"
    real=$(grep -vi "Using a password" "$ERR")
    if [ -n "$real" ]; then echo "ОШИБКА: $real"; rm -f "$ERR"; exit 1; fi
    echo "База восстановлена из $FILE"
    ;;
  *)
    echo "использование: db.sh {backup|restore} [имя-дампа, по умолчанию seed]"
    rm -f "$ERR"; exit 1 ;;
esac
rm -f "$ERR"
