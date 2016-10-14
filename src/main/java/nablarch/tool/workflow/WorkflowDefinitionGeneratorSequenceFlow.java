package nablarch.tool.workflow;

import nablarch.integration.workflow.definition.SequenceFlow;

/**
 * シーケンスフローの定義を表すクラス。
 * 自動生成ツール使用時、フローコンディションのクラスがクラスパス上に存在していない可能性があるため
 * 親クラスでフローコンディションのインスタンスを保持させず、本クラスで文字列としてフローコンディションを保持する。
 *
 * @author Satoshi Taromaru
 * @since 1.4.2
 */
public class WorkflowDefinitionGeneratorSequenceFlow extends SequenceFlow {

    /**
     * フローコンディション
     */
    private final String condition;

    /**
     * シーケンスフロー定義を生成する。
     *
     * @param sequenceFlowId   シーケンスフローID
     * @param sequenceFlowName シーケンスフロー名
     * @param sourceFlowNodeId 遷移元シーケンスフローID
     * @param targetFlowNodeId 遷移先シーケンスフロー名
     * @param condition        フローコンディション
     */
    public WorkflowDefinitionGeneratorSequenceFlow(
            String sequenceFlowId,
            String sequenceFlowName,
            String sourceFlowNodeId,
            String targetFlowNodeId,
            String condition) {
        super(sequenceFlowId, sequenceFlowName, sourceFlowNodeId, targetFlowNodeId, null);
        this.condition = condition;
    }

    /**
     * フローコンディションを取得する。
     *
     * @return フローコンディション
     */
    public String getCondition() {
        return condition;
    }
}
