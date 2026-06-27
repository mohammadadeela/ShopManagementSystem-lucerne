package com.lucerne.util;

import javafx.stage.FileChooser;
import javafx.stage.Window;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class ExportUtil {
    private ExportUtil() { }
    public static Path chooseCsv(Window owner, String suggestedName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export filtered results");
        chooser.setInitialFileName(suggestedName.endsWith(".csv") ? suggestedName : suggestedName + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        var file = chooser.showSaveDialog(owner);
        return file == null ? null : file.toPath();
    }
    public static void writeCsv(Path path, List<String> columns, List<Map<String, Object>> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write('\ufeff');
            writer.write(columns.stream().map(ExportUtil::escape).reduce((a,b) -> a + "," + b).orElse(""));
            writer.newLine();
            for (Map<String, Object> row : rows) {
                String line = columns.stream().map(c -> escape(String.valueOf(row.getOrDefault(c, ""))))
                        .reduce((a,b) -> a + "," + b).orElse("");
                writer.write(line); writer.newLine();
            }
        } catch (IOException exception) {
            LoggerUtil.warning(ExportUtil.class, "CSV export failed for " + path, exception);
            throw exception;
        }
    }
    private static String escape(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }
}
