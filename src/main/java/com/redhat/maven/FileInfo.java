package com.redhat.maven;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** Information about specific file
 */
class FileInfo implements Comparable<FileInfo> {
    private String md5;
    private String relativePath;
    private long size;
    private String repositoryName;

    /** Constructor
     *
     * @param size file size
     * @param relativePath relative path of the file within the repository
     * @param md5 MD5 checksum
     */
    public FileInfo(long size, String relativePath, String md5, String repositoryName) {
        this.md5 = md5;
        this.relativePath = relativePath;
        this.size = size;
        this.repositoryName = repositoryName;
    }


    public String getMd5() {
        return md5;
    }

    public long getSize() {
        return size;
    }

    public String getRelativePath() {
        return relativePath;
    }

    /** Compares if files are similar
     *
     * @param infos file infos to
     * @return true if the files are similar (checksum, releative path)
     */
    static boolean compareInfos(FileInfo... infos ) {
        if(infos == null || infos.length == 0)
            return false;

        if(infos.length == 1)
            return true;


        for(int i = 0; i < (infos.length-1); i++) {
            if( infos[i].getSize() != infos[i+1].getSize() ||
                    !Objects.equals(infos[i].getRelativePath(), infos[i+1].getRelativePath()) ||
                    !Objects.equals(infos[i].getMd5(), infos[i+1].getMd5()))
                return false;
        }
        return true;

    }

    public String getRepositoryName() {
        return repositoryName;
    }

    @Override
    public int compareTo(@NotNull FileInfo o) {
        int result = getRelativePath().compareTo(o.getRelativePath());
        if(result == 0)
            result = getRepositoryName().compareTo(o.getRepositoryName());

        return result;
    }
}
