package nablarch.tool.workflow;

import java.util.ArrayList;
import java.util.List;

/**
 * ワークフロー定義生成ツール内で発生する例外情報を保持するクラス。
 *
 * @author Satoshi Taromaru
 * @since 1.4.2
 */
public class WorkflowDefinitionException extends RuntimeException {

    /**
     * 精査エラーメッセージ。
     */
    private final List<String> messages = new ArrayList<String>();

    /**
     * コンストラクタ。
     * 例外オブジェクトを親クラスに設定する。
     *
     * @param message エラーメッセージ
     */
    public WorkflowDefinitionException(String message) {
        this.messages.add(message);
    }

    /**
     * コンストラクタ。
     * エラーメッセージをインスタンス変数に設定する。
     *
     * @param messages エラーメッセージ
     */
    public WorkflowDefinitionException(List<String> messages) {
        this.messages.addAll(messages);
    }

    /**
     * 精査エラーメッセージを取得する。
     * @return 精査エラーメッセージ
     */
    public List<String> getMessages() {
        return messages;
    }
}
