package nablarch.tool.workflow.helper;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * ログ出力確認用のreaderクラス。
 *
 * @author Satoshi Taromaru
 * @since 1.4.2
 */
public class ErrorLogReader {

    /**
     * エラー情報出力用writer。
     */
    private BufferedReader reader;

    /**
     * コンストラクタ。
     *
     * @param filePath ログファイルパス
     * @throws Exception 想定外エラー
     */
    public ErrorLogReader(String filePath) throws Exception {
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"));
    }

    public List<String> getLog() throws IOException {
        List<String> list = new ArrayList<String>();
        String str = reader.readLine();
        while (str != null) {
            list.add(str);
            str = reader.readLine();
        }
        return list;
    }
}
