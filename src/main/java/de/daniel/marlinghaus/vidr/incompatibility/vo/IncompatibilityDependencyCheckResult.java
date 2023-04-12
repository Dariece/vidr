package de.daniel.marlinghaus.vidr.incompatibility.vo;

import de.daniel.marlinghaus.vidr.incompatibility.type.GeneralIncompatibilityType;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.eclipse.collections.api.factory.Lists;

@Getter
@Builder
//TODO extend from GAVDependency -> make abstract class
public class IncompatibilityDependencyCheckResult {
 private String name;
 private GeneralIncompatibilityType type;
 private int riskLevel;
 private boolean incompatible;
 private List<IncompatibilityDependency> incompatibleDependencies;
 private String actualVersion;
 @Builder.Default
 private List<String> fixingVersions = Lists.mutable.empty();
}
