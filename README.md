# RunPod Worker

**RunPod Worker** adalah layanan backend berbasis Spring Boot 4.x yang berfungsi sebagai jembatan antara client aplikasi dengan [RunPod](https://www.runpod.io/) — platform GPU cloud untuk menjalankan model AI. Layanan ini menerima job dari Redis Pub/Sub, meneruskannya ke endpoint RunPod, dan mengembalikan hasilnya ke client melalui Redis setelah RunPod mengirim webhook callback.

---

## 📋 Daftar Isi

- [Arsitektur](#arsitektur)
- [Fitur](#fitur)
- [Persyaratan](#persyaratan)
- [Teknologi](#teknologi)
- [Struktur Proyek](#struktur-proyek)
- [Konfigurasi](#konfigurasi)
- [Menjalankan Aplikasi](#menjalankan-aplikasi)
- [API Endpoint](#api-endpoint)
- [Alur Kerja](#alur-kerja)
- [Job Types](#job-types)
- [Mode Sinkron vs Asinkron](#mode-sinkron-vs-asinkron)
- [Database Schema](#database-schema)
- [Scheduler](#scheduler)
- [Redis Key Convention](#redis-key-convention)
- [Menambahkan Job Type Baru](#menambahkan-job-type-baru)
- [Logging](#logging)

---

## Arsitektur

```
┌──────────────────────────────────────────────────────────────────┐
│                         CLIENT APP                               │
└─────────────────────────┬────────────────────────────────────────┘
                          │ SET key → payload (TTL)
                          │ PUBLISH channel → key
                          ▼
┌──────────────────────────────────────────────────────────────────┐
│                         REDIS                                    │
│   Pub/Sub : job-upscaler-request  │  job-rembg-request          │
│   KV Store: <requestId>  → JSON payload (TTL)                   │
└─────────────────────────┬────────────────────────────────────────┘
                          │ Subscribe (terima key)
                          ▼
┌──────────────────────────────────────────────────────────────────┐
│                    RUNPOD WORKER (App ini)                        │
│                                                                  │
│  ┌──────────────────────┐   ┌─────────────────────────────────┐ │
│  │  AbstractConsumer    │──▶│  AbstractJob.consume()          │ │
│  │  (Redis Listener)    │   │  - GET payload dari Redis KV    │ │
│  └──────────────────────┘   │  - Kirim ke RunPod API          │ │
│                             │  - Simpan Job ke PostgreSQL     │ │
│  ┌──────────────────────┐   └─────────────────────────────────┘ │
│  │  WebhookController   │──▶  AbstractJob.callback() [@Async]   │
│  │  POST /webhook/*     │    - Update Job di PostgreSQL         │ │
│  └──────────────────────┘    - SET hasil ke Redis KV (TTL)      │ │
│                              - PUBLISH key ke response channel  │ │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  RunpodEndpointScheduler  (setiap N menit)               │   │
│  │  - GET /v1/endpoints/{id}?includeWorkers=true            │   │
│  │  - Simpan worker count → Redis: RUNPOD:{NAME}:{STATUS}   │   │
│  └──────────────────────────────────────────────────────────┘   │
└───────────────────┬──────────────────────┬───────────────────────┘
                    │                      │
                    ▼                      ▼
        ┌───────────────────┐   ┌──────────────────────┐
        │   RUNPOD API      │   │    POSTGRESQL         │
        │  (GPU Workers)    │   │  (Job Tracking)       │
        └──────────┬────────┘   └──────────────────────┘
                   │ Webhook Callback
                   ▼
        ┌──────────────────────────────────────┐
        │  REDIS                               │
        │  KV: <workerJobId> → JSON (TTL 20s)  │
        │  Pub: job-*-response → key           │──▶ CLIENT APP
        └──────────────────────────────────────┘
```

---

## Fitur

- 🚀 **Multi Job Type** — Mendukung Upscaler dan Remove Background (Rembg), mudah diperluas
- 📨 **Redis Pub/Sub + Key-Value** — Payload disimpan di Redis KV (dengan TTL), channel hanya membawa key
- 🔗 **RunPod API Integration** — Integrasi langsung dengan RunPod serverless endpoint
- 🔀 **Async & Sync Mode** — Mendukung `call_runpod_sync: true` untuk job sinkron langsung
- 🪝 **Webhook Callback** — Terima callback asynchronous dari RunPod (`@Async`)
- 💾 **Job Tracking** — Setiap job disimpan di PostgreSQL dengan status, request, response, dan webhook payload (JSONB)
- 📊 **Worker Status Scheduler** — Sync jumlah worker RunPod ke Redis secara periodik
- 📝 **HTTP Logging** — Log otomatis setiap request/response HTTP masuk & keluar

---

## Persyaratan

| Dependency | Versi Minimum |
|---|---|
| Java | 17+ |
| PostgreSQL | 13+ |
| Redis | 6+ |
| Maven | 3.8+ |
| Docker (opsional) | 20+ |

---

## Teknologi

| Teknologi | Versi | Kegunaan |
|---|---|---|
| Spring Boot | 4.0.1 | Framework utama |
| Spring Data JPA | 4.0.1 | ORM / Database access |
| Spring Data Redis | 4.0.1 | Redis Pub/Sub, Key-Value, Template |
| Hibernate | 7.2.0 | JPA implementation |
| Jackson 3.x (`tools.jackson`) | 3.0.3 | JSON serialization (Spring MVC, Consumer, Service) |
| Jackson 2.x (`com.fasterxml`) | 2.17.1 | JSON serialization (Hibernate JSONB) |
| PostgreSQL Driver | latest | Database driver |
| Lombok | latest | Boilerplate reduction |

> **Catatan Dual Jackson:**
> - **Jackson 3.x** (`tools.jackson`) → Spring MVC `@RequestBody`/`@ResponseBody` dan `ObjectMapper` di consumer/service
> - **Jackson 2.x** (`com.fasterxml.jackson`) → Hibernate untuk kolom `JSONB` di PostgreSQL. **Wajib ada** di classpath, jika tidak → `HibernateException: FormatMapper not found`

---

## Struktur Proyek

```
runpod-worker/
├── src/main/java/id/rockierocker/runpodworker/
│   ├── RunpodWorkerApplication.java          # Entry point + @EnableScheduling
│   │
│   ├── component/
│   │   ├── HttpRequest.java                  # Wrapper HTTP calls ke RunPod
│   │   └── RedisPublisher.java               # Publish ke Redis (Pub/Sub + Key-Value)
│   │
│   ├── config/
│   │   ├── RedisConfig.java                  # RedisTemplate, MessageListenerContainer
│   │   ├── RestTemplateConfig.java           # RestTemplate + BufferingClientHttpRequestFactory
│   │   ├── RestTemplateInterceptor.java      # Logging HTTP request/response keluar
│   │   ├── HttpLoggingFilter.java            # Logging HTTP request/response masuk
│   │   └── RunpodServerlessProperties.java   # @ConfigurationProperties binding serverless list
│   │
│   ├── consumer/
│   │   ├── AbstractConsumer.java             # Base: baca key dari channel, ambil payload dari Redis KV
│   │   ├── JobUpscalerConsumer.java          # Consumer job Upscaler
│   │   └── JobRembgConsumer.java             # Consumer job Remove Background
│   │
│   ├── controller/
│   │   └── WebhookController.java            # Endpoint webhook callback dari RunPod
│   │
│   ├── dto/
│   │   ├── ConsumerRequest.java              # Wrapper Redis (requestId + data + callRunpodSync)
│   │   ├── HttpRequestDto.java               # Parameter HTTP call
│   │   ├── JobRequest.java                   # Payload ke RunPod: { "input": {...} }
│   │   ├── JobResponse.java                  # Response submit job ke RunPod
│   │   ├── RunpodEndpointDto.java            # DTO response GET /v1/endpoints/{id}
│   │   ├── RembgRequestDto.java              # Parameter job Remove Background
│   │   ├── RembgResponseDto.java             # Webhook response job Remove Background
│   │   ├── UpscalerRequestDto.java           # Parameter job Upscaler
│   │   └── UpscalerResponseDto.java          # Webhook response job Upscaler
│   │
│   ├── entity/
│   │   ├── BaseEntity.java                   # created, updated, deleted (soft delete)
│   │   └── Job.java                          # Entity tabel job
│   │
│   ├── enums/
│   │   ├── JobType.java                      # UPSCALER, REMBG
│   │   └── JobStatus.java                    # IN_QUEUE, IN_PROGRESS, COMPLETED, FAILED
│   │
│   ├── repository/
│   │   └── JobRepository.java                # JPA repository
│   │
│   ├── scheduler/
│   │   └── RunpodEndpointScheduler.java      # Sync worker status dari RunPod ke Redis
│   │
│   └── service/
│       ├── AbstractJobInterface.java          # Interface: consume() + callback()
│       ├── AbstractJob.java                   # Base logic: consume, callback, sync/async
│       ├── UpscalerJobService.java            # Service job Upscaler
│       └── RembgJobService.java              # Service job Remove Background
│
├── src/main/resources/
│   ├── application.properties                # Konfigurasi utama
│   └── application-local.properties          # Konfigurasi lokal (dev)
│
├── Dockerfile
├── docker-compose.yml
├── .env.example
└── pom.xml
```

---

## Konfigurasi

Salin `.env.example` dan sesuaikan:

```bash
cp .env.example src/main/resources/application-local.properties
```

### Database

```properties
spring.datasource.url=jdbc:postgresql://<HOST>:<PORT>/<DATABASE>
spring.datasource.username=<USERNAME>
spring.datasource.password=<PASSWORD>
spring.jpa.hibernate.ddl-auto=update
```

### Redis

```properties
spring.data.redis.host=<HOST>
spring.data.redis.port=6379
spring.data.redis.password=<PASSWORD>   # kosongkan jika tidak ada password
spring.data.redis.timeout=5000ms

redis.channel.job-upscaler-request=job-upscaler-request
redis.channel.job-upscaler-response=job-upscaler-response
redis.channel.job-rembg-request=job-rembg-request
redis.channel.job-rembg-response=job-rembg-response
```

### RunPod

```properties
runpod.api-token=<RUNPOD_API_TOKEN>
runpod.base-url=https://api.runpod.ai
runpod.run.path.async=/run
runpod.run.path.sync=/runsync

# ID endpoint per job type
runpod.serverless.id.rembg=<REMBG_ENDPOINT_ID>
runpod.serverless.id.upscaler=<UPSCALER_ENDPOINT_ID>

# URL lengkap (otomatis dari base URL + ID)
runpod.serverless.rembg.url=${runpod.base-url}/v2/${runpod.serverless.id.rembg}
runpod.serverless.upscaler.url=${runpod.base-url}/v2/${runpod.serverless.id.upscaler}

# RunPod REST API (untuk scheduler)
runpod.endpoint.url=https://rest.runpod.io/v1/endpoints

# List serverless yang dipantau scheduler
# Key "serverless" harus sesuai field di RunpodServerlessProperties
runpod.serverless[0].id=${runpod.serverless.id.rembg}
runpod.serverless[0].name=rembg
runpod.serverless[1].id=${runpod.serverless.id.upscaler}
runpod.serverless[1].name=upscaler
```

### Scheduler

```properties
# Interval sync worker status (ms) — default 15 menit
runpod.scheduler.serverless-status.interval=900000
```

### Jackson

```properties
spring.jackson.deserialization.fail-on-unknown-properties=false
spring.jackson.deserialization.accept-empty-string-as-null-object=true
```

### Logging

```properties
logging.level.org.springframework.web=DEBUG   # ubah ke INFO di production
logging.level.org.hibernate.SQL=DEBUG         # ubah ke INFO di production
```

---

## Menjalankan Aplikasi

### 1. Lokal (Maven)

```bash
git clone <repo-url>
cd runpod-worker

cp .env.example src/main/resources/application-local.properties
# Edit application-local.properties

./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Atau build dulu
./mvnw clean package -DskipTests
java -jar target/runpod-worker-*.jar --spring.profiles.active=local
```

### 2. Docker Compose

```bash
cp .env.example .env
# Edit .env

docker compose up --build -d
docker compose logs -f runpod-worker
docker compose down
```

---

## API Endpoint

Base URL: `http://localhost:8003/worker/api`

Endpoint ini dipanggil **oleh RunPod** setelah job selesai — tidak perlu dipanggil manual.

| Method | Path | Deskripsi |
|---|---|---|
| `POST` | `/webhook/upscaler` | Callback dari RunPod untuk job Upscaler |
| `POST` | `/webhook/rembg` | Callback dari RunPod untuk job Remove Background |

### `POST /webhook/upscaler`

```json
{
  "job_id": "2e930e4e-ca9a-4954-9d99-3e5f6a1f10ea-e2",
  "status": "COMPLETED",
  "output_url": "https://s3.../output.png",
  "output_format": "png",
  "output_quality": 90,
  "output_size": [4096, 3072],
  "processing_time": 12.5,
  "scale": 4,
  "webhook_triggered_at": "2026-05-03T04:00:24.053911+00:00"
}
```

### `POST /webhook/rembg`

```json
{
  "job_id": "abc123-rembg-e1",
  "status": "COMPLETED",
  "output_url": "https://s3.../output.png",
  "processing_time": 3.2,
  "webhook_triggered_at": "2026-05-03T04:00:24.053911+00:00"
}
```

> Kedua endpoint langsung return `200 OK` karena proses DB update & Redis publish berjalan `@Async`.

---

## Alur Kerja

### Async Mode (Default)

```
1. CLIENT APP
   └─▶ SET "req-001" → JSON payload di Redis (dengan TTL)
   └─▶ PUBLISH "job-upscaler-request" → "req-001"

2. AbstractConsumer.onMessage()
   └─▶ Terima key = "req-001"
   └─▶ GET "req-001" dari Redis → ambil JSON payload
   └─▶ Parse ke ConsumerRequest<UpscalerRequestDto>
   └─▶ Panggil UpscalerJobService.consume()

3. AbstractJob.consume()
   └─▶ Simpan Job awal ke PostgreSQL
   └─▶ POST ke RunPod: /v2/<id>/run
       Body: { "input": { "image": "...", "scale": 4, ... } }
   └─▶ Response: { "id": "cf893...", "status": "IN_QUEUE" }
   └─▶ Update Job: workerJobId="cf893...", status="IN_QUEUE"

4. RunPod memproses job (async)

5. RunPod POST ke /worker/api/webhook/upscaler:
   { "job_id": "cf893...", "status": "COMPLETED", ... }

6. WebhookController → UpscalerJobService.callback() [@Async]
   └─▶ Cari Job by workerJobId = "cf893..."
   └─▶ Update Job: status="COMPLETED", jobWebhookResponse={...}
   └─▶ SET "cf893..." → JSON ConsumerRequest di Redis (TTL 20 detik)
   └─▶ PUBLISH "job-upscaler-response" → "cf893..."

7. CLIENT APP
   └─▶ Subscribe "job-upscaler-response", terima key "cf893..."
   └─▶ GET "cf893..." dari Redis → ambil hasil
```

### Sync Mode (`call_runpod_sync: true`)

```
1. Client publish dengan call_runpod_sync: true
2. AbstractJob.consume() → POST ke /runsync (blocking, tunggu output)
3. RunPod kembalikan output langsung di response body
4. Jika status != IN_QUEUE → publish hasil ke Redis langsung
5. Webhook tetap diterima, tapi tidak publish ulang
   (karena job sudah dipublish saat consume)
```

---

## Job Types

### 1. Image Upscaler

**Channel In:** `job-upscaler-request` | **Channel Out:** `job-upscaler-response`
**Webhook:** `POST /webhook/upscaler`

```json
{
  "request_id": "req-001",
  "call_runpod_sync": false,
  "data": {
    "image": "s3://bucket/input.jpg",
    "scale": 4,
    "output_format": "png",
    "output_quality": 90
  }
}
```

| Field | Type | Default | Keterangan |
|---|---|---|---|
| `image` | String | — | Path/URL gambar (wajib) |
| `scale` | Integer | `2` | Faktor upscaling: 2 atau 4 |
| `output_format` | String | `"png"` | `png` / `jpg` |
| `output_quality` | Integer | `90` | 1–100 |

---

### 2. Remove Background (Rembg)

**Channel In:** `job-rembg-request` | **Channel Out:** `job-rembg-response`
**Webhook:** `POST /webhook/rembg`

```json
{
  "request_id": "req-002",
  "call_runpod_sync": false,
  "data": {
    "image": "s3://bucket/input.jpg",
    "model": "birefnet-general",
    "output_format": "png",
    "output_quality": 100
  }
}
```

| Field | Type | Default | Keterangan |
|---|---|---|---|
| `image` | String | — | Path/URL gambar (wajib) |
| `model` | String | `"birefnet-general"` | Model AI |
| `output_format` | String | `"png"` | `png` / `jpg` |
| `output_quality` | Integer | `100` | 1–100 |

---

## Mode Sinkron vs Asinkron

Field `call_runpod_sync` di `ConsumerRequest` mengontrol cara job dikirim ke RunPod:

| `call_runpod_sync` | RunPod Endpoint | Publish ke Redis |
|---|---|---|
| `false` / tidak diisi | `/run` (async, non-blocking) | Saat webhook callback diterima |
| `true` | `/runsync` (blocking, tunggu output) | Langsung setelah response RunPod |

> **Gunakan sync mode** jika membutuhkan hasil segera dan RunPod worker tersedia. Perhatikan timeout — `/runsync` bisa gagal jika eksekusi terlalu lama.

---

## Database Schema

### Tabel: `job`

| Kolom | Tipe | Keterangan |
|---|---|---|
| `id` | BIGSERIAL PK | Auto-increment |
| `request_id` | VARCHAR(50) | ID dari client app |
| `worker_job_id` | VARCHAR(50) | Job ID dari RunPod |
| `worker_id` | VARCHAR(20) | Worker ID (diisi saat callback) |
| `job_type` | VARCHAR(10) | `UPSCALER`, `REMBG` |
| `status` | VARCHAR(20) | `IN_QUEUE`, `IN_PROGRESS`, `COMPLETED`, `FAILED` |
| `is_sync` | BOOLEAN | `true` jika job dijalankan secara sinkron |
| `sync_response_time` | TIMESTAMP | Waktu response sync diterima dari RunPod |
| `job_request` | JSONB | Payload yang dikirim ke RunPod |
| `job_response` | JSONB | Response dari RunPod saat submit job |
| `job_webhook_response` | JSONB | Payload webhook callback dari RunPod |
| `execution_time` | FLOAT | Waktu eksekusi di RunPod (detik) |
| `delay_time` | FLOAT | Waktu antri di RunPod (detik) |
| `created` | TIMESTAMP | Auto set saat insert |
| `updated` | TIMESTAMP | Auto update saat save |
| `deleted` | TIMESTAMP | Soft delete (null = aktif) |

> **Soft Delete:** `@SQLRestriction("deleted is null")` — semua query otomatis hanya mengambil record aktif.

---

## Scheduler

### `RunpodEndpointScheduler`

Berjalan setiap **N menit** (default 15 menit) berdasarkan `runpod.scheduler.serverless-status.interval`.

**Yang dilakukan per iterasi:**
1. Baca semua serverless dari `RunpodServerlessProperties` (`runpod.serverless[*]`)
2. Panggil `GET https://rest.runpod.io/v1/endpoints/{id}?includeWorkers=true`
3. Hitung jumlah worker per status
4. Simpan ke Redis Key-Value:

```
RUNPOD:UPSCALER:RUNNING    = "2"
RUNPOD:UPSCALER:EXITED     = "0"
RUNPOD:UPSCALER:TERMINATED = "1"
RUNPOD:REMBG:RUNNING       = "1"
RUNPOD:REMBG:EXITED        = "0"
RUNPOD:REMBG:TERMINATED    = "0"
```

**Konfigurasi:**
```properties
runpod.serverless[0].id=gi2addwjw5pfrx
runpod.serverless[0].name=rembg
runpod.serverless[1].id=nz2i1oqylno3r6
runpod.serverless[1].name=upscaler

runpod.endpoint.url=https://rest.runpod.io/v1/endpoints
runpod.scheduler.serverless-status.interval=900000
```

**Contoh log:**
```
[RunpodEndpointScheduler] Starting scheduler for sync availability status serverless endpoints...
[RunpodEndpointScheduler] Fetching detail for serverless upscaler endpoint(s)...
[RunpodEndpointScheduler] Updated status for serverless (nz2i1oqylno3r6) upscaler : RUNNING=2, EXITED=0, TERMINATED=0
```

---

## Redis Key Convention

| Key Pattern | Value | TTL | Ditulis oleh |
|---|---|---|---|
| `<requestId>` | JSON `ConsumerRequest` | Ditentukan client | Client App |
| `<workerJobId>` | JSON `ConsumerRequest` | 20 detik | `AbstractJob.callback()` |
| `RUNPOD:<NAME>:RUNNING` | jumlah worker (String) | Tidak ada | `RunpodEndpointScheduler` |
| `RUNPOD:<NAME>:EXITED` | jumlah worker (String) | Tidak ada | `RunpodEndpointScheduler` |
| `RUNPOD:<NAME>:TERMINATED` | jumlah worker (String) | Tidak ada | `RunpodEndpointScheduler` |

> **Pola Redis Message:** Consumer tidak menerima payload langsung di channel — hanya menerima **key**. Jika key sudah expired saat consumer membaca, pesan akan di-skip dengan `log.warn("Message expired")`.

---

## Menambahkan Job Type Baru

Contoh: menambahkan `VectorizeJob`.

### 1. Enum

```java
// enums/JobType.java
VECTORIZE("VECTORIZE");
```

### 2. DTO Request & Response

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VectorizeRequestDto {
    private String image;
    private String outputFormat = "svg";
}

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VectorizeResponseDto extends JobWebhookRequestDto {
    @JsonProperty("output_url")
    private String outputUrl;
    @JsonProperty("webhook_triggered_at")
    private OffsetDateTime webhookTriggeredAt;
}
```

### 3. Service

```java
@Slf4j @Service
public class VectorizeJobService extends AbstractJob<VectorizeRequestDto, VectorizeResponseDto> {

    @Value("${runpod.serverless.vectorize.url}")
    private String url;
    @Value("${redis.channel.job-vectorize-response}")
    private String redisChannel;

    public VectorizeJobService(HttpRequest httpRequest, JobRepository jobRepository,
                                RedisPublisher redisPublisher, ObjectMapper objectMapper) {
        super(httpRequest, jobRepository, redisPublisher, objectMapper);
    }

    @Override public String getRedisChannelPublishName() { return redisChannel; }
    @Override public String getRunpodUrl() { return url; }
    @Override public JobType getJobType() { return JobType.VECTORIZE; }
}
```

### 4. Consumer

```java
@Component
public class JobVectorizeConsumer extends AbstractConsumer<VectorizeRequestDto> {
    public JobVectorizeConsumer(VectorizeJobService service, ObjectMapper objectMapper,
                                 RedisTemplate<String, String> redisTemplate) {
        super(service, objectMapper, redisTemplate);
    }
}
```

### 5. Daftarkan di `RedisConfig.java`

```java
@Value("${redis.channel.job-vectorize-request}")
private String jobVectorizeChannel;

// Di redisMessageListenerContainer():
container.addMessageListener(
    new MessageListenerAdapter(jobVectorizeConsumer, "onMessage"),
    new ChannelTopic(jobVectorizeChannel)
);
```

### 6. Webhook di `WebhookController.java`

```java
private final VectorizeJobService vectorizeJobService;

@PostMapping("/vectorize")
public String vectorizeWebhook(@RequestBody VectorizeResponseDto dto) {
    vectorizeJobService.callback(dto.getJobId(), dto.getStatus(), dto);
    return "Webhook received successfully";
}
```

### 7. Properties

```properties
runpod.serverless.id.vectorize=<ENDPOINT_ID>
runpod.serverless.vectorize.url=${runpod.base-url}/v2/${runpod.serverless.id.vectorize}
redis.channel.job-vectorize-request=job-vectorize-request
redis.channel.job-vectorize-response=job-vectorize-response

# Tambahkan ke scheduler
runpod.serverless[2].id=${runpod.serverless.id.vectorize}
runpod.serverless[2].name=vectorize
```

---

## Logging

### HTTP Request/Response Masuk (`HttpLoggingFilter`)

```
>>> INCOMING REQUEST
    Method  : POST
    URI     : /worker/api/webhook/upscaler
    Headers : Content-Type=application/json
    Body    : {"job_id":"cf893...","status":"COMPLETED","output_url":"..."}

<<< OUTGOING RESPONSE
    URI        : /worker/api/webhook/upscaler
    Status     : 200
    Execute-In : 45 ms
    Body       : Webhook received successfully
```

### HTTP Request Keluar ke RunPod (`RestTemplateInterceptor`)

```
SENDING REQUEST ...
URI         : https://api.runpod.ai/v2/.../run
Method      : POST
Request body: {"input":{"image":"...","scale":4}}

HTTP Logs:
Status       = 200 OK
Execute-In   = 402 ms
Resp body    = {"id":"cf893...","status":"IN_QUEUE"}
```

> **Production:** Kurangi verbosity log:
> ```properties
> logging.level.org.springframework.web=INFO
> logging.level.org.hibernate.SQL=INFO
> ```

---

## Catatan Teknis

### Dual Jackson Version

```
Jackson 3.x (tools.jackson.*)
└── Spring MVC: @RequestBody, @ResponseBody
└── Consumer & Service: ObjectMapper, TypeReference
└── @JsonNaming hanya dikenali dari package ini di Spring Boot 4.x

Jackson 2.x (com.fasterxml.jackson.*)
└── Hibernate: kolom @JdbcTypeCode(SqlTypes.JSON) / JSONB di PostgreSQL
└── WAJIB ada di classpath — tanpa ini: HibernateException: FormatMapper not found
```

### Snake Case Mapping

- `@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)` dari `tools.jackson` untuk Spring MVC
- Harus diterapkan di **setiap class dalam hierarki** (parent & child)
- Untuk field di parent class, gunakan `@JsonProperty` eksplisit agar lebih reliable

### Async Callback

`AbstractJob.callback()` diberi `@Async` agar webhook dari RunPod langsung mendapat `200 OK` tanpa menunggu proses DB update dan Redis publish selesai.

### Redis Message Pattern

Consumer tidak menerima payload langsung di channel — hanya menerima **key**. Alasannya:
- Menghindari limitasi ukuran pesan Pub/Sub
- Memungkinkan consumer retry membaca data
- Jika key expired → pesan di-skip dengan `log.warn` (tidak error)
