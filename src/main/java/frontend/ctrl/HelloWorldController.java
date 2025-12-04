package frontend.ctrl;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import frontend.metrics.MetricsService;

@Controller
public class HelloWorldController {

    private final MetricsService metricsService;

    public HelloWorldController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/")
    @ResponseBody
    public String index() {
        metricsService.recordIndexVisit();
        return "Hello World!";
    }
}