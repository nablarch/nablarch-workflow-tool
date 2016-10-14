package nablarch.tool.workflow.bpmn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;

import org.omg.spec.bpmn._20100524.model.TBoundaryEvent;
import org.omg.spec.bpmn._20100524.model.TDefinitions;
import org.omg.spec.bpmn._20100524.model.TEndEvent;
import org.omg.spec.bpmn._20100524.model.TEventDefinition;
import org.omg.spec.bpmn._20100524.model.TExclusiveGateway;
import org.omg.spec.bpmn._20100524.model.TExpression;
import org.omg.spec.bpmn._20100524.model.TFlowElement;
import org.omg.spec.bpmn._20100524.model.TLane;
import org.omg.spec.bpmn._20100524.model.TLaneSet;
import org.omg.spec.bpmn._20100524.model.TLoopCharacteristics;
import org.omg.spec.bpmn._20100524.model.TMessage;
import org.omg.spec.bpmn._20100524.model.TMessageEventDefinition;
import org.omg.spec.bpmn._20100524.model.TMultiInstanceLoopCharacteristics;
import org.omg.spec.bpmn._20100524.model.TProcess;
import org.omg.spec.bpmn._20100524.model.TRootElement;
import org.omg.spec.bpmn._20100524.model.TSequenceFlow;
import org.omg.spec.bpmn._20100524.model.TStartEvent;
import org.omg.spec.bpmn._20100524.model.TTask;
import org.omg.spec.bpmn._20100524.model.TUserTask;

import nablarch.core.repository.SystemRepository;
import nablarch.tool.workflow.WorkflowDefinitionGeneratorSequenceFlow;
import nablarch.tool.workflow.WorkflowDefinitionGeneratorTask;

import nablarch.integration.workflow.definition.BoundaryEvent;
import nablarch.integration.workflow.definition.Event;
import nablarch.integration.workflow.definition.Gateway;
import nablarch.integration.workflow.definition.Lane;
import nablarch.integration.workflow.definition.SequenceFlow;
import nablarch.integration.workflow.definition.Task;
import nablarch.integration.workflow.definition.WorkflowDefinition;

/**
 * ワークフロー定義情報を変換するクラス。
 *
 * @author Satoshi Taromaru
 * @since 1.4.2
 */
public class BpmnWorkflowDefinitionConverter {

    /**
     * ワークフロー定義ファイルから取得したワークフロー情報をワークフロー定義に変換する。
     *
     * @param definitions   ワークフロー定義情報
     * @param workflowId    ワークフローID
     * @param workflowName  ワークフロー名
     * @param version       バージョン
     * @param effectiveDate 有効日
     * @return ワークフロー定義
     */
    public WorkflowDefinition convertToWorkflowDefinition(TDefinitions definitions, String workflowId,
                                                          String workflowName, int version, String effectiveDate) {
        WorkflowDefinition workflowDefinition = new WorkflowDefinition(workflowId, version, workflowName, effectiveDate);
        TProcess process = getProcesses(definitions).get(0);
        TLaneSet laneSet = process.getLaneSet().get(0);
        workflowDefinition.setLanes(convertToLanes(laneSet));

        Map<String, String> laneIds = createLaneIdMap(laneSet);
        workflowDefinition.setTasks(convertToTasks(process, laneIds));
        workflowDefinition.setEvents(convertToEvents(process, laneIds));
        workflowDefinition.setGateways(convertToGateways(process, laneIds));
        workflowDefinition.setBoundaryEvents(convertToBoundaryEvents(definitions, process, laneIds));
        workflowDefinition.setSequenceFlows(convertToSequenceFlow(process));

        return workflowDefinition;
    }

    /**
     * フローノードIDをキーにした、フローノードIDとレーンIDのMapを生成する。
     *
     * @param laneSet レーン定義情報
     * @return フローノードIDとレーンIDのMap
     */
    private Map<String, String> createLaneIdMap(TLaneSet laneSet) {
        Map<String, String> laneIds = new HashMap<String, String>();
        for (TLane lane : laneSet.getLane()) {
            String laneId = lane.getId();
            for (JAXBElement<Object> flowElm : lane.getFlowNodeRef()) {
                TFlowElement flowElement = (TFlowElement) flowElm.getValue();
                laneIds.put(flowElement.getId(), laneId);
            }
        }
        return laneIds;
    }

    /**
     * 境界イベント定義情報と紐付くメッセージ名を返却する。
     * 取得できない場合、空文字を返却する。
     *
     * @param definitions   ワークフロー定義情報
     * @param boundaryEvent 境界イベント定義情報
     * @return 境界イベント定義情報と紐付くメッセージ名
     */
    private String getMessageName(TDefinitions definitions, TBoundaryEvent boundaryEvent) {
        List<JAXBElement<? extends TEventDefinition>> eventDefinitions = boundaryEvent.getEventDefinition();
        TEventDefinition eventDefinition = eventDefinitions.get(0).getValue();
        TMessageEventDefinition messageEventDefinition = (TMessageEventDefinition) eventDefinition;
        String messageTrigger = messageEventDefinition.getMessageRef().getLocalPart();
        return createMessageNameMap(definitions).get(messageTrigger);
    }

    /**
     * ゲートウェイ定義に変換する。
     *
     * @param process プロセス定義情報
     * @param laneIds レーンID情報
     * @return ゲートウェイ定義
     */
    private List<Gateway> convertToGateways(TProcess process, Map<String, String> laneIds) {
        List<Gateway> gateways = new ArrayList<Gateway>();
        for (TFlowElement gateway : getGateways(process)) {
            String flowNodeId = gateway.getId();
            gateways.add(new Gateway(flowNodeId, gateway.getName(), laneIds.get(flowNodeId),
                    Gateway.GatewayType.EXCLUSIVE.toString(), new ArrayList<SequenceFlow>()));
        }
        return gateways;
    }

    /**
     * シーケンスフロー定義に変換する。
     *
     * @param process プロセス定義情報
     * @return シーケンスフロー定義
     */
    private List<SequenceFlow> convertToSequenceFlow(TProcess process) {
        List<SequenceFlow> sequenceFlows = new ArrayList<SequenceFlow>();
        for (TSequenceFlow sequenceFlow : getSequenceFlows(process)) {
            String sourceFlowNodeId = ((TFlowElement) sequenceFlow.getSourceRef()).getId();
            String targetFlowNodeId = ((TFlowElement) sequenceFlow.getTargetRef()).getId();
            String condition = getCondition(sequenceFlow.getConditionExpression(), "flowProceedCondition");
            sequenceFlows.add(
                    new WorkflowDefinitionGeneratorSequenceFlow(sequenceFlow.getId(), sequenceFlow.getName(), sourceFlowNodeId, targetFlowNodeId, condition));
        }
        return sequenceFlows;
    }

    /**
     * 完了条件、フロー進行条件取得。
     *
     * @param conditionExpression 条件
     * @param componentKey        コンポーネント定義ファイルで定義したキー文字列
     * @return 完了条件、またはフロー進行条件
     */
    @SuppressWarnings("unchecked")
    private String getCondition(TExpression conditionExpression, String componentKey) {
        if (conditionExpression == null || conditionExpression.getContent().isEmpty()) {
            return null;
        }

        String condition = conditionExpression.getContent().get(0).toString();
        String key = condition;
        int index = condition.indexOf("(");
        if (index > 0) {
            key = condition.substring(0, index);
        }

        Map<String, String> conditionMap = SystemRepository.get(componentKey);
        if (conditionMap.containsKey(key)) {
            condition = conditionMap.get(key) + condition.replace(key, "");
        }
        return condition;
    }

    /**
     * 境界イベント定義に変換する。
     *
     * @param definitions ワークフロー定義情報
     * @param process     プロセス定義情報
     * @param laneIds     レーンID情報
     * @return 境界イベント定義
     */
    private List<BoundaryEvent> convertToBoundaryEvents(TDefinitions definitions, TProcess process, Map<String, String> laneIds) {
        List<BoundaryEvent> boundaryEvents = new ArrayList<BoundaryEvent>();
        for (TBoundaryEvent boundaryEvent : getBoundaryEvents(process)) {
            String boundaryEventId = boundaryEvent.getId();
            String boundaryEventTriggerId = getMessageName(definitions, boundaryEvent);
            boundaryEvents.add(new BoundaryEvent(boundaryEventId, boundaryEvent.getName(), laneIds.get(boundaryEventId),
                    boundaryEventTriggerId, boundaryEventTriggerId, boundaryEvent.getAttachedToRef().getLocalPart(), new ArrayList<SequenceFlow>()));
        }
        return boundaryEvents;
    }

    /**
     * イベント定義に変換する。
     *
     * @param process プロセス定義情報
     * @param laneIds レーンID情報
     * @return イベント定義
     */
    private List<Event> convertToEvents(TProcess process, Map<String, String> laneIds) {
        List<Event> events = new ArrayList<Event>();
        TStartEvent startEvent = getStartEvents(process).get(0);
        events.add(new Event(startEvent.getId(), startEvent.getName(), laneIds.get(startEvent.getId()),
                Event.EventType.START.toString(), new ArrayList<SequenceFlow>()));

        for (TFlowElement endEvent : getEndEvents(process)) {
            events.add(new Event(endEvent.getId(), endEvent.getName(), laneIds.get(endEvent.getId()),
                    Event.EventType.TERMINATE.toString(), new ArrayList<SequenceFlow>()));
        }
        return events;
    }

    /**
     * タスク定義に変換する。
     *
     * @param process プロセス定義情報
     * @param laneIds レーンID情報
     * @return タスク定義
     */
    private List<Task> convertToTasks(TProcess process, Map<String, String> laneIds) {
        List<Task> tasks = new ArrayList<Task>();
        for (TTask task : getTasks(process)) {
            String flowNodeId = task.getId();
            String taskName = task.getName();

            String condition = null;
            Task.MultiInstanceType multiInstanceType = Task.MultiInstanceType.NONE;
            JAXBElement<? extends TLoopCharacteristics> loopCharacteristics = task.getLoopCharacteristics();
            if (loopCharacteristics != null) {
                TMultiInstanceLoopCharacteristics characteristics = (TMultiInstanceLoopCharacteristics) loopCharacteristics.getValue();
                if (characteristics.isIsSequential()) {
                    multiInstanceType = Task.MultiInstanceType.SEQUENTIAL;
                } else {
                    multiInstanceType = Task.MultiInstanceType.PARALLEL;
                }
                condition = getCondition(characteristics.getCompletionCondition(), "completionCondition");
            }
            tasks.add(new WorkflowDefinitionGeneratorTask(flowNodeId, taskName, laneIds.get(flowNodeId), multiInstanceType.toString(), condition));
        }
        return tasks;
    }

    /**
     * レーン定義に変換する。
     *
     * @param laneSet レーン定義情報
     * @return レーン定義
     */
    private List<Lane> convertToLanes(TLaneSet laneSet) {
        List<Lane> lanes = new ArrayList<Lane>();
        for (TLane lane : laneSet.getLane()) {
            lanes.add(new Lane(lane.getId(), lane.getName()));
        }
        return lanes;
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

    /**
     * シーケンスフロー定義情報を取得する。
     *
     * @param process プロセス定義情報
     * @return シーケンスフロー定義情報
     */
    private List<TSequenceFlow> getSequenceFlows(TProcess process) {
        List<TSequenceFlow> sequenceFlows = new ArrayList<TSequenceFlow>();
        for (JAXBElement<? extends TFlowElement> element : process.getFlowElement()) {
            if (element.getValue() instanceof TSequenceFlow) {
                sequenceFlows.add((TSequenceFlow) element.getValue());
            }
        }
        return sequenceFlows;
    }
}
