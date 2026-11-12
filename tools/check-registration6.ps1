param([string]$TomcatHome = 'C:\tools\apache-tomcat-9.0.123')
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    if (-not (Test-Path 'target/LibMan6/WEB-INF/classes')) { throw 'Run mvn clean package first.' }
    $testClasspath6 = "target/classes;target/LibMan6/WEB-INF/lib/*;$TomcatHome/lib/*;$TomcatHome/bin/tomcat-juli.jar"
    New-Item -ItemType Directory -Force 'target/check6' | Out-Null
    & javac -encoding UTF-8 -cp $testClasspath6 -d target/check6 tools/AuthTestSupport6.java tools/RegistrationCheck6.java
    if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
    & java -cp "target/check6;$testClasspath6" RegistrationCheck6
    if ($LASTEXITCODE -ne 0) { throw 'Integration check failed.' }
} finally { Pop-Location }
