# vidr
Vulnerability Incompatible Dependencies Resolver

## complications
- Das Gradle Testproject und die Lib zerstören sich dauernd den build cache
- GradleAPI Dependencies wie jackson oder http-client können nicht im Testprojekt referenziert werden
- Lokale Entwicklung dadurch sehr aufwendig
- Trivy hat zwischen Version 0.31.2 und 0.38.3 die cli Parameter geändert
- Trivy cache kann in client/server Verbund nicht auf WSL1 (Testumgebung) genutzt werden fs Fehler
  - failed to store blob (sha256:00d042e034b7a13c897b10463926736779047e2382ef7740e57a45a2bcbbb367) in cache:
    github.com/aquasecurity/trivy/pkg/fanal/artifact/sbom.Artifact.Inspect
    /home/runner/work/trivy/trivy/pkg/fanal/artifact/sbom/sbom.go:81
  - unable to store cache on the server:
    github.com/aquasecurity/trivy/pkg/cache.RemoteCache.PutBlob
    /home/runner/work/trivy/trivy/pkg/cache/remote.go:52
  - twirp error unavailable: Error from intermediary with HTTP status code 503 "Service Unavailable"
 
## lines of code
```bash
find src/main -name '*.java' | xargs grep -v '^\s*$' | wc -l
```
^\s*$|\/\*(.|[\r\n])*?\*\/|^(\s)*?(\/\/)(.)*?$
