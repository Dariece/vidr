package de.daniel.marlinghaus.vidr;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VidrGroups {

  REPORTING("reporting"),
  VERIFICATION("verification");
  private final String name;
}
