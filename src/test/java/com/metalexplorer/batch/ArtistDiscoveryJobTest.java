package com.metalexplorer.batch;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("batch-test")
class ArtistDiscoveryJobTest {

    static final WireMockServer WM = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

    static {
        WM.start();
    }

    @AfterAll
    static void stopWireMock() {
        WM.stop();
    }

    @DynamicPropertySource
    static void overrideLastfmUrl(DynamicPropertyRegistry registry) {
        registry.add("lastfm.base-url", () -> "http://localhost:" + WM.port());
    }

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        jdbc.execute("DELETE FROM tag_cooccurrence");
        jdbc.execute("DELETE FROM similar_artists");
        jdbc.execute("DELETE FROM artist_tags");
        jdbc.execute("DELETE FROM artists");
        WM.resetAll();
        stubLastfm();
    }

    @Test
    void jobCompletesAndPersistsArtists() throws Exception {
        JobExecution execution = jobLauncherTestUtils.launchJob();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        Integer artistCount = jdbc.queryForObject("SELECT COUNT(*) FROM artists", Integer.class);
        assertThat(artistCount).isGreaterThanOrEqualTo(2);

        Integer tagCount = jdbc.queryForObject("SELECT COUNT(*) FROM artist_tags", Integer.class);
        assertThat(tagCount).isGreaterThan(0);
    }

    @Test
    void jobSkipsArtistWhenLastfmReturnsError() throws Exception {
        WM.resetAll();

        // Iron Maiden fails → should be skipped (within skip limit of 10)
        WM.stubFor(get(urlPathEqualTo("/"))
                .withQueryParam("artist", equalTo("Iron Maiden"))
                .withQueryParam("method", equalTo("artist.getInfo"))
                .willReturn(serverError()));

        // Metallica succeeds
        stubArtist("Metallica");

        JobExecution execution = jobLauncherTestUtils.launchJob();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        long skipCount = execution.getStepExecutions().stream()
                .mapToLong(s -> s.getSkipCount())
                .sum();
        assertThat(skipCount).isGreaterThan(0);
    }

    private void stubLastfm() {
        stubArtist("Metallica");
        stubArtist("Iron Maiden");
    }

    private void stubArtist(String name) {
        WM.stubFor(get(urlPathEqualTo("/"))
                .withQueryParam("method", equalTo("artist.getInfo"))
                .withQueryParam("artist", equalTo(name))
                .willReturn(okJson(artistInfoJson(name))));

        WM.stubFor(get(urlPathEqualTo("/"))
                .withQueryParam("method", equalTo("artist.getTopTags"))
                .withQueryParam("artist", equalTo(name))
                .willReturn(okJson(topTagsJson())));

        WM.stubFor(get(urlPathEqualTo("/"))
                .withQueryParam("method", equalTo("artist.getSimilar"))
                .withQueryParam("artist", equalTo(name))
                .willReturn(okJson(similarJson())));
    }

    private static String artistInfoJson(String name) {
        String mbid = name.toLowerCase().replace(" ", "-") + "-mbid";
        return """
                {"artist":{"name":"%s","mbid":"%s","stats":{"listeners":"1000000","playcount":"50000000"},
                "bio":{"summary":"A great metal band."}}}
                """.formatted(name, mbid);
    }

    private static String topTagsJson() {
        return """
                {"toptags":{"tag":[
                    {"name":"heavy metal","count":"100"},
                    {"name":"metal","count":"90"}
                ]}}
                """;
    }

    private static String similarJson() {
        return """
                {"similarartists":{"artist":[]}}
                """;
    }
}
