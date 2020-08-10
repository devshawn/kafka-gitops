package com.devshawn.kafka.gitops.service;

import com.devshawn.kafka.gitops.domain.state.DesiredStateFile;
import com.devshawn.kafka.gitops.domain.state.settings.SettingsFiles;
import com.devshawn.kafka.gitops.exception.ValidationException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class ParserService {

    private static Logger log = LoggerFactory.getLogger(ParserService.class);

    private final ObjectMapper objectMapper;

    private final File file;

    public ParserService(File file) {
        this.objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
        objectMapper.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        objectMapper.registerModule(new Jdk8Module());
        this.file = file;
    }

    public DesiredStateFile parseStateFile() {
        DesiredStateFile desiredStateFile = parseStateFile(file);
        if (desiredStateFile.getSettings().isPresent() && desiredStateFile.getSettings().get().getFiles().isPresent()) {
            DesiredStateFile.Builder builder = new DesiredStateFile.Builder().mergeFrom(desiredStateFile);
            SettingsFiles settingsFiles = desiredStateFile.getSettings().get().getFiles().get();
            if (settingsFiles.getServices().isPresent()) {
                DesiredStateFile servicesFile = loadExternalFile(settingsFiles.getServices().get(), "Services");
                builder.putAllServices(servicesFile.getServices());
            }
            if (settingsFiles.getTopics().isPresent()) {
                DesiredStateFile topicsFile = loadExternalFile(settingsFiles.getTopics().get(), "Topics");
                builder.putAllTopics(topicsFile.getTopics());
            }
            if (settingsFiles.getUsers().isPresent()) {
                DesiredStateFile usersFile = loadExternalFile(settingsFiles.getUsers().get(), "Users");
                builder.putAllUsers(usersFile.getUsers());
            }
            return builder.build();
        }
        return desiredStateFile;
    }

    public DesiredStateFile parseStateFile(File stateFile) {
        log.info("Parsing desired state file...");

        try {
            return objectMapper.readValue(stateFile, DesiredStateFile.class);
        } catch (ValueInstantiationException ex) {
            List<String> fields = getYamlFields(ex);
            String joinedFields = String.join(" -> ", fields);
            throw new ValidationException(String.format("%s in state file definition: %s", ex.getCause().getMessage(), joinedFields));
        } catch (UnrecognizedPropertyException ex) {
            List<String> fields = getYamlFields(ex);
            String joinedFields = String.join(" -> ", fields.subList(0, fields.size() - 1));
            throw new ValidationException(String.format("Unrecognized field: [%s] in state file definition: %s", ex.getPropertyName(), joinedFields));
        } catch (InvalidFormatException ex) {
            List<String> fields = getYamlFields(ex);
            String value = ex.getValue().toString();
            String propertyName = fields.get(fields.size() - 1);
            String joinedFields = String.join(" -> ", fields.subList(0, fields.size() - 1));
            throw new ValidationException(String.format("Value '%s' is not a valid format for: [%s] in state file definition: %s", value, propertyName, joinedFields));
        } catch (JsonMappingException ex) {
            List<String> fields = getYamlFields(ex);
            String message = ex.getCause() != null ? ex.getCause().getMessage().split("\n")[0] : ex.getMessage().split("\n")[0];
            String joinedFields = String.join(" -> ", fields);
            throw new ValidationException(String.format("%s in state file definition: %s", message, joinedFields));
        } catch (FileNotFoundException ex) {
            throw new ValidationException("The specified state file could not be found.");
        } catch (IOException ex) {
            throw new ValidationException(String.format("Invalid state file. Unknown error: %s", ex.getMessage()));
        }
    }

    private DesiredStateFile loadExternalFile(String fileName, String type) {
        File externalFile = getAdditionalFile(fileName);
        if (!externalFile.exists()) {
            throw new ValidationException(String.format("%s file '%s' could not be found.", type, fileName));
        }
        return parseStateFile(externalFile);
    }

    private File getAdditionalFile(String fileName) {
        return new File(Paths.get(file.getAbsoluteFile().getParent(), fileName).toString());
    }

    private List<String> getYamlFields(JsonMappingException ex) {
        return ex.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .collect(Collectors.toList());
    }
}
