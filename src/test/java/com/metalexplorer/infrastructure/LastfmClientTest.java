package com.metalexplorer.infrastructure;

import com.metalexplorer.service.LastfmClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class LastfmClientTest {

    private MockWebServer mockWebServer;
    private LastfmClient lastfmClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/").toString();
        lastfmClient = new LastfmClient(RestClient.builder(), baseUrl, "test-api-key");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void searchArtist_parsesResponse() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        {
                          "results": {
                            "artistmatches": {
                              "artist": [
                                {"name": "Metallica", "mbid": "65f4f0c5-ef9e-490c-aee3-909e7ae6b2ab", "listeners": "5000000"}
                              ]
                            }
                          }
                        }
                        """)
                .addHeader("Content-Type", "application/json"));

        var result = lastfmClient.searchArtist("Metallica").get();

        assertThat(result).isNotNull();
        assertThat(result.results().artistmatches().artist()).hasSize(1);
        assertThat(result.results().artistmatches().artist().get(0).name()).isEqualTo("Metallica");
    }

    @Test
    void getArtistInfo_parsesResponse() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        {
                          "artist": {
                            "name": "Slayer",
                            "mbid": "abc-123",
                            "stats": {"listeners": "3000000", "playcount": "50000000"},
                            "bio": {"summary": "Thrash metal legends"}
                          }
                        }
                        """)
                .addHeader("Content-Type", "application/json"));

        var result = lastfmClient.getArtistInfo("Slayer").get();

        assertThat(result.artist().name()).isEqualTo("Slayer");
        assertThat(result.artist().stats().listeners()).isEqualTo("3000000");
        assertThat(result.artist().bio().summary()).isEqualTo("Thrash metal legends");
    }

    @Test
    void getTopTags_returnsTop10() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        {
                          "toptags": {
                            "tag": [
                              {"name": "thrash metal", "count": "100"},
                              {"name": "metal", "count": "90"}
                            ]
                          }
                        }
                        """)
                .addHeader("Content-Type", "application/json"));

        var result = lastfmClient.getTopTags("Slayer").get();

        assertThat(result.toptags().tag()).hasSize(2);
        assertThat(result.toptags().tag().get(0).name()).isEqualTo("thrash metal");
    }

    @Test
    void getSimilarArtists_parsesResponse() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        {
                          "similarartists": {
                            "artist": [
                              {"name": "Megadeth", "mbid": "def-456", "match": "0.87"}
                            ]
                          }
                        }
                        """)
                .addHeader("Content-Type", "application/json"));

        var result = lastfmClient.getSimilarArtists("Metallica").get();

        assertThat(result.similarartists().artist()).hasSize(1);
        assertThat(result.similarartists().artist().get(0).name()).isEqualTo("Megadeth");
        assertThat(result.similarartists().artist().get(0).match()).isEqualTo("0.87");
    }
}
