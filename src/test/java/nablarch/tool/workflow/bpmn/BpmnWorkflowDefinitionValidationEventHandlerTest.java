package nablarch.tool.workflow.bpmn;

import java.net.URL;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventLocator;

import org.junit.Test;
import org.w3c.dom.Node;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@link BpmnWorkflowDefinitionValidationEventHandler}のテストクラス。
 *
 * @author Satoshi Taromaru
 * @since 1.4.2
 */
public class BpmnWorkflowDefinitionValidationEventHandlerTest {

    /**
     * handleEventテスト。
     * エラーレベル：FATAL_ERRORのエラーが発生した場合判定用フラグがtrueとなり、
     * エラー発生行数とエラーレベルを含むエラーメッセージをインスタンスに保持すること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void testFatalError() throws Exception {
        BpmnWorkflowDefinitionValidationEventHandler sut = new BpmnWorkflowDefinitionValidationEventHandler();
        assertThat(sut.hasAnomalyDetection(), is(false));
        assertThat(sut.getErrorMessages().size(), is(0));

        // 実行
        assertThat(sut.handleEvent(new FatalValidationEvent()), is(true));

        // 検証
        assertThat(sut.hasAnomalyDetection(), is(true));
        assertThat(sut.getErrorMessages().size(), is(1));
        assertThat(sut.getErrorMessages().get(0), is("bpmn定義ファイル読み込み時にデータ不正を検知しました。bpmn定義ファイルが、モデリングツールで正常に開けるか確認してください。"
                + " lineNumber = [10] errorLevel = [FATAL_ERROR] errorMessage = [FATAL_ERRORメッセージ]"));
    }

    /**
     * handleEventテスト。
     * エラーレベル：ERRORのエラーが発生した場合判定用フラグがtrueとなり、
     * エラー発生行数とエラーレベルを含むエラーメッセージをインスタンスに保持すること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void testError() throws Exception {
        BpmnWorkflowDefinitionValidationEventHandler sut = new BpmnWorkflowDefinitionValidationEventHandler();
        assertThat(sut.hasAnomalyDetection(), is(false));
        assertThat(sut.getErrorMessages().size(), is(0));

        // 実行
        assertThat(sut.handleEvent(new ErrorValidationEvent()), is(true));

        // 検証
        assertThat(sut.hasAnomalyDetection(), is(true));
        assertThat(sut.getErrorMessages().size(), is(1));
        assertThat(sut.getErrorMessages().get(0), is("bpmn定義ファイル読み込み時にデータ不正を検知しました。bpmn定義ファイルが、モデリングツールで正常に開けるか確認してください。"
                + " lineNumber = [20] errorLevel = [ERROR] errorMessage = [ERRORメッセージ]"));
    }

    /**
     * handleEventテスト。
     * エラーレベル：ERRORのエラーが発生した場合、判定用フラグはfalseのままであること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void testWarningError() throws Exception {
        BpmnWorkflowDefinitionValidationEventHandler sut = new BpmnWorkflowDefinitionValidationEventHandler();
        assertThat(sut.hasAnomalyDetection(), is(false));
        assertThat(sut.getErrorMessages().size(), is(0));

        // 実行
        assertThat(sut.handleEvent(new WarningValidationEvent()), is(true));

        // 検証
        assertThat(sut.hasAnomalyDetection(), is(false));
        assertThat(sut.getErrorMessages().size(), is(0));
    }

    /**
     * テスト用ValidationEvent。
     * （エラーレベル：FATAL_ERROR）
     */
    private class FatalValidationEvent implements ValidationEvent {
        @Override
        public int getSeverity() {
            return ValidationEvent.FATAL_ERROR;
        }

        @Override
        public String getMessage() {
            return "FATAL_ERRORメッセージ";
        }

        @Override
        public Throwable getLinkedException() {
            throw new UnsupportedOperationException("本テストでは使用しない。");
        }

        @Override
        public ValidationEventLocator getLocator() {
            return new FatalValidationEventLocator();
        }
    }

    /**
     * テスト用ValidationEventLocator。
     * （エラーレベル：FATAL_ERROR）
     */
    private class FatalValidationEventLocator implements ValidationEventLocator {
        @Override
        public URL getURL() {
            throw new UnsupportedOperationException("本テストでは使用しない。");
        }

        @Override
        public int getOffset() {
            throw new UnsupportedOperationException("本テストでは使用しない。");
        }

        @Override
        public int getLineNumber() {
            return 10;
        }

        @Override
        public int getColumnNumber() {
            throw new UnsupportedOperationException("本テストでは使用しない。");
        }

        @Override
        public Object getObject() {
            throw new UnsupportedOperationException("本テストでは使用しない。");
        }

        @Override
        public Node getNode() {
            throw new UnsupportedOperationException("本テストでは使用しない。");
        }
    }

    /**
     * テスト用ValidationEvent。
     * （エラーレベル：ERROR）
     */
    private class ErrorValidationEvent implements ValidationEvent {
        @Override
        public int getSeverity() {
            return ValidationEvent.ERROR;
        }

        @Override
        public String getMessage() {
            return "ERRORメッセージ";
        }

        @Override
        public Throwable getLinkedException() {
            throw new UnsupportedOperationException("本テストでは使用しない。");
        }

        @Override
        public ValidationEventLocator getLocator() {
            return new ErrorValidationEventLocator();
        }
    }

    /**
     * テスト用ValidationEventLocator。
     * （エラーレベル：ERROR）
     */
    private class ErrorValidationEventLocator implements ValidationEventLocator {
        @Override
        public URL getURL() {
            throw new UnsupportedOperationException("本テストでは使用しない。");
        }

        @Override
        public int getOffset() {
            throw new UnsupportedOperationException("本テストでは使用しない。");
        }

        @Override
        public int getLineNumber() {
            return 20;
        }

        @Override
        public int getColumnNumber() {
            throw new UnsupportedOperationException("本テストでは使用しない。");
        }

        @Override
        public Object getObject() {
            throw new UnsupportedOperationException("本テストでは使用しない。");
        }

        @Override
        public Node getNode() {
            throw new UnsupportedOperationException("本テストでは使用しない。");
        }
    }

    /**
     * テスト用ValidationEvent。
     * （エラーレベル：WARNING）
     */
    private class WarningValidationEvent implements ValidationEvent {
        @Override
        public int getSeverity() {
            return ValidationEvent.WARNING;
        }

        @Override
        public String getMessage() {
            throw new UnsupportedOperationException("本テストでは使用しない。");
        }

        @Override
        public Throwable getLinkedException() {
            throw new UnsupportedOperationException("本テストでは使用しない。");
        }

        @Override
        public ValidationEventLocator getLocator() {
            throw new UnsupportedOperationException("本テストでは使用しない。");
        }
    }
}
