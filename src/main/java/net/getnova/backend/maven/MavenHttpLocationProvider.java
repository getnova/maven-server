package net.getnova.backend.maven;

import lombok.RequiredArgsConstructor;
import net.getnova.backend.codec.http.server.HttpLocationProvider;

@RequiredArgsConstructor
public class MavenHttpLocationProvider implements HttpLocationProvider<MavenHttpLocation> {

    private final String path;
    private final MavenServerConfig config;

    @Override
    public MavenHttpLocation getLocation() {
        return new MavenHttpLocation(this.path, this.config);
    }
}
