package org.kascoder.clean4m2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

public class Cleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(Cleaner.class);

    public static void main(String[] args) throws Exception {
        String configFilePath = "cleaner.yaml";
        if (args.length > 0) {
            configFilePath = args[0];
        }

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        Configuration configuration = objectMapper.readValue(new File(configFilePath), Configuration.class);
        List<Dependency> artifacts = new ArrayList<>();
        LOGGER.info("Searching for the artifacts...");
        for (String projectPath : configuration.getPaths()) {
            artifacts.addAll(findArtifacts(projectPath, null, null));
        }

        Map<String, Set<Dependency>> artifactInfoTable = artifacts.stream()
                .collect(Collectors.groupingBy(d -> String.join(".", d.getGroupId(), d.getArtifactId()), toSet()));
        LOGGER.info("{} artifacts were found.", artifactInfoTable.keySet().size());

        Path m2RepositoryPath = Paths.get(System.getProperty("user.home"), ".m2", "repository");
        LOGGER.debug(".m2 repository path - {}.", m2RepositoryPath.toString());

        LOGGER.info("Starting .m2 repository cleanup...");
        for (Map.Entry<String, Set<Dependency>> artifact : artifactInfoTable.entrySet()) {
            String[] pathTokens = artifact.getKey().split("\\.");
            File artifactDirectory = Paths.get(m2RepositoryPath.toString(), pathTokens).toFile();
            if (!artifactDirectory.isDirectory()) {
                continue;
            }

            Set<String> usedVersions = artifact.getValue()
                    .stream()
                    .map(Dependency::getVersion)
                    .collect(toSet());
            File[] obsoleteVersions = artifactDirectory.listFiles(f -> f.isDirectory() && !usedVersions.contains(f.getName()));
            List<File> unusedVersions = obsoleteVersions == null ? Collections.emptyList() : Arrays.asList(obsoleteVersions);

            LOGGER.info("Artifact '{}': count of used versions - {}, count of unused versions - {}.", artifact.getKey(), usedVersions.size(), unusedVersions.size());
            for (File unusedVersionDir : unusedVersions) {
                LOGGER.info("Unused artifact version path: {}", unusedVersionDir);
                Utils.removeFileOrDir(unusedVersionDir);
            }
        }
        LOGGER.info("Repository cleanup completed.");
    }

    private static List<Dependency> findArtifacts(String projectPath, String groupId, String version) {
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            return Collections.emptyList();
        }

        File[] pomFiles = projectDir.listFiles(f -> f.getName().equals("pom.xml") && f.canRead());
        if (pomFiles == null || pomFiles.length < 1) {
            return Collections.emptyList();
        }

        File pomFile = pomFiles[0];
        List<Dependency> artifacts = new ArrayList<>();
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model;
        try {
            model = reader.read(new FileReader(pomFile));
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }

        String _groupId = Utils.isNotBlankString(groupId) ? groupId : model.getGroupId();
        String _version = Utils.isNotBlankString(version) ? version : model.getVersion();

        Dependency dependency = new Dependency();
        dependency.setGroupId(_groupId);
        dependency.setArtifactId(model.getArtifactId());
        dependency.setVersion(_version);

        artifacts.add(dependency);
        model.getModules().forEach(moduleName -> {
            String path = Paths.get(projectDir.getPath(), moduleName).toString();
            artifacts.addAll(findArtifacts(path, _groupId, _version));
        });

        return artifacts;
    }
}
