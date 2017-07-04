package nablarch.tool.statemachine;

import java.util.ResourceBundle;

/**
 * メッセージに関するユーティティクラス。
 *
 * @author Naoki Yamamoto
 */
public class MessageUtil {

    /**
     * メッセージを取得する。
     * @param key キー
     * @return メッセージ
     */
    public static String getMessage(final String key) {
        return ResourceBundle.getBundle("messages").getString(key);
    }

    /**
     * メッセージを取得する。
     * @param key キー
     * @param parameters パラメータ
     * @return メッセージ
     */
    public static String getMessage(final String key, final String... parameters) {
        return String.format(getMessage(key), parameters);
    }
}
