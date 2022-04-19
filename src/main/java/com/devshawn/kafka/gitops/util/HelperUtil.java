package com.devshawn.kafka.gitops.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;

import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;

public class HelperUtil {

    public static List<String> uniqueCombine(List<String> listOne, List<String> listTwo) {
        Set<String> set = new LinkedHashSet<>(listOne);
        set.addAll(listTwo);
        return new ArrayList<>(set);
    }

    public static String generateDiff(ParsedSchema parsedSchema1, ParsedSchema parsedSchema2) {

        List<String> schemaStringList1 = lines(parsedSchema1);
        List<String> schemaStringList2 = lines(parsedSchema2);
        
        DiffRowGenerator generator = DiffRowGenerator.create()
                .showInlineDiffs(false)
                .inlineDiffByWord(false)
                .build();

        StringBuilder stringBuilder = new StringBuilder();
        List<DiffRow> diffRows = generator.generateDiffRows(schemaStringList1, schemaStringList2);
        for (Iterator<DiffRow> iterator = diffRows.iterator(); iterator.hasNext();) {
            if(stringBuilder.length() > 0) {
                stringBuilder.append("\n");
            }
            DiffRow diffRow = iterator.next();
            switch (diffRow.getTag()) {
            case INSERT:
                stringBuilder.append("+ ").append(diffRow.getNewLine());
                break;
            case DELETE:
                stringBuilder.append("- ").append(diffRow.getOldLine());
                break;
            case CHANGE:
                stringBuilder.append("- ").append(diffRow.getOldLine()).append("\n").append("+ ").append(diffRow.getNewLine());
                break;
            default:
                stringBuilder.append(diffRow.getOldLine());
                break;
            }
        }

        return stringBuilder.toString();
    }

    private static List<String> lines(ParsedSchema parsedSchema) {
        final String value;
        if(parsedSchema instanceof AvroSchema) {
            value = ((AvroSchema)parsedSchema).rawSchema().toString(true);
        } else if(parsedSchema instanceof ProtobufSchema) {
            value = ((ProtobufSchema)parsedSchema).rawSchema().toSchema();
        } else if(parsedSchema instanceof JsonSchema) {
            value =((JsonSchema) parsedSchema).toJsonNode().toPrettyString();
        } else {
            value = parsedSchema.canonicalString();
        }
        return Arrays.asList(value.split("\n"));
    }
}
