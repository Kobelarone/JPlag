package de.jplag.reporting.reportobject;

import static de.jplag.reporting.reportobject.mapper.SubmissionNameToIdMapper.buildSubmissionNameToIdMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.jplag.JPlag;
import de.jplag.JPlagComparison;
import de.jplag.JPlagResult;
import de.jplag.Language;
import de.jplag.Submission;
import de.jplag.options.JPlagOptions;
import de.jplag.reporting.FilePathUtil;
import de.jplag.reporting.jsonfactory.ComparisonReportWriter;
import de.jplag.reporting.reportobject.mapper.ClusteringResultMapper;
import de.jplag.reporting.reportobject.mapper.MetricMapper;
import de.jplag.reporting.reportobject.model.OverviewReport;
import de.jplag.reporting.reportobject.model.SubmissionFile;
import de.jplag.reporting.reportobject.model.SubmissionFileIndex;
import de.jplag.reporting.reportobject.model.Version;
import de.jplag.reporting.reportobject.writer.JPlagResultWriter;
import de.jplag.reporting.reportobject.writer.ZipWriter;

/**
 * Factory class, responsible for converting a JPlagResult object to Overview and Comparison DTO classes and writing it
 * to the disk.
 */
public class ReportObjectFactory {
    private static final Logger logger = LoggerFactory.getLogger(ReportObjectFactory.class);

    public static final Path OVERVIEW_FILE_NAME = Path.of("overview.json");

    public static final Path README_FILE_NAME = Path.of("README.txt");
    public static final Path OPTIONS_FILE_NAME = Path.of("options.json");
    private static final String[] README_CONTENT = new String[] {"This is a software plagiarism report generated by JPlag.",
            "To view the report go to https://jplag.github.io/JPlag/ and drag the generated zip file onto the page."};

    public static final Path SUBMISSION_FILE_INDEX_FILE_NAME = Path.of("submissionFileIndex.json");
    public static final Version REPORT_VIEWER_VERSION = JPlag.JPLAG_VERSION;

    private static final Path SUBMISSIONS_ROOT_PATH = Path.of("files");

    private Map<String, String> submissionNameToIdMap;
    private Function<Submission, String> submissionToIdFunction;
    private Map<String, Map<String, String>> submissionNameToNameToComparisonFileName;

    private final JPlagResultWriter resultWriter;

    /**
     * Creates a new report object factory, that can be used to write a report.
     * @param resultWriter The writer to use for writing report content
     */
    public ReportObjectFactory(JPlagResultWriter resultWriter) {
        this.resultWriter = resultWriter;
    }

    /**
     * Creates a new report object factory, that can be used to write a zip report.
     * @param zipFile The zip file to write the report to
     * @throws FileNotFoundException If the file cannot be opened for writing
     */
    public ReportObjectFactory(File zipFile) throws FileNotFoundException {
        this(new ZipWriter(zipFile));
    }

    /**
     * Creates all necessary report viewer files, writes them to the disk as zip.
     * @param result The JPlagResult to be converted into a report.
     */
    public void createAndSaveReport(JPlagResult result) {
        logger.info("Start writing report...");
        buildSubmissionToIdMap(result);

        copySubmissionFilesToReport(result);

        writeComparisons(result);
        writeOverview(result);
        writeSubmissionIndexFile(result);
        writeReadMeFile();
        writeOptionsFiles(result.getOptions());

        this.resultWriter.close();
    }

    private void buildSubmissionToIdMap(JPlagResult result) {
        submissionNameToIdMap = buildSubmissionNameToIdMap(result);
        submissionToIdFunction = (Submission submission) -> submissionNameToIdMap.get(submission.getName());
    }

    private void copySubmissionFilesToReport(JPlagResult result) {
        logger.info("Start to export results...");
        List<JPlagComparison> comparisons = result.getComparisons(result.getOptions().maximumNumberOfComparisons());
        Set<Submission> submissions = getSubmissions(comparisons);
        Language language = result.getOptions().language();
        for (Submission submission : submissions) {
            Path submissionRootPath = SUBMISSIONS_ROOT_PATH.resolve(FilePathUtil.createRelativePath(submissionToIdFunction.apply(submission)));
            for (File file : submission.getFiles()) {
                Path relativeFilePath = Path.of(submission.getRoot().getAbsolutePath()).relativize(Path.of(file.getAbsolutePath()));
                if (relativeFilePath.getNameCount() == 0 || relativeFilePath.equals(Path.of(""))) {
                    relativeFilePath = Path.of(file.getName());
                }
                Path zipPath = submissionRootPath.resolve(relativeFilePath);

                File fileToCopy = getFileToCopy(language, file);
                this.resultWriter.addFileContentEntry(zipPath, fileToCopy);
            }
        }
    }

    private File getFileToCopy(Language language, File file) {
        return language.useViewFiles() ? new File(file.getPath() + language.viewFileSuffix()) : file;
    }

    private void writeComparisons(JPlagResult result) {
        ComparisonReportWriter comparisonReportWriter = new ComparisonReportWriter(submissionToIdFunction, this.resultWriter);
        submissionNameToNameToComparisonFileName = comparisonReportWriter.writeComparisonReports(result);
    }

    private void writeOverview(JPlagResult result) {
        List<File> folders = new ArrayList<>();
        folders.addAll(result.getOptions().submissionDirectories());
        folders.addAll(result.getOptions().oldSubmissionDirectories());

        String baseCodePath = result.getOptions().hasBaseCode() ? result.getOptions().baseCodeSubmissionDirectory().getName() : "";
        ClusteringResultMapper clusteringResultMapper = new ClusteringResultMapper(submissionToIdFunction);

        int totalComparisons = result.getAllComparisons().size();
        int numberOfMaximumComparisons = result.getOptions().maximumNumberOfComparisons();
        int shownComparisons = Math.min(totalComparisons, numberOfMaximumComparisons);
        int missingComparisons = totalComparisons > numberOfMaximumComparisons ? (totalComparisons - numberOfMaximumComparisons) : 0;
        logger.info("Total Comparisons: {}. Comparisons in Report: {}. Omitted Comparisons: {}.", totalComparisons, shownComparisons,
                missingComparisons);
        OverviewReport overviewReport = new OverviewReport(REPORT_VIEWER_VERSION, folders.stream().map(File::getPath).toList(), // submissionFolderPath
                baseCodePath, // baseCodeFolderPath
                result.getOptions().language().getName(), // language
                result.getOptions().fileSuffixes(), // fileExtensions
                submissionNameToIdMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey)), // submissionIds
                submissionNameToNameToComparisonFileName, // result.getOptions().getMinimumTokenMatch(),
                List.of(), // failedSubmissionNames
                result.getOptions().excludedFiles(), // excludedFiles
                result.getOptions().minimumTokenMatch(), // matchSensitivity
                getDate(),// dateOfExecution
                result.getDuration(), // executionTime
                MetricMapper.getDistributions(result), // distribution
                new MetricMapper(submissionToIdFunction).getTopComparisons(result),// topComparisons
                clusteringResultMapper.map(result), // clusters
                totalComparisons); // totalComparisons

        this.resultWriter.addJsonEntry(overviewReport, OVERVIEW_FILE_NAME);

    }

    private void writeReadMeFile() {
        this.resultWriter.writeStringEntry(String.join(System.lineSeparator(), README_CONTENT), README_FILE_NAME);
    }

    private void writeSubmissionIndexFile(JPlagResult result) {
        List<JPlagComparison> comparisons = result.getComparisons(result.getOptions().maximumNumberOfComparisons());
        Set<Submission> submissions = getSubmissions(comparisons);
        SubmissionFileIndex fileIndex = new SubmissionFileIndex(new HashMap<>());

        List<Map<String, Map<String, SubmissionFile>>> submissionTokenCountList = submissions.stream().parallel().map(submission -> {
            Map<String, SubmissionFile> tokenCounts = new HashMap<>();
            for (Map.Entry<File, Integer> entry : submission.getTokenCountPerFile().entrySet()) {
                String key = FilePathUtil.getRelativeSubmissionPath(entry.getKey(), submission, submissionToIdFunction);
                tokenCounts.put(key, new SubmissionFile(entry.getValue()));
            }
            return Map.of(submissionNameToIdMap.get(submission.getName()), tokenCounts);
        }).toList();

        submissionTokenCountList.forEach(submission -> fileIndex.fileIndexes().putAll(submission));
        this.resultWriter.addJsonEntry(fileIndex, SUBMISSION_FILE_INDEX_FILE_NAME);
    }

    private void writeOptionsFiles(JPlagOptions options) {
        resultWriter.addJsonEntry(options, OPTIONS_FILE_NAME);
    }

    private Set<Submission> getSubmissions(List<JPlagComparison> comparisons) {
        Set<Submission> submissions = comparisons.stream().map(JPlagComparison::firstSubmission).collect(Collectors.toSet());
        Set<Submission> secondSubmissions = comparisons.stream().map(JPlagComparison::secondSubmission).collect(Collectors.toSet());
        submissions.addAll(secondSubmissions);
        return submissions;
    }

    private String getDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
        Date date = new Date();
        return dateFormat.format(date);
    }
}
