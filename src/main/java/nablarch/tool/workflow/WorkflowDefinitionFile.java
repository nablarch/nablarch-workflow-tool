package nablarch.tool.workflow;

/**
 * ワークフロー定義ファイルの情報を保持するクラス。
 *
 * @author Satoshi Taromaru
 * @since 1.4.2
 */
public class WorkflowDefinitionFile {

    /**
     * ファイル名。
     */
    private final String name;
    /**
     * ファイルパス。
     */
    private final String path;
    /**
     * ワークフローID。
     */
    private final String workflowId;
    /**
     * ワークフロー名。
     */
    private final String workflowName;
    /**
     * バージョン。
     */
    private final String version;
    /**
     * 適用日。
     */
    private final String effectiveDate;

    /**
     * コンストラクタ。
     *
     * @param name          ファイル名
     * @param path          ファイルパス
     * @param workflowId    ワークフローID
     * @param workflowName  ワークフロー名
     * @param version       バージョン
     * @param effectiveDate 適用日
     */
    public WorkflowDefinitionFile(String name, String path, String workflowId, String workflowName, String version, String effectiveDate) {
        this.name = name;
        this.path = path;
        this.workflowId = workflowId;
        this.workflowName = workflowName;
        this.version = version;
        this.effectiveDate = effectiveDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WorkflowDefinitionFile that = (WorkflowDefinitionFile) o;
        return workflowId.equals(that.workflowId) && version.equals(that.version);
    }

    @Override
    public int hashCode() {
        int result = workflowId.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }

    /**
     * ファイル名を取得する。
     *
     * @return ファイル名
     */
    public String getName() {
        return name;
    }

    /**
     * ファイルパスを取得する。
     *
     * @return ファイルパス。
     */
    public String getPath() {
        return path;
    }

    /**
     * ワークフローIDを取得する。
     *
     * @return ワークフローID
     */
    public String getWorkflowId() {
        return workflowId;
    }

    /**
     * ワークフロー名を取得する。
     *
     * @return ワークフロー名
     */
    public String getWorkflowName() {
        return workflowName;
    }

    /**
     * バージョンを取得する。
     *
     * @return バージョン
     */
    public String getVersion() {
        return version;
    }

    /**
     * 適用日を取得する。
     *
     * @return 適用日
     */
    public String getEffectiveDate() {
        return effectiveDate;
    }
}
