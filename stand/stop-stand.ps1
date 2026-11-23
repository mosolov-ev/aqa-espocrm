# Остановка локального стенда EspoCRM. Данные сохраняются.
#
# Нужен не для выключения компьютера (при выключении всё гаснет само),
# а чтобы освободить ~1.8 ГБ оперативной памяти, когда стенд больше не нужен,
# а работа за компьютером продолжается.

$here    = $PSScriptRoot
$wslHere = '/mnt/' + $here.Substring(0, 1).ToLower() + ($here.Substring(2) -replace '\\', '/')
$sh      = "$wslHere/stand.sh"

Write-Host ''
Write-Host 'Останавливаю контейнеры...' -ForegroundColor Cyan
wsl -u root -- bash $sh down | Out-Null

# wsl --shutdown гасит виртуалку целиком, вместе с фоновым якорем из start-stand.ps1.
Write-Host 'Гашу виртуалку WSL, чтобы освободить оперативную память...' -ForegroundColor Cyan
wsl --shutdown
Start-Sleep -Seconds 3

$vm = Get-Process vmmemWSL, vmmem -ErrorAction SilentlyContinue
Write-Host ''
if ($vm) {
    Write-Host '  Контейнеры остановлены, но виртуалка WSL ещё не погасла.' -ForegroundColor Yellow
    Write-Host '  Обычно она завершается в течение нескольких секунд.'
} else {
    Write-Host '  Стенд остановлен, память освобождена. Данные сохранены.' -ForegroundColor Green
}

Write-Host ''
try { Read-Host 'Нажми Enter, чтобы закрыть окно' | Out-Null } catch { }
