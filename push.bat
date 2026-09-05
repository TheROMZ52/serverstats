@echo off
git add .
git commit -m "uploaded files"

echo.
echo Pulling remote changes first...
git pull origin main --no-rebase

if errorlevel 1 (
    echo.
    echo ==============================================================
    echo   MERGE CONFLICT - git could not auto-merge.
    echo   Open the files it lists above, fix the conflict markers
    echo   ^(the lines with ^<^<^<^<^<^<^< / ======= / ^>^>^>^>^>^>^>^),
    echo   save, then run:
    echo       git add .
    echo       git commit -m "Merge"
    echo       git push
    echo ==============================================================
    pause
    exit /b 1
)

git push

echo.
echo Done. Now go to GitHub -^> Releases -^> Draft a new release, publish a NEW tag
echo (the build workflow only runs when a release is published), then download
echo the fresh jar from the Actions run and replace it on your server.
pause
