package com.github.olivergondza.saxeed;

import com.github.olivergondza.saxeed.ex.FailedWriting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;

public interface Target {

    OutputStream getOutputStream();
    String getName();

    class FileTarget implements Target {
        private final File file;

        public FileTarget(File file) {
            this.file = file;
        }

        public FileTarget(Path path) {
            this.file = path.toFile();
        }

        @Override
        public OutputStream getOutputStream() throws FailedWriting {
            try {
                return new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                throw new FailedWriting("Cannot create/open file: " + file.getAbsolutePath(), e);
            }
        }

        @Override
        public String getName() {
            return file.getAbsolutePath();
        }
    }

    class OutputStreamTarget implements Target {
        private final OutputStream os;

        public OutputStreamTarget(OutputStream os) {
            this.os = os;
        }

        @Override
        public OutputStream getOutputStream() {
            return os;
        }

        @Override
        public String getName() {
            return os.getClass().getName();
        }
    }
}
