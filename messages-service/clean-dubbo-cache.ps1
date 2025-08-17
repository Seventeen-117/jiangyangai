# PowerShell脚本：清理Dubbo缓存文件
Write-Host "清理Dubbo缓存文件..." -ForegroundColor Green

# 获取用户主目录
$userHome = $env:USERPROFILE
$dubboCachePath = Join-Path $userHome ".dubbo"

if (Test-Path $dubboCachePath) {
    Write-Host "发现Dubbo缓存目录: $dubboCachePath" -ForegroundColor Yellow
    
    # 获取所有缓存文件
    $files = Get-ChildItem -Path $dubboCachePath -File
    
    foreach ($file in $files) {
        if ($file.Name -match "messages-service|bgai-service|\.mapping\.|\.metadata\.") {
            Write-Host "删除冲突的缓存文件: $($file.Name)" -ForegroundColor Red
            try {
                Remove-Item $file.FullName -Force
                Write-Host "已删除: $($file.Name)" -ForegroundColor Green
            } catch {
                Write-Host "无法删除文件: $($file.FullName)" -ForegroundColor Red
            }
        }
    }
    
    Write-Host "Dubbo缓存清理完成" -ForegroundColor Green
} else {
    Write-Host "Dubbo缓存目录不存在: $dubboCachePath" -ForegroundColor Yellow
}

# 清理临时目录中的Dubbo文件
$tempDir = $env:TEMP
$tempDubboPath = Join-Path $tempDir "dubbo"

if (Test-Path $tempDubboPath) {
    Write-Host "清理临时目录中的Dubbo文件: $tempDubboPath" -ForegroundColor Yellow
    try {
        Remove-Item $tempDubboPath -Recurse -Force
        Write-Host "临时目录清理完成" -ForegroundColor Green
    } catch {
        Write-Host "无法清理临时目录" -ForegroundColor Red
    }
}

Write-Host "Cleanup completed!" -ForegroundColor Green
Read-Host "Press any key to continue"
