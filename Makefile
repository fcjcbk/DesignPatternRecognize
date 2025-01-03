all: build run

build:
	mvn clean install

run:
	java -jar target/javaparser-maven-sample-1.0-SNAPSHOT-shaded.jar