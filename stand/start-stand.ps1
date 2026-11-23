# Запуск локального стенда EspoCRM.
#
# Запускать можно двойным кликом (правой кнопкой -> "Выполнить с помощью PowerShell")
# или из консоли, находясь в этой папке:  .\start-stand.ps1

$ErrorActionPreference = 'Continue'

# Путь к этой папке в формате WSL:  C:\Users\Ivan\proj  ->  /mnt/c/Users/Ivan/proj
# Вычисляется на лету, поэтому стенд можно перенести в любую папку или на другой диск,
# и скрипт продолжит работать без правок.
$here    = $PSScriptRoot
$wslHere = '/mnt/' + $here.Substring(0, 1).ToLower() + ($here.Substring(2) -replace '\\', '/')
$sh      = "$wslHere/stand.sh"

Write-Host ''
Write-Host 'Поднимаю Docker и контейнеры в WSL...' -ForegroundColor Cyan
wsl -u root -- bash $sh up | Out-Null

# Якорь.
# WSL гасит виртуалку, как только не остаётся ни одной активной сессии,
# и вместе с ней умирает стенд. Этот фоновый процесс держит виртуалку живой,
# пока не выполнишь stop-stand.ps1. Без него стенд отключится сразу после
# закрытия этого окна.
$anchor = wsl -u root -- bash -lc "pgrep -f '[s]leep 99999999' >/dev/null && echo yes || echo no"
if ($anchor -notmatch 'yes') {
    Start-Process -FilePath 'wsl.exe' -ArgumentList '-u', 'root', '--', 'sleep', '99999999' -WindowStyle Hidden
    Write-Host 'Запущен фоновый якорь, удерживающий виртуалку WSL.' -ForegroundColor DarkGray
} else {
    Write-Host 'Якорь уже работает.' -ForegroundColor DarkGray
}

Write-Host 'Жду готовности EspoCRM...' -ForegroundColor Cyan
$ok = $false
for ($i = 0; $i -lt 60; $i++) {
    try {
        $r = Invoke-WebRequest -Uri 'http://localhost:8080/' -UseBasicParsing -TimeoutSec 5
        if ($r.StatusCode -eq 200) { $ok = $true; break }
    } catch {
        Start-Sleep -Seconds 3
    }
}

Write-Host ''
if ($ok) {
    Write-Host '  Стенд готов: http://localhost:8080' -ForegroundColor Green
    Write-Host '  Логин: admin'
    Write-Host '  Пароль: в файле .env рядом с этим скриптом (параметр ESPOCRM_ADMIN_PASSWORD)'
    Write-Host ''
    Write-Host '  Окно можно закрывать, стенд продолжит работать.' -ForegroundColor DarkGray
    Write-Host '  Чтобы остановить стенд и освободить память, запусти stop-stand.ps1' -ForegroundColor DarkGray
} else {
    Write-Host '  Не дождался готовности стенда.' -ForegroundColor Red
    Write-Host '  Посмотреть логи приложения:' -ForegroundColor Red
    Write-Host "  wsl -u root -- bash $sh applog"
}

Write-Host ''
try { Read-Host 'Нажми Enter, чтобы закрыть окно' | Out-Null } catch { }
