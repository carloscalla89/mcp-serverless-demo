# MCP Serverless Demo — MCP + Quarkus 3.40 + Cloud Run

MCP server en Java construido con **Quarkus 3.40 (LTS)**, **Java 21**, **Clean Architecture** y programación **imperativa**, expuesto por **Streamable HTTP** (`/mcp`) y pensado para desplegarse en **Google Cloud Run** como **imagen nativa** (cold start en milisegundos).

Demo para la conferencia **“MCP serverless en Java con Quarkus”**.

---

## 1. Arquitectura

```
Cliente MCP (Claude / Cursor / Inspector / Postman)
        │  JSON-RPC sobre Streamable HTTP
        ▼
┌───────────────────────────────────────────────┐
│ /mcp  →  infrastructure/input/mcp             │
│          FinanceMcpTools (@Tool)              │
│                     │                         │
│                     ▼                         │
│          application (use cases)              │
│        LoanQuoteUseCase │ CurrencyQuoteUseCase│
│             │                     │           │
│             ▼                     ▼           │
│   domain/LoanCalculator   domain/CurrencyRateProvider (puerto)
│             │                     │           │
│             │                     ▼           │
│             │      infrastructure/output/client
│             │      CurrencyRateClient ──► open.er-api.com
└───────────────────────────────────────────────┘
```

Las dependencias apuntan **solo hacia adentro**: `domain` no conoce `application` ni `infrastructure`; los adaptadores (MCP y REST Client) implementan/invocan el dominio.

### Estructura de paquetes

```
pe.com.incoda.mcpserverlessdemo
├── domain
│   ├── model      LoanRequest, LoanQuote, ExchangeQuote   (records)
│   └── service    LoanCalculator (puro), CurrencyRateProvider (puerto)
├── application    LoanQuoteUseCase, CurrencyQuoteUseCase
└── infrastructure
    ├── input/mcp      FinanceMcpTools
    └── output/client  CurrencyRateApi, CurrencyRateClient, CurrencyRateResponse
```

### Tools expuestas

| Tool | Tipo | Descripción |
|---|---|---|
| `calcular_cuota_prestamo` | Dominio puro | Cuota fija, interés total y total a pagar (sistema francés). |
| `cotizacion_divisa` | Adapter de salida | Tipo de cambio vía REST Client a open.er-api.com (sin API key; más de 160 divisas, incluye PEN). |

> Imperativo: los métodos `@Tool` devuelven `ToolResponse` (tipo no asíncrono), por lo que el runtime MCP los ejecuta fuera del event loop; además se anotan con `@RunOnVirtualThread`.

---

## 2. Requisitos

- **Java 21**
- **Maven 3.9+** (o el wrapper `./mvnw`)
- **Docker** para construir la imagen nativa (Mandrel 25)
- **gcloud CLI** para el despliegue
- Opcional: `npx` para MCP Inspector

---

## 3. Ejecutar localmente

```bash
./mvnw quarkus:dev
```

- Endpoint MCP: `http://localhost:8080/mcp`
- Dev UI: `http://localhost:8080/q/dev-ui`
- En dev se habilita el **traffic logging** JSON-RPC y CORS para el Inspector.

### Probar con curl

```bash
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
```

```bash
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"calcular_cuota_prestamo","arguments":{"principal":100000,"tasaAnualPct":12,"meses":36}}}'
```

```bash
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"cotizacion_divisa","arguments":{"monedaBase":"USD","monedaDestino":"PEN"}}}'
```

### Probar con Postman

Importá `postman/mcp-serverless-demo.postman_collection.json` y ejecutá:

1. `01 - Tools/List`
2. `02 - Call calcular_cuota_prestamo`
3. `03 - Call cotizacion_divisa (EUR)`
4. `04 - Call cotizacion_divisa (PEN)`

La variable de colección `baseUrl` vale `http://localhost:8080`. Para Cloud Run, cambiala por `https://<servicio>-<hash>.a.run.app`.

> La app usa `quarkus.mcp.server.http.streamable.auto-init=true`: cada request es independiente, así que **no hace falta reutilizar `Mcp-Session-Id`**. Es el modo “stateless” ideal para serverless y para Postman.

### Probar con MCP Inspector

```bash
npx @modelcontextprotocol/inspector
```

Configurá transporte **Streamable HTTP** y URL `http://localhost:8080/mcp`.

### Conectar un cliente real (Claude Desktop / Cursor / Claude Code)

Ejemplo de configuración genérica de un servidor MCP HTTP:

```json
{
  "mcpServers": {
    "finanzas": {
      "type": "http",
      "url": "https://<servicio>-<hash>.a.run.app/mcp"
    }
  }
}
```

En local usá `http://localhost:8080/mcp`.

---

## 4. Imágenes Docker

### JVM (rápida para iterar)

```bash
docker build -f docker/Dockerfile.jvm -t mcp-serverless-demo:jvm .
docker run --rm -p 8080:8080 mcp-serverless-demo:jvm
```

### Nativa (la que se despliega)

```bash
docker build -f docker/Dockerfile.native -t mcp-serverless-demo:native .
docker run --rm -p 8080:8080 mcp-serverless-demo:native
```

> Quarkus 3.40 exige **GraalVM/Mandrel 25**. El `Dockerfile.native` ya usa la imagen `ubi9-quarkus-mandrel-builder-image:jdk-25`. Si tu `JAVA_HOME` local tiene GraalVM 21, `mvn package -Pnative` fallará: construí la nativa con Docker (o instalá Mandrel 25).

---

## 5. Despliegue en Cloud Run

- **Proyecto:** `training-lab-504513`
- **Región:** `us-central1`
- **Servicio:** `mcp-serverless-demo`
- **Imagen:** `us-central1-docker.pkg.dev/training-lab-504513/mcp-serverless-demo/mcp-serverless-demo`

### 5.1 Deploy manual (validación)

```bash
gcloud config set project training-lab-504513
gcloud services enable run.googleapis.com artifactregistry.googleapis.com
gcloud artifacts repositories create mcp-serverless-demo \
  --repository-format=docker --location=us-central1
gcloud auth configure-docker us-central1-docker.pkg.dev

docker build -f docker/Dockerfile.native \
  -t us-central1-docker.pkg.dev/training-lab-504513/mcp-serverless-demo/mcp-serverless-demo:latest .
docker push us-central1-docker.pkg.dev/training-lab-504513/mcp-serverless-demo/mcp-serverless-demo:latest

gcloud run deploy mcp-serverless-demo \
  --image us-central1-docker.pkg.dev/training-lab-504513/mcp-serverless-demo/mcp-serverless-demo:latest \
  --region us-central1 \
  --allow-unauthenticated \
  --port 8080 \
  --cpu 1 --memory 512Mi \
  --min-instances 0 --max-instances 3
```

La URL pública queda como `https://mcp-serverless-demo-<hash>.a.run.app`. El endpoint MCP es esa URL + `/mcp`.

### 5.2 CI/CD con GitHub Actions

1. Configurá GCP (Artifact Registry + Service Account + Workload Identity Federation):

   ```bash
   export GITHUB_REPO="tu-org/mcp-serverless-demo"
   bash scripts/gcp-setup.sh
   ```

2. Copiá los dos valores que imprime el script como **secrets** del repo:
   - `GCP_WORKLOAD_IDENTITY_PROVIDER`
   - `GCP_SERVICE_ACCOUNT`

3. Hacé push a `main`. El workflow `.github/workflows/deploy.yml` construye la **imagen nativa**, la publica en Artifact Registry y despliega en Cloud Run.

> El build nativo en el runner tarda ~10-15 min. No se necesita `cloudbuild.yaml`: se usa `docker build` directamente con `docker/Dockerfile.native`.

---

## 6. Configuración relevante

| Propiedad | Valor | Motivo |
|---|---|---|
| `quarkus.mcp.server.http.streamable.auto-init` | `true` | Modo stateless (cada request independiente), ideal Cloud Run/Postman. |
| `quarkus.rest-client.currency-rate.url` | `https://open.er-api.com` | API de cotizaciones (más de 160 divisas, incluye PEN). |
| `quarkus.mcp.server.traffic-logging.enabled` | `true` (`%dev`) | Ver JSON-RPC en vivo durante la demo. |
| `quarkus.http.cors.*` | solo `%dev` | Permitir MCP Inspector en el navegador. |
| `quarkus.mcp.server.http.dns-rebinding-check.enabled` | `false` (`%dev`) | Evitar 403 por `Origin` en la demo local. En producción se desactiva solo porque Quarkus bindea `0.0.0.0`. |

> open.er-api.com actualiza las tasas aproximadamente **una vez al día**; la respuesta trae `time_last_update_utc`, que se muestra como fecha de referencia. Free tier ~1500 requests/mes.

> Si necesitás funcionalidades avanzadas de MCP (sampling, roots, progress, elicitation), quitá `auto-init` y volvé al flujo con sesión (`initialize` + `Mcp-Session-Id`).

---

## 7. Guion sugerido para la charla

1. **Contexto**: qué es MCP y por qué un servidor de tools.
2. **Arquitectura**: mostrar cómo el adapter MCP entra a Clean Architecture (domain → application → infrastructure).
3. **Local**: `quarkus:dev` + Postman (`tools/list` y ambas tools) mostrando el tráfico JSON-RPC.
4. **Cliente real**: conectar Claude/Cursor o MCP Inspector y pedir “calculá la cuota de un préstamo de 100.000 a 12% en 36 meses y decime la cotización USD→PEN”.
5. **Serverless**: push a `main` → GitHub Actions → Cloud Run; mostrar la imagen nativa y el cold start.
6. **Cierre**: cambiar `baseUrl` en Postman por la URL pública y repetir las llamadas; mencionar escala a cero y seguridad con OIDC como siguiente paso.

---

## 8. Notas

- **Sin tests automatizados** por decisión del alcance de la demo; la verificación es manual (Postman / Inspector / cliente real).
- **Seguridad**: el endpoint es público para la demo (`--allow-unauthenticated`). Para producción, proteger `/mcp` con bearer/OIDC (`quarkus-oidc`) y CORS restringido.
- **Costos**: con `--min-instances 0` la demo escala a cero cuando no hay tráfico.
