# Products API – Historical prices

- [Català](#català)
- [English](#english)

---

# Català

## 1. Com he plantejat la prova

A l'acabar de llegir l'enunciat de la prova em vaig quedar amb la frase *"Queremos ver cómo piensas, no solo cómo codificas"*. Per això he treballat seguint una metodologia similar a la que faig servir amb el meu equip actualment. A continuació explico mes detalls:

### Metodologia

1. Abans d'escriure codi vaig llegir el repositori sencer, vaig detectar-ne alguns errors i ambigüitats, i vaig voler tancar la majoria de decissions possible (les explico mes endevant).
2. Cada commit inclou una feature o canvi independent (de forma modular).
3. Cada commit va passar una revisió abans de continuar. El que sortia de la revisió ho corregia a la fase següent, o ho descartava amb un motiu. Quan no hi estava d'acord, he mantingut el meu criteri i l'explico.
4. Cada decisió i cada trade-off està escrit amb el seu motiu, així com algunes limitacions.

### Ús de la IA

Faig servir la IA cada dia. Treballar en aquesta prova sense fer us d'aquesta senzillament no te cap sentit, però tampoc en tindria utilitzar-la amb un prompt que m'ho faci tot a la primera. Com he dit al principi, entenc que en aquesta prova es vol veure com penso, i per això he fet servir la IA per les següents tasques específiques:

- Per reduir el cold start he generat l'esquelet del projecte amb un prompt, així com alguna configuració inicial i codi de partida. La majoria del temps de desenvolupament ha estat la evalucació i presa de decissions.
- He fet us de la IA per a que em qüestioni les decisions i em busqui casos límit i riscos, com ho faria un company en una revisió de codi.
- Un dels meus punts dèbils és el testeig de component o integració, ja que tot i ser un equip petit disposem d'un QA. Per això he utilitzat la IA per generar algunes proves (tests unitaris i d'integració, així com algunes mesures de rendiment).
- Finalment, part de la documentació està feta a partir d'esborranys generats per IA, o revisats per aquesta.

Les decisions i el criteri són meus, i tot el que entrego ho he revisat i ho entenc. Per tal de maximitzar la transparència, **adjunto la conversa sencera exportada en aquesta [URL](https://claude.ai/share/ab12e487-66a1-4542-894c-776c1549d07b)**. He fet servir la meva llicència de Claude Pro personal, creant un chat amb el model Opus 5.5.

## 2. Com executar-ho

L'únic requisit és **Docker amb Docker Compose v2**. Tot es compila dins dels containers.

| Què | Ordre |
|---|---|
| Aixecar l'API (amb imatge nativa, ho explico mes endevant) | `docker compose up -d --build db app` |
| Aixecar l'API (amb la JVM original) | `APP_DOCKERFILE=Dockerfile.jvm docker compose up -d --build db app` |
| Executar el benchmark | `API_PASSWORD=<contrasenya> docker compose up --build` |
| Comprovar que està aixecada | `curl http://localhost:8080/actuator/health` |
| Reiniciar la base de dades | `docker compose down -v` |

- **El primer build natiu triga uns minuts i necessita fins a 4 GB de memòria.** Si el Docker té poca memòria assignada, feu servir la versió JVM.
- **Usuari de l'API: `mango_test`.** La contrasenya no és al repositori: la rebreu els avaluadors pel mateix fil de correus en què vaig rebre el repositori original.
- **`JWT_SECRET`** té un valor per defecte al `docker-compose.yml` perquè tot arrenqui sense configurar res. Només serveix per a l'entorn local. Es pot substituir amb `JWT_SECRET=<mínim 32 bytes>`.
- **Postgres** només és accessible des de `127.0.0.1:5432` (usuari `postgres`, contrasenya `postgres`).
- **Els tests** s'executen dins del build de Docker. Amb un JDK 21 local, també amb `./gradlew test`.

## 3. API

Totes les rutes, menys `POST /auth/login` i `/actuator/health`, necessiten `Authorization: Bearer <token>`.

| Mètode | Ruta | Correcte | Errors |
|---|---|---|---|
| `POST` | `/auth/login` | `200` amb el token | `400` · `401` |
| `POST` | `/products` | `201` amb l'`id` | `400` |
| `POST` | `/products/prices?productId=` | `201` amb el preu | `400` · `409` si se solapa |
| `GET` | `/products/prices?productId=&date=` | `200` amb el preu | `204` si no n'hi ha · `400` |
| `GET` | `/products/prices/history?productId=` | `200` amb el producte i els preus | `400` |
| `PUT` | `/products/prices?priceId=` | `200` amb el preu | `400` · `409` si se solapa |
| `DELETE` | `/products/prices?priceId=` | `204` | `400` |

- Sense token, o amb un token invàlid: `401`. Sense permís per a la ruta: `403`.
- Tots els errors surten en format `application/problem+json` (RFC 9457).
- Cos d'un preu: `{"value": 99.99, "currency": "EUR", "initDate": "2024-01-01", "endDate": "2024-06-30"}`. La moneda és opcional (`EUR` per defecte), i `endDate: null` vol dir un preu obert.

## 4. Detalls que he assumit

- Treballem amb dades d'un sol tenant, amb la zona horària `Europe/Madrid` a l'app i a Postgres.
- `endDate` és inclusiu, com fa pensar l'exemple de l'enunciat, i `initDate < endDate` és estricte. Per tant, un preu no pot començar i acabar el mateix dia.
- Les dates han d'estar entre `1900-01-01` i `9999-12-31`.
- Un preu que es solapa amb un altre, també amb un de obert, es rebutja amb un `409`. Cap petició modifica en silenci un altre pre. Per poder tancar un preu obert es pot fer servir el `PUT`.
- Els diners es guarden en dues columnes (valor i moneda). El valor ha de ser `≥ 0` (el 0 es permet, per exemple per a promocions), amb 2 decimals com a màxim. La moneda són 3 lletres majúscules. Només hi pot haver un preu per dia, sigui quina sigui la moneda.
- Els imports surten sempre amb 2 decimals. Si a la base de dades n'hi ha algun amb més (no hauria de passar), s'arrodoneix `HALF_UP` i es registra un `warn`.
- L'enunciat no defineix els codis d'error, així que els he triat jo:
  - `400` en lloc de `404` per a un producte o un preu que no existeix. Per experiència, el `404` es confon amb una ruta mal escrita. Amb aquest disseny la ruta sempre existeix, i és el paràmetre el que no és vàlid.
  - `204` quan no hi ha preu en una data, i una llista buida (`200`) quan un producte no té preus, perquè la resposta també porta les dades del producte.

## 5. Canvis respecte a l'enunciat

L'enunciat permet canviar els endpoints si es justifica. Aquests són els canvis que trenquen la compatibilitat amb els endpoints originals, i per tant **els tests automàtics sobre aquests endpoints fallaran**:

- **Identificadors com a query params** (`?productId=`, `?priceId=`), no al path. Sé que l'id al path és la convenció REST habitual, però per experiència, amb permisos definits per patrons de ruta, tenir identificadors al path fa aquests patrons més complexos i fràgils. També complica el logging i el monitoring.
- **L'historial té una ruta pròpia (`/history`).** A l'enunciat, la mateixa ruta retorna dues formes de resposta segons si porta `date`. Un query param hauria de filtrar un recurs, no canviar-ne la forma.
- **Totes les rutes necessiten un token** (reptes opcionals), excepte el login i el health check.

Un altre canvi que no hauria de ser problema és que **les respostes porten més dades** (l'`id` del producte i del preu, i la moneda).

### Reptes opcionals

He fet el suport per a diverses monedes, el `PUT` i el `DELETE` de preus, l'autenticació amb token, l'script de dades de prova (`db/init`) i la prova de rendiment. He deixat fora Swagger/OpenAPI i la paginació, ordenació i filtrat de l'historial.

### Errors del repositori original

Alguns errors a l'enunciat impedien compilar o executar el projecte. Ho he corregit. Aquests son: els dos jars que trencaven el `COPY *.jar`, el `command` del compose que s'afegia a l'`ENTRYPOINT` i apuntava a una classe inexistent, el `version` obsolet del compose (generava warnings), el `bc` que faltava al benchmark, Gradle 8.5 en lloc del wrapper, i l'`id` extret amb `grep`.

## 6. Decisions tècniques

### Stack

- **Java 21 i Spring Boot 3.5.** És el que conec millor, és l'estàndard en sistemes com aquest, i el benchmark ja espera l'`/actuator/health` de Spring Boot. La 3.5 ja no té suport OSS, però les 4.x només suporten imatge nativa amb Java 25, i el projecte és amb Java 21.
- **Imatge nativa (GraalVM i Spring AOT)** per compensar l'arrencada lenta de Spring Boot. Mantinc `Dockerfile.jvm` per comparar i com a alternativa. Afegeixo aquí la comparació que he mesurat entre les dos:

  | | JVM | Natiu |
  |---|---|---|
  | Arrencada | 7,1 s | 0,26 s |
  | Memòria (en repòs → amb càrrega) | 153 → 205 MB | 59 → 76 MB |
  | CPU amb càrrega | 92 % | 28 % |
  | Peticions per segon | ~300 | ~300 (vegeu la secció 7) |

- **PostgreSQL amb JdbcClient, sense ORM.** Menys dependències, i control total de les consultes i els locks. Una estructura en memòria hauria estat suficient, però he plantejat el servei com si hagués d'escalar.
- **Virtual threads** i un pool de Hikari petit (5 connexions) contra un Postgres de 0,5 CPU.

### Arquitectura

`web` (controllers) → `business` (regles i validació) → `service` (transaccions) → `repository` (SQL), amb tres representacions separades (DTO, VO i entitat) perquè cada capa sigui independent.

### Integritat i concurrència

La validació de cada petició es fa a la capa de negoci, abans d'obrir la transacció. Així, una petició invàlida es rebutja sense ocupar cap connexió del pool, i l'error arriba amb un missatge que ho explica. Per aquest motiu no he posat `CHECK` ni triggers a la base de dades (com si estic avituat a fer)

Un cop validada, l'escriptura d'un preu es fa en una sola transacció. Primer es bloqueja la fila del producte amb `SELECT ... FOR UPDATE`, després es comprova si el preu nou se solapa amb algun dels existents, i finalment s'escriu. El lock fa que dues peticions sobre el mateix producte s'executin una darrere l'altra, de manera que la segona ja veu el que ha escrit la primera. Les peticions sobre productes diferents continuen executant-se en paral·lel. El `PUT` i el `DELETE` hi afegeixen un pas extra: quan ja tenen el lock, tornen a comprovar que el preu encara existeix. Si no ho fessin, un `DELETE` que s'executés just abans podria fer que responguessin un `409` o un `200` sobre un preu que ja no hi és (ho he vist al provar).

He afegit una garantia adicional a la base de dades (equiparable a un row-level security). La taula de preus té una restricció `EXCLUDE` amb un índex GiST que impedeix que dos preus del mateix producte tinguin rangs de dates que se solapin. Això està fet de forma òptima i que no impliqui mes temps gràcies a l'index.

### Seguretat

L'autenticació és un filtre propi, sense Spring Security. Per a un sol filtre no calia tot el framework, i així el comportament és fàcil de seguir. De Spring Security només faig servir `spring-security-crypto`, per a bcrypt.

La majoria del codi l'he reaprofitat d'un repositori que ja tinc. El login compara la contrasenya amb el hash bcrypt guardat a la base de dades i retorna un JWT HS256 que caduca en una hora, generat amb nimbus-jose-jwt. Si l'usuari no existeix, igualment es calcula un bcrypt contra un hash fictici, perquè el temps de resposta no reveli quins usuaris existeixen. A cada petició, el filtre comprova la signatura, l'algoritme, l'emissor i la caducitat del token. El secret de signatura (`JWT_SECRET`) és obligatori i ha de tenir com a mínim 32 bytes, i si no hi és l'app no arrenca.

Els permisos són grants per usuari en forma de patrons de ruta (per exemple, `/products/**`). Es carreguen a memòria a l'arrencada i es comparen amb la ruta normalitzada de la petició. Finalment, l'app es connecta a Postgres amb un rol de mínim privilegi, que només pot llegir i modificar les dades de les taules que necessita, i no pot tocar l'esquema.

### Tests

Tests unitaris (Mockito) i de slice (`@WebMvcTest`), sense connexió directe a la base de dades (perquè el build de Docker els pugui executar mes fàcil). Les comprovacions end-to-end les fa el benchmark.sh amb la app arrancada.

## 7. Benchmark

Canvis al `benchmark.sh`:
- **Per adaptar-lo a l'API:** fa login i envia el token, fa servir les rutes noves, i llegeix l'`id` i construeix el JSON del login amb `jq`.
- **Propostes** (marcades com a `PROPOSAL` al script):
  1. espera l'API cada 0,5 s, amb un temps màxim;
  2. comprova el codi i el valor de cada petició funcional, i si alguna falla no fa les proves de càrrega;
  3. limita les peticions en paral·lel (`CONCURRENCY`, per defecte 50);
  4. mostra la taxa d'èxit i les peticions correctes per segon.

No he tocat els límits de CPU i memòria de l'app ni del benchmark. El contenidor auxiliar `db` té 0,5 CPU i 1 GB.

**Limitació de la mesura:** el script obre un procés `curl` i una connexió nova per a cada petició. En una prova fora del compose, aquest client va arribar a unes 400 peticions/s, mentre que un sol `curl --parallel` en va arribar a unes 5.500. Per tant, **les peticions per segon que dona el benchmark les limita sobretot el client**.

## 8. Limitacions acceptades

- Un canvi de grants requereix reiniciar l'app.
- Un JWT no es pot revocar durant l'hora que dura.
- No hi ha límit d'intents de login (throotle limit)
- El `JWT_SECRET` per defecte és públic, i només serveix per a l'entorn local.
- El build natiu triga uns 3-5 minuts i necessita fins a 4 GB de memòria (l'alternativa és `Dockerfile.jvm`).
- El contenidor de l'app s'executa com a `root`, i les imatges base no estan fixades a una versió concreta (en un entorn real de producció faria servir un usuari sense privilegis i versions fixades).
- Les peticions per segon del benchmark estan limitades pel client, com he dit al final.

---

# English

## 1. How I approached the challenge

After reading the statement, the sentence that stuck with me was *"Queremos ver cómo piensas, no solo cómo codificas"*. That is why I worked with a methodology similar to the one I use with my team today. More details below:

### Methodology

1. Before writing any code I read the whole repository, found some bugs and ambiguities, and tried to settle as many decisions as possible (I explain them later).
2. Each commit contains one independent feature or change (in a modular way).
3. Each commit went through a review before moving on. What came out of the review I fixed in the next phase, or discarded with a reason. Where I disagreed, I kept my own criteria and explain them.
4. Every decision and trade-off is written down with its reason, as well as some limitations.

### Use of AI

I use AI every day. Working on this challenge without it simply makes no sense, but neither would using it with a single prompt that does everything at once. As I said at the start, I understand this challenge is meant to show how I think, so I used AI for these specific tasks:

- To reduce the cold start, I generated the project skeleton with a prompt, along with some initial configuration and starting code. Most of the development time went into evaluating and making decisions.
- I used AI to challenge my decisions and look for edge cases and risks, as a colleague would in a code review.
- One of my weak points is component and integration testing, because even though we are a small team, we have a QA. That is why I used AI to generate some of the tests (unit and integration tests, and some performance measurements).
- Finally, part of the documentation is based on drafts generated by AI, or reviewed by it.

The decisions and the criteria are mine, and I have reviewed and understand everything I deliver. For full transparency, **the whole exported conversation is included in my delivery through this [URL](https://claude.ai/share/ab12e487-66a1-4542-894c-776c1549d07b)**. I used my personal Claude Pro licence, in a chat with the Opus 5.5 model.

## 2. How to run it

The only requirement is **Docker with Docker Compose v2**. Everything is built inside the containers.

| What | Command |
|---|---|
| Start the API (native image, explained later) | `docker compose up -d --build db app` |
| Start the API (on the original JVM) | `APP_DOCKERFILE=Dockerfile.jvm docker compose up -d --build db app` |
| Run the benchmark | `API_PASSWORD=<password> docker compose up --build` |
| Check that it is up | `curl http://localhost:8080/actuator/health` |
| Reset the database | `docker compose down -v` |

- **The first native build takes a few minutes and needs up to 4 GB of memory.** If Docker has little memory assigned, use the JVM version.
- **API user: `mango_test`.** The password is not in the repository: reviewers receive it in the same email thread in which I received the original repository.
- **`JWT_SECRET`** has a default value in `docker-compose.yml` so that everything starts without configuration. It is only for local use. It can be replaced with `JWT_SECRET=<at least 32 bytes>`.
- **Postgres** is only reachable on `127.0.0.1:5432` (user `postgres`, password `postgres`).
- **The tests** run inside the Docker build. With a local JDK 21, also with `./gradlew test`.

## 3. API

Every route except `POST /auth/login` and `/actuator/health` requires `Authorization: Bearer <token>`.

| Method | Route | Success | Errors |
|---|---|---|---|
| `POST` | `/auth/login` | `200` with the token | `400` · `401` |
| `POST` | `/products` | `201` with the `id` | `400` |
| `POST` | `/products/prices?productId=` | `201` with the price | `400` · `409` on overlap |
| `GET` | `/products/prices?productId=&date=` | `200` with the price | `204` if none · `400` |
| `GET` | `/products/prices/history?productId=` | `200` with the product and its prices | `400` |
| `PUT` | `/products/prices?priceId=` | `200` with the price | `400` · `409` on overlap |
| `DELETE` | `/products/prices?priceId=` | `204` | `400` |

- Without a token, or with an invalid one: `401`. Without a grant for the route: `403`.
- Every error is returned as `application/problem+json` (RFC 9457).
- Price body: `{"value": 99.99, "currency": "EUR", "initDate": "2024-01-01", "endDate": "2024-06-30"}`. The currency is optional (`EUR` by default), and `endDate: null` means an open price.

## 4. Assumptions

- The data belongs to a single tenant, with the `Europe/Madrid` time zone in the app and in Postgres.
- `endDate` is inclusive, as the statement's example suggests, and `initDate < endDate` is strict. So a price cannot start and end on the same day.
- Dates must be between `1900-01-01` and `9999-12-31`.
- A price that overlaps another one, including an open one, is rejected with `409`. No request silently changes another price. An open price can be closed with `PUT`.
- Money is stored in two columns (value and currency). The value must be `≥ 0` (zero is allowed, for example for promotions), with at most 2 decimals. The currency is 3 uppercase letters. There can only be one price per day, whatever the currency.
- Amounts are always returned with 2 decimals. If the database holds one with more (it should not happen), it is rounded `HALF_UP` and a `warn` is logged.
- The statement does not define the error codes, so I chose them:
  - `400` instead of `404` for a product or price that does not exist. In my experience, `404` is confused with a mistyped route. With this design the route always exists, and it is the parameter that is invalid.
  - `204` when there is no price on a date, and an empty list (`200`) when a product has no prices, because the response also carries the product data.

## 5. Changes to the statement

The statement allows changing the endpoints if the change is justified. These are the changes that break compatibility with the original endpoints, so **automated tests on those endpoints will fail**:

- **Identifiers as query parameters** (`?productId=`, `?priceId=`), not in the path. I know an id in the path is the usual REST convention, but in my experience, with permissions defined by route patterns, ids in the path make those patterns more complex and fragile. It also complicates logging and monitoring.
- **The history has its own route (`/history`).** In the statement, the same route returns two response shapes depending on whether `date` is present. A query parameter should filter a resource, not change its shape.
- **Every route requires a token** (optional challenges), except the login and the health check.

Another change, which should not be a problem, is that **responses carry more data** (the product and price `id`, and the currency).

### Optional challenges

I did multi-currency support, `PUT` and `DELETE` of prices, token authentication, the test data script (`db/init`) and the performance test. I left out Swagger/OpenAPI and pagination, sorting and filtering of the history.

### Bugs in the original repository

Some bugs in the original repository prevented the project from building or running. I fixed them. They are: the two jars that broke `COPY *.jar`, the compose `command` that was appended to the `ENTRYPOINT` and pointed to a class that does not exist, the obsolete compose `version` (it produced warnings), the `bc` missing from the benchmark, Gradle 8.5 instead of the wrapper, and the `id` extracted with `grep`.

## 6. Technical decisions

### Stack

- **Java 21 and Spring Boot 3.5.** It is what I know best, it is the standard in systems like this one, and the benchmark already expects Spring Boot's `/actuator/health`. 3.5 no longer has OSS support, but the 4.x versions only support native images on Java 25, and the project is on Java 21.
- **Native image (GraalVM and Spring AOT)** to make up for Spring Boot's slow startup. I keep `Dockerfile.jvm` to compare and as a fallback. This is the comparison I measured between the two:

  | | JVM | Native |
  |---|---|---|
  | Startup | 7.1 s | 0.26 s |
  | Memory (idle → under load) | 153 → 205 MB | 59 → 76 MB |
  | CPU under load | 92 % | 28 % |
  | Requests per second | ~300 | ~300 (see section 7) |

- **PostgreSQL with JdbcClient, no ORM.** Fewer dependencies, and full control over queries and locks. An in-memory structure would have been enough, but I treated the service as if it had to scale.
- **Virtual threads** and a small Hikari pool (5 connections) against a 0.5 CPU Postgres.

### Architecture

`web` (controllers) → `business` (rules and validation) → `service` (transactions) → `repository` (SQL), with three separate representations (DTO, VO and entity) so that each layer is independent.

### Integrity and concurrency

Each request is validated in the business layer, before the transaction is opened. This way an invalid request is rejected without taking a connection from the pool, and the error comes back with a message that explains it. For this reason there are no `CHECK` constraints or triggers in the database (unlike what I usually do).

Once validated, a price is written in a single transaction. First the product row is locked with `SELECT ... FOR UPDATE`, then the new price is checked for overlaps with the existing ones, and finally it is written. The lock makes two requests on the same product run one after the other, so the second one already sees what the first one wrote. Requests on different products still run in parallel. `PUT` and `DELETE` add an extra step: once they hold the lock, they check again that the price still exists. Otherwise, a `DELETE` that ran just before could make them return a `409` or a `200` for a price that is no longer there (I saw it happen while testing).

I added an extra guarantee in the database (comparable to row-level security). The prices table has an `EXCLUDE` constraint with a GiST index that prevents two prices of the same product from having overlapping date ranges. Thanks to the index, this is done efficiently and adds no extra time.

### Security

Authentication is an own filter, without Spring Security. A single filter did not need the whole framework, and this way the behaviour is easy to follow. From Spring Security I only use `spring-security-crypto`, for bcrypt.

Most of this code is reused from a repository I already have. The login compares the password with the bcrypt hash stored in the database and returns an HS256 JWT that expires in one hour, generated with nimbus-jose-jwt. If the user does not exist, a bcrypt is still computed against a dummy hash, so the response time does not reveal which users exist. On every request, the filter checks the token's signature, algorithm, issuer and expiry. The signing secret (`JWT_SECRET`) is mandatory and must be at least 32 bytes long; without it the app does not start.

Permissions are per-user grants written as route patterns (for example, `/products/**`). They are loaded into memory at startup and matched against the normalized path of the request. Finally, the app connects to Postgres with a least-privilege role, which can only read and modify data in the tables it needs and cannot touch the schema.

### Tests

Unit tests (Mockito) and slice tests (`@WebMvcTest`), with no direct connection to the database (so that the Docker build can run them more easily). End-to-end checks are done by `benchmark.sh` with the app running.

## 7. Benchmark

Changes to `benchmark.sh`:
- **To adapt it to the API:** it logs in and sends the token, uses the new routes, and reads the `id` and builds the login JSON with `jq`.
- **Proposals** (marked as `PROPOSAL` in the script):
  1. it waits for the API every 0.5 s, with a time limit;
  2. it checks the status code and value of every functional request, and if one fails it does not run the load tests;
  3. it limits the parallel requests (`CONCURRENCY`, 50 by default);
  4. it reports the success rate and the successful requests per second.

I did not change the CPU and memory limits of the app or the benchmark. The auxiliary `db` container has 0.5 CPU and 1 GB.

**Measurement limitation:** the script starts one `curl` process and one new connection per request. In a test outside the compose, this client reached about 400 requests/s, while a single `curl --parallel` reached about 5,500. So **the requests per second reported by the benchmark are mostly limited by the client**.

## 8. Accepted limitations

- A change of grants requires restarting the app.
- A JWT cannot be revoked during its one-hour lifetime.
- There is no limit on login attempts (rate limiting).
- The default `JWT_SECRET` is public, and only for local use.
- The native build takes about 3-5 minutes and needs up to 4 GB of memory (the fallback is `Dockerfile.jvm`).
- The app container runs as `root`, and the base images are not pinned to a specific version (in a real production environment I would use an unprivileged user and pinned versions).
- The benchmark's requests per second are limited by the client, as explained in section 7.
