package com.example.project;

import com.example.filesystem.LocalFileSystem;

import java.io.File;

public class ProjectTemplate {

    public enum Type {
        EMPTY("Empty Workspace", "Creates a clean, empty workspace directory."),
        WEB("Web Starter (HTML/CSS/JS)", "Generates index.html, style.css, and app.js."),
        PYTHON("Python Workspace", "Generates main.py and requirements.txt."),
        SQL("SQL Database Workspace", "Generates schema.sql and queries.sql."),
        MARKDOWN("Markdown Notes", "Generates README.md and notes.md.");

        private final String title;
        private final String description;

        Type(String title, String description) {
            this.title = title;
            this.description = description;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }
    }

    public static Project createProjectFromTemplate(File parentDir, String folderName, Type type) throws Exception {
        LocalFileSystem fs = LocalFileSystem.getInstance();
        File projectDir = fs.createDirectory(parentDir, folderName);

        switch (type) {
            case WEB:
                File indexFile = fs.createFile(projectDir, "index.html");
                fs.writeStringToFile(indexFile, "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n  <title>" + folderName + "</title>\n  <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n  <h1>Welcome to " + folderName + "</h1>\n  <script src=\"app.js\"></script>\n</body>\n</html>\n");

                File cssFile = fs.createFile(projectDir, "style.css");
                fs.writeStringToFile(cssFile, "/* " + folderName + " Styles */\nbody {\n  font-family: system-ui, sans-serif;\n  margin: 2rem;\n  background-color: #f8fafc;\n  color: #0f172a;\n}\n");

                File jsFile = fs.createFile(projectDir, "app.js");
                fs.writeStringToFile(jsFile, "// " + folderName + " Script\nconsole.log(\"Initialized " + folderName + "\");\n");
                break;

            case PYTHON:
                File pyFile = fs.createFile(projectDir, "main.py");
                fs.writeStringToFile(pyFile, "# " + folderName + " Main Entry\n\ndef main():\n    print(\"Hello from " + folderName + "\")\n\nif __name__ == \"__main__\":\n    main()\n");

                File reqFile = fs.createFile(projectDir, "requirements.txt");
                fs.writeStringToFile(reqFile, "# Project dependencies\n");
                break;

            case SQL:
                File schemaFile = fs.createFile(projectDir, "schema.sql");
                fs.writeStringToFile(schemaFile, "-- Schema definitions for " + folderName + "\nCREATE TABLE IF NOT EXISTS users (\n    id INTEGER PRIMARY KEY AUTOINCREMENT,\n    username TEXT NOT NULL,\n    created_at DATETIME DEFAULT CURRENT_TIMESTAMP\n);\n");

                File queryFile = fs.createFile(projectDir, "queries.sql");
                fs.writeStringToFile(queryFile, "-- Practice queries\nSELECT * FROM users;\n");
                break;

            case MARKDOWN:
                File readmeFile = fs.createFile(projectDir, "README.md");
                fs.writeStringToFile(readmeFile, "# " + folderName + "\n\nWorkspace created with DroidCode.\n");

                File notesFile = fs.createFile(projectDir, "notes.md");
                fs.writeStringToFile(notesFile, "## Development Notes\n\n- Task 1\n- Task 2\n");
                break;

            case EMPTY:
            default:
                File gitKeep = fs.createFile(projectDir, ".gitkeep");
                fs.writeStringToFile(gitKeep, "");
                break;
        }

        return Project.fromDirectory(projectDir, type.getTitle());
    }
}
