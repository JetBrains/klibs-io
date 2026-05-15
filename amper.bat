@echo off

@rem
@rem Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
@rem

@rem Possible environment variables:
@rem   AMPER_DOWNLOAD_ROOT        Maven repository to download Amper dist from
@rem                              default: https://packages.jetbrains.team/maven/p/amper/amper
@rem   AMPER_JRE_DOWNLOAD_ROOT    Url prefix to download Amper JRE from.
@rem                              default: https:/
@rem   AMPER_BOOTSTRAP_CACHE_DIR  Cache directory to store extracted JRE and Amper distribution
@rem   AMPER_JAVA_HOME            JRE to run Amper itself (optional, does not affect compilation)
@rem   AMPER_JAVA_OPTIONS         JVM options to pass to the JVM running Amper (does not affect the user's application)
@rem   AMPER_NO_WELCOME_BANNER    Disables the first-run welcome message if set to a non-empty value

setlocal

@rem The version of the Amper distribution to provision and use
set amper_version=0.11.0-dev-3937
@rem Establish chain of trust from here by specifying exact checksum of Amper distribution to be run
set amper_sha256=44aac0b8766dc9904f7fc60a950bdc8bdfdae5414b8875a7fc1e9238a7dff5d3

if not defined AMPER_DOWNLOAD_ROOT set AMPER_DOWNLOAD_ROOT=https://packages.jetbrains.team/maven/p/amper/amper
if not defined AMPER_BOOTSTRAP_CACHE_DIR set AMPER_BOOTSTRAP_CACHE_DIR=%LOCALAPPDATA%\JetBrains\Amper
@rem remove trailing \ if present
if [%AMPER_BOOTSTRAP_CACHE_DIR:~-1%] EQU [\] set AMPER_BOOTSTRAP_CACHE_DIR=%AMPER_BOOTSTRAP_CACHE_DIR:~0,-1%

goto :after_function_declarations

REM ********** Download and extract any zip or .tar.gz archive **********

:download_and_extract
setlocal

set moniker=%~1
set url=%~2
set target_dir=%~3
set sha=%~4
set sha_size=%~5
set show_banner_on_cache_miss=%~6

set flag_file=%target_dir%\.flag
if exist "%flag_file%" (
    set /p current_flag=<"%flag_file%"
    if "%current_flag%" == "%sha%" exit /b
)

@rem This multiline string is actually passed as a single line to powershell, meaning #-comments are not possible.
@rem So here are a few comments about the code below:
@rem  - we need to support both .zip and .tar.gz archives (for the Amper distribution and the JRE)
@rem  - tar should be present in all Windows machines since 2018 (and usable from both cmd and powershell)
@rem  - tar requires the destination dir to exist
@rem  - We use (New-Object Net.WebClient).DownloadFile instead of Invoke-WebRequest for performance. See the issue
@rem    https://github.com/PowerShell/PowerShell/issues/16914, which is still not fixed in Windows PowerShell 5.1
@rem  - DownloadFile requires the directories in the destination file's path to exist
set download_and_extract_ps1= ^
Set-StrictMode -Version 3.0; ^
$ErrorActionPreference = 'Stop'; ^
 ^
$createdNew = $false; ^
$lock = New-Object System.Threading.Mutex($true, ('Global\amper-bootstrap.' + '%target_dir%'.GetHashCode().ToString()), [ref]$createdNew); ^
if (-not $createdNew) { ^
    Write-Host 'Another Amper instance is bootstrapping. Waiting for our turn...'; ^
    [void]$lock.WaitOne(); ^
} ^
 ^
try { ^
    if ((Get-Content '%flag_file%' -ErrorAction Ignore) -ne '%sha%') { ^
        if (('%show_banner_on_cache_miss%' -eq 'true') -and [string]::IsNullOrEmpty('%AMPER_NO_WELCOME_BANNER%')) { ^
            Write-Host '*** Welcome to Amper v.%amper_version%! ***'; ^
            Write-Host ''; ^
            Write-Host 'This is the first run of this version, so we need to download the actual Amper distribution.'; ^
            Write-Host 'Please give us a few seconds now, subsequent runs will be faster.'; ^
            Write-Host ''; ^
        } ^
        $temp_file = '%AMPER_BOOTSTRAP_CACHE_DIR%\' + [System.IO.Path]::GetRandomFileName(); ^
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; ^
        Write-Host 'Downloading %moniker%...'; ^
        [void](New-Item '%AMPER_BOOTSTRAP_CACHE_DIR%' -ItemType Directory -Force); ^
        if (Get-Command curl.exe -errorAction SilentlyContinue) { ^
            curl.exe -L --silent --show-error --fail --output $temp_file '%url%'; ^
        } else { ^
            (New-Object Net.WebClient).DownloadFile('%url%', $temp_file); ^
        } ^
 ^
        $actualSha = (Get-FileHash -Algorithm SHA%sha_size% -Path $temp_file).Hash.ToString(); ^
        if ($actualSha -ne '%sha%') { ^
            $writeErr = if ($Host.Name -eq 'ConsoleHost') { [Console]::Error.WriteLine } else { $host.ui.WriteErrorLine } ^
            $writeErr.Invoke(\"ERROR: Checksum mismatch for $temp_file (downloaded from %url%): expected checksum %sha% but got $actualSha\"); ^
            exit 1; ^
        } ^
 ^
        if (Test-Path '%target_dir%') { ^
            Remove-Item '%target_dir%' -Recurse; ^
        } ^
        if ($temp_file -like '*.zip') { ^
            Add-Type -A 'System.IO.Compression.FileSystem'; ^
            [IO.Compression.ZipFile]::ExtractToDirectory($temp_file, '%target_dir%'); ^
        } else { ^
            [void](New-Item '%target_dir%' -ItemType Directory -Force); ^
            tar -xzf $temp_file -C '%target_dir%'; ^
        } ^
        Remove-Item $temp_file; ^
 ^
        Set-Content '%flag_file%' -Value '%sha%'; ^
        Write-Host 'Download complete.'; ^
        Write-Host ''; ^
    } ^
} ^
finally { ^
    $lock.ReleaseMutex(); ^
}

rem We reset the PSModulePath in case this batch script was called from PowerShell Core
rem See https://github.com/PowerShell/PowerShell/issues/18108#issuecomment-2269703022
set PSModulePath=
set powershell=%SystemRoot%\system32\WindowsPowerShell\v1.0\powershell.exe
"%powershell%" -NonInteractive -NoProfile -NoLogo -Command %download_and_extract_ps1%
if errorlevel 1 exit /b 1
exit /b 0

:find_project_context
@rem Search upwards for an amper.bat wrapper file and/or project.yaml
@rem Sets wrapper_script to the found wrapper path, or empty string if not found.
@rem Returns errorlevel 0 if a valid wrapper (that is not this script itself) was found, 1 otherwise.
set wrapper_script=
set this_script=%~f0
set project_dir=%CD%

:find_loop
set wrapper_candidate=%project_dir%\amper.bat
if "%this_script%"=="%wrapper_candidate%" (
    @rem Found itself (local wrapper case), no need to update any version or search further.
    exit /b 1
)

if exist "%wrapper_candidate%" (
    @rem Found a wrapper — check that a project context exists alongside it
    if exist "%project_dir%\project.yaml" (
        set wrapper_script=%wrapper_candidate%
        exit /b 0
    )
    if exist "%project_dir%\module.yaml" (
        set wrapper_script=%wrapper_candidate%
        exit /b 0
    )
    echo WARNING: Found wrapper script '%wrapper_candidate%', but no project.yaml or module.yaml near it. Skipping. >&2
    @rem Continue the search
    goto :find_next_parent
)

if exist "%project_dir%\project.yaml" (
    @rem Found project.yaml but no wrapper alongside it
    echo WARNING: Found a project.yaml in '%project_dir%', but the wrapper script is missing; using $amper_version. >&2
    exit /b 1
)

:find_next_parent
@rem Move to parent directory
for %%P in ("%project_dir%\..") do set parent_dir=%%~fP
if "%parent_dir%"=="%project_dir%" (
    @rem Reached the root, stop searching
    exit /b 1
)
set project_dir=%parent_dir%
goto :find_loop

:parse_project_context
@rem Parse amper_version and amper_sha256 from the found wrapper_script without executing it.
set parsed_amper_version=
set parsed_amper_sha256=

for /f "tokens=2 delims==" %%A in ('findstr /r /c:"^set amper_version=[A-Za-z0-9._+-]*$" "%wrapper_script%"') do (
    if not defined parsed_amper_version set parsed_amper_version=%%A
)
for /f "tokens=2 delims==" %%A in ('findstr /r /c:"^set amper_sha256=[0-9a-fA-F]*$" "%wrapper_script%"') do (
    if not defined parsed_amper_sha256 set parsed_amper_sha256=%%A
)

if not defined parsed_amper_version (
    echo ERROR: Suspicious local wrapper script: failed to detect the distribution version in '%wrapper_script%' >&2
    exit /b 1
)
if not defined parsed_amper_sha256 (
    echo ERROR: Suspicious local wrapper script: failed to detect the distribution checksum in '%wrapper_script%' >&2
    exit /b 1
)

@rem Overwrite builtin values and proceed
set amper_version=%parsed_amper_version%
set amper_sha256=%parsed_amper_sha256%
exit /b 0

:fail
echo ERROR: Amper bootstrap failed, see errors above
exit /b 1

:after_function_declarations

REM ********** Project-local version detection **********

if defined AMPER_WRAPPER_ALWAYS_USE_INTRINSIC_VERSION goto :after_local_version_detection

call :find_project_context
if errorlevel 1 goto :after_local_version_detection
call :parse_project_context
if errorlevel 1 goto fail
:after_local_version_detection

REM ********** Provision Amper distribution **********

set amper_url=%AMPER_DOWNLOAD_ROOT%/org/jetbrains/amper/amper-cli/%amper_version%/amper-cli-%amper_version%-dist.tgz
set amper_target_dir=%AMPER_BOOTSTRAP_CACHE_DIR%\amper-cli-%amper_version%
call :download_and_extract "Amper distribution v%amper_version%" "%amper_url%" "%amper_target_dir%" "%amper_sha256%" "256" "true"
if errorlevel 1 goto fail

REM ********** Launch Amper **********

rem Determine the correct busybox binary based on architecture
if "%PROCESSOR_ARCHITECTURE%"=="ARM64" (
    set busybox_exe=%amper_target_dir%\bin\busybox64a.exe
) else if "%PROCESSOR_ARCHITECTURE%"=="AMD64" (
    set busybox_exe=%amper_target_dir%\bin\busybox64u.exe
) else (
    echo Unsupported architecture %PROCESSOR_ARCHITECTURE% >&2
    goto fail
)

rem We use busybox here because it doesn't reinterpret the user-passed command-line arguments (that we pass via %*).
rem Also this way we can use the unified launcher script (.sh)
set AMPER_WRAPPER_PATH=%~f0
"%busybox_exe%" sh "%amper_target_dir%\bin\launcher.sh" %*
exit /B %ERRORLEVEL%
