package net.getnova.backend.maven;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import net.getnova.backend.config.ConfigValue;

import java.util.List;

@Data
@Setter(AccessLevel.NONE)
public class MavenServerConfig {

    @ConfigValue(id = "dataDirectory", comment = "The directory where the repositories are stored")
    private String dataDirectory = "data/";

    @ConfigValue(id = "repositories", comment = "List of all repositories")
    private List<String> repositories = List.of("snapshots", "releases");

    @ConfigValue(id = "username", comment = "Username of the default user")
    private String username = "";

    @ConfigValue(id = "password", comment = "Password of the default user")
    private String password = "";
}
