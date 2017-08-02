package nablarch.tool.workflow.bpmn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.omg.spec.bpmn._20100524.model.TBoundaryEvent;
import org.omg.spec.bpmn._20100524.model.TCollaboration;
import org.omg.spec.bpmn._20100524.model.TConversationNode;
import org.omg.spec.bpmn._20100524.model.TDefinitions;
import org.omg.spec.bpmn._20100524.model.TEndEvent;
import org.omg.spec.bpmn._20100524.model.TEventDefinition;
import org.omg.spec.bpmn._20100524.model.TExclusiveGateway;
import org.omg.spec.bpmn._20100524.model.TExpression;
import org.omg.spec.bpmn._20100524.model.TFlowElement;
import org.omg.spec.bpmn._20100524.model.TLaneSet;
import org.omg.spec.bpmn._20100524.model.TLoopCharacteristics;
import org.omg.spec.bpmn._20100524.model.TMessage;
import org.omg.spec.bpmn._20100524.model.TMessageEventDefinition;
import org.omg.spec.bpmn._20100524.model.TMultiInstanceLoopCharacteristics;
import org.omg.spec.bpmn._20100524.model.TProcess;
import org.omg.spec.bpmn._20100524.model.TRootElement;
import org.omg.spec.bpmn._20100524.model.TSequenceFlow;
import org.omg.spec.bpmn._20100524.model.TStartEvent;
import org.omg.spec.bpmn._20100524.model.TTerminateEventDefinition;
import org.omg.spec.bpmn._20100524.model.TUserTask;

import nablarch.core.util.StringUtil;
import nablarch.tool.workflow.WorkflowDefinitionException;

/**
 * ワークフロー定義情報の精査を行うクラス。
 * 変換時、FWが提供するワークフロー機能の制約に準拠しているか精査を行う。
 *
 * @author Satoshi Taromaru
 * @since 1.4.2
 */
public class BpmnWorkflowDefinitionValidator {


    /**
     * フロー進行条件、完了条件のクラス名、パラメータ抽出用正規表現。
     */
    private static final Pattern CONDITION_PATTERN = Pattern.compile("([^　\\s\\(]+)(?:\\(([^\\)]+)\\))?");

    /**
     * ワークフロー定義情報がワークフローエンジン機能の制約に準拠しているか精査を行う。
     *
     * @param definitions ワークフロー定義情報
     */
    public void validateWorkflowDefinition(TDefinitions definitions) {
        List<TProcess> processes = getProcesses(definitions);
        if (processes.size() != 1) {
            throw new WorkflowDefinitionException("ワークフローには、必ず一つのプールを配置してください。");
        }
        TProcess process = processes.get(0);

        validateLanes(process);
        validateElements(definitions, process);
        validateReachable(process);
    }

    /**
     * 到達可能精査。
     * 停止イベントからフローノードを辿り、到達出来ないフローノードが存在するか精査する。
     *
     * @param process プロセス定義情報
     */
    private void validateReachable(TProcess process) {
        List<String> errorList = new ArrayList<String>();
        List<String> reachedFlowNodeIds = new ArrayList<String>();
        for (TEndEvent endEvent : getEndEvents(process)) {
            addReachedFlowNodeIds(process, reachedFlowNodeIds, endEvent.getId());
        }

        for (TFlowElement unUsedFlowNode : getUnUsedFlowNodes(process, reachedFlowNodeIds)) {
            errorList.add(String.format("フローノードが停止イベントから辿れません。停止イベントへ辿れるようにシーケンスフローを配置してください。 id = [%s] name = [%s] ",
                    unUsedFlowNode.getId(), unUsedFlowNode.getName()));
        }

        if (!errorList.isEmpty()) {
            throw new WorkflowDefinitionException(errorList);
        }
    }

    /**
     * フロー要素精査。
     * プロセス定義情報内のフロー要素に対し、精査を行う。
     *
     * @param definitions ワークフロー定義情報
     * @param process     プロセス定義情報
     */
    private void validateElements(TDefinitions definitions, TProcess process) {
        List<String> errorList = new ArrayList<String>();
        errorList.addAll(validateTasks(process));
        errorList.addAll(validateStartEvent(process));
        errorList.addAll(validateEndEvents(process));
        errorList.addAll(validateGateways(process));
        errorList.addAll(validateBoundaryEvents(process, createMessageNameMap(definitions)));
        errorList.addAll(validateUnsupportedElement(definitions, process));

        if (!errorList.isEmpty()) {
            throw new WorkflowDefinitionException(errorList);
        }
    }

    /**
     * レーンに対し、精査を行う。
     *
     * @param process プロセス定義情報
     */
    private void validateLanes(TProcess process) {
        if (process.getLaneSet().isEmpty()) {
            throw new WorkflowDefinitionException("ワークフローにレーンは必須です。レーンを配置してください。");
        }

        List<String> errorList = new ArrayList<String>();
        List<TLaneSet> laneSet = process.getLaneSet();

        if (!errorList.isEmpty()) {
            throw new WorkflowDefinitionException(errorList);
        }
    }

    /**
     * サポート外精査。
     *
     * @param definitions ワークフロー定義情報
     * @param process     プロセス定義情報
     * @return 精査エラーメッセージ
     */
    private List<String> validateUnsupportedElement(TDefinitions definitions, TProcess process) {
        List<String> errorList = new ArrayList<String>();
        for (JAXBElement<? extends TRootElement> elm : definitions.getRootElement()) {
            TRootElement rootElement = elm.getValue();
            if (rootElement instanceof TCollaboration) {
                TCollaboration collaboration = (TCollaboration) rootElement;
                for (JAXBElement<? extends TConversationNode> conversationNode : collaboration.getConversationNode()) {
                    TConversationNode value = conversationNode.getValue();
                    errorList.add(String.format("サポート対象外の要素です。 id = [%s] name = [%s]",
                            value.getId(), value.getName()));
                }
            } else if (!(rootElement instanceof TProcess || rootElement instanceof TMessage)) {
                errorList.add(String.format("サポート対象外の要素です。 id = [%s]", rootElement.getId()));
            }
        }

        for (JAXBElement<? extends TFlowElement> elm : process.getFlowElement()) {
            TFlowElement flowElement = elm.getValue();
            if (!isSupportFlowElement(flowElement)) {
                errorList.add(String.format("サポート対象外の要素です。 id = [%s] name = [%s]", flowElement.getId(), flowElement.getName()));
            }
        }
        return errorList;
    }

    /**
     * フロー要素がサポート対象か判定する。
     *
     * @param flowElement フロー要素定義情報
     * @return フロー要素がサポート対象の場合、true
     */
    private boolean isSupportFlowElement(TFlowElement flowElement) {
        return flowElement instanceof TUserTask || flowElement instanceof TExclusiveGateway || flowElement instanceof TSequenceFlow
                || flowElement instanceof TStartEvent || flowElement instanceof TEndEvent || flowElement instanceof TBoundaryEvent;
    }

    /**
     * タスクに対し精査を行う。
     *
     * @param process プロセス定義情報
     * @return 精査エラーメッセージ
     */
    private List<String> validateTasks(TProcess process) {
        List<String> errorList = new ArrayList<String>();
        List<TUserTask> tasks = getTasks(process);
        if (tasks.isEmpty()) {
            errorList.add("ワークフローにタスクは必須です。タスクを配置してください。");
            return errorList;
        }

        for (TUserTask task : tasks) {
            errorList.addAll(validateMultiInstance(task));
            errorList.addAll(validateSourceTask(process, task));
            errorList.addAll(validateTargetTask(process, task));
        }
        return errorList;
    }

    /**
     * マルチインスタンス・アクティビティに対し精査を行う。
     *
     * @param task タスク定義情報
     * @return 精査エラーメッセージ
     */
    private List<String> validateMultiInstance(TUserTask task) {
        List<String> errorList = new ArrayList<String>();
        JAXBElement<? extends TLoopCharacteristics> loopCharacteristics = task.getLoopCharacteristics();
        if (loopCharacteristics == null) {
            return errorList;
        }

        TMultiInstanceLoopCharacteristics characteristics = (TMultiInstanceLoopCharacteristics) loopCharacteristics.getValue();
        TExpression completionCondition = characteristics.getCompletionCondition();
        String flowNodeId = task.getId();
        String flowNodeName = task.getName();
        if (completionCondition == null || completionCondition.getContent().isEmpty()) {
            errorList.add(String.format("マルチインスタンス・アクティビティの完了条件は必須です。[Completion condition]を設定してください。 id = [%s] name = [%s]", flowNodeId, flowNodeName));
        } else {
            String condition = completionCondition.getContent().get(0).toString();
            errorList.addAll(validateConditionFormat(condition, flowNodeId, flowNodeName));
        }
        return errorList;
    }

    /**
     * フロー進行条件、完了条件の入力値に対し、フォーマット精査を行う。
     * 入力値に対し、以下の精査を行う。
     * <pre>
     *     クラス名（省略記法） + (パラメータ1, パラメータ2, ....)の形式であること。※パラメータが無い場合、括弧を書かないこと
     *     クラス名（省略記法）部分に、空白文字（全角空白を含む）を含まないこと。
     *     クラス名（省略記法）部分が、.（ピリオド）で開始、終了していないこと。
     *     クラス名（省略記法）部分に、連続する.（ピリオド）を含まないこと。
     *     空白文字のみのパラメータが指定されていないこと。
     *
     *     例）
     *     "Condition" ※パラメータが無い場合、括弧は不要
     *     "test.Condition(param , 1)" ※ピリオドを含む場合、ピリオド区切りであること
     *     "Condition( param , 1 )" ※括弧内の半角スペースは許容する
     * </pre>
     *
     * @param condition フロー進行条件または完了条件
     * @param id        シーケンスフローIDまたはフローノードID
     * @param name      シーケンスフロー名またはフローノード名
     * @return 精査エラーメッセージ
     */
    private List<String> validateConditionFormat(String condition, String id, String name) {
        List<String> errorList = new ArrayList<String>();

        Matcher matcher;
        matcher = CONDITION_PATTERN.matcher(condition);
        if (!matcher.matches()) {
            errorList.add(String.format("条件の形式が不正です。「完全修飾名(パラメータ1, パラメータ2, ....)」の形式で入力してください。"
                    + "完全修飾名は空白を含んではいけません。パラメータが無い場合、括弧を含んではいけません。"
                    + "完全修飾名の代わりに、あらかじめ登録された省略記法を利用することも出来ます。入力値 = [%s] id = [%s] name = [%s]", condition, id, name));
            return errorList;
        }

        String className = matcher.group(1);
        if (className.startsWith(".") || className.endsWith(".")) {
            errorList.add(String.format("クラス名、省略記法はピリオドで開始、終了してはいけません。入力値 = [%s] id = [%s] name = [%s]", condition, id, name));
        }
        if (className.contains("..")) {
            errorList.add(String.format("クラス名、省略記法は連続したピリオドを含んではいけません。入力値 = [%s] id = [%s] name = [%s]", condition, id, name));
        }

        String params = matcher.group(2);
        if (params == null) {
            return errorList;
        }
        for (String param : params.split(",", -1)) {
            if (StringUtil.isNullOrEmpty(param.trim())) {
                errorList.add(String.format("空文字となるパラメータを含んではいけません。入力値 = [%s] id = [%s] name = [%s]", condition, id, name));
                return errorList;
            }
        }

        return errorList;
    }

    /**
     * タスクが遷移元として不正か精査を行う。
     *
     * @param process プロセス定義情報
     * @param task    タスク定義情報
     * @return 精査エラーメッセージ
     */
    private List<String> validateSourceTask(TProcess process, TUserTask task) {
        List<String> errorList = new ArrayList<String>();

        String flowNodeId = task.getId();
        String flowNodeName = task.getName();
        List<TSequenceFlow> sourceSequenceFlows = findSourceSequenceFlows(process, flowNodeId);
        if (sourceSequenceFlows.isEmpty()) {
            errorList.add(String.format("タスクが遷移元に設定されていません。タスクから伸びるようにシーケンスフローを配置してください。 id = [%s] name = [%s]", flowNodeId, flowNodeName));
        } else if (sourceSequenceFlows.size() > 1) {
            errorList.add(String.format("タスクが複数の遷移元に設定されています。タスクから伸びるシーケンスフローが1つになるように配置してください。 id = [%s] name = [%s]", flowNodeId, flowNodeName));
        }
        return errorList;
    }

    /**
     * タスクが遷移先として不正か精査を行う。
     *
     * @param process プロセス定義情報
     * @param task    タスク定義情報
     * @return 精査エラーメッセージ
     */
    private List<String> validateTargetTask(TProcess process, TUserTask task) {
        List<String> errorList = new ArrayList<String>();
        String flowNodeId = task.getId();
        String flowNodeName = task.getName();
        if (findTargetSequenceFlows(process, flowNodeId).isEmpty()) {
            errorList.add(String.format("タスクが遷移先に設定されていません。タスクに向かうようにシーケンスフローを配置してください。 id = [%s] name = [%s]", flowNodeId, flowNodeName));
        }
        return errorList;
    }

    /**
     * 開始イベントに対し、精査を行う。
     *
     * @param process プロセス定義情報
     * @return 精査エラーメッセージ
     */
    private List<String> validateStartEvent(TProcess process) {
        List<String> errorList = new ArrayList<String>();
        List<TStartEvent> startEvents = getStartEvents(process);
        if (startEvents.isEmpty()) {
            errorList.add("ワークフローに開始イベントは必須です。開始イベントを配置してください。");
            return errorList;
        } else if (startEvents.size() > 1) {
            errorList.add("ワークフローに配置できる開始イベントは一つのみです。開始イベントを配置し直してください。");
            return errorList;
        }

        TStartEvent startEvent = startEvents.get(0);
        String startEventId = startEvent.getId();
        String startEventName = startEvent.getName();
        if (!startEvent.getEventDefinition().isEmpty()) {
            errorList.add(String.format("サポート対象外の開始イベントです。 id = [%s] name = [%s]", startEventId, startEventName));
            return errorList;
        }

        errorList.addAll(validateSourceStartEvent(process, startEvent));
        return errorList;
    }

    /**
     * 開始イベントが遷移元として不正か精査を行う。
     *
     * @param process    プロセス定義情報
     * @param startEvent 開始イベント定義情報
     * @return 精査エラーメッセージ
     */
    private List<String> validateSourceStartEvent(TProcess process, TStartEvent startEvent) {
        List<String> errorList = new ArrayList<String>();
        String flowNodeId = startEvent.getId();
        String flowNodeName = startEvent.getName();
        List<TSequenceFlow> sourceSequenceFlows = findSourceSequenceFlows(process, flowNodeId);
        if (sourceSequenceFlows.isEmpty()) {
            errorList.add(String.format("開始イベントが遷移元に設定されていません。開始イベントから伸びるようにシーケンスフローを配置してください。 id = [%s] name = [%s]", flowNodeId, flowNodeName));
        }
        if (sourceSequenceFlows.size() > 1) {
            errorList.add(String.format("開始イベントが複数の遷移元に設定されています。開始イベントから伸びるシーケンスフローが1つになるように配置してください。 id = [%s] name = [%s]", flowNodeId, flowNodeName));
        }
        return errorList;
    }

    /**
     * 停止イベントに対し、精査を行う。
     *
     * @param process プロセス定義情報
     * @return 精査エラーメッセージ
     */
    private List<String> validateEndEvents(TProcess process) {
        List<String> errorList = new ArrayList<String>();
        List<TEndEvent> endEvents = getEndEvents(process);
        if (endEvents.isEmpty()) {
            errorList.add("ワークフローに停止イベントは必須です。停止イベントを配置してください。");
            return errorList;
        }

        for (TEndEvent endEvent : endEvents) {
            errorList.addAll(validateEndEvent(process, endEvent));
        }
        return errorList;
    }

    /**
     * 停止イベントに対し、精査を行う。
     *
     * @param process  プロセス定義情報
     * @param endEvent 停止イベント定義情報
     * @return 精査エラーメッセージ
     */
    private List<String> validateEndEvent(TProcess process, TEndEvent endEvent) {
        List<String> errorList = new ArrayList<String>();
        String endEventId = endEvent.getId();
        String endEventName = endEvent.getName();
        List<JAXBElement<? extends TEventDefinition>> eventDefinitions = endEvent.getEventDefinition();
        if (eventDefinitions.size() != 1 || !(eventDefinitions.get(0).getValue() instanceof TTerminateEventDefinition)) {
            errorList.add(String.format("サポート対象外の終了イベントです。 id = [%s] name = [%s]", endEventId, endEventName));
            return errorList;
        }

        errorList.addAll(validateTargetEndEvent(process, endEvent));
        return errorList;
    }

    /**
     * 停止イベントが遷移先として不正か精査を行う。
     *
     * @param process  プロセス定義情報
     * @param endEvent 停止イベント定義情報
     * @return 精査エラーメッセージ
     */
    private List<String> validateTargetEndEvent(TProcess process, TEndEvent endEvent) {
        List<String> errorList = new ArrayList<String>();
        if (findTargetSequenceFlows(process, endEvent.getId()).isEmpty()) {
            errorList.add(String.format("停止イベントが遷移先に設定されていません。停止イベントに向かうようにシーケンスフローを配置してください。 id = [%s] name = [%s]", endEvent.getId(), endEvent.getName()));
        }
        return errorList;
    }

    /**
     * ゲートウェイに対し、精査を行う。
     *
     * @param process プロセス定義情報
     * @return 精査エラーメッセージ
     */
    private List<String> validateGateways(TProcess process) {
        List<String> errorList = new ArrayList<String>();
        for (TExclusiveGateway gateway : getGateways(process)) {
            errorList.addAll(validateSourceGateway(process, gateway));
            errorList.addAll(validateTargetGateway(process, gateway));
            errorList.addAll(validateSequenceFlowsFromGateway(process, gateway));
        }
        return errorList;
    }

    /**
     * ゲートウェイが遷移元として不正か精査を行う。
     *
     * @param process プロセス定義情報
     * @param gateway ゲートウェイ定義情報
     * @return 精査エラーメッセージ
     */
    private List<String> validateSourceGateway(TProcess process, TExclusiveGateway gateway) {
        List<String> errorList = new ArrayList<String>();
        String flowNodeId = gateway.getId();
        if (findSourceSequenceFlows(process, flowNodeId).isEmpty()) {
            errorList.add(String.format("ゲートウェイが遷移元に設定されていません。ゲートウェイから伸びるようにシーケンスフローを配置してください。"
                    + " id = [%s] name = [%s]", flowNodeId, gateway.getName()));
        }
        return errorList;
    }

    /**
     * ゲートウェイが遷移先として不正か精査を行う。
     *
     * @param process プロセス定義情報
     * @param gateway ゲートウェイ定義情報
     * @return 精査エラーメッセージ
     */
    private List<String> validateTargetGateway(TProcess process, TExclusiveGateway gateway) {
        List<String> errorList = new ArrayList<String>();
        String flowNodeId = gateway.getId();
        String flowNodeName = gateway.getName();
        List<TSequenceFlow> targetSequenceFlows = findTargetSequenceFlows(process, flowNodeId);
        if (targetSequenceFlows.isEmpty()) {
            errorList.add(String.format("ゲートウェイが遷移先に設定されていません。ゲートウェイに向かうようにシーケンスフローを配置してください。 id = [%s] name = [%s]", flowNodeId, flowNodeName));
        }
        if (targetSequenceFlows.size() > 1) {
            errorList.add(String.format("ゲートウェイが複数の遷移先に設定されています。ゲートウェイに向かうシーケンスフローが1つになるように配置してください。 id = [%s] name = [%s]", flowNodeId, flowNodeName));
        }
        return errorList;
    }

    /**
     * ゲートウェイから伸びるシーケンスフローに対し、精査を行う。
     *
     * @param process プロセス定義情報
     * @param gateway ゲートウェイ定義情報
     * @return 精査エラーメッセージ
     */
    private List<String> validateSequenceFlowsFromGateway(TProcess process, TExclusiveGateway gateway) {
        List<String> errorList = new ArrayList<String>();
        for (TSequenceFlow sequenceFlow : findSourceSequenceFlows(process, gateway.getId())) {
            String sequenceFlowId = sequenceFlow.getId();
            String sequenceFlowName = sequenceFlow.getName();
            TExpression conditionExpression = sequenceFlow.getConditionExpression();
            if (conditionExpression == null || conditionExpression.getContent().isEmpty()) {
                errorList.add(String.format("ゲートウェイから伸びるシーケンスフローの場合、フロー進行条件は必須です。[条件]を設定してください。 id = [%s] name = [%s]", sequenceFlowId, sequenceFlowName));
            } else {
                String condition = conditionExpression.getContent().get(0).toString();
                errorList.addAll(validateConditionFormat(condition, sequenceFlowId, sequenceFlowName));
            }
        }
        return errorList;
    }


    /**
     * 境界イベントに対し、精査を行う。
     *
     * @param process  ワークフロー定義情報
     * @param messageNames      メッセージ名
     * @return 精査エラーメッセージ
     */
    private List<String> validateBoundaryEvents(TProcess process, Map<String, String> messageNames) {
        List<String> errorList = new ArrayList<String>();
        for (TBoundaryEvent boundaryEvent : getBoundaryEvents(process)) {
            errorList.addAll(validateBoundaryEvent(process, messageNames, boundaryEvent));
        }
        return errorList;
    }

    /**
     * 境界イベントに対し、精査を行う。
     *
     * @param process       プロセス定義情報
     * @param messageNames      メッセージ名
     * @param boundaryEvent 境界イベント定義情報
     * @return 精査エラーメッセージ
     */
    private List<String> validateBoundaryEvent(TProcess process, Map<String, String> messageNames, TBoundaryEvent boundaryEvent) {
        List<String> errorList = new ArrayList<String>();
        String boundaryEventId = boundaryEvent.getId();
        List<JAXBElement<? extends TEventDefinition>> eventDefinitions = boundaryEvent.getEventDefinition();
        String boundaryEventName = boundaryEvent.getName();
        if (eventDefinitions.size() != 1 || !(eventDefinitions.get(0).getValue() instanceof TMessageEventDefinition)) {
            errorList.add(String.format("サポート対象外の境界イベントです。 id = [%s] name = [%s]", boundaryEventId, boundaryEventName));
            return errorList;
        }
        errorList.addAll(validateBoundaryEventTriggerId(messageNames, boundaryEvent));

        if (!boundaryEvent.isCancelActivity()) {
            errorList.add(String.format("境界イベントのCancelActivityがtrueではありません。CancelActivityをtrueにしてください。 id = [%s] name = [%s]",
                    boundaryEvent.getId(), boundaryEvent.getName()));
        }
        errorList.addAll(validateSourceBoundaryEvent(process, boundaryEvent));
        return errorList;
    }

    /**
     * 境界イベントが遷移元として不正か精査を行う。
     *
     * @param process       プロセス定義情報
     * @param boundaryEvent 境界イベント定義情報
     * @return 精査エラーメッセージ
     */
    private List<String> validateSourceBoundaryEvent(TProcess process, TBoundaryEvent boundaryEvent) {
        List<String> errorList = new ArrayList<String>();
        String flowNodeId = boundaryEvent.getId();
        String flowNodeName = boundaryEvent.getName();
        List<TSequenceFlow> sourceSequenceFlows = findSourceSequenceFlows(process, flowNodeId);
        if (sourceSequenceFlows.isEmpty()) {
            errorList.add(String.format("境界イベントが遷移元に設定されていません。境界イベントから伸びるようにシーケンスフローを配置してください。 id = [%s] name = [%s]", flowNodeId, flowNodeName));
        }

        if (sourceSequenceFlows.size() > 1) {
            errorList.add(String.format("境界イベントが複数の遷移元に設定されています。境界イベントから伸びるシーケンスフローが1つになるように配置してください。 id = [%s] name = [%s]", flowNodeId, flowNodeName));
        }
        return errorList;
    }

    /**
     * 境界イベントトリガーIDの精査を行う。
     *
     * @param messageNames      メッセージ名
     * @param boundaryEvent 境界イベント定義情報
     * @return 精査エラーメッセージ
     */
    private List<String> validateBoundaryEventTriggerId(Map<String, String> messageNames, TBoundaryEvent boundaryEvent) {
        List<String> errorList = new ArrayList<String>();
        TMessageEventDefinition messageEventDefinition = (TMessageEventDefinition) boundaryEvent.getEventDefinition().get(0).getValue();
        QName messageRef = messageEventDefinition.getMessageRef();
        if (messageRef == null || StringUtil.isNullOrEmpty(messageNames.get(messageRef.getLocalPart()))) {
            errorList.add(String.format("境界イベントの境界イベントトリガーIDは必須です。[Message]を設定してください。 id = [%s] name = [%s]",
                    boundaryEvent.getId(), boundaryEvent.getName()));
            return errorList;
        }

        return errorList;
    }

    /**
     * 指定されたノードIDを遷移元とするシーケンスフローを取得する。
     *
     * @param process    プロセス定義情報
     * @param flowNodeId フローノードID
     * @return シーケンスフロー定義情報
     */
    private List<TSequenceFlow> findSourceSequenceFlows(TProcess process, final String flowNodeId) {
        List<TSequenceFlow> sequenceFlows = new ArrayList<TSequenceFlow>();
        for (JAXBElement<? extends TFlowElement> element : process.getFlowElement()) {
            if (element.getValue() instanceof TSequenceFlow) {
                TSequenceFlow sequenceFlow = (TSequenceFlow) element.getValue();
                if (isSourceSourceFlow(flowNodeId, sequenceFlow)) {
                    sequenceFlows.add(sequenceFlow);
                }
            }
        }
        return sequenceFlows;
    }

    /**
     * 指定されたノードIDを遷移先とするシーケンスフローを取得する。
     *
     * @param process    プロセス定義情報
     * @param flowNodeId フローノードID
     * @return シーケンスフロー定義情報
     */
    private List<TSequenceFlow> findTargetSequenceFlows(TProcess process, final String flowNodeId) {
        List<TSequenceFlow> sequenceFlows = new ArrayList<TSequenceFlow>();
        for (JAXBElement<? extends TFlowElement> element : process.getFlowElement()) {
            if (element.getValue() instanceof TSequenceFlow) {
                TSequenceFlow sequenceFlow = (TSequenceFlow) element.getValue();
                if (isTargetSequenceFlow(flowNodeId, sequenceFlow)) {
                    sequenceFlows.add(sequenceFlow);
                }
            }
        }
        return sequenceFlows;
    }

    /**
     * シーケンスフローの遷移先が指定したフローノードIDか判定
     *
     * @param flowNodeId   フローノードID
     * @param sequenceFlow シーケンスフロー定義情報
     * @return シーケンスフローの遷移先が指定したフローノードIDの場合、true
     */
    private boolean isTargetSequenceFlow(String flowNodeId, TSequenceFlow sequenceFlow) {
        TFlowElement targetRef = (TFlowElement) sequenceFlow.getTargetRef();
        return flowNodeId.equals(targetRef.getId());
    }

    /**
     * シーケンスフローの遷移元が指定したフローノードIDか判定
     *
     * @param flowNodeId   フローノードID
     * @param sequenceFlow シーケンスフロー定義情報
     * @return シーケンスフローの遷移元が指定したフローノードIDの場合、true
     */
    private boolean isSourceSourceFlow(String flowNodeId, TSequenceFlow sequenceFlow) {
        TFlowElement sourceRef = (TFlowElement) sequenceFlow.getSourceRef();
        return flowNodeId.equals(sourceRef.getId());
    }

    /**
     * 未使用フローノードを取得する。
     *
     * @param process         プロセス定義情報
     * @param usedFlowNodeIds 使用済フローノード
     * @return 未使用フローノード
     */
    private List<TFlowElement> getUnUsedFlowNodes(TProcess process, final List<String> usedFlowNodeIds) {
        List<TFlowElement> unUsedFlowNodes = new ArrayList<TFlowElement>();
        for (JAXBElement<? extends TFlowElement> element : process.getFlowElement()) {
            TFlowElement flowElement = element.getValue();
            if (!usedFlowNodeIds.contains(flowElement.getId()) && !(flowElement instanceof TSequenceFlow)) {
                unUsedFlowNodes.add(flowElement);
            }
        }
        return unUsedFlowNodes;
    }

    /**
     * 指定したフローノードIDから到達可能なフローノードIDをListに格納する。
     *
     * @param process            プロセス定義情報
     * @param reachedFlowNodeIds 到達済フローノードID
     * @param flowNodeId         フローノードID
     */
    private void addReachedFlowNodeIds(TProcess process, List<String> reachedFlowNodeIds, String flowNodeId) {
        reachedFlowNodeIds.add(flowNodeId);

        for (TSequenceFlow sequenceFlow : findTargetSequenceFlows(process, flowNodeId)) {
            TFlowElement sourceRef = (TFlowElement) sequenceFlow.getSourceRef();
            String sourceFlowNodeId = sourceRef.getId();
            if (!reachedFlowNodeIds.contains(sourceFlowNodeId)) {
                addReachedFlowNodeIds(process, reachedFlowNodeIds, sourceFlowNodeId);
            }
        }
    }

    /**
     * プロセス定義情報を取得する。
     *
     * @param definitions ワークフロー定義情報
     * @return プロセス定義情報
     */
    private List<TProcess> getProcesses(TDefinitions definitions) {
        List<TProcess> processes = new ArrayList<TProcess>();
        for (JAXBElement<? extends TRootElement> element : definitions.getRootElement()) {
            if (element.getValue() instanceof TProcess) {
                processes.add((TProcess) element.getValue());
            }
        }
        return processes;
    }

    /**
     * メッセージIDをキーにしたメッセージ名のマップを取得する。
     *
     * @param definitions ワークフロー定義情報
     * @return メッセージIDをキーにしたメッセージ名のマップ
     */
    private Map<String, String> createMessageNameMap(TDefinitions definitions) {
        Map<String, String> messageIdMap = new HashMap<String, String>();

        for (JAXBElement<? extends TRootElement> element : definitions.getRootElement()) {
            if (element.getValue() instanceof TMessage) {
                TMessage message = (TMessage) element.getValue();
                messageIdMap.put(message.getId(), message.getName());
            }
        }
        return messageIdMap;
    }

    /**
     * ユーザタスク定義情報を取得する。
     *
     * @param process プロセス定義情報
     * @return ユーザタスク定義情報
     */
    private List<TUserTask> getTasks(TProcess process) {
        List<TUserTask> tasks = new ArrayList<TUserTask>();
        for (JAXBElement<? extends TFlowElement> element : process.getFlowElement()) {
            if (element.getValue() instanceof TUserTask) {
                tasks.add((TUserTask) element.getValue());
            }
        }
        return tasks;
    }

    /**
     * 開始イベント定義情報を取得する。
     *
     * @param process プロセス定義情報
     * @return 開始イベント定義情報
     */
    private List<TStartEvent> getStartEvents(TProcess process) {
        List<TStartEvent> startEvents = new ArrayList<TStartEvent>();
        for (JAXBElement<? extends TFlowElement> element : process.getFlowElement()) {
            if (element.getValue() instanceof TStartEvent) {
                startEvents.add((TStartEvent) element.getValue());
            }
        }
        return startEvents;
    }

    /**
     * 停止イベント定義情報を取得する。
     *
     * @param process プロセス定義情報
     * @return 停止イベント定義情報
     */
    private List<TEndEvent> getEndEvents(TProcess process) {
        List<TEndEvent> endEvents = new ArrayList<TEndEvent>();
        for (JAXBElement<? extends TFlowElement> element : process.getFlowElement()) {
            if (element.getValue() instanceof TEndEvent) {
                endEvents.add((TEndEvent) element.getValue());
            }
        }
        return endEvents;
    }

    /**
     * ゲートウェイ定義情報を取得する。
     *
     * @param process プロセス定義情報
     * @return ゲートウェイ定義情報
     */
    private List<TExclusiveGateway> getGateways(TProcess process) {
        List<TExclusiveGateway> exclusiveGateways = new ArrayList<TExclusiveGateway>();
        for (JAXBElement<? extends TFlowElement> element : process.getFlowElement()) {
            if (element.getValue() instanceof TExclusiveGateway) {
                exclusiveGateways.add((TExclusiveGateway) element.getValue());
            }
        }
        return exclusiveGateways;
    }

    /**
     * 境界イベント定義情報を取得する。
     *
     * @param process プロセス定義情報
     * @return 境界イベント定義情報
     */
    private List<TBoundaryEvent> getBoundaryEvents(TProcess process) {
        List<TBoundaryEvent> boundaryEvents = new ArrayList<TBoundaryEvent>();
        for (JAXBElement<? extends TFlowElement> element : process.getFlowElement()) {
            if (element.getValue() instanceof TBoundaryEvent) {
                boundaryEvents.add((TBoundaryEvent) element.getValue());
            }
        }
        return boundaryEvents;
    }
}
