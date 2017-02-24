@echo off
if %1% == c goto compile
if %1% == r goto run
if %1% == cr goto compile_run
else goto compile 
:compile 
    javac -g -classpath .;OSP.jar -d . *.java
    goto end
:run 
    java -classpath .;OSP.jar osp.OSP -noGUI
    goto end
:compile_run
    javac -g -classpath .;OSP.jar -d . *.java
    java -classpath .;OSP.jar osp.OSP
:end