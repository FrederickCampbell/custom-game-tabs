package com.freddy.verticaltabs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class PluginHubComplianceTest
{
    private static final Map<String, Pattern> FORBIDDEN = new LinkedHashMap<>();

    static
    {
        forbid("Client.menuAction", "client\\s*\\.\\s*menuAction\\s*\\(");
        forbid("Thread.sleep", "Thread\\s*\\.\\s*sleep\\s*\\(");
        forbid("java.awt.Robot", "\\bjava\\.awt\\.Robot\\b|\\bnew\\s+Robot\\s*\\(");
        forbid("ProcessBuilder", "\\bProcessBuilder\\b");
        forbid("Runtime.exec", "Runtime\\s*\\.\\s*getRuntime\\s*\\(\\s*\\)\\s*\\.\\s*exec\\s*\\(");
        forbid("reflection", "\\bjava\\.lang\\.reflect\\b|\\.setAccessible\\s*\\(");
        forbid("Unsafe", "\\b(?:sun|jdk\\.internal\\.misc)\\.Unsafe\\b");
        forbid("native library loading", "System\\s*\\.\\s*(?:load|loadLibrary)\\s*\\(");
        forbid("Desktop launcher", "\\bjava\\.awt\\.Desktop\\b|Desktop\\s*\\.\\s*getDesktop\\s*\\(");
        forbid("HttpURLConnection", "\\bHttpURLConnection\\b");
        forbid("java.net.http", "\\bjava\\.net\\.http\\b");
        forbid("Java serialization", "\\bObject(?:Input|Output)Stream\\b");
    }

    @Test
    public void mainSourceAvoidsPluginHubForbiddenPatterns() throws IOException
    {
        final Path sourceRoot = Paths.get("src", "main", "java");
        final List<String> violations = new ArrayList<>();

        try (Stream<Path> files = Files.walk(sourceRoot))
        {
            files
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(path -> scan(path, violations));
        }

        assertTrue(
            "Plugin Hub compliance scan failed:\n" + String.join("\n", violations),
            violations.isEmpty()
        );
    }

    private static void forbid(String name, String regex)
    {
        FORBIDDEN.put(name, Pattern.compile(regex));
    }

    private static void scan(Path path, List<String> violations)
    {
        final String source;
        try
        {
            source = Files.readString(path);
        }
        catch (IOException ex)
        {
            throw new RuntimeException(ex);
        }

        for (Map.Entry<String, Pattern> rule : FORBIDDEN.entrySet())
        {
            if (rule.getValue().matcher(source).find())
            {
                violations.add(rule.getKey() + ": " + path);
            }
        }
    }
}
