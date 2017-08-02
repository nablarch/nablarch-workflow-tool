package nablarch.tool;

import java.util.List;

import nablarch.integration.workflow.definition.WorkflowDefinition;

/**
 * BPMNファイルから定義情報をロード＆バリデーションするインタフェース。
 */
public interface BpmnLoader {

    /**
     * ロードする。
     * @param loadDir ロード対象ディレクトリ
     * @return ロードした定義情報。
     */
    List<WorkflowDefinition> load(final String loadDir);
}
