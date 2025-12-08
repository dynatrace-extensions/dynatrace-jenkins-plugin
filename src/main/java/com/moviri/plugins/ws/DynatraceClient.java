package com.moviri.plugins.ws;

import com.moviri.plugins.Preload;
import com.moviri.plugins.config.DynatraceConfiguration;
import io.jenkins.cli.shaded.org.slf4j.Logger;
import io.jenkins.cli.shaded.org.slf4j.LoggerFactory;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.json.JSONArray;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Preload
public class DynatraceClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynatraceClient.class);
    private CloseableHttpClient client;

    public DynatraceClient() {
        this.client = getDefaultHttpClient();
        this.tryCreateHttpClient();
    }

    private void tryCreateHttpClient() {
        DynatraceConfiguration config = getDynatraceConfiguration();
        String proxyUrl = config.getProxyUrl();
        if (proxyUrl != null && !proxyUrl.isEmpty()) {
            URI uri = URI.create(proxyUrl);
            HttpHost proxyHost = new HttpHost(uri.getScheme(), uri.getHost(), uri.getPort());
            HttpClientBuilder builder = HttpClients.custom().setRoutePlanner(new DefaultProxyRoutePlanner(proxyHost));

            String username = config.getProxyUsername();
            String password = config.getProxyPassword();
            if (username != null && password != null) {
                BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                        new AuthScope(proxyHost),
                        new UsernamePasswordCredentials(username, password.toCharArray())
                );
                builder.setDefaultCredentialsProvider(credentialsProvider);
            }
            this.client = builder.build();
        }
    }

    public void postMintMetrics(List<MintMetric> mintMetrics) {
        var url = this.formatUrl("/api/v2/metrics/ingest");

        HttpPost request = new HttpPost(url);
        request.addHeader("Content-Type", "text/plain");
        request.addHeader(new AuthenticationHeader());
        if (mintMetrics.isEmpty()) {
            LOGGER.warn("Tried to report mint metrics with an empty body.");
            return;
        }

        request.setEntity(new StringEntity(formatMetricLines(mintMetrics)));
        try {
            CloseableHttpResponse response = this.client.execute(request);
            int statusCode = response.getCode();
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            LOGGER.info("Sent metric lines (" + statusCode + "): " + body);
            this.checkErrors(response);
        } catch (IOException e) {
            LOGGER.error("Error sending mint metric: {}", e);
        } catch (ParseException e) {
            LOGGER.error("Error parsing mint metric response: {}", e);
        }
    }

    public void postLogLines(List<LogLine> logLines) {
        var url = this.formatUrl("/api/v2/logs/ingest");

        HttpPost request = new HttpPost(url);
        request.addHeader("Content-Type", "application/json; charset=utf-8");
        request.addHeader(new AuthenticationHeader());
        var logLineMap = logLines.stream().map(LogLine::toMap).collect(Collectors.toList());
        if (logLineMap.isEmpty()) {
            LOGGER.info("No new log lines to report.");
            return;
        }
        var jsonString = new JSONArray(logLineMap).toString();
        request.setEntity(new StringEntity(jsonString));

        try {
            CloseableHttpResponse response = this.client.execute(request);
            this.checkErrors(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String formatUrl(String endpoint) {
        var url = getDynatraceConfiguration().getTenant();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url + endpoint;
    }

    protected String formatMetricLines(List<MintMetric> mintMetrics) {
        return mintMetrics.stream().map(MintMetric::toString).collect(Collectors.joining("\n"));
    }

    private void checkErrors(CloseableHttpResponse response) {
        int code = response.getCode();
        if (code >= 300) {
            try {
                LOGGER.error(
                        "API call to Dynatrace returned errors: (Status Code: " +
                                code +
                                ") " +
                                EntityUtils.toString(response.getEntity()));
            } catch (IOException e) {
                LOGGER.error("Error performing API call to Dynatrace: " + e);
            } catch (ParseException e) {
                LOGGER.error("Error parsing HTTP response: " + e);
            }
        }
    }

    protected DynatraceConfiguration getDynatraceConfiguration() {
        return DynatraceConfiguration.get();
    }

    protected CloseableHttpClient getDefaultHttpClient() {
        return HttpClients.createDefault();
    }
}
