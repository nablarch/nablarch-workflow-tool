package nablarch.tool;

import java.util.List;

import org.apache.maven.plugin.logging.Log;

/**
 * BPMNのバリデーション結果を出力するクラス。
 *
 * @author siosio
 */
public final class ErrorPrinter {

    /**
     * 隠蔽コンストラクタ。
     */
    private ErrorPrinter() {
    }

    /**
     * エラー情報を標準エラー出力に吐き出す。
     * @param fileName ファイル名
     * @param messages ファイル名に対応したメッセージ情報
     * @param log ロガー
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void print(final String fileName, final List<String> messages, final Log log) {
        log.error(fileName);
        for (String message : messages) {
            log.error('\t' + message);
        }
    }
}
