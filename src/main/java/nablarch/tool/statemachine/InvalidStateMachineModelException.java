package nablarch.tool.statemachine;

import java.util.Collections;
import java.util.List;

/**
 * 不正なステートマシン図であることを示す例外クラス。
 *
 * @author Naoki Yamamoto
 */
public class InvalidStateMachineModelException extends Exception {

    /** メッセージリスト */
    private final List<String> messages;

    /**
     * 単一のエラーメッセージを保持するインスタンスを生成する。
     * @param message メッセージ
     */
    public InvalidStateMachineModelException(final String message) {
        this(Collections.singletonList(message));
    }

    /**
     * 複数のエラーメッセージを保持するインスタンスを生成する。
     * @param messages メッセージリスト
     */
    public InvalidStateMachineModelException(final List<String> messages) {
        this.messages = messages;
    }

    @Override
    public String getMessage() {
        final StringBuilder builder = new StringBuilder(256);
        for (final String message : messages) {
            if (builder.length() != 0) {
                builder.append(", ");
            }
            builder.append(message);
        }
        return builder.toString();
    }

    /**
     * メッセージリストを取得する。
     * @return メッセージリスト
     */
    public List<String> getMessages() {
        return messages;
    }
}

