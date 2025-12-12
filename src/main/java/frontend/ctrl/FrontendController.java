package frontend.ctrl;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import team6.version.VersionUtil;
import frontend.data.PageLoadReport;
import frontend.data.Sms;
import frontend.data.TimeOnSiteReport;
import frontend.metrics.MetricsService;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(path = "/sms")
public class FrontendController {

    private String modelHost;
    private final RestTemplateBuilder rest;
    private final MetricsService metricsService;

    public FrontendController(RestTemplateBuilder rest, Environment env, MetricsService metricsService) {
        this.rest = rest;
        this.metricsService = metricsService;
        this.modelHost = env.getProperty("MODEL_HOST");
        assertModelHost();

        // F1: Use VersionUtil from lib-version for system information
        VersionUtil versionUtil = VersionUtil.getInstance();
        System.out.printf("[SMS Checker App] %s%n", versionUtil.getFullVersionInfo());
        System.out.printf("[SMS Checker App] Library version: %s%n", versionUtil.getVersion());
    }

    private void assertModelHost() {
        if (modelHost == null || modelHost.strip().isEmpty()) {
            System.err.println("ERROR: ENV variable MODEL_HOST is null or empty");
            System.exit(1);
        }
        modelHost = modelHost.strip();
        if (modelHost.indexOf("://") == -1) {
            var m = "ERROR: ENV variable MODEL_HOST is missing protocol, like \"http://...\" (was: \"%s\")\n";
            System.err.printf(m, modelHost);
            System.exit(1);
        } else {
            System.out.printf("Working with MODEL_HOST=\"%s\"\n", modelHost);
        }
    }

    @GetMapping("")
    public String redirectToSlash(HttpServletRequest request) {
        // relative REST requests in JS will end up on / and not on /sms
        return "redirect:" + request.getRequestURI() + "/";
    }

    @GetMapping("/")
    public String index(Model m) {
        // navigation path: user reached /sms/
        metricsService.recordSmsPageVisit();
        m.addAttribute("hostname", modelHost);
        return "sms/index";
    }

    @PostMapping({ "", "/" })
    @ResponseBody
    public Sms predict(@RequestBody Sms sms) {
        // this is your "button click" – user clicked submit
        metricsService.recordButtonClick();

        System.out.printf("Requesting prediction for \"%s\" ...\n", sms.sms);
        sms.result = getPrediction(sms);
        System.out.printf("Prediction: %s\n", sms.result);

        return sms;
    }

    private String getPrediction(Sms sms) {
        try {
            var url = new URI(modelHost + "/predict");
            var c = rest.build().postForEntity(url, sms, Sms.class);
            return c.getBody().result.trim();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/page-load")
    @ResponseBody
    public void reportPageLoad(@RequestBody PageLoadReport report) {
        metricsService.recordPageLoad(report.getDurationSeconds(), report.getPage());
    }

    @PostMapping("/time-on-site")
    @ResponseBody
    public void reportTimeOnSite(@RequestBody TimeOnSiteReport report) {
        metricsService.recordTimeOnSite(report.getTimeOnSiteMillis());
    }

    /**
     * F1: Version information endpoint for monitoring purposes.
     * Demonstrates usage of VersionUtil from lib-version library.
     */
    @GetMapping("/version")
    @ResponseBody
    public String getVersionInfo() {
        VersionUtil versionUtil = VersionUtil.getInstance();
        return String.format("{\"library\": \"%s\", \"version\": \"%s\", \"buildTime\": \"%s\"}",
                "lib-version",
                versionUtil.getVersion(),
                versionUtil.getBuildTime());
    }
}