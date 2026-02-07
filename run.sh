#!/bin/bash

# --- Configuration des chemins ---
JAVA_FX_PATH=~/javafx-sdk-23/lib
LIB_DIR=lib
OUT_DIR=out

# Crée les dossiers si nécessaire
mkdir -p $LIB_DIR
mkdir -p $OUT_DIR

# --- Téléchargement des librairies ---

# HikariCP
HIKARI_JAR="$LIB_DIR/HikariCP-5.0.1.jar"
if [ ! -f $HIKARI_JAR ]; then
    echo "Téléchargement de HikariCP..."
    wget -O $HIKARI_JAR https://repo1.maven.org/maven2/com/zaxxer/HikariCP/5.0.1/HikariCP-5.0.1.jar
fi

# MySQL Connector
MYSQL_JAR="$LIB_DIR/mysql-connector-j-9.5.0.jar"
if [ ! -f $MYSQL_JAR ]; then
    echo "Téléchargement du connecteur MySQL..."
    wget -O $MYSQL_JAR https://repo1.maven.org/maven2/mysql/mysql-connector-java/8.1.0/mysql-connector-java-8.1.0.jar
fi

# --- Compilation ---
echo "Compilation du projet..."
javac --module-path $JAVA_FX_PATH --add-modules javafx.controls,javafx.fxml \
    -cp "$LIB_DIR/*" -d $OUT_DIR $(find src -name "*.java")

if [ $? -ne 0 ]; then
    echo "Erreur de compilation. Vérifie les messages ci-dessus."
    exit 1
fi

# --- Exécution ---
echo "Exécution de l'application..."
java --module-path $JAVA_FX_PATH --add-modules javafx.controls,javafx.fxml \
    -cp "$OUT_DIR:$LIB_DIR/*" cryptographie.maya.MainApp
