package nablarch.tool;

import java.util.List;

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
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void print(final String fileName, final List<String> messages) {
        System.err.println(fileName);
        for (String message : messages) {
            System.err.println('\t' + message);
        }
    }
}
