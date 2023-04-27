package de.daniel.marlinghaus.vidr.incompatibility.vo;

import de.daniel.marlinghaus.vidr.incompatibility.type.GeneralIncompatibilityType;
import de.daniel.marlinghaus.vidr.vulnerability.resolve.vo.GavDependency;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.eclipse.collections.api.factory.Lists;

@Getter
@SuperBuilder
public class IncompatibilityDependencyCheckResult extends GavDependency {
 private GeneralIncompatibilityType type;
 private int riskLevel;
 private boolean incompatible;
 @Builder.Default
 private List<IncompatibilityDependency> incompatibleDependencies = Lists.mutable.empty();
 @Builder.Default
 private List<String> fixingVersions = Lists.mutable.empty();
}
