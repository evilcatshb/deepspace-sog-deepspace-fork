@echo off
setlocal
cd /d "%~dp0"

echo ================================================
echo DeepSpace - Sky of Grind GitHub sync
echo ================================================

where git >nul 2>&1
if errorlevel 1 (
  echo ERROR: Git was not found in PATH.
  exit /b 1
)

git switch main
if errorlevel 1 exit /b 1

rem Stage source, assets, KubeJS scripts, docs and bundled compile-time libraries.
git add -A
if errorlevel 1 exit /b 1

git diff --cached --quiet
if errorlevel 1 (
  git commit -m "Sync DeepSpace Sky of Grind source changes"
  if errorlevel 1 exit /b 1
) else (
  echo No local changes to commit.
)

git push -u deepspace main
if errorlevel 1 (
  echo ERROR: Push failed. Local commits are preserved and can be pushed again.
  exit /b 1
)

echo.
echo Successfully synced to https://github.com/evilcatshb/deepspace-sog-deepspace-fork
exit /b 0
