# vidr
Vulnerability Incompatible Dependencies Resolver

TODO: Internetquellen als PDF sichern
## complications
- Allgemeines:
  - Das Gradle Testproject und die Lib zerstören sich gegenseitig den build cache
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
  - Trivy in echter Linux Umgebung nutzbar
- Sicherheitslückenprüfung:
  - Trivy (CVE?) lässt quantifier bei fix-versionen weg 
    - Beispiel Guava: fixVersion 30.0, echte version 30.0-jre
    - Ungenauigkeit der Beschreibung
    - Keine einheitliche Versionskonvention in Dependency-Repositories wie Mavencentral spring-web v. 6.0.0
  - Eingeschränkt nutzbar
- Sicherheitslückenbehebung:
  - Dependencies die mit BOMs arbeiten können falsche versionen referenzieren
    - Beispiel: jackson-databind:2.13.4.1 referenziert jackson-bom:2.13.4.1 welche nicht existiert
  - Transitive Abhängigkeiten mit Sicherheitslücken werden nicht behoben, da das Risiko einer Inkompatibilität zu direkten Abhängigkeiten bei Versionsänderungen zu groß ist
  - Eingeschränkt nutzbar
- Inkompatibilität prüfen: nutzen von vorhandenem
  - Problem: Bereits vorhandene Projekte sind nur Java 8 kompatibel und oder ausschließlich für Maven Projekte geeignet
  - Beispiel Decca: Verwendet Bibliothek soot (Vorgänger von sootUp) nur für Java Version < 9 geeignet und Maven Plugin
  - Beispiel Riddle: Baut auf Decca auf
  - Beispiel Sensor: Baut auf Decca auf, Quellcode nicht verfügbar
- Inkompatibilität prüfen: Eigenimplementierung
  - Die Jar-Artefakt Pfade zu Abhängigkeiten können erst bestimmt werden, nachdem das DependencyManagement eine Version für die Laufzeit bestimmt hat
  - Duplikat Abhängigkeiten in anderer Version müssen daher eigenständig ermittelt werden
  - Da das Framework soot zur Statischen Bytecodeanalyse nicht Java 17 geeignet ist, muss das Nachfolgeprojekt sootUp verwendet werden https://github.com/soot-oss/SootUp#sootup-improvements
    - Andere Projekte wie WALA (IBM) sind derzeit auch nur Java 8 bytecode kompatibel https://github.com/wala/WALA/wiki/Getting-Started 
  - Problem: sootUp kann Klassen derzeit nicht zwischen verschiedenen Views (jede View ein .jar Archive) vergleichen -> NullpointerException
    - java.lang.NullPointerException: Node for <com.fasterxml.jackson.annotation.ObjectIdGenerators$PropertyGenerator: void <init>(java.lang.Class)> has not been added yet
  - Werden alle Klassen aus verschiedenen Abhängigkeiten die benötigt werden in eine View geladen, läuft der RAM voll
    - Caused by: java.lang.OutOfMemoryError: Java heap space
      at com.esotericsoftware.kryo.io.Input.readString(Input.java:464) ...
    - Mehr als 4GB RAM werden benötigt, da zu einem Zeitpunkt auf einmal auf alle Klassen zugegriffen wird und diese in den Speicher geladen werden
    - Gleiches Problem bei Archiven mit zu vielen Klassen: https://github.com/soot-oss/SootUp/discussions/579 
  - Werden alle Klassen aller Abhängigkeiten in die View des Softwareprojektes geladen, ist zum Zeitpunkt des Auslesens bereits das Dateisystem für ein .jar Archive geschlossen https://github.com/soot-oss/SootUp/discussions/587
  - Unter diesen Umständen ist die Inkompatibilitätsprüfung derzeit im Kontext Gradle Java 17 nicht möglich
- Zukünftige Forschung
  - Eine falsche Bedienung des Frameworks sootUp kann für die aufgetretenen Probleme verantwortlich sein
    - In diesem Fall ist eine genaue Analyse des sootUp Quellcodes erforderlich
  - Besteht ein Bug im Framework, muss entweder auf einen Patch gewartet werden oder ein anderes Open Source Framework gesucht werden, welches zur statischen Bytecodeanalyse inkl. Kontrollflussgraphen in der Lage ist
    - CFG: https://link.springer.com/article/10.1007/BF03160273
    - Die Bytecodeanalyse selbst zu implementieren ist möglich, steht aber in keinem Verhältnis zum Umsetzungsaufwand
 
## lines of code
Ohne leere Zeilen
```bash
find src/main -name '*.java' | xargs grep -v '^\s*$' | wc -l
```
Ohne leere Zeilen und Kommentare
```bash
find src/main -name '*.java' | xargs grep -vP '^\s*$|\/\*(.|[\r\n])*?\*\/|^(\s)*?(\/\/)+(.)*?$' | wc -l
```
2339
