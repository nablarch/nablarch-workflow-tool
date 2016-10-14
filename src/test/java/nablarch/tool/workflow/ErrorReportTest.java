package nablarch.tool.workflow;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import nablarch.tool.workflow.helper.ErrorLogReader;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * {@link ErrorReport}のテストクラス。
 *
 * @author Satoshi Taromaru
 * @since 1.4.2
 */
public class ErrorReportTest {

    /**
     * ログファイルディレクトリが存在しない場合。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void invalidLogFileDir() throws Exception {
        try {
            ErrorReport.open("notExistLogDir/error.log");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("エラーログファイルパスが不正です。ツールの設定ファイルに、存在するディレクトリを指定してください。 設定値 = [notExistLogDir/error.log]"));
        }
    }

    /**
     * ファイルへのログ出力確認。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void testWrite() throws Exception {
        // 事前処理
        String logFilePath = "src/test/work/testWrite.log";
        ErrorReport.open(logFilePath);
        List<String> errorList = new ArrayList<String>();
        errorList.add("エラー１");
        errorList.add("エラー２");

        // テスト実施
        try {
            ErrorReport.writeError("fileName.bpmn", errorList);
        } finally {
            ErrorReport.close();
        }

        // 検証
        ErrorLogReader reader = new ErrorLogReader(logFilePath);
        List<String> expectedErrorList = new ArrayList<String>();
        expectedErrorList.add("fileName = [fileName.bpmn] エラー１");
        expectedErrorList.add("fileName = [fileName.bpmn] エラー２");
        assertThat(reader.getLog(), is(expectedErrorList));
    }

    /**
     * ログ出力せずにcloseした場合、標準エラー出力にメッセージが表示されないことを確認。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void noWrite() throws Exception {
        // 事前処理
        String logFilePath = "src/test/work/testNoWrite.log";
        ErrorReport.open(logFilePath);

        // テスト実施
        ErrorReport.close();

        // 検証
        ErrorLogReader reader = new ErrorLogReader(logFilePath);
        List<String> emptyList = new ArrayList<String>();
        assertThat(reader.getLog(), is(emptyList));
    }

    /**
     * openせずにwriteErrorした場合、例外が発生することを確認。
     * @throws Exception
     */
    @Test(expected = RuntimeException.class)
    public void writeBeforeOpen() throws Exception {
        List<String> errorList = new ArrayList<String>();
        errorList.add("エラー１");
        ErrorReport.writeError("fileName", errorList);
    }
}
