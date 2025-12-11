package frontend.ctrl;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import frontend.metrics.MetricsService;

@RestController
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping(
        value = "/metrics",
        produces = MediaType.TEXT_PLAIN_VALUE
    )
    public String metrics() {
        return metricsService.buildMetrics();
    }
}
