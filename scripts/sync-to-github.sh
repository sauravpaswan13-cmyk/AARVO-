#!/usr/bin/env bash
# ==============================================================================
# AARVO GitHub Auto-Push & Build Trigger Script
# ==============================================================================

set -euo pipefail

WORKSPACE_DIR="$(pwd)"
REPO_OWNER="sauravpaswan13-cmyk"
REPO_NAME="AARVO-"
REPO_URL="https://github.com/${REPO_OWNER}/${REPO_NAME}.git"
WORKFLOW_FILE="build-apk.yml"
BRANCH="main"
TMP_REPO="/tmp/aarvo_repo"

COMMIT_MSG="${1:-Sync AARVO Android UI and web preview updates}"

echo "=================================================="
echo "  AARVO GitHub Auto-Push & Build Trigger"
echo "=================================================="

# Check if GITHUB_TOKEN is available
if [ -z "${GITHUB_TOKEN:-}" ] && [ -f "${WORKSPACE_DIR}/.env" ]; then
  TOKEN_LINE="$(grep -E '^GITHUB_TOKEN=' "${WORKSPACE_DIR}/.env" | tail -n 1 || true)"
  if [ -n "$TOKEN_LINE" ]; then
    GITHUB_TOKEN="${TOKEN_LINE#GITHUB_TOKEN=}"
    GITHUB_TOKEN="${GITHUB_TOKEN%\"}"
    GITHUB_TOKEN="${GITHUB_TOKEN#\"}"
    GITHUB_TOKEN="${GITHUB_TOKEN%\'}"
    GITHUB_TOKEN="${GITHUB_TOKEN#\'}"
    export GITHUB_TOKEN
  fi
fi

if [ -z "${GITHUB_TOKEN:-}" ]; then
  echo "GITHUB_TOKEN is not set."
  echo "Set GITHUB_TOKEN in environment or .env, then run:"
  echo "  bash scripts/sync-to-github.sh"
  exit 1
fi

AUTH_URL="https://x-access-token:${GITHUB_TOKEN}@github.com/${REPO_OWNER}/${REPO_NAME}.git"

echo "1. Preparing clean clone in ${TMP_REPO}..."
rm -rf "${TMP_REPO}"
GIT_TERMINAL_PROMPT=0 git clone --depth 1 --branch "${BRANCH}" "${AUTH_URL}" "${TMP_REPO}" >/dev/null 2>&1

echo "2. Syncing workspace files into ${TMP_REPO}..."
tar -C "${WORKSPACE_DIR}" \
  --exclude='.git' \
  --exclude='node_modules' \
  --exclude='dist' \
  --exclude='build' \
  --exclude='.gradle' \
  --exclude='.aistudio' \
  --exclude='.env' \
  --exclude='.env.local' \
  --exclude='.env.*.local' \
  --exclude='public/downloads' \
  --exclude='*.apk' \
  --exclude='*.jks' \
  --exclude='*.keystore' \
  -cf - . | tar -C "${TMP_REPO}" --no-same-owner -xf -

git config --global --add safe.directory "${TMP_REPO}" 2>/dev/null || true
cd "${TMP_REPO}"
git remote set-url origin "${REPO_URL}"
git config user.name "sauravpaswan13"
git config user.email "sauravpaswan13@gmail.com"

git add -A
if git diff --cached --quiet; then
  echo "No file changes detected relative to ${BRANCH}."
else
  git commit -m "${COMMIT_MSG}"
  echo "Committed: ${COMMIT_MSG}"
  echo "3. Pushing to GitHub (${REPO_OWNER}/${REPO_NAME} -> ${BRANCH})..."
  GIT_TERMINAL_PROMPT=0 git push "${AUTH_URL}" "${BRANCH}" >/dev/null 2>&1
  echo "Successfully pushed changes to GitHub!"
fi

echo "4. Checking workflow dispatch (${WORKFLOW_FILE})..."
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST \
  -H "Accept: application/vnd.github.v3+json" \
  -H "Authorization: Bearer ${GITHUB_TOKEN}" \
  "https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/actions/workflows/${WORKFLOW_FILE}/dispatches" \
  -d "{\"ref\":\"${BRANCH}\"}")

if [ "$RESPONSE" = "204" ]; then
  echo "Workflow '${WORKFLOW_FILE}' triggered."
else
  echo "Push to '${BRANCH}' triggers CI workflows automatically (dispatch status: ${RESPONSE})."
fi

echo "Monitor build: https://github.com/${REPO_OWNER}/${REPO_NAME}/actions"
rm -rf "${TMP_REPO}"

