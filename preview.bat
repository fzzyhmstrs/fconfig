start cmd.exe /k gradlew previewDocs
timeout /t 20 /nobreak
start "" "http://localhost:3000"

exit