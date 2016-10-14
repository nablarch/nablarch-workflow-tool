package nablarch.tool.workflow;

/**
 * ワークフロー定義データ生成ツールに関する各種設定値を保持するクラス。
 *
 * @author Satoshi Taromaru
 * @since 1.4.2
 */
public class WorkflowDefinitionGeneratorSettings {

    /**
     * ワークフロー定義ファイル格納ディレクトリ
     */
    private String inputFileDir;

    /**
     * ワークフロー定義ファイルの拡張子。
     */
    private String[] inputFileExtensions;

    /**
     * 精査エラー詳細出力用ファイルパス
     */
    private String logFilePath;

    /**
     * ワークフローID桁数。
     */
    private int workflowIdColumnLength;

    /**
     * ワークフロー定義情報の読み込みクラス。
     */
    private WorkflowDefinitionReader workflowDefinitionReader;

    /**
     * ワークフロー定義情報の書き込みクラス。
     */
    private WorkflowDefinitionWriter workflowDefinitionWriter;

    /**
     * ワークフロー定義ファイル格納ディレクトリを取得する。
     *
     * @return ワークフロー定義ファイル格納ディレクトリ
     */
    public String getInputFileDir() {
        return inputFileDir;
    }

    /**
     * ワークフロー定義ファイル格納ディレクトリを設定する。
     *
     * @param inputFileDir ワークフロー定義ファイル格納ディレクトリ
     */
    public void setInputFileDir(String inputFileDir) {
        this.inputFileDir = inputFileDir;
    }

    /**
     * ワークフロー定義ファイルの拡張子を取得する。
     *
     * @return ワークフロー定義ファイルの拡張子
     */
    public String[] getInputFileExtensions() {
        return inputFileExtensions.clone();
    }

    /**
     * ワークフロー定義ファイルの拡張子を設定する。
     *
     * @param inputFileExtensions ワークフロー定義ファイルの拡張子
     */
    public void setInputFileExtensions(String[] inputFileExtensions) {
        this.inputFileExtensions = inputFileExtensions.clone();
    }

    /**
     * 精査エラー詳細出力用ファイルパスを取得する。
     *
     * @return 精査エラー詳細出力用ファイルパス
     */
    public String getLogFilePath() {
        return logFilePath;
    }

    /**
     * 精査エラー詳細出力用ファイルパスを設定する。
     *
     * @param logFilePath 精査エラー詳細出力用ファイルパス
     */
    public void setLogFilePath(String logFilePath) {
        this.logFilePath = logFilePath;
    }

    /**
     * ワークフローID桁数を取得する。
     *
     * @return ワークフローID桁数
     */
    public int getWorkflowIdColumnLength() {
        return workflowIdColumnLength;
    }

    /**
     * ワークフローID桁数を設定する。
     *
     * @param workflowIdColumnLength ワークフローID桁数
     */
    public void setWorkflowIdColumnLength(int workflowIdColumnLength) {
        this.workflowIdColumnLength = workflowIdColumnLength;
    }

    /**
     * ワークフロー定義情報の読み込みクラスを取得する。
     *
     * @return ワークフロー定義情報の読み込みクラス
     */
    public WorkflowDefinitionReader getWorkflowDefinitionReader() {
        return workflowDefinitionReader;
    }

    /**
     * ワークフロー定義情報の読み込みクラスを設定する。
     *
     * @param workflowDefinitionReader ワークフロー定義情報の読み込みクラス
     */
    public void setWorkflowDefinitionReader(WorkflowDefinitionReader workflowDefinitionReader) {
        this.workflowDefinitionReader = workflowDefinitionReader;
    }

    /**
     * ワークフロー定義情報の書き込みクラスを取得する。
     *
     * @return ワークフロー定義情報の書き込みクラス
     */
    public WorkflowDefinitionWriter getWorkflowDefinitionWriter() {
        return workflowDefinitionWriter;
    }

    /**
     * ワークフロー定義情報の書き込みクラスを設定する。
     *
     * @param workflowDefinitionWriter ワークフロー定義情報の書き込みクラス
     */
    public void setWorkflowDefinitionWriter(WorkflowDefinitionWriter workflowDefinitionWriter) {
        this.workflowDefinitionWriter = workflowDefinitionWriter;
    }
}

