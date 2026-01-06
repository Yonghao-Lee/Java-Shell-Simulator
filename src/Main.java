import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    // System.getProperty("user.dir") retrieves the current working dir of the Java app as a String
    // Then Paths.get() converts the String into a nio.file.Path object
    private static Path currentPath = Paths.get(System.getProperty("user.dir"))
            .toAbsolutePath()
            .normalize();
    private static final Map<String, Command> COMMANDS = new HashMap<>();

    static {
        // Register commands here
        COMMANDS.put("exit", args -> System.exit(0));
        COMMANDS.put("echo", args -> System.out.println(String.join(" ", args)));
        COMMANDS.put("type", args -> {
            if(args.length > 0) handleType(args[0]);
        });
        COMMANDS.put("pwd", args -> {
            System.out.println(currentPath.toAbsolutePath());
        });
        COMMANDS.put("cd", Main::handleCd);
    }

    private static void handleCd(String[] args) {
        String homeDir = System.getenv("HOME");
        if (homeDir == null ) {
            homeDir = System.getProperty("user.home");
        }
        String target = (args.length == 0 || args[0].equals("~")) ? homeDir : args[0];

        Path newPath = currentPath.resolve(target).toAbsolutePath().normalize();        // This handles both absolute and relative paths
        if (Files.isDirectory(newPath)) {
            currentPath = newPath.normalize();
        } else {
            System.out.println("cd: " + target + ": No such file or directory");
        }

    }

    private static File getExecutablePath(String cmd) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) return null;
        String[] directories = pathEnv.split(java.io.File.pathSeparator);
        for (String directory : directories) {
            File file = new File(directory, cmd);
            if (file.exists() && file.canExecute()) return file;
        }
        return null;
    }

    private static void handleType(String target) {
        if (COMMANDS.containsKey(target)) {
            System.out.println(target + " is a shell builtin");
            return;
        }

        File executable = getExecutablePath(target);
        if (executable != null) {
            System.out.println(target + " is " + executable.getAbsolutePath());
        } else {
            System.out.println(target + ": not found");
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            // Split into command and args
            List<String> list = new ArrayList<>();
            // Use robust regex
            String patternString = "([^\"]\\S*|\".+?\")\\s*";
            Pattern pattern = Pattern.compile(patternString);
            Matcher matcher = pattern.matcher(input);
            while (matcher.find()) {
                list.add(matcher.group(1).replaceAll("\"", ""));
            }
            String[] fullArgs = list.toArray(new String[0]);
            String cmdName = fullArgs[0];
            String[] cmdArgs = new String[fullArgs.length - 1];
            System.arraycopy(fullArgs, 1, cmdArgs, 0, fullArgs.length - 1);

            Command command = COMMANDS.get(cmdName);
            if (command != null) {
                command.execute(cmdArgs);
            } else {
                // Execute external program
                File externalFile = getExecutablePath(cmdName);
                // Search for an executable with the given name in the directories listed in PATH (just like type does)
                if (externalFile != null)  {
                    try {
                        // Process building
                        ProcessBuilder pb = new ProcessBuilder(fullArgs);
                        pb.inheritIO();
                        Process p = pb.start();
                        p.waitFor(); // Parent process blocks here
                    } catch (Exception e) {
                        System.out.println("Error executing: " + cmdName);                    }
                } else {
                    System.out.println(cmdName + ": command not found");
                }
            }
        }
    }
}
