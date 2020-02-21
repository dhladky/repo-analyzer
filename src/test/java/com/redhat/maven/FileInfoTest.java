package com.redhat.maven;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class FileInfoTest {
    private FileInfo f1, f2, f3, f4;

    @Test
    public void compareInfosFree() {
        assertFalse(FileInfo.compareInfos((FileInfo[]) null));
        assertFalse(FileInfo.compareInfos());
    }

    @Test
    public void compareInfosSingle() {
        f1 = new FileInfo(10, "org/jboss/something", "1234", "repo1");
        assertTrue(FileInfo.compareInfos( f1));
    }
    @Test
    public void compareInfosMultipleTrue() {
        f1 = new FileInfo(10, "org/jboss/something", "1234", "repo1");
        f2 = new FileInfo(10, "org/jboss/something", "1234", "repo2");
        f3 = new FileInfo(10, "org/jboss/something", "1234", "repo3");
        f4 = new FileInfo(10, "org/jboss/something", "1234", "repo4");

        assertTrue(FileInfo.compareInfos(f1, f2, f3, f4));
    }

    @Test
    public void compareInfosMultipleFalse() {
        f1 = new FileInfo(10, "org/jboss/something", "1234", "repo1");
        f2 = new FileInfo(10, "org/jboss/something", "1234", "repo2");
        f3 = new FileInfo(10, "org/jboss/something", "1234", "repo3");
        f4 = new FileInfo(10, "org/jboss/something", "wrong", "repo4");

        assertFalse(FileInfo.compareInfos(f1, f2, f3, f4));
        assertFalse(FileInfo.compareInfos(f4, f3, f2, f1));
        assertFalse(FileInfo.compareInfos(f3, f2, f1, f4));
        assertFalse(FileInfo.compareInfos(f1, f4, f3, f2));
    }

    @Test
    public void compareInfosMultiplePathWrong() {
        f1 = new FileInfo(10, "org/jboss/something", "1234", "repo1");
        f2 = new FileInfo(10, "org/jboss/something_else", "1234", "repo2");
        assertFalse(FileInfo.compareInfos(f1, f2));
    }

    @Test
    public void compareInfosMultipleSizeWrong() {
        f1 = new FileInfo(10, "org/jboss/something", "1234", "repo1");
        f2 = new FileInfo(15, "org/jboss/something", "1234", "repo2");

        assertFalse(FileInfo.compareInfos(f1, f2));
    }

    @Test
    public void compareInfosMultipleMD5Wrong() {
        f1 = new FileInfo(10, "org/jboss/something", "1234", "repo1");
        f2 = new FileInfo(10, "org/jboss/something", "1235", "repo2");
        assertFalse(FileInfo.compareInfos(f1,f2));
    }

    @Before
    public void setup() {
        f1 = new FileInfo(10, "org/jboss/something", "1234", "repo1");
        f2 = new FileInfo(10, "org/jboss/something", "1234", "repo2");
        f3 = new FileInfo(10, "org/jboss/something", "1234", "repo3");
        f4 = new FileInfo(10, "org/jboss/something", "1234", "repo4");
    }
}