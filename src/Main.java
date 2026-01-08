import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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
            if (args.length > 0) handleType(args[0]);
        });
        COMMANDS.put("pwd", args -> {
            System.out.println(currentPath.toAbsolutePath());
        });
        COMMANDS.put("cd", Main::handleCd);
    }

    private static void handleCd(String[] args) {
        String homeDir = System.getenv("HOME");
        if (homeDir == null) {
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

    private static List<String> parseArgs(String input) {
        List<String> args = new ArrayList<>();
        StringBuilder currentArg = new StringBuilder();

        boolean inDoubleQuotes = false;
        boolean inSingleQuotes = false;
        boolean escaped = false; // Track if the previous char was '\'

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (escaped) {
                // If we are here, then the previous '\' was outside quotes
                currentArg.append(c);
                escaped = false;
            } else if (c == '\\' && !inDoubleQuotes && !inSingleQuotes) {
                escaped = true;
            } else if (c == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
            } else if (c == '\"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
            } else if (Character.isWhitespace(c) && !inDoubleQuotes && !inSingleQuotes) {
                // Space outside quotes or escaping ends the current argument
                if (!currentArg.isEmpty()) {
                    args.add(currentArg.toString());
                    currentArg.setLength(0);
                }
            } else {
                currentArg.append(c);
            }
        }
        // If the input string does not end with whitespace, add it as the last argument
        if(!currentArg.isEmpty()){
            args.add(currentArg.toString());
        }
        return args;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;
            List<String> list = parseArgs(input);
            String[] fullArgs = list.toArray(new String[0]);
            String cmdName = fullArgs[0];
            String[] cmdArgs = Arrays.copyOfRange(fullArgs, 1, fullArgs.length);

            Command command = COMMANDS.get(cmdName);
            if (command != null) {
                command.execute(cmdArgs);
            } else {
                // Execute external program
                File externalFile = getExecutablePath(cmdName);
                // Search for an executable with the given name in the directories listed in PATH (just like type does)
                if (externalFile != null) {
                    try {
                        // Process building
                        ProcessBuilder pb = new ProcessBuilder(fullArgs);
                        pb.inheritIO();
                        Process p = pb.start();
                        p.waitFor(); // Parent process blocks here
                    } catch (Exception e) {
                        System.out.println("Error executing: " + cmdName);
                    }
                } else {
                    System.out.println(cmdName + ": command not found");
                }
            }
        }
    }
}
