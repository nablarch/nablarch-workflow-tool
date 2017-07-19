package nablarch.tool;

import java.io.File;
import java.util.List;

import nablarch.integration.workflow.definition.WorkflowDefinition;

/**
 * {@link WorkflowDefinition}の定義をもとに外部ファイルへの書き込みを行うインタフェース。
 *
 * @author Naoki Yamamoto
 */
public interface DefinitionWriter {

    /**
     * 外部ファイルへの書き込みを行う。
     *
     * @param workflowDefinitions ワークフロー定義
     * @param outputDir 出力先ディレクトリ
     */
    void write(List<WorkflowDefinition> workflowDefinitions, File outputDir);
}
