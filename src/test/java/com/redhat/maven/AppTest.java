package com.redhat.maven;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    @Test
    public void analyzeParametersNothing() {
        String[] args = new String[]{};
        test.resolve(args); // nothing fails
    }

    @Test
    public void analyzeParametersHelp() {
        String[] args = new String[]{"--help"};
        test.resolve(args); // nothing fails
    }

    @Test
    public void analyzeParametersWrongArgument() throws IOException {
        initializeDirectories();
        String[] args = new String[]{"--wrong", sourceDirectory1, sourceDirectory2};
        try {
            test.resolve(args);
            throw new RuntimeException("App should have failed!");
        } catch (EndAppException e) {
            assertEquals(App.ERR_WRONG_PARAMETERS, e.getReturnValue());
        }
    }

    @Test
    public void analyzeParametersIncompleteDefinitionInclude() throws IOException {
        initializeDirectories();
        String[] args = new String[]{"--include", "--include", sourceDirectory1, sourceDirectory2};
        try {
            test.analyzeParameters(args);
            throw new RuntimeException("App should have failed!");
        } catch (EndAppException e) {
            assertEquals(App.ERR_WRONG_PARAMETERS, e.getReturnValue());
        }
    }

    @Test
    public void analyzeParametersIncompleteDefinitionExclude() throws IOException {
        initializeDirectories();
        String[] args = new String[]{"--exclude", "--include", sourceDirectory1, sourceDirectory2};
        try {
            test.analyzeParameters(args); // nothing fails
            throw new RuntimeException("App should have failed!");
        } catch (EndAppException e) {
            assertEquals(App.ERR_WRONG_PARAMETERS, e.getReturnValue());
        }
    }

    @Test
    public void analyzeParametersNoExceptions() throws IOException {
        initializeDirectories();
        String[] args = new String[]{sourceDirectory1, sourceDirectory2};

        test.analyzeParameters(args); // nothing fails
        assertTrue(test.getExcludeRepos().isEmpty());
        assertEquals(1, test.getIncludeRepos().size());
        assertEquals("", test.getIncludeRepos().get(0));
    }

    @Test
    public void analyzeParametersIncludesExcludes() throws IOException {
        initializeDirectories();
        String[] args = new String[]{"--include", "incl", "--exclude", "excl1", "--exclude", "excl2",  sourceDirectory1, sourceDirectory2};

        test.analyzeParameters(args); // nothing fails
        assertEquals(1, test.getIncludeRepos().size());
        assertEquals("incl", test.getIncludeRepos().get(0));

        assertEquals(2, test.getExcludeRepos().size());
        assertTrue(test.getExcludeRepos().contains("excl1"));
        assertTrue(test.getExcludeRepos().contains("excl2"));

        Set<String> recognizedFiles = test.getSourceDirectories().stream().map(File::getAbsolutePath).collect(Collectors.toSet());
        assertTrue(recognizedFiles.contains(sourceDirectory1));
        assertTrue(recognizedFiles.contains(sourceDirectory2));
    }




    @Test
    public void processCompare() throws IOException {
        initializeWithTestFiles();

        String[] args = new String[]{sourceDirectory1, sourceDirectory2};
        test.resolve(args);

        assertEquals("Detected artifacts", 6, test.getFileInfosMap().size());

        // Check all artifacts present
        assertEquals(4, test.getFileInfosMap().get("com/redhat/something/same1.txt").size());
        assertEquals(2, test.getFileInfosMap().get("com/redhat/same3.txt").size());
        assertEquals(2 , test.getFileInfosMap().get("com/redhat/something/same2.txt").size());
        assertEquals(1, test.getFileInfosMap().get("com/redhat/something/justOneFile.txt").size());
        assertEquals(2, test.getFileInfosMap().get("com/redhat/something/different.txt").size());
        assertEquals(2, test.getFileInfosMap().get("com/redhat/something/different2.txt").size());

        assertEquals("Not matching file patterns found", 2, test.getErrorsFound().size());
        assertTrue(test.getErrorsFound().contains("com/redhat/something/different.txt"));
        assertTrue(test.getErrorsFound().contains("com/redhat/something/different2.txt"));
    }

    @Test
    public void testInclude() throws IOException {
        initializeWithTestFiles();

        String[] args = new String[]{"--include", "repo", sourceDirectory1, sourceDirectory2};

        test.resolve(args);

        assertEquals("repo", test.getIncludeRepos().get(0));

        // All repos match this pattern - should be just like without
        assertEquals(6, test.getFileInfosMap().size());
        assertEquals(4, test.getFileInfosMap().get("com/redhat/something/same1.txt").size());
        assertEquals(2, test.getFileInfosMap().get("com/redhat/same3.txt").size());
        assertEquals(2 , test.getFileInfosMap().get("com/redhat/something/same2.txt").size());
        assertEquals(1, test.getFileInfosMap().get("com/redhat/something/justOneFile.txt").size());
        assertEquals(2, test.getFileInfosMap().get("com/redhat/something/different.txt").size());
        assertEquals(2, test.getFileInfosMap().get("com/redhat/something/different2.txt").size());

        assertEquals("Not matching file patterns found", 2, test.getErrorsFound().size());
        assertTrue(test.getErrorsFound().contains("com/redhat/something/different.txt"));
        assertTrue(test.getErrorsFound().contains("com/redhat/something/different2.txt"));

        args = new String[]{"--include", "repo3", "--include", "repo4",  sourceDirectory1, sourceDirectory2};
        test = new App(new ExitResolverTest());
        test.resolve(args);

        assertTrue(test.getIncludeRepos().contains("repo3"));
        assertTrue(test.getIncludeRepos().contains("repo4"));

        assertTrue("Offending file ignored.", test.getErrorsFound().isEmpty());
        assertEquals(2, test.getFileInfosMap().size());

        assertTrue(test.getFileInfosMap().values().stream().noneMatch( value -> value.size() > 2 || value.size()<1));
    }


    @Test
    public void testNoReposSelected() throws IOException {
        try {
            initializeWithTestFiles();
            String[] args = new String[]{"--exclude", "repo", sourceDirectory1, sourceDirectory2};
            test.resolve(args);
            throw new RuntimeException("Application should have failed");
        } catch (EndAppException e) {
            assertEquals(App.ERR_NOT_ENOUGH_REPOSITORIES, e.getReturnValue());
        }

    }

    @Test
    public void testOneReposSelected() throws IOException {
        try {
            initializeWithTestFiles();
            String[] args = new String[]{"--include", "repo1", sourceDirectory1, sourceDirectory2};
            test.resolve(args);
            throw new RuntimeException("Application should have failed");
        } catch (EndAppException e) {
            assertEquals(App.ERR_NOT_ENOUGH_REPOSITORIES, e.getReturnValue());
        }

    }

    @Test
    public void testExclude() throws IOException {
        initializeWithTestFiles();

        String[] args = new String[]{"--exclude", "repo1", sourceDirectory1, sourceDirectory2};
        test = new App(new ExitResolverTest());
        test.resolve(args);

        assertTrue(test.getFileInfosMap().keySet()
                .stream().noneMatch( key -> test.getFileInfosMap().get(key).stream().anyMatch(fileInfo -> fileInfo.getRepositoryName().startsWith("repo1"))));


        ArrayList<FileInfo> limitedSet = test.getFileInfosMap().get("com/redhat/something/same1.txt");
        assertTrue(limitedSet.stream().map(FileInfo::getRepositoryName).noneMatch("repo1"::startsWith));
        assertTrue(limitedSet.stream().map(FileInfo::getRepositoryName).anyMatch("repo2"::startsWith));
        assertTrue(limitedSet.stream().map(FileInfo::getRepositoryName).anyMatch("repo3"::startsWith));
        assertTrue(limitedSet.stream().map(FileInfo::getRepositoryName).anyMatch("repo4"::startsWith));

        args = new String[]{"--exclude", "repo1", "--exclude", "repo3", sourceDirectory1, sourceDirectory2};
        test = new App(new ExitResolverTest());
        test.resolve(args);

        assertTrue(test.getFileInfosMap().keySet()
                .stream().noneMatch( key -> test.getFileInfosMap().get(key).stream().anyMatch(fileInfo -> fileInfo.getRepositoryName().startsWith("repo1"))));


        limitedSet = test.getFileInfosMap().get("com/redhat/something/same1.txt");
        assertTrue(limitedSet.stream().map(FileInfo::getRepositoryName).noneMatch("repo1"::startsWith));
        assertTrue(limitedSet.stream().map(FileInfo::getRepositoryName).anyMatch("repo2"::startsWith));
        assertTrue(limitedSet.stream().map(FileInfo::getRepositoryName).noneMatch("repo3"::startsWith));
        assertTrue(limitedSet.stream().map(FileInfo::getRepositoryName).anyMatch("repo4"::startsWith));
    }




    private App test;

    private String sourceDirectory1;
    private String sourceDirectory2;

    private void initializeDirectories() throws IOException {
        sourceDirectory1 = temporaryFolder.newFolder("source1").getAbsolutePath();
        sourceDirectory2 = temporaryFolder.newFolder("source2").getAbsolutePath();
    }


    /** Create a test file and fill it with some content.
     *
     * @param repo repository where the file is to be put
     * @param pathWithinRepo path within the repo
     * @param content the text to be written to the file
     */

    private static void writeFile(String repo, String pathWithinRepo, String content) throws IOException {
        StringBuilder newPath = new StringBuilder(repo);

        if(pathWithinRepo.startsWith("/")) {
            newPath.append(pathWithinRepo);
        } else
            newPath.append('/').append(pathWithinRepo);

        File file = (new File(newPath.toString()));


        //noinspection ResultOfMethodCallIgnored
        (new File(file.getParent())).mkdirs(); // create the path to the repository if necessary

        assertTrue("Unable to create the test file!", file.createNewFile());

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(file.getPath())))) {
            writer.println(content);
        }
    }

    @Test
    public void testOverWriteFileFail() throws IOException {
        initializeWithTestFiles();

        TemporaryFolder newOutFolder = new TemporaryFolder();
        newOutFolder.create();
        File outputFile = newOutFolder.newFile("output.csv");


        String[] args = new String[]{"--file", outputFile.getPath(), sourceDirectory1, sourceDirectory2};
        test = new App(new ExitResolverTest());
        try {
            test.resolve(args);
            throw new RuntimeException("Exception should have been thrown!");
        } catch (EndAppException e) {
            assertEquals(App.ERR_FILE_ALREADY_EXIST, e.getReturnValue());
            assertEquals(0, outputFile.length());
        }
    }

    @Test
    public void testOverWriteFileForce() throws IOException {
        initializeWithTestFiles();

        TemporaryFolder newOutFolder = new TemporaryFolder();
        newOutFolder.create();
        File outputFile = newOutFolder.newFile("output.csv");

        String[] args = new String[]{"--file", outputFile.getPath(), "-F", sourceDirectory1, sourceDirectory2};
        test = new App(new ExitResolverTest());
        test.resolve(args);

        assertTrue(outputFile.isFile());
        assertTrue(0 < outputFile.length());

    }




    private void initializeWithTestFiles() throws IOException {
        initializeDirectories();

        // multiple identical instances across the same and different directories
        writeFile(sourceDirectory1, "repo1/com/redhat/something/same1.txt", "same1 text");
        writeFile(sourceDirectory1, "repo3/com/redhat/something/same1.txt", "same1 text");
        writeFile(sourceDirectory1, "repo4/com/redhat/something/same1.txt", "same1 text");
        writeFile(sourceDirectory2, "repo2/com/redhat/something/same1.txt", "same1 text");

        // some more
        writeFile(sourceDirectory1, "repo1/com/redhat/something/same2.txt", "same2 text");
        writeFile(sourceDirectory2, "repo2/com/redhat/something/same2.txt", "same2 text");
        writeFile(sourceDirectory1, "repo1/com/redhat/same3.txt", "same3 text");
        writeFile(sourceDirectory2, "repo2/com/redhat/same3.txt", "same3 text");

        // a file just once in the system
        writeFile(sourceDirectory1, "repo1/com/redhat/something/justOneFile.txt", "File just once");

        // different in different directories
        writeFile(sourceDirectory1, "repo1/com/redhat/something/different.txt", "One text");
        writeFile(sourceDirectory2, "repo2/com/redhat/something/different.txt", "Another text");

        // different in the same directories
        writeFile(sourceDirectory1, "repo1/com/redhat/something/different2.txt", "One text");
        writeFile(sourceDirectory1, "repo3/com/redhat/something/different2.txt", "Another text");
    }


    @Before
    public void setup() {
        test = new App(new ExitResolverTest());
    }

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
}
