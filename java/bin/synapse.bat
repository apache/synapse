@echo off

REM  Copyright 2001,2004-2005 The Apache Software Foundation
REM
REM  Licensed under the Apache License, Version 2.0 (the "License");
REM  you may not use this file except in compliance with the License.
REM  You may obtain a copy of the License at
REM
REM      http://www.apache.org/licenses/LICENSE-2.0
REM
REM  Unless required by applicable law or agreed to in writing, software
REM  distributed under the License is distributed on an "AS IS" BASIS,
REM  WITHOUT WARRSYNAPSEIES OR CONDITIONS OF ANY KIND, either express or implied.
REM  See the License for the specific language governing permissions and
REM  limitations under the License.

if "%OS%"=="Windows_NT" @setlocal
if "%OS%"=="WINNT" @setlocal

rem %~dp0 is expanded pathname of the current script under NT
set DEFAULT_SYNAPSE_HOME=%~dp0..

if "%SYNAPSE_HOME%"=="" set SYNAPSE_HOME=%DEFAULT_SYNAPSE_HOME%
set DEFAULT_SYNAPSE_HOME=

set _USE_CLASSPATH=yes

rem Slurp the command line arguments. This loop allows for an unlimited number
rem of arguments (up to the command line limit, anyway).
set SYNAPSE_CMD_LINE_ARGS=%1
if ""%1""=="""" goto doneStart
shift
:setupArgs
if ""%1""=="""" goto doneStart
if ""%1""==""-noclasspath"" goto clearclasspath
set SYNAPSE_CMD_LINE_ARGS=%SYNAPSE_CMD_LINE_ARGS% %1
shift
goto setupArgs

rem here is there is a -noclasspath in the options
:clearclasspath
set _USE_CLASSPATH=no
shift
goto setupArgs

rem This label provides a place for the argument list loop to break out
rem and for NT handling to skip to.

:doneStart
rem find SYNAPSE_HOME if it does not exist due to either an invalid value passed
rem by the user or the %0 problem on Windows 9x
if exist "%SYNAPSE_HOME%\README.TXT" goto checkJava

:noSYNAPSEHome
echo SYNAPSE_HOME is set incorrectly or SYNAPSE could not be located. Please set SYNAPSE_HOME.
goto end

:checkJava
set _JAVACMD=%JAVACMD%

if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=%JAVA_HOME%\bin\java.exe
if  "%SYNAPSE_CMD_LINE_ARGS%" == "" goto defaultParams

goto runSynapse

:defaultParams
set SYNAPSE_CMD_LINE_ARGS=-p8080 %SYNAPSE_HOME%\synapse_repository
goto runSynapse

:noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=java.exe

:runSynapse
@echo on
"%_JAVACMD%" -Daxis2.xml=%SYNAPSE_HOME%\synapse_repository\conf\axis2.xml -Djava.ext.dirs=%SYNAPSE_HOME%\lib;%EXT_DIRS%;%SYNAPSE_HOME% -cp %SYNAPSE_HOME%\lib org.apache.axis2.transport.http.SimpleHTTPServer %SYNAPSE_CMD_LINE_ARGS%
goto end

:end
set _JAVACMD=
set SYNAPSE_CMD_LINE_ARGS=

if "%OS%"=="Windows_NT" @endlocal
if "%OS%"=="WINNT" @endlocal

:mainEnd

