package nablarch.tool.statemachine;

import java.util.ArrayList;
import java.util.List;

import org.omg.spec.bpmn._20100524.model.TExclusiveGateway;
import org.omg.spec.bpmn._20100524.model.TExpression;
import org.omg.spec.bpmn._20100524.model.TFlowElement;
import org.omg.spec.bpmn._20100524.model.TGateway;
import org.omg.spec.bpmn._20100524.model.TSequenceFlow;

import nablarch.tool.Utils;

/**
 * ゲートウェイのバリデーションを行う{@link Validator}。
 *
 * @author Naoki Yamamoto
 */
public class GatewayValidator implements Validator {

    /** ゲートウェイリスト */
    private final List<TGateway> gateways;

    /** ゲートウェイと同じプロセス（サププロセス）のシーケンスフロー一覧 */
    private final List<TSequenceFlow> sequenceFlowList;

    /**
     * インスタンスを生成する。
     * @param gateways ゲートウェイリスト
     * @param sequenceFlowList シーケンスフローのリスト
     */
    public GatewayValidator(final List<TGateway> gateways,
            final List<TSequenceFlow> sequenceFlowList) {
        this.gateways = gateways;
        this.sequenceFlowList = sequenceFlowList;
    }

    @Override
    public void validate(final ValidateContext context) {
        for (final TGateway gateway : gateways) {
            validateType(context, gateway);
            validateIncoming(context, gateway);
            validateOutgoing(context, gateway);
        }
    }

    /**
     * ゲートウェイの遷移先をバリデーションする。
     * @param context バリデーションコンテキスト
     * @param gateway ゲートウェイ
     */
    private void validateOutgoing(final ValidateContext context, final TGateway gateway) {
        if (gateway.getOutgoing()
                   .isEmpty()) {
            context.addMessage(MessageUtil.getMessage("gateway.outgoing.notfound", gateway.getId(), gateway.getName()));
        }

        for (final TSequenceFlow flow : findSequenceFlowWhereFormNodeId(gateway.getId())) {
            final TExpression expression = flow.getConditionExpression();
            if (expression == null || expression.getContent()
                                                .isEmpty()) {
                context.addMessage(MessageUtil.getMessage("gateway.outgoing.notcondition", flow.getId(), flow.getName()));
            } else {
                final String condition = expression.getContent()
                                           .get(0)
                                           .toString();
                final List<String> errorMessages = Utils.validateConditionFormat(condition, flow.getId(), flow.getName());
                for (final String message : errorMessages) {
                    context.addMessage(message);
                }
            }
        }
    }

    /**
     * 指定のノードIDからソースにもつシーケンスフローのリストを取得する。
     * @param nodeId ノードID
     * @return 条件に一致するシーケンスフローのリスト
     */
    private List<TSequenceFlow> findSequenceFlowWhereFormNodeId(final String nodeId) {
        final List<TSequenceFlow> flows = new ArrayList<TSequenceFlow>();
        for (final TSequenceFlow flow : sequenceFlowList) {
            TFlowElement sourceRef = (TFlowElement) flow.getSourceRef();
            if (nodeId.equals(sourceRef.getId())) {
                flows.add(flow);
            }
        }
        return flows;
    }

    /**
     * ゲートウェイの遷移元をバリデーションする。
     * @param context バリデーションコンテキスト
     * @param gateway ゲートウェイ
     */
    private void validateIncoming(final ValidateContext context, final TGateway gateway) {
        if (gateway.getIncoming()
                   .isEmpty()) {
            context.addMessage(MessageUtil.getMessage("gateway.incoming.notfound", gateway.getId(), gateway.getName()));
        }
    }

    /**
     * ゲートウェイタイプをバリデーションする。
     * @param context バリデーションコンテキスト
     * @param gateway ゲートウェイ
     */
    private void validateType(final ValidateContext context, final TGateway gateway) {
        if (!(gateway instanceof TExclusiveGateway)) {
            context.addMessage(MessageUtil.getMessage("unsupported.gateway", gateway.getId(), gateway.getName()));
        }
    }
}
