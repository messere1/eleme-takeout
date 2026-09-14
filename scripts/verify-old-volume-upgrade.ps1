# Requires WSL Ubuntu-20.04 with Docker and the local postgres:16 image.
# Creates and removes only the exact disposable container/volume below.
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$name = 'takeout-upgrade-verification-20260914'
$createdVolume = $false
$createdContainer = $false

function Invoke-Docker {
    & wsl -d Ubuntu-20.04 -- docker @args
    if ($LASTEXITCODE -ne 0) { throw "Docker command failed: $($args -join ' ')" }
}

if ($name -notmatch '^takeout-upgrade-verification-[0-9]{8}$') { throw 'Unsafe probe name' }
& wsl -d Ubuntu-20.04 -- docker volume inspect $name *> $null
if ($LASTEXITCODE -eq 0) { throw "Probe volume already exists: $name" }
& wsl -d Ubuntu-20.04 -- docker container inspect $name *> $null
if ($LASTEXITCODE -eq 0) { throw "Probe container already exists: $name" }

try {
    Invoke-Docker volume create $name | Out-Null
    $createdVolume = $true
    Invoke-Docker run -d --name $name -e POSTGRES_DB=take_out -e POSTGRES_USER=take_out -e POSTGRES_PASSWORD=take_out -v "${name}:/var/lib/postgresql/data" postgres:16 | Out-Null
    $createdContainer = $true

    $ready = $false
    for ($i = 0; $i -lt 30; $i++) {
        & wsl -d Ubuntu-20.04 -- docker exec $name pg_isready -U take_out -d take_out *> $null
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw 'PostgreSQL probe did not become ready' }

    git -C $repo show '1a16150:backend/src/main/resources/schema.sql' |
        & wsl -d Ubuntu-20.04 -- docker exec -i $name psql -v ON_ERROR_STOP=1 -U take_out -d take_out | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Old schema initialization failed' }
    $seed = @"
INSERT INTO users(id,username,phone,password_hash,nickname,created_at)
VALUES(9001,'upgrade_probe','13800009001','test','保留用户',CURRENT_TIMESTAMP);
INSERT INTO merchants(id,merchant_name,phone,password_hash,business_scope,created_at)
VALUES(9001,'升级测试商家','13900009001','test','中式快餐',CURRENT_TIMESTAMP);
INSERT INTO shops(id,merchant_id,shop_name,status)
VALUES(9001,9001,'升级测试店铺','OPEN');
INSERT INTO orders(id,order_no,user_id,shop_id,total_amount,status,address,created_at)
VALUES(9001,'UPGRADE-9001',9001,9001,8.50,'CREATED','旧版收货地址',CURRENT_TIMESTAMP);
"@
    $seed | & wsl -d Ubuntu-20.04 -- docker exec -i $name psql -v ON_ERROR_STOP=1 -U take_out -d take_out | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Old data seed failed' }
    Invoke-Docker restart $name | Out-Null

    $migration = Join-Path $repo 'database/migrations/20260914_upgrade_existing_volume.sql'
    if (-not (Test-Path -LiteralPath $migration)) { throw "Migration is missing: $migration" }
    foreach ($run in 1..2) {
        Get-Content -LiteralPath $migration -Raw |
            & wsl -d Ubuntu-20.04 -- docker exec -i $name psql -v ON_ERROR_STOP=1 -U take_out -d take_out | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "Migration run $run failed" }
    }
    $query = @"
SELECT u.username, u.enabled, s.shop_name,
       (s.image_url IS NULL AND s.cover_image_url IS NULL AND s.shop_address IS NULL),
       (SELECT COUNT(*) FROM information_schema.tables
         WHERE table_schema='public' AND table_name IN
           ('business_categories','cart_delivery_info','administrators',
            'riders','refund_requests','admin_audit_logs')),
       (SELECT COUNT(*) FROM information_schema.columns
         WHERE table_name='orders' AND column_name IN
           ('payment_status','payment_deadline','recipient_name','recipient_phone','delivery_address','paid_at','rider_id')),
       (SELECT delivery_address FROM orders WHERE id=9001)
FROM users u JOIN merchants m ON m.id=9001 JOIN shops s ON s.merchant_id=m.id
WHERE u.id=9001;
"@
    $actual = ($query | & wsl -d Ubuntu-20.04 -- docker exec -i $name psql -At -v ON_ERROR_STOP=1 -U take_out -d take_out).Trim()
    if ($LASTEXITCODE -ne 0 -or $actual -ne 'upgrade_probe|t|升级测试店铺|t|6|7|旧版收货地址') {
        throw "Upgraded data/columns mismatch: $actual"
    }
    Write-Output "PASS: old data retained, new columns and tables present, migration idempotent ($actual)"
} finally {
    if ($createdContainer) { & wsl -d Ubuntu-20.04 -- docker rm -f $name | Out-Null }
    if ($createdVolume) { & wsl -d Ubuntu-20.04 -- docker volume rm $name | Out-Null }
}
