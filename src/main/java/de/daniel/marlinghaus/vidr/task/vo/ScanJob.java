package de.daniel.marlinghaus.vidr.task.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScanJob {
    private String applicationName;
    private String format;
    private String stage;
    private String pipelineRun;
}
