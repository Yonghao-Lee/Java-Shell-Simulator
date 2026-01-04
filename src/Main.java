import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {
    private static final Map<String, Command> COMMANDS = new HashMap<>();

    static {
        // Register commands here
        COMMANDS.put("exit", args -> System.exit(0));
        COMMANDS.put("echo", System.out::println);
        COMMANDS.put("type", args -> { if(args.length > 0) handleType(args[0]);});
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
            String[] fullArgs = input.split("\\s+"); // Multiple args
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
