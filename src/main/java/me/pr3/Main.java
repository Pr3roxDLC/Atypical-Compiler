package me.pr3;

import me.pr3.atypical.compiler.StructureCompiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

/**
 * @author ${USER}
 */
public class Main {
    public static void main(String[] args) throws IOException {
       List<String> lines = Files.readAllLines(Path.of("./example/Arrays.atp"));
       String input = String.join("\n", lines);
        StructureCompiler compiler = new StructureCompiler(Map.of("Arrays.atp",input));
        Map<String, byte[]> result = compiler.compile();

        for (Map.Entry<String, byte[]> entry : result.entrySet()) {
            String fileName = entry.getKey();
            Path outputPath = Path.of("./example/" + fileName + ".class");
            Files.deleteIfExists(outputPath);
            Files.createDirectories(outputPath.getParent());
            Files.createFile(outputPath);
            Files.write(outputPath, entry.getValue(), StandardOpenOption.WRITE);
        }

    }
}