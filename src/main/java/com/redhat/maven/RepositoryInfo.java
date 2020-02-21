package com.redhat.maven;

import java.io.File;
import java.util.List;

/** Information about a Maven Repository
 *
 */
public class RepositoryInfo {
    private final File directory;
    private List<FileInfo> files;

    public RepositoryInfo(File directory) {
        this.directory = directory;
    }

    /** Returns name of the repository.
     *
     * @return name of the directory the data are stored in
     */
    public String getName() {
        return directory.getName();
    }
}
