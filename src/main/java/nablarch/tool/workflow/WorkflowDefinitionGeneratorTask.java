package nablarch.tool.workflow;

import java.util.ArrayList;

import nablarch.integration.workflow.definition.SequenceFlow;
import nablarch.integration.workflow.definition.Task;

/**
 * タスク定義を表すクラス。
 * 自動生成ツール使用時、完了条件のクラスがクラスパス上に存在していない可能性があるため
 * 親クラスで完了条件のインスタンスを保持させず、本クラスで文字列として完了条件を保持する。
 *
 * @author Satoshi Taromaru
 * @since 1.4.2
 */
public class WorkflowDefinitionGeneratorTask extends Task {

    /** 完了条件 */
    private final String condition;

    /**
     * タスク定義を生成する。
     *
     * @param flowNodeId フローノードID
     * @param flowNodeName フローノード名
     * @param laneId レーンID
     * @param multiInstanceType マルチインスタンスタイプ
     * @param condition 完了条件
     */
    public WorkflowDefinitionGeneratorTask(
            String flowNodeId,
            String flowNodeName,
            String laneId,
            String multiInstanceType,
            String condition) {
        super(flowNodeId, flowNodeName, laneId, multiInstanceType, null, new ArrayList<SequenceFlow>());
        this.condition = condition;
    }

    /**
     * 完了条件を取得する。
     *
     * @return 完了条件
     */
    public String getCondition() {
        return condition;
    }
}
