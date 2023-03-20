package de.daniel.marlinghaus.vidr.task.vo;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Value object of the job
 */
@Getter
@Builder
public class ScanJob {

  /**
   * Name of application to scan
   */
  private String applicationName;
  /**
   * Scan format
   */
  private ScanFormat format;
  /**
   * Stage of dedicated deployment
   */
  private String stage;

  /**
   * Name of the dedicated pipelineRun
   */
  private String pipelineRun;

  /**
   * Severity list that filters the trivy report output
   */
  private List<CvssSeverity> severities;
}
