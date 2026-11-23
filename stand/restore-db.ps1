# Восстановление базы стенда из дампа.
#
#   .\restore-db.ps1             -> из backup\espocrm-seed.sql.gz
#   .\restore-db.ps1 -Name myrun -> из backup\espocrm-myrun.sql.gz
#
# Используется в двух случаях:
#   1) развернул стенд на новой машине и хочешь получить готовые тестовые данные;
#   2) тесты намусорили в базе, и нужно быстро вернуть её в исходное состояние.
#
# ВНИМАНИЕ: восстановление перезаписывает текущие данные в базе espocrm.

param([string]$Name = 'seed')

$here    = $PSScriptRoot
$wslHere = '/mnt/' + $here.Substring(0, 1).ToLower() + ($here.Substring(2) -replace '\\', '/')

Write-Host ''
Write-Host "Восстанавливаю базу из дампа (имя: $Name)." -ForegroundColor Cyan
Write-Host 'Текущие данные в базе будут перезаписаны.' -ForegroundColor Yellow
wsl -u root -- bash "$wslHere/db.sh" restore $Name

Write-Host ''
try { Read-Host 'Нажми Enter, чтобы закрыть окно' | Out-Null } catch { }
