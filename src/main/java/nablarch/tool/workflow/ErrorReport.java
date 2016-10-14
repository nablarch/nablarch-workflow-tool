package nablarch.tool.workflow;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import nablarch.core.util.FileUtil;

/**
 * エラー情報出力クラス。
 *
 * @author Satoshi Taromaru
 * @since 1.4.2
 */
public final class ErrorReport {

    /**
     * エラー情報出力用writer。
     */
    private static OutputStreamWriter logWriter;

    /**
     * エラー検知フラグ。
     */
    private static boolean anomalyDetection = false;

    /**
     * 隠蔽コンストラクタ。
     */
    private ErrorReport() {
    }

    /**
     * エラー情報出力ファイルを開く。
     *
     * @param logFilePath エラー情報出力ファイルパス
     */
    public static void open(String logFilePath) {
        try {
            logWriter = new OutputStreamWriter(new FileOutputStream(logFilePath), "UTF-8");
        } catch (IOException e) {
            throw new IllegalArgumentException("エラーログファイルパスが不正です。ツールの設定ファイルに、存在するディレクトリを指定してください。 "
                    + "設定値 = [" + logFilePath + "]", e);
        }
        anomalyDetection = false;
    }

    /**
     * エラー情報出力ファイルを閉じる。
     */
    public static void close() {
        FileUtil.closeQuietly(logWriter);
    }

    /**
     * エラー情報を出力する。
     *
     * @param fileName      ファイル名
     * @param errorMessages エラー情報
     */
    public static void writeError(String fileName, List<String> errorMessages) {
        for (String errorMessage : errorMessages) {
            try {
                logWriter.write(String.format("fileName = [%s] " + errorMessage, fileName) + System.getProperty("line.separator"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        anomalyDetection = true;
    }

    /**
     * 精査エラーが発生したか判定する。
     *
     * @return 精査エラーが発生した場合、true
     */
    public static boolean hasAnomalyDetection() {
        return anomalyDetection;
    }
}
