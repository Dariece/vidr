package de.daniel.marlinghaus.vidr;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VidrTasks {

  CREATE_SBOM("createSbom"),
  CREATE_VULNERABILITY_REPORT("createVulnerabilityReport"),
  RESOLVE_DEPENDENCY_FIX("resolveDependencyFix"),
  CHECK_COMPATIBILITY("checkCompatibility"),
  FIX_COMPATIBILITY("fixCompatibility"),
  UPLOAD_FIX("uploadFix"),
  BUILD_WITH_FIX("buildWithFix");
  private final String name;
}
