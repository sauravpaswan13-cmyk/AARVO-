#!/usr/bin/env bash
# ==============================================================================
# AARVO GitHub Auto-Push & Build Trigger Script
# ==============================================================================
# This script automatically commits changes, pushes to the GitHub repository,
# and triggers the GitHub Actions workflow for building the APK.
# ==============================================================================

set -e

REPO_URL="https://github.com/sauravpaswan13-cmyk/AARVO-.git"
REPO_OWNER="sauravpaswan13-cmyk"
REPO_NAME="AARVO-"
WORKFLOW_FILE="aarvo-build-trigger.yml"
BRANCH="main"

COMMIT_MSG="${1:-Auto sync and build trigger from AI Studio}"

echo "=================================================="
echo "  AARVO GitHub Auto-Push & Build Trigger"
echo "=================================================="

# Check if GITHUB_TOKEN is available
if [ -z "$GITHUB_TOKEN" ]; then
  # Try reading from .env if present
  if [ -f .env ]; then
    export $(grep -v '^#' .env | grep -E '^GITHUB_TOKEN=' | xargs 2>/dev/null || true)
  fi
fi

if [ -z "$GITHUB_TOKEN" ]; then
  echo "⚠️  GITHUB_TOKEN is not set."
  echo ""
  echo "To allow automatic push and workflow triggering:"
  echo "1. Create a GitHub Personal Access Token at https://github.com/settings/tokens/new with 'repo' scope."
  echo "2. Run this command with your token:"
  echo "   GITHUB_TOKEN=\"ghp_your_token\" ./scripts/sync-to-github.sh"
  echo "   or add GITHUB_TOKEN=ghp_your_token to your .env file."
  echo "=================================================="
  exit 1
fi

echo "📦 1. Preparing files and commit..."
cd /tmp/aarvo_repo 2>/dev/null || cd .

git config user.name "sauravpaswan13"
git config user.email "sauravpaswan13@gmail.com"

git add -A
if git diff-index --quiet HEAD --; then
  echo "ℹ️  No new changes to commit. Proceeding to trigger build..."
else
  git commit -m "$COMMIT_MSG"
  echo "✅ Changes committed: $COMMIT_MSG"
fi

echo "🚀 2. Pushing to GitHub (origin/$BRANCH)..."
git push "https://${GITHUB_TOKEN}@github.com/${REPO_OWNER}/${REPO_NAME}.git" "$BRANCH"
echo "✅ Successfully pushed changes to GitHub!"

echo "⚙️ 3. Triggering GitHub Actions Workflow..."
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST \
  -H "Accept: application/vnd.github.v3+json" \
  -H "Authorization: token ${GITHUB_TOKEN}" \
  "https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/actions/workflows/${WORKFLOW_FILE}/dispatches" \
  -d "{\"ref\":\"${BRANCH}\"}")

if [ "$RESPONSE" = "204" ]; then
  echo "🎉 Success! GitHub Actions workflow '${WORKFLOW_FILE}' has been triggered."
  echo "👉 Monitor your build here: https://github.com/${REPO_OWNER}/${REPO_NAME}/actions"
else
  echo "ℹ️  Push succeeded. Workflow will trigger automatically via GitHub push event."
  echo "👉 Check Actions tab: https://github.com/${REPO_OWNER}/${REPO_NAME}/actions"
fi

echo "=================================================="
