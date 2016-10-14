package nablarch.tool.workflow;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nablarch.core.util.DateUtil;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;

import nablarch.integration.workflow.definition.WorkflowDefinition;

/**
 * ワークフロー定義データを生成するクラス。
 * 本クラスがツールのmainメソッドを提供する。
 *
 * @author Satoshi Taromaru
 * @since 1.4.2
 */
public final class WorkflowDefinitionGenerator {

    /**
     * ファイル名Pattern。
     */
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("(.+)_(.+)_ver(.+)_(.+)\\.(.+)");

    /**
     * 隠蔽コンストラクタ。
     */
    private WorkflowDefinitionGenerator() {
    }

    /**
     * ワークフロー定義データを生成する。
     *
     * @param args 実行時引数
     */
    public static void main(String... args) {
        if (args.length != 1) {
            System.err.println("引数には、コンポーネント定義ファイルのパスを一つだけ指定してください。"
                    + "（例：java nablarch.tool.workflow.WorkflowDefinitionGenerator WorkflowDefinitionGenerator.xml） 実際の引数 = " + Arrays.toString(args));
            return;
        }
        new WorkflowDefinitionGenerator().generate(args[0]);
    }

    /**
     * ワークフロー定義データを生成する。
     *
     * @param componentDefinitionFilePath コンポーネント定義ファイルパス
     */
    private void generate(String componentDefinitionFilePath) {
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(componentDefinitionFilePath);
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
        WorkflowDefinitionGeneratorSettings settings = SystemRepository.get("workflowDefinitionGeneratorSettings");

        String inputFileDirPath = settings.getInputFileDir();
        File inputFileDir = new File(inputFileDirPath);
        if (!inputFileDir.exists()) {
            System.err.println(String.format("ワークフロー定義ファイル格納ディレクトリが不正です。存在するディレクトリを指定してください。"
                    + " 設定値 = [%s]", inputFileDirPath));
            return;
        }
        if (!inputFileDir.isDirectory()) {
            System.err.println(String.format("ワークフロー定義ファイル格納ディレクトリが不正です。ディレクトリを指定してください。"
                    + " 設定値 = [%s]", inputFileDirPath));
            return;
        }

        String logFilePath = settings.getLogFilePath();
        ErrorReport.open(logFilePath);
        WorkflowDefinitionWriter writer = settings.getWorkflowDefinitionWriter();
        writer.open();
        try {
            for (WorkflowDefinitionFile definitionFile : findDefinitionFiles(new File(inputFileDirPath))) {
                writeWorkflowDefinition(definitionFile, writer);
            }
        } finally {
            writer.close();
            ErrorReport.close();
        }

        if (ErrorReport.hasAnomalyDetection()) {
            System.err.println(String.format("精査エラーが発生しました。詳細は次のファイルを確認してください。 [%s]", logFilePath));
        }
    }


    /**
     * ワークフロー定義ファイル格納ディレクトリ内の、読み取り対象ワークフロー定義ファイルを取得する。
     *
     * @param inputDir ワークフロー定義ファイル格納ディレクトリ
     * @return 読み取り対象ワークフロー定義ファイル
     */
    private List<WorkflowDefinitionFile> findDefinitionFiles(File inputDir) {
        List<WorkflowDefinitionFile> files = new ArrayList<WorkflowDefinitionFile>();

        for (File file : inputDir.listFiles(new WorkflowDefinitionFileFilter())) {
            if (file.isDirectory()) {
                files.addAll(findDefinitionFiles(file));
                continue;
            }

            String fileName = file.getName();
            try {
                WorkflowDefinitionFile definitionFile = parseDefinitionFileName(file);
                if (files.contains(definitionFile)) {
                    throw new WorkflowDefinitionException("ワークフローID、バージョンの組み合わせが重複しています。");
                }
                files.add(definitionFile);

            } catch (WorkflowDefinitionException e) {
                ErrorReport.writeError(fileName, e.getMessages());
            }
        }

        return files;
    }

    /**
     * ファイル名を精査し、ワークフロー定義ファイル情報を返却する。
     *
     * @param file ワークフロー定義ファイル
     * @return ワークフロー定義ファイル情報
     */
    private WorkflowDefinitionFile parseDefinitionFileName(File file) {
        String fileName = file.getName();
        Matcher m = FILE_NAME_PATTERN.matcher(fileName);
        if (!m.matches()) {
            throw new WorkflowDefinitionException("ファイル名が不正です。ファイル名を[ワークフローID]_[ワークフロー名]_ver[バージョン]_[YYYYMMDD].[拡張子]に修正してください。"
                    + " (例: WP0001_workflowName_ver1_20140804.bpmn)");
        }
        String workflowId = m.group(1);
        String effectiveDate = m.group(4);
        validateWorkflowIdLength(workflowId);
        validateEffectiveDate(effectiveDate);
        return new WorkflowDefinitionFile(fileName, file.getPath(), workflowId, m.group(2), m.group(3), effectiveDate);
    }

    /**
     * ワークフロー定義ファイルを読み込み、ブックに出力する。
     *
     * @param definitionFile ワークフロー定義ファイル
     * @param writer         出力用writer
     */
    private void writeWorkflowDefinition(WorkflowDefinitionFile definitionFile, WorkflowDefinitionWriter writer) {
        try {
            WorkflowDefinitionGeneratorSettings settings = SystemRepository.get("workflowDefinitionGeneratorSettings");
            WorkflowDefinition workflowDefinition = settings.getWorkflowDefinitionReader().load(definitionFile);
            writer.write(workflowDefinition);
        } catch (WorkflowDefinitionException e) {
            ErrorReport.writeError(definitionFile.getName(), e.getMessages());
        }
    }

    /**
     * ワークフローID桁数精査。
     * ワークフローIDの桁数がID体系で定められた桁数と異なる場合、精査エラーとする。
     *
     * @param workflowId ワークフローID
     */
    private void validateWorkflowIdLength(String workflowId) {
        WorkflowDefinitionGeneratorSettings settings = SystemRepository.get("workflowDefinitionGeneratorSettings");
        int workflowIdLength = settings.getWorkflowIdColumnLength();
        if (workflowId.length() != workflowIdLength) {
            throw new WorkflowDefinitionException(String.format("ワークフローIDの桁数がID体系で定められた桁数と異なります。ファイル名を修正してください。 （設定値:%s 実際:%s）",
                    workflowIdLength, workflowId.length()));
        }
    }

    /**
     * 適用日精査。
     * 適用日が有効な日付ではない場合、精査エラーとする。
     *
     * @param effectiveDate 適用日
     */
    private void validateEffectiveDate(String effectiveDate) {
        if (!DateUtil.isValid(effectiveDate, "yyyyMMdd")) {
            throw new WorkflowDefinitionException("適用日が不正です。有効な日付をYYYYMMDD形式でファイル名に含めてください。 (例: WP0001_workflowName_ver1_20140804.bpmn)");
        }
    }

    /**
     * ワークフロー定義ファイルの抽出フィルタ。
     * ディレクトリ、または環境設定ファイルで指定された拡張子のファイルか判定する。
     */
    private static class WorkflowDefinitionFileFilter implements FileFilter {
        @Override
        public boolean accept(File file) {
            if (file.isDirectory()) {
                return true;
            }

            WorkflowDefinitionGeneratorSettings settings = SystemRepository.get("workflowDefinitionGeneratorSettings");
            String fileName = file.getName();
            for (String inputFileExtension : settings.getInputFileExtensions()) {
                if (fileName.endsWith(inputFileExtension)) {
                    return true;
                }
            }
            return false;
        }
    }
}
