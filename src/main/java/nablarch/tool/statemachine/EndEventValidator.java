package nablarch.tool.statemachine;

import java.util.List;

import javax.xml.bind.JAXBElement;

import org.omg.spec.bpmn._20100524.model.TEndEvent;
import org.omg.spec.bpmn._20100524.model.TEventDefinition;
import org.omg.spec.bpmn._20100524.model.TTerminateEventDefinition;

/**
 * 停止イベントのバリデーションを行う{@link Validator}。
 *
 * @author Naoki Yamamoto
 */
public class EndEventValidator implements Validator {

    /** 停止イベントリスト */
    final List<TEndEvent> endEvents;

    /**
     * インスタンスを生成する。
     * @param endEvents 停止イベントリスト
     */
    public EndEventValidator(final List<TEndEvent> endEvents) {
        this.endEvents = endEvents;
    }

    @Override
    public void validate(final ValidateContext context) {
        validateCount(context);
        
        for (TEndEvent endEvent : endEvents) {
            final List<JAXBElement<? extends TEventDefinition>> eventDefinition = endEvent.getEventDefinition();

            if (eventDefinition.isEmpty() || !(eventDefinition.get(0).getValue() instanceof TTerminateEventDefinition)) {
                final String message = MessageUtil.getMessage("unsupported.end.event", endEvent.getId(), endEvent.getName());
                context.addMessage(message);
            }

            if (endEvent.getIncoming().isEmpty()) {
                final String message = MessageUtil.getMessage("end.event.incoming.notfound", endEvent.getId(), endEvent.getName());
                context.addMessage(message);
            }
        }
    }

    /**
     * 停止イベントの数をバリデーションする。
     * @param context バリデーションコンテキスト
     */
    void validateCount(final ValidateContext context) {
        if (endEvents.isEmpty()) {
            context.addMessage(MessageUtil.getMessage("end.event.count"));
        }
    }
}
