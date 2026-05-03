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
- [Database Schema](#database-schema)
- [Menambahkan Job Type Baru](#menambahkan-job-type-baru)
- [Logging](#logging)

---

## Arsitektur

```
┌──────────────────────────────────────────────────────────────────┐
│                         CLIENT APP                               │
└─────────────────────────┬────────────────────────────────────────┘
                          │ Publish ke Redis Channel
                          ▼
┌──────────────────────────────────────────────────────────────────┐
│                      REDIS PUB/SUB                               │
│   job-upscaler-request  │  job-rembg-request                     │
└─────────────────────────┬────────────────────────────────────────┘
                          │ Subscribe
                          ▼
┌──────────────────────────────────────────────────────────────────┐
│                    RUNPOD WORKER (App ini)                        │
│                                                                  │
│  ┌──────────────────┐    ┌─────────────────────────────────┐    │
│  │  Redis Consumer  │───▶│  AbstractJob.consume()          │    │
│  │  (Pub/Sub)       │    │  - Kirim job ke RunPod API      │    │
│  └──────────────────┘    │  - Simpan Job ke PostgreSQL     │    │
│                          └─────────────────────────────────┘    │
│  ┌──────────────────┐    ┌─────────────────────────────────┐    │
│  │ WebhookController│───▶│  AbstractJob.callback()         │    │
│  │ POST /webhook/*  │    │  - Update Job di PostgreSQL     │    │
│  └──────────────────┘    │  - Publish hasil ke Redis       │    │
│                          └─────────────────────────────────┘    │
└───────────────────┬──────────────────────┬───────────────────────┘
                    │                      │
                    ▼                      ▼
        ┌───────────────────┐   ┌──────────────────────┐
        │   RUNPOD API      │   │    POSTGRESQL         │
        │  (GPU Workers)    │   │  (Job Tracking)       │
        └──────────┬────────┘   └──────────────────────┘
                   │ Webhook Callback
                   ▼
        ┌──────────────────┐
        │  REDIS PUB/SUB   │
        │  job-*-response  │──▶ CLIENT APP
        └──────────────────┘
```

---

## Fitur

- 🚀 **Multi Job Type** — Mendukung Upscaler dan Remove Background (Rembg), mudah diperluas
- 📨 **Redis Pub/Sub Integration** — Terima job via Redis channel, kirim hasil ke Redis channel
- 🔗 **RunPod API Integration** — Integrasi langsung dengan RunPod serverless endpoint
- 🪝 **Webhook Callback** — Terima callback asynchronous dari RunPod setelah job selesai
- 💾 **Job Tracking** — Setiap job disimpan di PostgreSQL dengan status dan payload lengkap
- 📝 **HTTP Logging** — Log otomatis setiap request dan response HTTP yang masuk
- 🔄 **Async Callback Processing** — Callback diproses secara asynchronous dengan `@Async`

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
| Spring Data Redis | 4.0.1 | Redis Pub/Sub & Template |
| Hibernate | 7.2.0 | JPA implementation |
| Jackson 3.x (tools.jackson) | 3.0.3 | JSON serialization (Spring MVC) |
| Jackson 2.x (com.fasterxml) | 2.17.1 | JSON serialization (Hibernate JSONB) |
| PostgreSQL Driver | latest | Database driver |
| Lombok | latest | Boilerplate reduction |

> **Catatan:** Proyek ini menggunakan **dua versi Jackson** secara bersamaan:
> - **Jackson 3.x** (`tools.jackson`) → digunakan oleh Spring MVC untuk `@RequestBody`/`@ResponseBody`
> - **Jackson 2.x** (`com.fasterxml.jackson`) → digunakan oleh Hibernate untuk mapping kolom `JSONB`

---

## Struktur Proyek

```
runpod-worker/
├── src/main/java/id/rockierocker/runpodworker/
│   ├── RunpodWorkerApplication.java       # Entry point
│   │
│   ├── component/
│   │   ├── HttpRequest.java               # Wrapper untuk HTTP calls ke RunPod
│   │   └── RedisPublisherService.java     # Service untuk publish pesan ke Redis
│   │
│   ├── config/
│   │   ├── RedisConfig.java               # Konfigurasi Redis template & listener
│   │   ├── RestTemplateConfig.java        # Konfigurasi RestTemplate + interceptor
│   │   ├── RestTemplateInterceptor.java   # Logging setiap HTTP request/response keluar
│   │   └── HttpLoggingFilter.java         # Logging setiap HTTP request/response masuk
│   │
│   ├── consumer/
│   │   └── JobUpscalerConsumer.java       # Redis subscriber untuk job upscaler
│   │
│   ├── controller/
│   │   └── WebhookController.java         # Endpoint untuk menerima callback dari RunPod
│   │
│   ├── dto/
│   │   ├── ConsumerRequest.java           # Wrapper pesan Redis (requestId + data)
│   │   ├── HttpRequestDto.java            # Parameter untuk HTTP call
│   │   ├── JobRequest.java                # Payload yang dikirim ke RunPod
│   │   ├── JobResponse.java               # Response saat job dibuat di RunPod
│   │   ├── RembgRequestDto.java           # Parameter job Remove Background
│   │   ├── RembgResponseDto.java          # Response webhook job Remove Background
│   │   ├── UpscalerRequestDto.java        # Parameter job Upscaler
│   │   └── UpscalerResponseDto.java       # Response webhook job Upscaler
│   │
│   ├── entity/
│   │   ├── BaseEntity.java                # Base entity (created, updated, deleted)
│   │   └── Job.java                       # Entity tabel job
│   │
│   ├── enums/
│   │   └── JobType.java                   # Enum tipe job (UPSCALER, REMBG)
│   │
│   ├── repository/
│   │   └── JobRepository.java             # JPA repository untuk Job entity
│   │
│   └── service/
│       ├── AbstractJob.java               # Abstract base class untuk semua job
│       ├── UpscalerJobService.java        # Service untuk job Image Upscaler
│       └── RembgJobService.java           # Service untuk job Remove Background
│
├── src/main/resources/
│   ├── application.properties             # Konfigurasi utama
│   └── application-local.properties      # Konfigurasi lokal (dev)
│
├── Dockerfile                             # Docker image definition
├── docker-compose.yml                     # Docker Compose setup
├── .env.example                           # Contoh environment variables
└── pom.xml                                # Maven dependencies
```

---

## Konfigurasi

### `application.properties` / `application-local.properties`

Salin `.env.example` menjadi `.env` dan sesuaikan nilainya:

```bash
cp .env.example .env
```

#### Database

```properties
spring.datasource.url=jdbc:postgresql://<HOST>:<PORT>/<DATABASE>
spring.datasource.username=<USERNAME>
spring.datasource.password=<PASSWORD>
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update    # create / update / validate / none
```

#### Redis

```properties
spring.data.redis.host=<HOST>
spring.data.redis.port=6379
spring.data.redis.password=<PASSWORD>   # kosongkan jika tidak ada password
spring.data.redis.timeout=5000ms

# Channel names (sesuaikan dengan client app)
redis.channel.job-upscaler-request=job-upscaler-request
redis.channel.job-upscaler-response=job-upscaler-response
redis.channel.job-rembg-request=job-rembg-request
redis.channel.job-rembg-response=job-rembg-response
```

#### RunPod

```properties
runpod.api-token=<RUNPOD_API_TOKEN>
runpod.worker.upscaler.url=https://api.runpod.ai/v2/<ENDPOINT_ID>/run
runpod.worker.rembg.url=https://api.runpod.ai/v2/<ENDPOINT_ID>/run
```

> RunPod API Token bisa didapat di [RunPod Settings → API Keys](https://www.runpod.io/console/user/settings).

#### Jackson

```properties
spring.jackson.deserialization.fail-on-unknown-properties=false
spring.jackson.deserialization.accept-empty-string-as-null-object=true
```

#### Logging

```properties
logging.level.org.springframework.web=DEBUG   # ubah ke INFO di production
logging.level.org.hibernate.SQL=DEBUG         # ubah ke INFO di production
```

---

## Menjalankan Aplikasi

### 1. Lokal (Maven)

```bash
# Clone repository
git clone <repo-url>
cd runpod-worker

# Copy dan edit konfigurasi
cp .env.example src/main/resources/application-local.properties
# Edit application-local.properties sesuai environment lokal

# Jalankan dengan profile local
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Atau build terlebih dahulu
./mvnw clean package -DskipTests
java -jar target/runpod-worker-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

### 2. Docker Compose

```bash
# Copy dan edit environment file
cp .env.example .env
# Edit .env sesuai environment

# Build dan jalankan
docker compose up --build -d

# Lihat logs
docker compose logs -f runpod-worker

# Stop
docker compose down
```

#### `.env` untuk Docker Compose

```dotenv
server.port=8003
spring.datasource.url=jdbc:postgresql://192.168.1.1:5432/runpodworker
spring.datasource.username=user
spring.datasource.password=secret
spring.data.redis.host=192.168.1.1
spring.data.redis.port=6379
runpod.api-token=rpa_xxxxxxxxxxxx
runpod.worker.upscaler.url=https://api.runpod.ai/v2/<ENDPOINT_ID>/run
runpod.worker.rembg.url=https://api.runpod.ai/v2/<ENDPOINT_ID>/run

# Redis channels
redis.channel.job-upscaler-request=job-upscaler-request
redis.channel.job-upscaler-response=job-upscaler-response
redis.channel.job-rembg-request=job-rembg-request
redis.channel.job-rembg-response=job-rembg-response
```

### 3. Verifikasi

```bash
# Cek health
curl http://localhost:8003/worker/api/actuator/health

# Atau lihat log startup
# Harus muncul: "Started RunpodWorkerApplication in X seconds"
```

---

## API Endpoint

Base URL: `http://localhost:8003/worker/api`

### Webhook

Endpoint ini dipanggil **oleh RunPod** setelah job selesai diproses. Tidak perlu dipanggil manual oleh client.

| Method | Path | Deskripsi |
|---|---|---|
| `POST` | `/webhook/upscaler` | Callback dari RunPod untuk job Image Upscaler |
| `POST` | `/webhook/rembg` | Callback dari RunPod untuk job Remove Background |

#### `POST /webhook/upscaler`

RunPod akan memanggil endpoint ini setelah job upscaler selesai.

**Request Body:**
```json
{
  "job_id": "2e930e4e-ca9a-4954-9d99-3e5f6a1f10ea-e2",
  "status": "COMPLETED",
  "format": "png",
  "original_size": [1024, 768],
  "output_format": "png",
  "output_quality": 90,
  "output_size": [4096, 3072],
  "output_url": "https://s3.../output.png",
  "processing_time": 12.5,
  "scale": 4,
  "input_storage_mode": "s3",
  "output_storage_mode": "s3",
  "webhook_triggered_at": "2026-05-03T04:00:24.053911+00:00"
}
```

**Response:**
```
Webhook received successfully
```

#### `POST /webhook/rembg`

RunPod akan memanggil endpoint ini setelah job remove background selesai.

**Request Body:**
```json
{
  "job_id": "abc123-rembg-e1",
  "status": "COMPLETED",
  "output_url": "https://s3.../output.png",
  "processing_time": 3.2,
  "webhook_triggered_at": "2026-05-03T04:00:24.053911+00:00"
}
```

> **Catatan:** Untuk mengaktifkan webhook di RunPod, set `webhook` URL saat submit job. RunPod akan POST ke URL tersebut saat job selesai.

---

## Alur Kerja

### Alur Lengkap — Upscaler Job

```
1. CLIENT APP
   └─▶ Publish ke Redis channel "job-upscaler-request":
       {
         "request_id": "req-001",
         "data": {
           "image": "s3://bucket/input.jpg",
           "scale": 4,
           "output_format": "png",
           "output_quality": 90
         }
       }

2. JobUpscalerConsumer.onMessage()
   └─▶ Parse JSON ke ConsumerRequest<UpscalerRequestDto>
   └─▶ Panggil UpscalerJobService.consume()

3. AbstractJob.consume()
   └─▶ Bungkus data ke JobRequest { "input": { ... } }
   └─▶ POST ke RunPod API: https://api.runpod.ai/v2/<endpoint>/run
       Authorization: Bearer <token>
       Body: { "input": { "image": "...", "scale": 4, ... } }
   └─▶ RunPod response: { "id": "cf893...", "status": "IN_QUEUE" }
   └─▶ Simpan Job ke PostgreSQL:
       - request_id = "req-001"
       - worker_job_id = "cf893..."
       - status = "IN_QUEUE"
       - job_type = "UPSCALER"
       - job_request = { ... } (JSONB)

4. RunPod memproses job (async, bisa makan waktu)

5. RunPod POST Webhook ke /worker/api/webhook/upscaler:
   { "job_id": "cf893...", "status": "COMPLETED", "output_url": "...", ... }

6. WebhookController.upscalerReceiveWebhook()
   └─▶ Panggil UpscalerJobService.callback() [async - @Async]

7. AbstractJob.callback()
   └─▶ Cari Job di DB berdasarkan worker_job_id = "cf893..."
   └─▶ Update Job:
       - status = "COMPLETED"
       - job_webhook_response = { ... } (JSONB)
   └─▶ Publish ke Redis channel "job-upscaler-response":
       {
         "request_id": "req-001",
         "data": { "job_id": "cf893...", "output_url": "...", ... }
       }

8. CLIENT APP
   └─▶ Subscribe ke "job-upscaler-response", terima hasil
```

---

## Job Types

### 1. Image Upscaler

Meningkatkan resolusi gambar menggunakan model AI di RunPod.

**Channel In:** `job-upscaler-request`
**Channel Out:** `job-upscaler-response`
**Webhook:** `POST /webhook/upscaler`

**Input (`data` field di ConsumerRequest):**
```json
{
  "image": "s3://bucket/path/to/input.jpg",
  "scale": 4,
  "output_format": "png",
  "output_quality": 90
}
```

| Field | Type | Default | Keterangan |
|---|---|---|---|
| `image` | String | - | Path/URL gambar input (wajib) |
| `scale` | Integer | `2` | Faktor upscaling (2, 4) |
| `output_format` | String | `"png"` | Format output: `png`, `jpg` |
| `output_quality` | Integer | `90` | Kualitas output (1-100) |

---

### 2. Remove Background (Rembg)

Menghapus background gambar menggunakan model AI di RunPod.

**Channel In:** `job-rembg-request`
**Channel Out:** `job-rembg-response`
**Webhook:** `POST /webhook/rembg`

**Input (`data` field di ConsumerRequest):**
```json
{
  "image": "s3://bucket/path/to/input.jpg",
  "model": "birefnet-general",
  "output_format": "png",
  "output_quality": 100
}
```

| Field | Type | Default | Keterangan |
|---|---|---|---|
| `image` | String | - | Path/URL gambar input (wajib) |
| `model` | String | `"birefnet-general"` | Model AI yang digunakan |
| `output_format` | String | `"png"` | Format output: `png`, `jpg` |
| `output_quality` | Integer | `100` | Kualitas output (1-100) |

---

## Database Schema

### Tabel: `job`

| Kolom | Tipe | Keterangan |
|---|---|---|
| `id` | BIGSERIAL PK | Auto-increment primary key |
| `request_id` | VARCHAR(50) | ID unik dari client app |
| `worker_job_id` | VARCHAR(50) | Job ID yang dikembalikan RunPod |
| `worker_id` | VARCHAR(20) | Worker ID dari RunPod (diisi saat callback) |
| `job_type` | VARCHAR(10) | Tipe job: `UPSCALER`, `REMBG` |
| `status` | VARCHAR(20) | Status: `IN_QUEUE`, `IN_PROGRESS`, `COMPLETED`, `FAILED` |
| `job_request` | JSONB | Payload yang dikirim ke RunPod |
| `job_response` | JSONB | Response dari RunPod saat submit |
| `job_webhook_response` | JSONB | Data webhook callback dari RunPod |
| `execution_time` | FLOAT | Waktu eksekusi di RunPod (detik) |
| `delay_time` | FLOAT | Waktu antri di RunPod (detik) |
| `created` | TIMESTAMP | Waktu dibuat (auto) |
| `updated` | TIMESTAMP | Waktu terakhir diupdate (auto) |
| `deleted` | TIMESTAMP | Soft delete timestamp |

> **Soft Delete:** Tabel menggunakan soft delete via `@SQLRestriction("deleted is null")`. Record yang di-delete hanya akan di-set `deleted = NOW()`, tidak benar-benar dihapus dari database.

---

## Menambahkan Job Type Baru

Ikuti langkah berikut untuk menambahkan job type baru (contoh: `VectorizeJob`):

### 1. Tambahkan enum di `JobType.java`

```java
public enum JobType {
    UPSCALER("UPSCALER"),
    REMBG("REMBG"),
    VECTORIZE("VECTORIZE");   // ← tambahkan ini
    // ...
}
```

### 2. Buat DTO Request

```java
// dto/VectorizeRequestDto.java
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data @Builder @AllArgsConstructor @RequiredArgsConstructor
public class VectorizeRequestDto {
    private String image;
    private String outputFormat = "svg";
}
```

### 3. Buat DTO Response (untuk webhook)

```java
// dto/VectorizeResponseDto.java
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class VectorizeResponseDto extends JobWebhookResponseDto {
    private String outputUrl;
    private OffsetDateTime webhookTriggeredAt;
}
```

### 4. Buat Service

```java
// service/VectorizeJobService.java
@Slf4j
@Service
public class VectorizeJobService extends AbstractJob<VectorizeRequestDto, VectorizeResponseDto> {

    @Value("${runpod.worker.vectorize.url}")
    private String runpodWorkerUrl;

    @Value("${redis.channel.job-vectorize-response}")
    private String redisChannel;

    public VectorizeJobService(HttpRequest httpRequest, JobRepository jobRepository,
                                RedisPublisherService redisPublisherService, ObjectMapper objectMapper) {
        super(httpRequest, jobRepository, redisPublisherService, objectMapper);
    }

    @Override public String getRedisChannelPublishName() { return redisChannel; }
    @Override public String getRunpodUrl() { return runpodWorkerUrl; }
    @Override public JobType getJobType() { return JobType.VECTORIZE; }
}
```

### 5. Buat Consumer

```java
// consumer/JobVectorizeConsumer.java
@Slf4j @Component @RequiredArgsConstructor
public class JobVectorizeConsumer implements MessageListener {
    private final ObjectMapper objectMapper;
    private final VectorizeJobService vectorizeService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            ConsumerRequest<VectorizeRequestDto> req = objectMapper.readValue(
                new String(message.getBody()),
                new TypeReference<ConsumerRequest<VectorizeRequestDto>>() {}
            );
            vectorizeService.consume(req);
        } catch (Exception e) {
            log.error("Failed to process message: {}", e.getMessage(), e);
        }
    }
}
```

### 6. Daftarkan Consumer di `RedisConfig.java`

```java
@Value("${redis.channel.job-vectorize-request}")
private String jobVectorizeChannel;

@Bean public ChannelTopic jobVectorizeTopic() {
    return new ChannelTopic(jobVectorizeChannel);
}

@Bean
public MessageListenerAdapter jobVectorizeListenerAdapter(JobVectorizeConsumer consumer) {
    return new MessageListenerAdapter(consumer, "onMessage");
}

// Tambahkan di redisMessageListenerContainer():
container.addMessageListener(jobVectorizeListenerAdapter, jobVectorizeTopic());
```

### 7. Tambahkan Webhook Endpoint di `WebhookController.java`

```java
private final VectorizeJobService vectorizeJobService;

@PostMapping("/vectorize")
public String vectorizeReceiveWebhook(@RequestBody VectorizeResponseDto dto) {
    vectorizeJobService.callback(dto.getJobId(), dto.getStatus(), dto);
    return "Webhook received successfully";
}
```

### 8. Tambahkan konfigurasi di `.env` / `application.properties`

```properties
runpod.worker.vectorize.url=https://api.runpod.ai/v2/<ENDPOINT_ID>/run
redis.channel.job-vectorize-request=job-vectorize-request
redis.channel.job-vectorize-response=job-vectorize-response
```

---

## Logging

### HTTP Request/Response Masuk

Semua request yang masuk ke aplikasi otomatis di-log oleh `HttpLoggingFilter`:

```
>>> INCOMING REQUEST
    Method  : POST
    URI     : /worker/api/webhook/upscaler
    Query   : null
    Headers : Accept=application/json, Content-Type=application/json
    Body    : {"job_id":"cf893...","status":"COMPLETED","output_url":"..."}

<<< OUTGOING RESPONSE
    URI        : /worker/api/webhook/upscaler
    Status     : 200
    Execute-In : 45 ms
    Body       : Webhook received successfully
```

### HTTP Request Keluar (ke RunPod)

Setiap HTTP call ke RunPod di-log oleh `RestTemplateInterceptor`:

```
SENDING REQUEST ...
URI         : https://api.runpod.ai/v2/.../run
Method      : POST
Headers     : [Authorization:"Bearer rpa_...", Content-Type:"application/json"]
Request body: {"input":{"image":"...","scale":4}}

HTTP Logs:
Status       = 200 OK
Execute-In   = 402 ms
Resp body    = {"id":"cf893...","status":"IN_QUEUE"}
```

> **Production:** Ubah log level ke `INFO` untuk mengurangi verbosity:
> ```properties
> logging.level.org.springframework.web=INFO
> logging.level.org.hibernate.SQL=INFO
> ```

---

## Catatan Teknis

### Dual Jackson Version

Project ini menggunakan dua versi Jackson yang berbeda secara bersamaan:

```
Jackson 3.x (tools.jackson.*)
└── Digunakan oleh: Spring MVC (RestTemplate, @RequestBody, @ResponseBody)
└── @JsonNaming hanya dikenali dari package ini di Spring Boot 4.x

Jackson 2.x (com.fasterxml.jackson.*)
└── Digunakan oleh: Hibernate untuk kolom @JdbcTypeCode(SqlTypes.JSON) / JSONB
└── Dependency ini wajib ada agar Hibernate bisa baca/tulis kolom JSONB
```

### Snake Case Mapping

DTO yang me-mapping JSON snake_case dari/ke Java camelCase menggunakan:
- `@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)` dari `tools.jackson` untuk Spring MVC
- Class-level annotation di **semua class dalam hierarki** (parent & child) agar mapping berjalan konsisten

### Async Callback

Method `callback()` di `AbstractJob` diberi anotasi `@Async` agar webhook dari RunPod langsung mendapat response HTTP `200` tanpa menunggu proses DB update dan Redis publish selesai.

