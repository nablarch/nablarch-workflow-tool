package nablarch.tool;

import nablarch.integration.workflow.definition.WorkflowDefinition;
import nablarch.tool.workflow.WorkflowDefinitionFile;

/**
 * {@link WorkflowDefinition}の生成を行うインタフェース。
 *
 * @author Naoki Yamamoto
 */
public interface DefinitionCreator {

    /**
     * {@link WorkflowDefinition}の生成を行う。
     * @param workflowDefinitionFile ワークフロー定義ファイル
     * @return ワークフロー定義
     */
    WorkflowDefinition create(WorkflowDefinitionFile workflowDefinitionFile);
}
