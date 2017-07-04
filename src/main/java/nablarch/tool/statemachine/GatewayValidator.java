package nablarch.tool.statemachine;

import java.util.List;

import org.omg.spec.bpmn._20100524.model.TExclusiveGateway;
import org.omg.spec.bpmn._20100524.model.TGateway;

/**
 * ゲートウェイのバリデーションを行う{@link Validator}。
 *
 * @author Naoki Yamamoto
 */
public class GatewayValidator implements Validator {

    /** ゲートウェイリスト */
    private final List<TGateway> gateways;

    /**
     * インスタンスを生成する。
     * @param gateways ゲートウェイリスト
     */
    public GatewayValidator(final List<TGateway> gateways) {
        this.gateways = gateways;
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
