# Environment Variables

Do not commit real secret values. Keep local credentials in `.env`, shell environment variables, or ignored local Spring property files.

Required for Google OAuth login:

```text
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
```

Local redirect URI:

```text
http://localhost:8080/login/oauth2/code/google
```

Required for Kakao place search:

```text
KAKAO_REST_API_KEY=
```

Reserved for future LLM-backed Agent integration:

```text
OPENAI_API_KEY=
```

Spring configuration can be supplied through environment variables or an ignored local file such as `src/main/resources/application-local.properties`.

Local development uses the `local` profile by default when running:

```text
./gradlew bootRun
```

If you override boot arguments, include the profile explicitly:

```text
./gradlew bootRun --args='--spring.profiles.active=local --server.port=8081'
```

Google redirect URIs must match the host and port used in the browser. For example:

```text
http://localhost:8080/login/oauth2/code/google
http://127.0.0.1:8081/login/oauth2/code/google
```
