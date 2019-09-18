# play-cqrs-scala

## Create docker image

This image creates the DB with the schema for journal and snapshot in place. Just use it!

```
cd docker
docker build  -t akka-jdbc-postgres .
cd ../
```

 ## Create container

 User docker-compose.yml

 ```
 docker-compose up -d
 ```

## Start Play

```sbt "~run"```

 ## Calls

Using httpie

http POST localhost:9000/deposit/abc-def amount:=350  
http POST localhost:9000/withdraw/abc-def amount:=100  
http localhost:9000/balance/abc-def  