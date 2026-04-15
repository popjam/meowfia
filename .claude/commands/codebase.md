Run the repo-local wrapper script to generate a full text snapshot of the repository. It resolves the git repo root from the current directory, creates `docs/` if needed, and writes the snapshot to `docs/meowfia_codebase.txt`, replacing any previous file at that path.

Run this command:
```
bash "/Users/james/stuff-large/meowfia/scripts/codebase.sh"
```

After the command finishes, verify that `docs/meowfia_codebase.txt` was written successfully and use that file as the generated codebase snapshot.
