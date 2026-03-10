# dev.ps1 — Compila o projeto, inicia o servidor e reinicia automaticamente ao detectar mudanças em arquivos .java
# Uso: powershell -ExecutionPolicy Bypass -File dev.ps1

$ErrorActionPreference = 'SilentlyContinue'
$script:proc = $null

function Compile {
    Write-Host "`n[dev] Compilando..." -ForegroundColor Cyan
    $javaFiles = (Get-ChildItem "src" -Include "*.java" -Recurse).FullName
    $out = javac -encoding UTF-8 -d bin $javaFiles 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[dev] ERRO de compilacao:" -ForegroundColor Red
        $out | ForEach-Object { Write-Host "  $_" }
        return $false
    }
    Write-Host "[dev] Compilacao OK." -ForegroundColor Green
    return $true
}

function Start-Server {
    $script:proc = Start-Process -FilePath "java" -ArgumentList "-cp bin server.AppServer" -PassThru -NoNewWindow
    Write-Host "[dev] Servidor iniciado (PID $($script:proc.Id)) — http://localhost:8080" -ForegroundColor Green
}

function Stop-Server {
    if ($script:proc -and -not $script:proc.HasExited) {
        Stop-Process -Id $script:proc.Id -Force
        Start-Sleep -Milliseconds 400
        Write-Host "[dev] Servidor parado." -ForegroundColor Yellow
    }
}

# Criar pasta bin se não existir
if (-not (Test-Path "bin")) { New-Item -ItemType Directory -Path "bin" | Out-Null }

# Compilação e início iniciais
if (Compile) { Start-Server }

# Watcher de arquivos .java
$watcher = New-Object System.IO.FileSystemWatcher
$watcher.Path = (Resolve-Path "src").Path
$watcher.Filter = "*.java"
$watcher.IncludeSubdirectories = $true
$watcher.EnableRaisingEvents = $true

Write-Host "[dev] Monitorando src\**\*.java — Ctrl+C para parar`n" -ForegroundColor Yellow

try {
    while ($true) {
        $ev = $watcher.WaitForChanged([System.IO.WatcherChangeTypes]::All, 500)
        if (-not $ev.TimedOut) {
            Write-Host "[dev] Mudanca detectada: $($ev.Name)" -ForegroundColor Magenta
            Start-Sleep -Milliseconds 500   # debounce: aguarda saves simultâneos
            Stop-Server
            if (Compile) { Start-Server }
        }
    }
} finally {
    Stop-Server
    $watcher.Dispose()
    Write-Host "[dev] Encerrado." -ForegroundColor Red
}
