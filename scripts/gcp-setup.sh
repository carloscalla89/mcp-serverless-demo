#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Setup de GCP para desplegar la demo MCP en Cloud Run con GitHub Actions + WIF.
#
# Uso:
#   1. Editá GITHUB_REPO con tu owner/repo (ej. "incoda/mcp-serverless-demo").
#   2. Ejecutá:  bash scripts/gcp-setup.sh
#   3. Copiá los dos valores finales como secrets en GitHub:
#        - GCP_WORKLOAD_IDENTITY_PROVIDER
#        - GCP_SERVICE_ACCOUNT
# -----------------------------------------------------------------------------
set -euo pipefail

PROJECT_ID="training-lab-504513"
REGION="us-central1"
AR_REPO="mcp-serverless-demo"
SA_NAME="mcp-serverless-deployer"
POOL="github-pool"
PROVIDER="github-provider"

# >>> EDITAR: owner/repo de GitHub (sin https://) <<<
GITHUB_REPO="${GITHUB_REPO:?Definí GITHUB_REPO, ej: export GITHUB_REPO=incoda/mcp-serverless-demo}"

gcloud config set project "${PROJECT_ID}"

echo ">> Habilitando APIs..."
gcloud services enable \
  run.googleapis.com \
  artifactregistry.googleapis.com \
  iamcredentials.googleapis.com \
  cloudresourcemanager.googleapis.com

echo ">> Creando repositorio de Artifact Registry..."
gcloud artifacts repositories create "${AR_REPO}" \
  --repository-format=docker \
  --location="${REGION}" \
  --description="Imágenes de la demo MCP serverless" || true

echo ">> Creando Service Account..."
gcloud iam service-accounts create "${SA_NAME}" \
  --display-name="MCP Serverless Demo deployer" || true

SA_EMAIL="${SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"

echo ">> Asignando roles..."
for role in roles/run.admin roles/artifactregistry.writer roles/iam.serviceAccountUser; do
  gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
    --member="serviceAccount:${SA_EMAIL}" \
    --role="${role}" \
    --quiet
done

echo ">> Creando Workload Identity Pool y Provider (GitHub OIDC)..."
gcloud iam workload-identity-pools create "${POOL}" \
  --location="global" \
  --display-name="GitHub pool" || true

gcloud iam workload-identity-pools providers create-oidc "${PROVIDER}" \
  --location="global" \
  --workload-identity-pool="${POOL}" \
  --display-name="GitHub provider" \
  --issuer-uri="https://token.actions.githubusercontent.com" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository" \
  --attribute-condition="assertion.repository=='${GITHUB_REPO}'" || true

PROJECT_NUMBER="$(gcloud projects describe "${PROJECT_ID}" --format='value(projectNumber)')"

echo ">> Permitiendo que el repo de GitHub use la Service Account..."
gcloud iam service-accounts add-iam-policy-binding "${SA_EMAIL}" \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${POOL}/attribute.repository/${GITHUB_REPO}" \
  --quiet

echo ""
echo "======================================================================"
echo "Configurá estos secrets en GitHub (Settings > Secrets and variables > Actions):"
echo ""
echo "GCP_WORKLOAD_IDENTITY_PROVIDER=projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${POOL}/providers/${PROVIDER}"
echo "GCP_SERVICE_ACCOUNT=${SA_EMAIL}"
echo "======================================================================"
