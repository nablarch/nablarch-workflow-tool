package nablarch.tool.workflow;

import nablarch.integration.workflow.definition.WorkflowDefinition;

/**
 * ワークフロー定義情報の書き込み処理を行うインタフェース。
 *
 * @author Satoshi Taromaru
 * @since 1.4.2
 */
public interface WorkflowDefinitionWriter {

    /**
     * 書き込み先リソースを開く。
     */
    void open();

    /**
     * 書き込み先リソースを閉じる。
     */
    void close();

    /**
     * ワークフロー定義情報の書き込み処理を行う。
     *
     * @param workflowDefinition ワークフロー定義
     */
    void write(WorkflowDefinition workflowDefinition);
}
