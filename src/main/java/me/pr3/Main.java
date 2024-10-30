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
       List<String> lines = Files.readAllLines(Path.of("./example/Input.atp"));
       String input = String.join("\n", lines);
        StructureCompiler compiler = new StructureCompiler(Map.of("Input.atp",input));
        Map<String, byte[]> result = compiler.compile();

        for (Map.Entry<String, byte[]> entry : result.entrySet()) {
            Files.deleteIfExists(Path.of("./example/" + entry.getKey() + ".class"));
            Files.createFile(Path.of("./example/" + entry.getKey() + ".class"));
            Files.write(Path.of("./example/" + entry.getKey() + ".class"), entry.getValue(), StandardOpenOption.WRITE);
        }

    }
}