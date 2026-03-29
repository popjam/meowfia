Run code2prompt on the meowfia repository to get a full text snapshot of the codebase. Exclude build artifacts, generated files, and the .claude worktrees directory.

Run this command:
```
code2prompt "C:/Users/james/Desktop/stuff/stuff-large/projects/meowfia" --exclude="build,*.class,.gradle,.idea,.claude/worktrees,*.apk,*.aab,*.bin,*.jar" --output="/tmp/meowfia_codebase.txt" && cat /tmp/meowfia_codebase.txt
```

The cat pipes the full codebase into the conversation context via Bash stdout (persisted automatically for large outputs), bypassing the Read tool's per-call token limit. Confirm you've ingested the full codebase.
