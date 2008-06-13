@echo off

REM Copyright (c) 2006, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
REM
REM Licensed under the Apache License, Version 2.0 (the "License");
REM you may not use this file except in compliance with the License.
REM You may obtain a copy of the License at
REM
REM      http://www.apache.org/licenses/LICENSE-2.0
REM
REM Unless required by applicable law or agreed to in writing, software
REM distributed under the License is distributed on an "AS IS" BASIS,
REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM See the License for the specific language governing permissions and
REM limitations under the License.

if "%OS%"=="Windows_NT" @setlocal
if "%OS%"=="WINNT" @setlocal

rem %~dp0 is expanded pathname of the current script under NT
set SYNAPSE_HOME=%~dps0..

set _SYNAPSE_XML=-Dsynapse.xml="%SYNAPSE_HOME%\repository\conf\synapse.xml"
set _XDEBUG=

rem Slurp the command line arguments. This loop allows for an unlimited number
rem of arguments (up to the command line limit, anyway).

:setupArgs
if ""%1""=="""" goto doneStart
if ""%1""==""-sample"" goto SYNAPSESample
if ""%1""==""-xdebug"" goto xdebug
shift
goto setupArgs

rem is there is a -xdebug in the options
:xdebug


set _XDEBUG="wrapper.java.additional.10=-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000"
shift
goto setupArgs

:SYNAPSESample
shift
set _SYNAPSE_XML=-Dsynapse.xml="%SYNAPSE_HOME%\repository\conf\sample\synapse_sample_%1.xml"
shift
goto setupArgs

:doneStart
rem find SYNAPSE_HOME if it does not exist due to either an invalid value passed
rem by the user or the %0 problem on Windows 9x
if exist "%SYNAPSE_HOME%\README.TXT" goto checkJava

:noSYNAPSEHome
echo SYNAPSE_HOME is set incorrectly or WSO2 SYNAPSE could not be located. Please set SYNAPSE_HOME.
goto end

:checkJava
set _JAVACMD=%JAVACMD%

if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD="%JAVA_HOME%\bin\java.exe"
goto runServer

:noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=java.exe
echo JAVA_HOME variable not defined or incorrect. Please set JAVA_HOME.

:runServer
@rem @echo on
cd %SYNAPSE_HOME%
echo "Starting WSO2 Enterprise Service Bus ..."
echo Using SYNAPSE_HOME:        %SYNAPSE_HOME%
echo Using JAVA_HOME:       %JAVA_HOME%
echo Using SYNAPSE_XML:     %_SYNAPSE_XML%

rem Decide on the wrapper binary.
set _WRAPPER_BASE=wrapper
set _WRAPPER_DIR=%SYNAPSE_HOME%\bin\native\
set _WRAPPER_EXE=%_WRAPPER_DIR%%_WRAPPER_BASE%-windows-x86-32.exe
if exist "%_WRAPPER_EXE%" goto conf
set _WRAPPER_EXE=%_WRAPPER_DIR%%_WRAPPER_BASE%-windows-x86-64.exe
if exist "%_WRAPPER_EXE%" goto conf
set _WRAPPER_EXE=%_WRAPPER_DIR%%_WRAPPER_BASE%.exe
if exist "%_WRAPPER_EXE%" goto conf
echo Unable to locate a Wrapper executable using any of the following names:
echo %_WRAPPER_DIR%%_WRAPPER_BASE%-windows-x86-32.exe
echo %_WRAPPER_DIR%%_WRAPPER_BASE%-windows-x86-64.exe
echo %_WRAPPER_DIR%%_WRAPPER_BASE%.exe
pause
goto :eof

rem
rem Find the wrapper.conf
rem
:conf
set _WRAPPER_CONF="%SYNAPSE_HOME%\repository\conf\wrapper.conf"

rem
rem Start the Wrapper
rem
:startup
"%_WRAPPER_EXE%" -c %_WRAPPER_CONF% wrapper.java.additional.1=%_SYNAPSE_XML% %_XDEBUG%

if not errorlevel 1 goto :eof
pause


:end
set _JAVACMD=
set SYNAPSE_CMD_LINE_ARGS=

if "%OS%"=="Windows_NT" @endlocal
if "%OS%"=="WINNT" @endlocal

:mainEnd
