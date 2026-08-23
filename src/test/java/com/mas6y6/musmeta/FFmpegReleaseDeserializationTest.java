package com.mas6y6.musmeta;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class FFmpegReleaseDeserializationTest {

    @Test
    void testDeserializeGithubReleaseWithExtraProperties() throws Exception {
        String json = """
                {
                  "url": "https://api.github.com/repos/GyanD/codexffmpeg/releases/123456",
                  "assets_url": "https://api.github.com/repos/GyanD/codexffmpeg/releases/123456/assets",
                  "upload_url": "https://uploads.github.com/repos/GyanD/codexffmpeg/releases/123456/assets{?name,label}",
                  "html_url": "https://github.com/GyanD/codexffmpeg/releases/tag/7.1",
                  "id": 123456,
                  "node_id": "RE_kwDO...",
                  "tag_name": "7.1",
                  "target_commitish": "master",
                  "name": "7.1",
                  "draft": false,
                  "prerelease": false,
                  "created_at": "2024-09-30T10:00:00Z",
                  "published_at": "2024-09-30T11:00:00Z",
                  "assets": [
                    {
                      "url": "https://api.github.com/repos/GyanD/codexffmpeg/releases/assets/1001",
                      "id": 1001,
                      "node_id": "RA_kwDO...",
                      "name": "ffmpeg-7.1-full_build.zip",
                      "label": null,
                      "uploader": {
                        "login": "GyanD",
                        "id": 999
                      },
                      "content_type": "application/zip",
                      "state": "uploaded",
                      "size": 150000000,
                      "download_count": 5000,
                      "created_at": "2024-09-30T10:30:00Z",
                      "updated_at": "2024-09-30T10:35:00Z",
                      "browser_download_url": "https://github.com/GyanD/codexffmpeg/releases/download/7.1/ffmpeg-7.1-full_build.zip"
                    },
                    {
                      "url": "https://api.github.com/repos/GyanD/codexffmpeg/releases/assets/1002",
                      "id": 1002,
                      "node_id": "RA_kwDO...",
                      "name": "ffmpeg-7.1-essentials_build.zip",
                      "label": null,
                      "uploader": {
                        "login": "GyanD",
                        "id": 999
                      },
                      "content_type": "application/zip",
                      "state": "uploaded",
                      "size": 100000000,
                      "download_count": 12000,
                      "created_at": "2024-09-30T10:30:00Z",
                      "updated_at": "2024-09-30T10:35:00Z",
                      "browser_download_url": "https://github.com/GyanD/codexffmpeg/releases/download/7.1/ffmpeg-7.1-essentials_build.zip"
                    }
                  ],
                  "tarball_url": "https://api.github.com/repos/GyanD/codexffmpeg/tarball/7.1",
                  "zipball_url": "https://api.github.com/repos/GyanD/codexffmpeg/zipball/7.1",
                  "body": "FFmpeg release 7.1"
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Utils.GithubRelease release = mapper.readValue(json, Utils.GithubRelease.class);

        assertNotNull(release);
        assertEquals("7.1", release.tag_name());
        assertEquals("7.1", release.name());
        assertNotNull(release.assets());
        assertEquals(2, release.assets().length);

        Utils.GithubAsset essentials = Arrays.stream(release.assets())
                .filter(a -> a.name().endsWith("essentials_build.zip"))
                .findFirst()
                .orElse(null);

        assertNotNull(essentials);
        assertEquals("ffmpeg-7.1-essentials_build.zip", essentials.name());
        assertEquals("https://github.com/GyanD/codexffmpeg/releases/download/7.1/ffmpeg-7.1-essentials_build.zip", essentials.browser_download_url());
        assertEquals(100000000L, essentials.size());
    }
}
