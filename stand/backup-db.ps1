# Резервная копия базы стенда.
#
#   .\backup-db.ps1              -> backup\espocrm-seed.sql.gz   (эталонный набор данных)
#   .\backup-db.ps1 -Name myrun  -> backup\espocrm-myrun.sql.gz  (произвольный снимок)
#
# Дамп "seed" лежит в репозитории: именно из него восстанавливаются тестовые данные
# на новой машине, без запуска seed-data.ps1 и без обращения к интернету.

param([string]$Name = 'seed')

$here    = $PSScriptRoot
$wslHere = '/mnt/' + $here.Substring(0, 1).ToLower() + ($here.Substring(2) -replace '\\', '/')

Write-Host ''
Write-Host "Снимаю дамп базы (имя: $Name)..." -ForegroundColor Cyan
wsl -u root -- bash "$wslHere/db.sh" backup $Name

Write-Host ''
try { Read-Host 'Нажми Enter, чтобы закрыть окно' | Out-Null } catch { }
