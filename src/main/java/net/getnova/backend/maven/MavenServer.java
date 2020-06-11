package net.getnova.backend.maven;

import net.getnova.backend.codec.http.server.HttpServerService;
import net.getnova.backend.config.ConfigService;
import net.getnova.backend.service.Service;
import net.getnova.backend.service.event.PreInitService;
import net.getnova.backend.service.event.PreInitServiceEvent;

import javax.inject.Inject;
import java.io.File;

@Service(value = "maven-server", depends = {HttpServerService.class, ConfigService.class})
public class MavenServer {

    private final MavenServerConfig config;

    @Inject
    private HttpServerService httpServerService;

    @Inject
    public MavenServer(final ConfigService configService) {
        this.config = configService.addConfig("mavenServer", new MavenServerConfig());
    }

    @PreInitService
    public void preInit(final PreInitServiceEvent event) {
        final File data = new File(this.config.getDataDirectory());
        if (!data.exists()) data.mkdirs();
        this.httpServerService.addLocationProvider("maven", new MavenHttpLocationProvider(data.getAbsolutePath(), config));
    }
}
