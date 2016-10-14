package nablarch.tool.workflow;

import nablarch.integration.workflow.definition.WorkflowDefinition;

/**
 * ワークフロー定義情報の読み込み処理を行うインタフェース。
 *
 * @author Taromaru Satoshi
 * @since 1.4.2
 */
public interface WorkflowDefinitionReader {

    /**
     * ワークフロー定義情報をロードする。
     *
     * @param definitionFile ワークフロー定義ファイル
     * @return ワークフロー定義情報
     */
    WorkflowDefinition load(WorkflowDefinitionFile definitionFile);
}
