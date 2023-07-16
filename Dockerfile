FROM sbtscala/scala-sbt:eclipse-temurin-jammy-11.0.17_8_1.9.2_2.13.11
WORKDIR /app
COPY . .
RUN sbt compile 
CMD [ "sbt", "run" ]
EXPOSE 9000