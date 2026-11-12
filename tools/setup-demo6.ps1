$ErrorActionPreference = 'Stop'
$previousDemoPassword6 = $env:LIBMAN6_DEMO_PASSWORD
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    if (-not (Test-Path 'target/LibMan6/WEB-INF/classes')) { throw 'Run mvn clean package first.' }
    $demoClasspath6 = 'target/classes;target/LibMan6/WEB-INF/lib/*'
    New-Item -ItemType Directory -Force 'target/check6' | Out-Null
    & javac -encoding UTF-8 -cp $demoClasspath6 -d target/check6 tools/DemoSetup6.java
    if ($LASTEXITCODE -ne 0) { throw 'Setup compilation failed.' }
    if ([string]::IsNullOrEmpty($env:LIBMAN6_DEMO_PASSWORD)) {
        $secureDemoPassword6 = Read-Host 'Choose a local demo password (at least 12 characters)' -AsSecureString
        $demoPasswordPointer6 = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureDemoPassword6)
        try { $env:LIBMAN6_DEMO_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($demoPasswordPointer6) }
        finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($demoPasswordPointer6)
            $secureDemoPassword6.Dispose()
        }
    }
    & java -cp "target/check6;$demoClasspath6" DemoSetup6
    if ($LASTEXITCODE -ne 0) { throw 'Demo setup failed.' }
} finally {
    $env:LIBMAN6_DEMO_PASSWORD = $previousDemoPassword6
    Pop-Location
}
