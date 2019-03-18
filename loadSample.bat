mvn clean install;
mvn dependency:copy-dependencies;
java -classpath target\docdoku-plm-sample-data.jar;target\dependency\docdoku-api-java-2.5.4.jar:target\dependency\* com.docdoku.loaders.Main
