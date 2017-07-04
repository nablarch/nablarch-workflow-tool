package nablarch.tool.statemachine;

import java.util.List;

import javax.xml.namespace.QName;

import org.omg.spec.bpmn._20100524.model.TStartEvent;

/**
 * 開始イベントのバリデーションを行う{@link Validator}。
 */
public class StartEventValidator implements Validator {

    /** 開始イベントリスト */
    final List<TStartEvent> startEvents;

    /**
     * インスタンスを生成する。
     * @param startEvents 開始イベント
     */
    public StartEventValidator(final List<TStartEvent> startEvents) {
        this.startEvents = startEvents;
    }

    @Override
    public void validate(final ValidateContext context) {
        validateCount(context);
        if (startEvents.size() != 1) {
            return;
        }
        validateEventType(context);
        validateOutgoing(context);
    }

    /**
     * 開始イベントの遷移先をバリデーションする。
     * @param context バリデーションコンテキスト
     */
    void validateOutgoing(final ValidateContext context) {
        final TStartEvent startEvent = startEvents.get(0);
        final List<QName> outgoing = startEvent
                                                .getOutgoing();
        if (outgoing.size() != 1) {
            final String message = MessageUtil.getMessage("start.event.outgoing.notfound", startEvent.getId(), startEvent.getName());
            context.addMessage(message);
        }
    }

    /**
     * 開始イベントタイプをバリデーションする。
     * @param context バリデーションコンテキスト
     */
    void validateEventType(final ValidateContext context) {
        final TStartEvent startEvent = startEvents.get(0);
        if (!startEvent.getEventDefinition().isEmpty()) {
            final String message = MessageUtil.getMessage("unsupported.start.event", startEvent.getId(),
                    startEvent.getName());
            context.addMessage(message);
        }
    }

    /**
     * 開始イベント数をバリデーションする。
     * @param context バリデーションコンテキスト
     */
    void validateCount(final ValidateContext context) {
        if (startEvents.size() != 1) {
            context.addMessage(MessageUtil.getMessage("start.event.count"));
        }
    }
}
