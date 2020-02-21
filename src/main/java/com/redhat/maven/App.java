package com.redhat.maven;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Application to analyze file conflicts in Nexus repositories
 *
 */
public class App 
{

    private ExitResolver exitResolver;

    App(ExitResolver exitResolver) {
        this.exitResolver = exitResolver;
    }

    public App() {
        this.exitResolver = new ExitResolver();
    }

    public static void main( String[] args ) {
        (new App()).resolve(args);
    }

    /** Class for exiting the program with a return value. It will be replaced in tests.
     */
    static class ExitResolver {
         void finishProcessing(int value) {
            System.exit(value);
        }
    }

    void resolve(String[] args)
    {
        if(args.length < 1 || args[0].equals("--help")) {
            help();
            return;
        }

        analyzeParameters(args);

        List<File> repositoriesToAnalyze = new ArrayList<>();

        sourceDirectories.forEach(dir -> {
            File[] repositories = dir.listFiles(File::isDirectory);
            if(repositories != null) {
                Arrays.stream(repositories)
                    .filter(repo -> includeRepos.stream().anyMatch(repo.getName()::startsWith))
                    .filter(repo -> excludeRepos.stream().noneMatch(repo.getName()::startsWith))
                    .forEach(repositoriesToAnalyze::add);
            }
        });

        switch (repositoriesToAnalyze.size()) {
            case 0:
                System.err.println("No repositories to analyze. Check arguments!");
                exitResolver.finishProcessing(ERR_NOT_ENOUGH_REPOSITORIES);
            case 1:
                System.err.println("Only single repository matched the requirements!");
                exitResolver.finishProcessing(ERR_NOT_ENOUGH_REPOSITORIES);
            default:
                System.out.println("Analyzing "+repositoriesToAnalyze.size()+" repositories.");
        }

        if(StringUtils.isNotBlank(outputFileName)) {
            File testFile = new File(outputFileName);
            if(testFile.isFile() && !forceOverwrite) {
                System.err.println("Error: File "+outputFileName+" already exist!");
                exitResolver.finishProcessing(ERR_FILE_ALREADY_EXIST);
            }

            if(testFile.isFile() && !testFile.canWrite()) {
                System.err.println("Error: Writing to "+outputFileName+" is forbidden!");
                exitResolver.finishProcessing(ERR_ACCESS_DENIED);
            }
        }

        for (File file : repositoriesToAnalyze) {
            processRepository(file);
        }

        System.out.println("Processing differences.");
        for(String path : fileInfosMap.keySet()) {
            if(!FileInfo.compareInfos(fileInfosMap.get(path).toArray(new FileInfo[0]))) {
                errorsFound.add(path);
            }
        }

        if(errorsFound.isEmpty()) {
            System.out.println("No discrepancies found.");
        } else {
            if(outputFileName == null) {
                System.out.println("Writing results to "+outputFileName);
                PrintWriter output = new PrintWriter(System.out);
                printHeader(output);
                errorsFound.stream().sorted().forEach(path -> getFileInfosMap().get(path).stream().sorted().forEach(fileInfo -> printEntry(fileInfo, output)));
            } else {
                try (PrintWriter output =  new PrintWriter(Files.newOutputStream(Paths.get(outputFileName)))) {
                    printHeader(output);
                    errorsFound.stream().sorted().forEach(path -> getFileInfosMap().get(path).stream().sorted().forEach(fileInfo -> printEntry(fileInfo, output)));
                } catch (IOException e) {
                    e.printStackTrace();
                    exitResolver.finishProcessing(ERR_ACCESS_DENIED);
                }
            }
        }
    }


    /** Writes one formatted fileInfo entity
     *
     * @param fileInfo information about file
     * @param printWriter target writer object
     */
    private static void printEntry(FileInfo fileInfo, PrintWriter printWriter) {
        printWriter.format("\"%s\",\"%s\",%s,%d%n", fileInfo.getRepositoryName(), fileInfo.getRelativePath(), fileInfo.getMd5(), fileInfo.getSize());
    }

    /** Prints the header of the CSV file
     *
     * @param printWriter print writer for outpu
     */
    private static void printHeader(PrintWriter printWriter) {
        printWriter.println("\"repository\",\"file\",\"checksum\",\"size\"");
    }

    public List<String> getErrorsFound() {
        return errorsFound;
    }

    private List<String> errorsFound = new ArrayList<>();

    private void processRepository(File repository) {
        System.out.println("... processing "+repository.getName());
        processDirectory(repository, repository);
    }

    private HashMap<String, ArrayList<FileInfo>> fileInfosMap = new HashMap<>();

    private  void processDirectory(@NotNull File directory, File repository) {
        final File[] files = directory.listFiles(file -> {
            if (!file.canRead() ) {
                System.err.println("Error: Can not read " + file.getAbsolutePath() + "!");
                exitResolver.finishProcessing(ERR_ACCESS_DENIED);
            }

            if(file.getName().startsWith(".") || file.getName().startsWith("maven-metadata.xml") || file.getName().endsWith(".md5") || file.getName().endsWith(".sha1"))
                return false; // remove Nexus index files and directories and ignore maven-metadata.xml, that will be generated by Nexus

            if (file.isDirectory()) {
                processDirectory(file, repository);
                return false; // a directory was parsed in the previous step and does not need to be processed again
            } else {
                //
                return true;
            }

        });

        if(files == null)
            return;

        for (File file : files) {
            // this is a file and needs to be processed

            try (InputStream is = Files.newInputStream(Paths.get(file.getPath()))) {
                MessageDigest md = MessageDigest.getInstance("MD5");
                DigestInputStream digestInputStream = new DigestInputStream(is, md);

                byte[] buffer = new byte[(int)Long.min(file.length(), 0xFFFF)];

                //noinspection StatementWithEmptyBody
                while(digestInputStream.read(buffer) > 0);  // read whole file

                FileInfo fileInfo = new FileInfo(file.length(), file.getAbsolutePath().substring(repository.getAbsolutePath().length()+1), DatatypeConverter.printHexBinary(md.digest()), repository.getName());
                fileInfosMap.computeIfAbsent(fileInfo.getRelativePath(), key->new ArrayList<>()).add(fileInfo);
            } catch (IOException | NoSuchAlgorithmException e) {
                System.err.println("Error calculating checksum: "+e.getMessage());
                e.printStackTrace();
            }



        }

    }

    private  List<String> includeRepos = new ArrayList<>(),
        excludeRepos = new ArrayList<>();

    private  List<File> sourceDirectories = new ArrayList<>();

    private static final int INCLUDE = 1;
    private static final int EXCLUDE = 2;
    private static final int OUTPUT_FILE = 3;
    private static final int NOTHING = 0;


    static final int ERR_WRONG_PARAMETERS = 1;
    static final int ERR_ACCESS_DENIED = 2;
    static final int ERR_NOT_ENOUGH_REPOSITORIES = 3;
    static final int ERR_FILE_ALREADY_EXIST = 4;

    private boolean forceOverwrite = false;
    private String outputFileName;

    void analyzeParameters(String[] args) {

        int status = NOTHING;

        for(int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--force":
                case "-F":
                    forceOverwrite = true; break;
                case "--file":
                    if(status != NOTHING) {
                        System.err.println("Chained switches on parameter "+i+1 );
                        exitResolver.finishProcessing(ERR_WRONG_PARAMETERS);
                    }
                    status = OUTPUT_FILE;
                    break;
                case "--include":
                    if(status != NOTHING) {
                        System.err.println("Chained switches on parameter "+i+1 );
                        exitResolver.finishProcessing(ERR_WRONG_PARAMETERS);
                    }
                    status = INCLUDE;
                    break;
                case "--exclude":
                    if(status != NOTHING) {
                        System.err.println("Chained switches on parameter "+i+1 );
                        exitResolver.finishProcessing(ERR_WRONG_PARAMETERS);
                    }
                    status = EXCLUDE;
                    break;
                case "--help":
                    help(); return;
                default:
                    if(args[i].startsWith("-")) {
                        System.err.println("Invalid switch: "+args[i]);
                        exitResolver.finishProcessing(ERR_WRONG_PARAMETERS);
                    }

                    switch (status) {
                        case INCLUDE: includeRepos.add(args[i]); status = NOTHING; break;
                        case EXCLUDE: excludeRepos.add(args[i]); status = NOTHING; break;
                        case OUTPUT_FILE:
                            if(StringUtils.isBlank(outputFileName)) {
                                outputFileName = args[i];
                                status = NOTHING;
                                break;
                            } else {
                                System.err.println("Error: Output file was entered more than once!");
                                exitResolver.finishProcessing(ERR_WRONG_PARAMETERS);
                            }
                        default:
                            File file = new File(args[i]);
                            if(file.isDirectory()) {
                                if(file.canRead()) {
                                    sourceDirectories.add(file);
                                } else {
                                    System.err.println("Error: Can not read directory "+file.getAbsolutePath());
                                    exitResolver.finishProcessing(ERR_ACCESS_DENIED);
                                }
                            }
                    }
            }
        }

        if(includeRepos.isEmpty())
            includeRepos.add("");

        if(sourceDirectories.isEmpty()) {
            System.err.println("Error: You must supply at least one source directory.");
            help();
            exitResolver.finishProcessing(ERR_WRONG_PARAMETERS);
        }
    }

    HashMap<String, ArrayList<FileInfo>> getFileInfosMap() {
        return fileInfosMap;
    }

    List<String> getIncludeRepos() {
        return includeRepos;
    }

    List<String> getExcludeRepos() {
        return excludeRepos;
    }

    List<File> getSourceDirectories() {
        return sourceDirectories;
    }

    private static void help() {
        System.out.println("Maven Repository Collision Analyzer");
        System.out.println("-----------------------------------");
        System.out.println("java -jar repo-analyzer [options] [<directory>...]");
        System.out.println("At least one source directory must be supplied.");
        System.out.println("Options:");
        System.out.println("--file <file name> - output file for the data");
        System.out.println("-F /--force - force overwriting the output file");
        System.out.println("--include <start of repository name> - name of the repository (folders starting with this string will be included");
        System.out.println("--exclude <start of repository to exclude> - repos starting on this will be excluded. --excluded has a higher priority");
        System.out.println("       than --include");
        System.out.println("There can be multiple --include and --exclude parameters");
    }
}
