RDRCT — Production-Grade URL Shortener
A complete, production-minded URL shortening platform (Bitly/TinyURL-class) built with
Spring Boot, PostgreSQL, Redis, and Kafka — designed and documented as a system-design
portfolio piece, and built incrementally in four shippable phases rather than as one
unfinished "Google-scale" attempt.
> **Built by:** [Asad Khan](https://github.com/meranaamkhann) — Final Year Project
> **Stack:** Java 21 · Spring Boot 3 · PostgreSQL 16 · Redis 7 · Apache Kafka · Docker · Kubernetes
🔗 Live demo: https://url-shortener-production-21.up.railway.app
📄 Repo: https://github.com/meranaamkhann/url-shortner
---
Screenshots
<!-- Add 2–4 screenshots here before sharing this repo — the landing page, the
     dashboard with a couple of links in it, and the QR/analytics view are the
Landing	Dashboard
(add screenshot)	(add screenshot)
---
Why this project is structured the way it is
Every folder, every table, every design decision in this repo is meant to be
defensible in an interview — not just "it works," but "here's why I built it this
way, here's what I traded off, and here's what I'd do differently at 10x the scale."
The companion docs in `/docs` go deep on each of those decisions. Start there
if you want the reasoning, not just the code.
Document	What's in it
`docs/ARCHITECTURE.md`	High-level architecture, request flow, caching/scaling strategy, short-code generation trade-offs
`docs/DATABASE_SCHEMA.md`	ER diagram, table-by-table rationale, indexing/partitioning/sharding strategy
`docs/API.md`	Full REST API reference with examples
`docs/SECURITY.md`	JWT/RBAC design, every "edge case" requirement mapped to its defense
`docs/TESTING.md`	Test pyramid, what's covered where, how to run each tier
`docs/DEPLOYMENT.md`	Docker, Kubernetes, CI/CD pipeline walkthrough
`docs/INTERVIEW_QA.md`	25+ likely interview questions about this exact project, answered
---
Functional coverage
✅ Shorten long URLs · ✅ Redirect short URLs · ✅ Custom aliases · ✅ URL expiration
(time-based and click-count-based) · ✅ URL analytics (totals, by-country, by-device,
top referrers, 30-day trend) · ✅ Click tracking (with bot filtering) · ✅ QR code
generation · ✅ JWT user authentication + RBAC · ✅ Public/private links · ✅ Password-
protected links (with a real browser unlock page, not just an API error) · ✅ URL
editing · ✅ URL disabling/enabling · ✅ Soft delete + scheduled hard delete · ✅ Bulk
URL creation (up to 100/request) · ✅ Custom domain support (schema + service layer) ·
✅ Link preview (OpenGraph metadata, SSRF-guarded) · ✅ Dark-themed responsive frontend
Non-functional coverage
High availability (multi-replica + PDB + topology spread) · Fault tolerance (cache-miss
fallback, fail-open rate limiter, Kafka decoupling, non-blocking async click publishing)
· Horizontal scalability (stateless JWT auth, HPA) · Low latency (Redis cache-aside on
the redirect hot path) · Security (see `docs/SECURITY.md`) ·
Observability (Actuator, Prometheus, Micrometer tracing, structured logs) ·
Maintainability (layered architecture, DTOs, global exception handling, Flyway-versioned
schema) · CI/CD (GitHub Actions: build, test, dependency/vulnerability scan, Docker
image publish to GHCR)
---
Build phases (how this was actually built, and how you should explain it)
This was deliberately not built as one monolithic "do everything" sprint. It was
built in four phases, each independently runnable and demoable — this is also how
I'd recommend walking an interviewer through it.
Phase 1 — Core (Spring Boot + PostgreSQL + JWT)
Shortening, redirect, registration/login/refresh, RBAC skeleton, global exception
handling, Flyway schema. Runnable with just Postgres.
Phase 2 — Product features
Analytics (raw events + daily rollups), custom aliases, expiration (time + click-count
based), URL editing/disabling, soft delete, bulk create, QR codes, link preview.
Phase 3 — Performance (Redis)
Cache-aside on the redirect hot path, negative caching against enumeration/brute-force
probing, cache eviction on mutation. Docker Compose stands up the full stack.
Phase 4 — Scale & Ops (Kafka + Rate Limiting + Observability)
Click events and audit events move off the synchronous request path onto Kafka.
Distributed, Redis-backed rate limiting (fixed-window, atomic Lua script). Prometheus
metrics, Grafana dashboards, alert rules, distributed tracing hooks.
---
About the live demo vs. the full stack in this repo
The live demo runs app + PostgreSQL + Redis only on Railway. Kafka, Zookeeper,
Prometheus, and Grafana are real, fully implemented, and runnable (see Quick Start
below) — they're just not worth running 24/7 for a low-traffic public demo. Practical
effect on the live demo: click analytics won't populate (the Kafka consumer that writes
them has nothing to connect to), and geo-location on click events is currently a
documented stub returning `"Unknown"` regardless (see `GeoLocationService` javadoc —
wiring in a real MaxMind GeoIP lookup is a clean drop-in, not a redesign). Every other
feature — auth, shortening, redirects, rate limiting, caching, password-protected
links, QR codes — runs exactly as it does locally.
---
Quick start (local, full stack)
Prerequisites: Docker + Docker Compose. That's it — Postgres, Redis, Kafka,
Prometheus, and Grafana are all provisioned for you.
```bash
git clone https://github.com/meranaamkhann/url-shortner.git
cd url-shortner/docker
docker compose up -d --build
```
Then:
App: http://localhost:8080
API docs: http://localhost:8080/swagger-ui.html
Health: http://localhost:8080/actuator/health
Prometheus: http://localhost:9090
Grafana: http://localhost:3000 (admin/admin)
Quick start (bare Java, Phase 1 only)
```bash
# Requires a local Postgres at localhost:5432 (see application.yml for credentials)
cd backend
mvn clean package -DskipTests
java -jar target/url-shortener.jar
```
Try it
```bash
# Shorten a URL (works anonymously)
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{"longUrl": "https://en.wikipedia.org/wiki/System_design"}'

# -> {"shortCode": "aB3xK9q", "shortUrl": "http://localhost:8080/r/aB3xK9q", ...}

curl -i http://localhost:8080/r/aB3xK9q
# -> HTTP/1.1 302 Found
# -> Location: https://en.wikipedia.org/wiki/System_design
```
---
Running the tests
```bash
cd backend
mvn test            # fast unit tests (no external dependencies, runs in seconds)
mvn verify           # + integration tests (Testcontainers spins up real Postgres/Redis)
```
See `docs/TESTING.md` for the full breakdown of what's tested
at each layer and why.
---
Deploying your own copy
The app deploys cleanly to Railway (app + managed Postgres + managed Redis — see
`application-prod.yml` for every environment variable it expects) or to a real
Kubernetes cluster using the manifests in `/k8s` plus the `deploy` jobs
in `.github/workflows/cd.yml`. See
`docs/DEPLOYMENT.md` for the full walkthrough either way.