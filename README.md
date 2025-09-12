# NNFS Java Monorepo (Maven)

Maven multi-module version of the Sentdex-inspired "Neural Networks from Scratch" build.

## Modules
- `nnfs-java-core` — pure Java NN + `Trainer` that writes `model/weights.json`
- `api-java-spring` — Spring Boot API exposing `/predict` and `/health`

Frontend: use the `web-vercel` (Next.js) from the previous archive or your own. Vercel hosts the web; deploy Java API elsewhere.

## Quickstart (Maven)
```bash
# 1) Train and save weights
mvn -q -pl nnfs-java-core -am package
mvn -q -pl nnfs-java-core exec:java

# 2) Copy weights into API resources
cp nnfs-java-core/model/weights.json api-java-spring/src/main/resources/model/weights.json

# 3) Run API
mvn -q -pl api-java-spring -am spring-boot:run
# -> http://localhost:8080/health
```

## Deploy
- Deploy `api-java-spring` JAR to Render/Railway/Fly/etc (JDK 17).
- Deploy your Next.js app on Vercel with `NEXT_PUBLIC_API_BASE_URL` pointing to the API.
