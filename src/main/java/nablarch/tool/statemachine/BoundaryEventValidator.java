package nablarch.tool.statemachine;

import java.util.List;

import javax.xml.bind.JAXBElement;

import org.omg.spec.bpmn._20100524.model.TBoundaryEvent;
import org.omg.spec.bpmn._20100524.model.TEventDefinition;
import org.omg.spec.bpmn._20100524.model.TMessageEventDefinition;

/**
 * 境界イベントのバリデーションを行う{@link Validator}。
 *
 * @author Naoki Yamamoto
 */
public class BoundaryEventValidator implements Validator {

    /** 境界イベントリスト */
    final List<TBoundaryEvent> boundaryEvents;

    /**
     * インスタンスを生成する。
     *
     * @param boundaryEvents 境界イベントリスト
     */
    public BoundaryEventValidator(final List<TBoundaryEvent> boundaryEvents) {
        this.boundaryEvents = boundaryEvents;
    }

    @Override
    public void validate(final ValidateContext context) {
        for (TBoundaryEvent boundaryEvent : boundaryEvents) {
            validateType(context, boundaryEvent);
            validateOutgoing(context, boundaryEvent);
        }
    }

    /**
     * 境界イベントの遷移先をバリデーションする
     * @param context バリデーションコンテキスト
     * @param event 境界イベント
     */
    private void validateOutgoing(final ValidateContext context, final TBoundaryEvent event) {
        if (event.getOutgoing()
                 .isEmpty()) {
            context.addMessage(MessageUtil.getMessage("boundary.outgoing.notfound", event.getId(), event.getName()));
        }
    }

    /**
     * 境界イベントタイプをバリデーションする。
     * @param context バリデーションコンテキスト
     * @param boundaryEvent 境界イベント
     */
    private void validateType(final ValidateContext context, final TBoundaryEvent boundaryEvent) {
        final List<JAXBElement<? extends TEventDefinition>> eventDefinition = boundaryEvent.getEventDefinition();
        if (eventDefinition.isEmpty() || !(eventDefinition.get(0)
                                                          .getValue() instanceof TMessageEventDefinition)) {
            context.addMessage(
                    MessageUtil.getMessage("unsupported.boundary.event", boundaryEvent.getId(),
                            boundaryEvent.getName()));
        }
    }
}
