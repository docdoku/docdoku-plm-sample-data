mvn clean install;
mvn dependency:copy-dependencies;
java -classpath target\docdoku-plm-sample-data.jar;target\dependency\docdoku-api-java-2.5.1-SNAPSHOT.jar:target\dependency\* com.docdoku.loaders.Main
