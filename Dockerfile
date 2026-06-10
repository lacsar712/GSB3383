FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM tomcat:10.1-jre17-temurin
WORKDIR /usr/local/tomcat
RUN rm -rf webapps/ROOT webapps/ROOT.war
COPY --from=build /workspace/target/online-ordering.war webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
