# Shell Implementation Progress

### Architecture
- **Command Registry**: Utilizes a `HashMap<String, Command>` where `Command` is a `@FunctionalInterface`. This replaces long conditional chains with a decoupled, pluggable command system.
- **State Management**: Maintains an internal `java.nio.file.Path` object to track the current working directory, independent of the JVM's static process directory.

### Core Logic
- **Path Resolution**: Employs `Path.resolve()` to handle relative and absolute inputs, and `Path.normalize()` to resolve `..` and `.` navigation.
- **Initialization**: Bootstraps the shell state using `System.getProperty("user.dir")` to capture the absolute path at execution.

### Current Builtins
- `pwd`: Displays the current logical absolute path.
- `cd [path]`: Validates directory existence and updates internal state.
- `echo [args]`: Prints arguments to stdout.
- `type [cmd]`: Identifies if a command is a shell builtin.
- `exit`: Safely terminates the shell.

### Technical Implementation Details
- **Language**: Java
- **API**: `java.nio.file` for robust filesystem interaction.
- **Execution**: Commands are invoked via Lambda expressions, enabling easy access to shared shell state.