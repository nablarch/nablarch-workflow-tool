package nablarch.tool.statemachine;

import java.util.ArrayList;
import java.util.List;

/**
 * バリデーションコンテキスト。
 *
 * @author Naoki Yamamoto
 */
public class ValidateContext {

    /** エラーメッセージのリスト */
    private final List<String> messages = new ArrayList<String>();

    /**
     * エラーメッセージを追加する。
     * @param message メッセージ
     */
    public void addMessage(final String message) {
        messages.add(message);
    }

    /**
     * 全てのエラーメッセージを保持する{@link InvalidStateMachineModelException}を送出する。
     * @throws InvalidStateMachineModelException 全てのエラーメッセージを保持する例外
     */
    public void throwError() throws InvalidStateMachineModelException {
        throw new InvalidStateMachineModelException(messages);
    }
}
