package nablarch.tool.workflow.bpmn;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;

/**
 * unmarshal時の例外イベントハンドラ。
 *
 * @author Satoshi Taromaru
 * @since 1.4.2
 */
class BpmnWorkflowDefinitionValidationEventHandler implements ValidationEventHandler {

    /**
     * エラー検知フラグ。
     */
    private boolean anomalyDetection;

    /**
     * エラー情報。
     */
    private List<String> errorMessages;

    /**
     * コンストラクタ。
     */
    BpmnWorkflowDefinitionValidationEventHandler() {
        anomalyDetection = false;
        errorMessages = new ArrayList<String>();
    }

    @Override
    public boolean handleEvent(ValidationEvent validationEvent) {
        int severity = validationEvent.getSeverity();
        if (severity == ValidationEvent.WARNING) {
            return true;
        }
        String errorLevel = severity == ValidationEvent.ERROR ? "ERROR" : "FATAL_ERROR";

        ValidationEventLocator locator = validationEvent.getLocator();
        errorMessages.add(String.format("bpmn定義ファイル読み込み時にデータ不正を検知しました。bpmn定義ファイルが、モデリングツールで正常に開けるか確認してください。"
                + " lineNumber = [%s] errorLevel = [%s] errorMessage = [%s]", locator.getLineNumber(), errorLevel, validationEvent.getMessage()));
        anomalyDetection = true;
        return true;
    }

    /**
     * 精査エラーが発生したか判定する。
     *
     * @return 精査エラーが発生した場合、true
     */
    boolean hasAnomalyDetection() {
        return anomalyDetection;
    }

    /**
     * エラー情報を取得する。
     *
     * @return エラー情報
     */
    List<String> getErrorMessages() {
        return errorMessages;
    }
}
