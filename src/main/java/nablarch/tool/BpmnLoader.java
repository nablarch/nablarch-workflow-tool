package nablarch.tool;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.logging.Log;

import nablarch.integration.workflow.definition.WorkflowDefinition;

/**
 * BPMNファイルから定義情報をロード＆バリデーションするインタフェース。
 * @author siosio
 */
public interface BpmnLoader {

    /**
     * ロードする。
     * @param loadDir ロード対象ディレクトリ
     * @param log logger
     * @return ロードした定義情報。
     */
    List<WorkflowDefinition> load(final File loadDir, final Log log);
}
