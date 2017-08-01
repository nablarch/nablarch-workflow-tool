package nablarch.tool.workflow;

import nablarch.tool.CsvDataWriter;
import nablarch.tool.DataWriter;

/**
 * ワークフロー定義データ生成ツールに関する各種設定値を保持するクラス。
 *
 * @author Satoshi Taromaru
 * @since 1.4.2
 */
public class WorkflowDefinitionGeneratorSettings {

    /** デフォルトのワークフロー定義データ書き込みクラス */
    private static final DataWriter DEFAULT_DATA_WRITER = new CsvDataWriter();

    /**
     * ワークフロー定義ファイル格納ディレクトリ
     */
    private String inputFileDir;

    /**
     * ワークフロー定義ファイルの拡張子。
     */
    private String[] inputFileExtensions;

    /**
     * ワークフロー定義データの出力先ディレクトリ
     */
    private String outputFileDir;

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
     * ワークフロー定義データ書き込みクラス
     */
    private DataWriter dataWriter;

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
     * ワークフロー定義データの出力先ディレクトリを取得する。
     *
     * @return ワークフロー定義データの出力先ディレクトリ
     */
    public String getOutputFileDir() {
        return outputFileDir;
    }

    /**
     * ワークフロー定義データの出力先ディレクトリを設定する。
     *
     * @param outputFileDir ワークフロー定義データの出力先ディレクトリ
     */
    public void setOutputFileDir(String outputFileDir) {
        this.outputFileDir = outputFileDir;
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
     * ワークフロー定義データ書き込みクラスを取得する。
     *
     * @return ワークフロー定義データ書き込みクラス
     */
    public DataWriter getDataWriter() {
        return dataWriter == null ? DEFAULT_DATA_WRITER : dataWriter;
    }

    /**
     * ワークフロー定義データ書き込みクラスを設定する。
     *
     * @param dataWriter ワークフロー定義データ書き込みクラス
     */
    public void setDataWriter(DataWriter dataWriter) {
        this.dataWriter = dataWriter;
    }
}

