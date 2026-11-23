# Наполнение локального стенда EspoCRM тестовыми данными через REST API.
#
# Запуск:  .\seed-data.ps1
# Повтор:  .\seed-data.ps1 -Force   (добавит данные ещё раз, даже если они уже есть)
#
# Данные подобраны в домене "металлообработка / машиностроение", чтобы полигон был
# похож на рабочий проект. Создаются: компании, контактные лица, сделки, лиды, задачи.

param([switch]$Force, [switch]$Clean)

$ErrorActionPreference = 'Stop'
$dir = Split-Path -Parent $MyInvocation.MyCommand.Path

# ---------- учётные данные из .env ----------
$envFile = Join-Path $dir '.env'
if (-not (Test-Path $envFile)) { throw "Не найден файл .env рядом со скриптом" }
$cfg = @{}
foreach ($line in Get-Content -LiteralPath $envFile -Encoding UTF8) {
    if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$') { $cfg[$matches[1]] = $matches[2] }
}
$port = $cfg['ESPOCRM_PORT']; if (-not $port) { $port = '8080' }
$base = "http://localhost:$port/api/v1"
$pair = $cfg['ESPOCRM_ADMIN_USERNAME'] + ':' + $cfg['ESPOCRM_ADMIN_PASSWORD']
$headers = @{
    Authorization = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($pair))
    # EspoCRM отдаёт 409 Conflict (X-Status-Reason: duplicate), если запись с таким же
    # именем уже есть. Для тестовых данных дубли допустимы — отключаем проверку.
    'X-Skip-Duplicate-Check' = 'true'
}

function Invoke-Espo {
    param([string]$Method, [string]$Path, $Body)
    $uri = "$base/$Path"
    if ($null -ne $Body) {
        if ($Method -eq 'POST' -and $script:adminId -and -not $Body.ContainsKey('assignedUserId')) {
            $Body['assignedUserId'] = $script:adminId
        }
        # PowerShell 5.1 кодирует строковое тело не в UTF-8, и кириллица приезжает битой
        # ("Malformed UTF-8 characters" на стороне EspoCRM). Отправляем тело байтами.
        $json = [System.Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 5 -Compress))
        return Invoke-RestMethod -Uri $uri -Method $Method -Headers $headers -Body $json `
                                 -ContentType 'application/json; charset=utf-8' -TimeoutSec 30
    }
    return Invoke-RestMethod -Uri $uri -Method $Method -Headers $headers -TimeoutSec 30
}

Write-Host ''
Write-Host 'Проверяю соединение со стендом...' -ForegroundColor Cyan
try {
    $me = Invoke-Espo GET 'App/user'
    Write-Host ("  вошли как: " + $me.user.userName + " (" + $me.user.type + ")") -ForegroundColor DarkGray
    $script:adminId = $me.user.id
} catch {
    Write-Host '  Стенд не отвечает. Запусти сначала start-stand.ps1' -ForegroundColor Red
    try { Read-Host 'Нажми Enter, чтобы закрыть окно' | Out-Null } catch { }
    exit 1
}

# ---------- валюта ----------
# По умолчанию в EspoCRM разрешён только USD, и сделки в рублях не создаются:
# валидация падает с "field: amount, type: validCurrency". Добавляем рубль.
try {
    $st = Invoke-Espo GET 'Settings'
    if ($st.currencyList -notcontains 'RUB') {
        Invoke-Espo PUT 'Settings' @{
            baseCurrency    = 'RUB'
            defaultCurrency = 'RUB'
            currencyList    = @('RUB', 'USD', 'EUR')
            currencyRates   = @{ USD = 95.0; EUR = 105.0 }
        } | Out-Null
        Write-Host '  добавлена валюта RUB и сделана основной' -ForegroundColor DarkGray
    }
} catch {
    Write-Host ('  не удалось настроить валюту: ' + $_.Exception.Message) -ForegroundColor Yellow
}

# ---------- сброс данных (ключ -Clean) ----------
if ($Clean) {
    Write-Host ''
    Write-Host 'Удаляю ранее созданные данные...' -ForegroundColor Cyan
    foreach ($e in @('Task', 'Opportunity', 'Contact', 'Lead', 'Account')) {
        $removed = 0
        for ($pass = 0; $pass -lt 20; $pass++) {
            $r = Invoke-Espo GET "$e`?maxSize=200"
            if ($r.list.Count -eq 0) { break }
            foreach ($row in $r.list) {
                try { Invoke-Espo DELETE ("$e/" + $row.id) | Out-Null; $removed++ } catch { }
            }
        }
        Write-Host ("  {0,-12} удалено: {1}" -f $e, $removed) -ForegroundColor DarkGray
    }
}

# ---------- защита от повторного наполнения ----------
$existing = Invoke-Espo GET 'Account?maxSize=1'
if ($existing.total -gt 0 -and -not $Force -and -not $Clean) {
    Write-Host ''
    Write-Host ("  В системе уже есть компании: " + $existing.total + " шт.") -ForegroundColor Yellow
    Write-Host '  Повторное наполнение пропущено, чтобы не плодить дубли.'
    Write-Host '  Если данные нужны всё равно, запусти:  .\seed-data.ps1 -Force'
    try { Read-Host 'Нажми Enter, чтобы закрыть окно' | Out-Null } catch { }
    exit 0
}

# ---------- справочные наборы ----------
$companies = @(
    @{ n = 'ООО «Стальной Двор»';            city = 'Москва';           ind = 'Manufacturing'; type = 'Customer' },
    @{ n = 'АО «Метиз-Трейд»';               city = 'Санкт-Петербург';  ind = 'Wholesale';     type = 'Customer' },
    @{ n = 'ООО «УралПрокат»';               city = 'Екатеринбург';     ind = 'Manufacturing'; type = 'Partner'  },
    @{ n = 'ЗАО «ТрубМеталл»';               city = 'Челябинск';        ind = 'Manufacturing'; type = 'Customer' },
    @{ n = 'ООО «Северный Профиль»';         city = 'Череповец';        ind = 'Construction';  type = 'Customer' },
    @{ n = 'АО «Волга-Сталь»';               city = 'Нижний Новгород';  ind = 'Manufacturing'; type = 'Reseller' },
    @{ n = 'ООО «ТехноКрепёж»';              city = 'Казань';           ind = 'Manufacturing'; type = 'Customer' },
    @{ n = 'ООО «Сибирский Металлоцентр»';   city = 'Новосибирск';      ind = 'Wholesale';     type = 'Partner'  },
    @{ n = 'АО «ДонМашДеталь»';              city = 'Ростов-на-Дону';   ind = 'Manufacturing'; type = 'Customer' },
    @{ n = 'ООО «Балтийская Арматура»';      city = 'Калининград';      ind = 'Construction';  type = 'Customer' },
    @{ n = 'ООО «ПромЛит»';                  city = 'Пермь';            ind = 'Manufacturing'; type = 'Customer' },
    @{ n = 'АО «Кузнецкие Поковки»';         city = 'Новокузнецк';      ind = 'Manufacturing'; type = 'Partner'  },
    @{ n = 'ООО «ЭнергоСвар»';               city = 'Самара';           ind = 'Energy';        type = 'Customer' },
    @{ n = 'ООО «ГрадСтройМеталл»';          city = 'Краснодар';        ind = 'Construction';  type = 'Customer' },
    @{ n = 'АО «Точмаш-Сервис»';             city = 'Тула';             ind = 'Manufacturing'; type = 'Customer' }
)

# Массивы параллельны по роду: индексы 0-9 мужские, 10-19 женские,
# поэтому имя и фамилия берутся по одному индексу $iName.
$lastNames  = @('Кузнецов','Соколов','Белов','Орлов','Зайцев','Волков','Лебедев','Медведев','Поляков','Громов',
                'Смирнова','Иванова','Карпова','Фомина','Ларина','Егорова','Жукова','Панова','Тихонова','Савина')
$firstNames = @('Андрей','Сергей','Дмитрий','Алексей','Михаил','Игорь','Павел','Роман','Никита','Олег',
                'Елена','Ольга','Марина','Анна','Татьяна','Ирина','Светлана','Юлия','Наталья','Ксения')
$titles     = @('Генеральный директор','Главный инженер','Начальник отдела снабжения','Менеджер по закупкам',
                'Технический директор','Руководитель производства','Специалист по тендерам','Коммерческий директор')

$oppNames = @('Поставка листового проката','Партия трубы бесшовной','Изготовление металлоконструкций',
              'Поставка арматуры А500С','Лазерная резка по чертежам','Поковки под механообработку',
              'Поставка швеллера и уголка','Изготовление сварных балок','Партия метизов и крепежа',
              'Оцинкованный профнастил')
$stages   = @('Prospecting','Qualification','Proposal','Negotiation','Closed Won','Closed Lost')
$leadStatuses = @('New','Assigned','In Process','Converted','Recycled','Dead')
$leadSources  = @('Call','Email','Web Site','Partner','Existing Customer','Other')

$rnd = New-Object System.Random 20261021
$created = @{ Account = 0; Contact = 0; Opportunity = 0; Lead = 0; Task = 0 }
$failed = 0

# ---------- компании ----------
Write-Host ''
Write-Host 'Создаю компании...' -ForegroundColor Cyan
$accountIds = @()
$i = 0
foreach ($c in $companies) {
    $i++
    $slug = 'company{0:d2}' -f $i
    try {
        $a = Invoke-Espo POST 'Account' @{
            name               = $c.n
            type               = $c.type
            industry           = $c.ind
            website            = "https://$slug.example.ru"
            emailAddress       = "info@$slug.example.ru"
            phoneNumber        = '+7 (495) 100-{0:d2}-{1:d2}' -f $i, ($i * 3)
            billingAddressCity = $c.city
            billingAddressCountry = 'Россия'
            description        = 'Тестовая компания для автотестов. ИНН 77{0:d8}.' -f ($i * 1234567)
        }
        $accountIds += @{ id = $a.id; name = $c.n }
        $created.Account++
    } catch { $failed++; Write-Host ("  не удалось: " + $c.n + " -> " + $_.Exception.Message) -ForegroundColor DarkYellow }
}
Write-Host ("  создано компаний: " + $created.Account) -ForegroundColor DarkGray

# ---------- контактные лица: по 2 на компанию ----------
Write-Host 'Создаю контактные лица...' -ForegroundColor Cyan
$contactIds = @()
$k = 0
foreach ($acc in $accountIds) {
    for ($j = 0; $j -lt 2; $j++) {
        $k++
        $iName = $rnd.Next(0, $lastNames.Count); $ln = $lastNames[$iName]
        $fn = $firstNames[$iName]
        try {
            $ct = Invoke-Espo POST 'Contact' @{
                firstName    = $fn
                lastName     = $ln
                accountId    = $acc.id
                title        = $titles[$rnd.Next(0, $titles.Count)]
                emailAddress = 'contact{0:d2}@example.ru' -f $k
                phoneNumber  = '+7 (900) 2{0:d2}-{1:d2}-{2:d2}' -f $k, ($k + 7), ($k + 13)
                description  = 'Контактное лицо компании ' + $acc.name
            }
            $contactIds += $ct.id
            $created.Contact++
        } catch { $failed++ }
    }
}
Write-Host ("  создано контактов: " + $created.Contact) -ForegroundColor DarkGray

# ---------- сделки ----------
Write-Host 'Создаю сделки...' -ForegroundColor Cyan
$oppIds = @()
for ($i = 1; $i -le 24; $i++) {
    $acc = $accountIds[$rnd.Next(0, $accountIds.Count)]
    $stage = $stages[$rnd.Next(0, $stages.Count)]
    $prob = 10 * $rnd.Next(1, 10)
    if ($stage -eq 'Closed Won')  { $prob = 100 }
    if ($stage -eq 'Closed Lost') { $prob = 0 }
    try {
        $o = Invoke-Espo POST 'Opportunity' @{
            name           = ($oppNames[$rnd.Next(0, $oppNames.Count)] + ' №' + (1000 + $i))
            accountId      = $acc.id
            stage          = $stage
            amount         = [math]::Round($rnd.Next(150, 8000) * 1000)
            amountCurrency = 'RUB'
            closeDate      = (Get-Date).AddDays($rnd.Next(-45, 120)).ToString('yyyy-MM-dd')
            probability    = $prob
            description    = 'Тестовая сделка для автотестов.'
        }
        $oppIds += $o.id
        $created.Opportunity++
    } catch { $failed++ }
}
Write-Host ("  создано сделок: " + $created.Opportunity) -ForegroundColor DarkGray

# ---------- лиды ----------
Write-Host 'Создаю лиды...' -ForegroundColor Cyan
for ($i = 1; $i -le 18; $i++) {
    $iName = $rnd.Next(0, $lastNames.Count); $ln = $lastNames[$iName]
    $fn = $firstNames[$iName]
    try {
        Invoke-Espo POST 'Lead' @{
            firstName    = $fn
            lastName     = $ln
            accountName  = 'ООО «Потенциальный клиент ' + $i + '»'
            status       = $leadStatuses[$rnd.Next(0, $leadStatuses.Count)]
            source       = $leadSources[$rnd.Next(0, $leadSources.Count)]
            title        = $titles[$rnd.Next(0, $titles.Count)]
            emailAddress = 'lead{0:d2}@example.ru' -f $i
            phoneNumber  = '+7 (901) 3{0:d2}-{1:d2}-{2:d2}' -f $i, ($i + 11), ($i + 21)
            description  = 'Тестовый лид для автотестов.'
        } | Out-Null
        $created.Lead++
    } catch { $failed++ }
}
Write-Host ("  создано лидов: " + $created.Lead) -ForegroundColor DarkGray

# ---------- задачи ----------
Write-Host 'Создаю задачи...' -ForegroundColor Cyan
$taskNames = @('Согласовать спецификацию','Выставить счёт','Подготовить коммерческое предложение',
               'Уточнить сроки поставки','Запросить чертежи','Проверить остатки на складе',
               'Созвониться с заказчиком','Отправить образцы','Подписать договор','Проконтролировать отгрузку')
$taskStatuses = @('Not Started','Started','Completed','Deferred')
$priorities   = @('Low','Normal','High','Urgent')
for ($i = 0; $i -lt 14; $i++) {
    try {
        $body = @{
            name     = $taskNames[$rnd.Next(0, $taskNames.Count)]
            status   = $taskStatuses[$rnd.Next(0, $taskStatuses.Count)]
            priority = $priorities[$rnd.Next(0, $priorities.Count)]
            dateEnd  = (Get-Date).AddDays($rnd.Next(-10, 30)).ToString('yyyy-MM-dd HH:mm:ss')
        }
        if ($oppIds.Count -gt 0) {
            $body['parentType'] = 'Opportunity'
            $body['parentId']   = $oppIds[$rnd.Next(0, $oppIds.Count)]
        }
        Invoke-Espo POST 'Task' $body | Out-Null
        $created.Task++
    } catch { $failed++ }
}
Write-Host ("  создано задач: " + $created.Task) -ForegroundColor DarkGray

# ---------- итог ----------
Write-Host ''
Write-Host 'Готово. Что лежит в системе сейчас:' -ForegroundColor Green
foreach ($e in @('Account','Contact','Opportunity','Lead','Task')) {
    try {
        $r = Invoke-Espo GET "$e`?maxSize=1"
        '  {0,-12} : {1}' -f $e, $r.total
    } catch { '  {0,-12} : ? (не удалось прочитать)' -f $e }
}
if ($failed -gt 0) { Write-Host ("  запросов с ошибкой: " + $failed) -ForegroundColor Yellow }
Write-Host ''
Write-Host '  Открывай http://localhost:8080 и смотри разделы Accounts / Contacts / Opportunities / Leads.' -ForegroundColor DarkGray
Write-Host ''
try { Read-Host 'Нажми Enter, чтобы закрыть окно' | Out-Null } catch { }
