package nablarch.tool.statemachine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBElement;

import nablarch.core.repository.SystemRepository;
import nablarch.integration.workflow.definition.BoundaryEvent;
import nablarch.integration.workflow.definition.Event;
import nablarch.integration.workflow.definition.Event.EventType;
import nablarch.integration.workflow.definition.Gateway;
import nablarch.integration.workflow.definition.Gateway.GatewayType;
import nablarch.integration.workflow.definition.Lane;
import nablarch.integration.workflow.definition.SequenceFlow;
import nablarch.integration.workflow.definition.Task;
import nablarch.integration.workflow.definition.Task.MultiInstanceType;
import nablarch.integration.workflow.definition.WorkflowDefinition;
import nablarch.tool.DefinitionCreator;
import nablarch.tool.workflow.WorkflowDefinitionFile;
import nablarch.tool.workflow.WorkflowDefinitionGeneratorSequenceFlow;
import nablarch.tool.workflow.WorkflowDefinitionGeneratorTask;
import org.omg.spec.bpmn._20100524.model.TBoundaryEvent;
import org.omg.spec.bpmn._20100524.model.TDefinitions;
import org.omg.spec.bpmn._20100524.model.TEndEvent;
import org.omg.spec.bpmn._20100524.model.TExpression;
import org.omg.spec.bpmn._20100524.model.TFlowElement;
import org.omg.spec.bpmn._20100524.model.TGateway;
import org.omg.spec.bpmn._20100524.model.TLane;
import org.omg.spec.bpmn._20100524.model.TMessage;
import org.omg.spec.bpmn._20100524.model.TMessageEventDefinition;
import org.omg.spec.bpmn._20100524.model.TProcess;
import org.omg.spec.bpmn._20100524.model.TRootElement;
import org.omg.spec.bpmn._20100524.model.TSequenceFlow;
import org.omg.spec.bpmn._20100524.model.TStartEvent;
import org.omg.spec.bpmn._20100524.model.TSubProcess;
import org.omg.spec.bpmn._20100524.model.TTask;

/**
 * ステートマシン定義を生成する{@link DefinitionCreator}実装クラス
 *
 * @author Naoki Yamamoto
 */
public class StateMachineDefinitionCreator implements DefinitionCreator {

    @Override
    public WorkflowDefinition create(final WorkflowDefinitionFile workflowDefinitionFile) {
        final File bpmnFile = new File(workflowDefinitionFile.getPath());
        final TDefinitions definitions;
        try {
            definitions = JAXB.unmarshal(new InputStreamReader(new FileInputStream(bpmnFile), Charset.forName("utf-8")), TDefinitions.class);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        final WorkflowDefinition workflowDefinition = new WorkflowDefinition(workflowDefinitionFile.getWorkflowId(), Integer.parseInt(workflowDefinitionFile.getVersion()),
                workflowDefinitionFile.getWorkflowName(), workflowDefinitionFile.getEffectiveDate());

        final TProcess process = getProcess(definitions);
        final List<TLane> tLanes = process.getLaneSet().get(0).getLane();
        final List<TFlowElement> flowElements = getFlowElements(process);

        final Map<String, String> laneIdMap = createLaneIdMap(tLanes);
        final Map<String, String> boundaryEventTriggerMap = createBoundaryEventTriggerMap(definitions);

        workflowDefinition.setLanes(getLanes(tLanes));
        workflowDefinition.setEvents(getEvents(flowElements, laneIdMap));
        workflowDefinition.setBoundaryEvents(getBoundaryEvent(flowElements, laneIdMap, boundaryEventTriggerMap));
        workflowDefinition.setSequenceFlows(getSequenceFlows(flowElements));
        workflowDefinition.setTasks(getTask(flowElements, laneIdMap));
        workflowDefinition.setGateways(getGateways(flowElements, laneIdMap));

        return workflowDefinition;
    }

    /**
     * プロセス情報を取得する。
     * @param definitions BPMN定義
     * @return プロセス情報
     */
    private TProcess getProcess(final TDefinitions definitions) {
        for (JAXBElement<? extends TRootElement> jaxbElement : definitions.getRootElement()) {
            if (jaxbElement.getValue() instanceof TProcess) {
                return ((TProcess) jaxbElement.getValue());
            }
        }
        throw new IllegalArgumentException("ステートマシンにプロセスが定義されていません。");
    }

    /**
     * レーンリストを取得する。
     * @param tLanes {@link TLane}のリスト
     * @return レーンリスト
     */
    private List<Lane> getLanes(final List<TLane> tLanes) {
        final List<Lane> lanes = new ArrayList<Lane>();
            for (TLane tLane : tLanes) {
                lanes.add(new Lane(tLane.getId(), tLane.getName()));
            }
        return lanes;
    }

    /**
     * イベントリストを取得する。
     * @param flowElements フロー要素リスト
     * @param laneIdMap レーンIDマップ
     * @return イベントリスト
     */
    private List<Event> getEvents(final List<TFlowElement> flowElements, final Map<String, String> laneIdMap) {
        final List<Event> events = new ArrayList<Event>();
        for (TFlowElement element : flowElements) {
            if (element instanceof TStartEvent) {
                final TStartEvent startEvent = (TStartEvent) element;
                events.add(new Event(startEvent.getId(), startEvent.getName(), laneIdMap.get(startEvent.getId()),
                        EventType.START.toString(), null));
            } else if (element instanceof TEndEvent) {
                final TEndEvent endEvent = (TEndEvent) element;
                events.add(new Event(endEvent.getId(), endEvent.getName(), laneIdMap.get(endEvent.getId()),
                        EventType.TERMINATE.toString(), null));
            }
        }
        return events;
    }

    /**
     * 境界イベントリストを取得する
     * @param flowElements フロー要素リスト
     * @param laneIdMap レーンIDマップ
     * @param boundaryEventTriggerMap 境界イベントトリガーマップ
     * @return 境界イベントリスト
     */
    private List<BoundaryEvent> getBoundaryEvent(final List<TFlowElement> flowElements, final Map<String, String> laneIdMap, final Map<String, String> boundaryEventTriggerMap) {
        final List<BoundaryEvent> boundaryEvents = new ArrayList<BoundaryEvent>();
        for (TFlowElement element : flowElements) {
            if (element instanceof TBoundaryEvent) {
                final TBoundaryEvent boundaryEvent = (TBoundaryEvent) element;
                final TMessageEventDefinition definition = ((TMessageEventDefinition) boundaryEvent.getEventDefinition().get(0).getValue());
                final String triggerId = definition.getMessageRef().getLocalPart();
                final String triggerName = boundaryEventTriggerMap.get(triggerId);
                boundaryEvents.add(new BoundaryEvent(boundaryEvent.getId(), boundaryEvent.getName(), laneIdMap.get(boundaryEvent.getId()),
                        triggerName, triggerName, boundaryEvent.getAttachedToRef().getLocalPart(), null));
            }
        }
        return boundaryEvents;
    }

    /**
     * ゲートウェイリストを取得する
     * @param flowElements フロー要素リスト
     * @param laneIdMap レーンIDマップ
     * @return ゲートウェイリスト
     */
    private List<Gateway> getGateways(final List<TFlowElement> flowElements, final Map<String, String> laneIdMap) {
        final List<Gateway> gateways = new ArrayList<Gateway>();
        for (TFlowElement element : flowElements) {
            if (element instanceof TGateway) {
                final TGateway gateway = (TGateway) element;
                gateways.add(new Gateway(gateway.getId(), gateway.getName(), laneIdMap.get(gateway.getId()),
                        GatewayType.EXCLUSIVE.toString(), null));
            }
        }
        return gateways;
    }

    /**
     * シーケンスフローリストを取得する
     * @param flowElements フロー要素リスト
     * @return シーケンスフローリスト
     */
    private List<SequenceFlow> getSequenceFlows(final List<TFlowElement> flowElements) {
        final List<SequenceFlow> sequenceFlows = new ArrayList<SequenceFlow>();
        for (TFlowElement element : flowElements) {
            if (element instanceof TSequenceFlow) {
                final TSequenceFlow sequenceFlow = (TSequenceFlow) element;
                final TFlowElement source = (TFlowElement) sequenceFlow.getSourceRef();
                final TFlowElement target = (TFlowElement) sequenceFlow.getTargetRef();

                sequenceFlows.add(new WorkflowDefinitionGeneratorSequenceFlow(sequenceFlow.getId(), sequenceFlow.getName(),
                        source.getId(), target.getId(), getCondition(sequenceFlow.getConditionExpression())));
            }
        }
        return sequenceFlows;
    }

    /**
     * タスクリストを取得する
     * @param flowElements フロー要素リスト
     * @param laneIdMap レーンIDマップ
     * @return タスクリスト
     */
    private List<Task> getTask(final List<TFlowElement> flowElements,  final Map<String, String> laneIdMap) {
        final List<Task> tasks = new ArrayList<Task>();
        for (TFlowElement element : flowElements) {
            if (element instanceof TTask) {
                final TTask task = (TTask) element;
                tasks.add(new WorkflowDefinitionGeneratorTask(task.getId(), task.getName(), laneIdMap.get(task.getId()),
                        MultiInstanceType.NONE.toString(), null));
            }
        }
        return tasks;
    }

    /**
     * フロー要素がどのレーンに属するかを保持するマップを生成する。
     * <p/>
     * key:フロー要素のID, value:レーンID
     * @param tLanes {@link TLane}のリスト
     * @return レーンIDマップ
     */
    private Map<String, String> createLaneIdMap(final List<TLane> tLanes) {
        final Map<String, String> laneIdMap = new HashMap<String, String>();
        for (TLane lane : tLanes) {
            for (JAXBElement<Object> element : lane.getFlowNodeRef()) {
                laneIdMap.putAll(createLaneIdMap(((TFlowElement) element.getValue()), lane.getId()));
            }
        }
        return laneIdMap;
    }

    /**
     * 指定したフロー要素がどのレーンに属するかを保持するマップを生成する。
     * <p/>
     * key:フロー要素ID, value:レーンID
     * @param element フロー要素
     * @param laneId レーンID
     * @return レーンIDマップ
     */
    private Map<String, String> createLaneIdMap(final TFlowElement element, final String laneId) {
        final Map<String, String> laneIdMap = new HashMap<String, String>();

        if (element instanceof TSubProcess) {
            final TSubProcess subProcess = (TSubProcess) element;
            for (JAXBElement<? extends TFlowElement> subElement : subProcess.getFlowElement()) {
                laneIdMap.putAll(createLaneIdMap((subElement.getValue()), laneId));
            }
        } else {
            laneIdMap.put(element.getId(), laneId);
        }
        return laneIdMap;
    }

    /**
     * 境界イベントトリガーのマップを生成する。
     * <p/>
     * key:境界イベントトリガーID, value:境界イベントトリガー名
     * @param definitions BPMN定義
     * @return 境界イベントトリガーマップ
     */
    private Map<String, String> createBoundaryEventTriggerMap(final TDefinitions definitions) {
        final Map<String, String> boundaryEventTriggerMap = new HashMap<String, String>();
        for (JAXBElement<? extends TRootElement> element : definitions.getRootElement()) {
            if (element.getValue() instanceof TMessage) {
                final TMessage message = (TMessage) element.getValue();
                boundaryEventTriggerMap.put(message.getId(), message.getName());
            }
        }
        return boundaryEventTriggerMap;
    }

    /**
     * フロー進行条件を取得する。
     * @param conditionExpression 条件式
     * @return フロー進行条件
     */
    private String getCondition(final TExpression conditionExpression) {
        if (conditionExpression == null || conditionExpression.getContent().isEmpty()) {
            return null;
        }

        final String condition = conditionExpression.getContent().get(0).toString();
        final int index = condition.indexOf('(');
        final String key = index > 0 ? condition.substring(0, index) : condition;

        final Map<String, String> conditionMap = SystemRepository.get("flowProceedCondition");
        if (conditionMap != null && conditionMap.containsKey(key)) {
            return condition.replace(key, conditionMap.get(key));
        }
        return condition;
    }

    /**
     * プロセスに定義された全てのフロー要素のリストを取得する。
     * @param process プロセス
     * @return フロー要素リスト
     */
    private List<TFlowElement> getFlowElements(final TProcess process) {

        final Map<String, List<TFlowElement>> flowElementMap = getFlowElementMap(process.getFlowElement(), process.getId());
        final List<TFlowElement> flowElements = new ArrayList<TFlowElement>();
        for (Entry<String, List<TFlowElement>> entry : flowElementMap.entrySet()) {
            for (TFlowElement flowElement : entry.getValue()) {

                if (isSubProcess(process.getId(), entry.getKey())) {
                    // サブプロセス内の開始・停止イベントを除外する
                    if (flowElement instanceof TStartEvent || flowElement instanceof TEndEvent) {
                        continue;
                    }
                    // サブプロセス内の開始イベントを遷移元とするシーケンスフローを除外する
                    if (flowElement instanceof TSequenceFlow && ((TSequenceFlow) flowElement).getSourceRef() instanceof TStartEvent) {
                        continue;
                    }
                }

                if (flowElement instanceof TSequenceFlow) {
                    final TSequenceFlow sequenceFlow = (TSequenceFlow) flowElement;
                    // サブプロセスを遷移元とするシーケンスフローを除外する
                    if (sequenceFlow.getSourceRef() instanceof TSubProcess) {
                        continue;
                    }
                    sequenceFlow.setTargetRef(getTargetRef(sequenceFlow, flowElementMap, entry.getKey()));
                }
                flowElements.add(flowElement);
            }
        }
        return flowElements;
    }

    /**
     * 対象のプロセスがサブプロセスか否かを判定する。
     * @param processId 自身のプロセスID
     * @param targetProcessId 対象のプロセスID
     * @return 対象のプロセスIDがサブプロセスであれば {@code true}
     */
    private boolean isSubProcess(final String processId, final String targetProcessId) {
        return !processId.equals(targetProcessId);
    }

    /**
     * プロセスIDをキーとするフロー要素のマップを取得する。
     * <p/>
     * key:プロセスID, value:そのプロセスに属するフロー要素リスト
     *
     * @param flowElements フロー要素リスト
     * @param processId プロセスID
     * @return フロー要素マップ
     */
    private Map<String, List<TFlowElement>> getFlowElementMap(final List<JAXBElement<? extends TFlowElement>> flowElements, final String processId) {
        final Map<String, List<TFlowElement>> flowElementMap = new HashMap<String, List<TFlowElement>>();
        for (JAXBElement<? extends TFlowElement> element : flowElements) {
            if (element.getValue() instanceof TSubProcess) {
                final TSubProcess subProcess = (TSubProcess) element.getValue();
                flowElementMap.putAll(getFlowElementMap(subProcess.getFlowElement(), subProcess.getId()));
            }

            if (flowElementMap.containsKey(processId)) {
                flowElementMap.get(processId).add(element.getValue());
            } else {
                final List<TFlowElement> elements = new ArrayList<TFlowElement>();
                elements.add(element.getValue());
                flowElementMap.put(processId, elements);
            }
        }
        return flowElementMap;
    }

    /**
     * 指定したプロセスの親プロセスIDを取得する。
     * @param flowElementMap フロー要素マップ
     * @param processId プロセスID
     * @return 親プロセスID。親プロセスが存在しない場合は{@code null}。
     */
    private String getParentProcessId(final Map<String, List<TFlowElement>> flowElementMap, final String processId) {
        for (Entry<String, List<TFlowElement>> entry : flowElementMap.entrySet()) {
            for (TFlowElement element : entry.getValue()) {
                if (element instanceof TSubProcess) {
                    final TSubProcess subProcess = (TSubProcess) element;
                    if (subProcess.getId().equals(processId)) {
                        return entry.getKey();
                    }
                }
            }
        }
        return null;
    }

    /**
     * シーケンスフローの遷移先を取得する。
     * <p/>
     * シーケンスフローの遷移先が停止イベントかつ、停止イベントがサブプロセス内に定義されている場合は、
     * 親プロセスに定義されているそのサブプロセスの遷移先のフロー要素を取得する。
     * <br/>
     * シーケンスフローの遷移先がサブプロセスの場合は、そのサブプロセス内の開始イベントの遷移先のフロー要素を取得する。
     *
     * @param sequenceFlow シーケンスフロー
     * @param flowElementMap フロー要素マップ
     * @param processId プロセスID
     * @return シーケンスフローの遷移先のフロー要素
     */
    private TFlowElement getTargetRef(final TSequenceFlow sequenceFlow, final Map<String, List<TFlowElement>> flowElementMap, final String processId) {
        final TFlowElement targetRef = (TFlowElement) sequenceFlow.getTargetRef();
        if (targetRef instanceof TEndEvent) {
            final String parentProcessId = getParentProcessId(flowElementMap, processId);
            if (parentProcessId == null) {
                return targetRef;
            }
            final List<TFlowElement> parentProcessElements = flowElementMap.get(parentProcessId);
            for (TFlowElement parentProcessElement : parentProcessElements) {
                if (parentProcessElement instanceof TSequenceFlow) {
                    final TSequenceFlow subProcessSequenceFlow = (TSequenceFlow) parentProcessElement;
                    if (subProcessSequenceFlow.getSourceRef() instanceof TSubProcess) {
                        final TSubProcess subProcess = (TSubProcess) subProcessSequenceFlow.getSourceRef();
                        if (subProcess.getId().equals(processId)) {
                            return getTargetRef(subProcessSequenceFlow, flowElementMap, parentProcessId);
                        }
                    }
                }
            }
        } else if (targetRef instanceof TSubProcess) {
            final String subProcessId = targetRef.getId();
            final List<TFlowElement> subProcessElements = flowElementMap.get(subProcessId);
            for (TFlowElement subProcessElement : subProcessElements) {
                if (subProcessElement instanceof TSequenceFlow) {
                    final TSequenceFlow subProcessSequenceFlow = (TSequenceFlow) subProcessElement;
                    if (subProcessSequenceFlow.getSourceRef() instanceof TStartEvent) {
                        return getTargetRef(subProcessSequenceFlow, flowElementMap, subProcessId);
                    }
                }
            }
        }
        return targetRef;
    }
}
