@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM Apache Maven Wrapper startup batch script, version 3.3.2

@IF "%__MVNW_ARG0_NAME__%"=="" (SET __MVNW_ARG0_NAME__=%~nx0)
@SET __MVNW_CMD__=
@SET __MVNW_ERROR__=
@SET __MVNW_SAVE_ERRORLEVEL__=
@SET __MVNW_SAVE_CD__=%CD%

@SETLOCAL

@SET DIRNAME=%~dp0
@IF "%DIRNAME%"=="" SET DIRNAME=.
@SET APP_BASE_NAME=%~n0
@SET APP_HOME=%DIRNAME%

@SET JAVA_EXE=java.exe
@IF NOT "%JAVA_HOME%"=="" (
    @SET JAVA_EXE=%JAVA_HOME%\bin\java.exe
)

@IF NOT EXIST "%JAVA_EXE%" (
    @ECHO Error: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
    @ECHO Please set the JAVA_HOME variable in your environment to match the 1>&2
    @ECHO location of your Java installation. 1>&2
    @EXIT /B 1
)

@SET WRAPPER_PROPERTIES=%APP_HOME%.mvn\wrapper\maven-wrapper.properties
@FOR /F "usebackq tokens=2 delims==" %%G IN (`findstr /b "distributionUrl" "%WRAPPER_PROPERTIES%"`) DO @SET DISTRIBUTION_URL=%%G

@SET MAVEN_USER_HOME=%USERPROFILE%\.m2
@SET MAVEN_DIST_NAME=apache-maven-3.9.6
@SET MAVEN_HOME=%MAVEN_USER_HOME%\wrapper\dists\%MAVEN_DIST_NAME%-bin\%MAVEN_DIST_NAME%

@IF NOT EXIST "%MAVEN_HOME%\bin\mvn.cmd" (
    @ECHO Downloading Maven from %DISTRIBUTION_URL%
    @MKDIR "%MAVEN_USER_HOME%\wrapper\dists\%MAVEN_DIST_NAME%-bin" 2>NUL
    @powershell -Command "Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%TEMP%\maven.zip'"
    @powershell -Command "Expand-Archive -Path '%TEMP%\maven.zip' -DestinationPath '%MAVEN_USER_HOME%\wrapper\dists\%MAVEN_DIST_NAME%-bin' -Force"
    @DEL "%TEMP%\maven.zip"
)

@"%MAVEN_HOME%\bin\mvn.cmd" %*
