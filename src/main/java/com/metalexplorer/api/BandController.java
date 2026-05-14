package com.metalexplorer.api;

import com.metalexplorer.api.dto.SearchResponse;
import com.metalexplorer.api.dto.SimilarArtistDto;
import com.metalexplorer.api.dto.TagDto;
import com.metalexplorer.service.ArtistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bands")
@RequiredArgsConstructor
@Validated
public class BandController {

    private final ArtistService artistService;

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(
            @RequestParam("q") @NotBlank String query,
            HttpServletRequest request) {
        String sessionId = resolveSessionId(request);
        return ResponseEntity.ok(artistService.search(query, sessionId));
    }

    @GetMapping("/{artistId}/tags")
    public ResponseEntity<List<TagDto>> getTags(@PathVariable UUID artistId) {
        return ResponseEntity.ok(artistService.getTagsByArtistId(artistId));
    }

    @GetMapping("/{artistId}/similar")
    public ResponseEntity<List<SimilarArtistDto>> getSimilar(@PathVariable UUID artistId) {
        return ResponseEntity.ok(artistService.getSimilarArtists(artistId));
    }

    @GetMapping("/")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Metal Band Explorer is running");
    }

    private String resolveSessionId(HttpServletRequest request) {
        String header = request.getHeader("X-Session-Id");
        if (header != null && !header.isBlank()) return header;
        var session = request.getSession(true);
        return session.getId();
    }
}
