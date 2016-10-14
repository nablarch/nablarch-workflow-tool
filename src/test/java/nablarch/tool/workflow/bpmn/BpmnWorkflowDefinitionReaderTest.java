package nablarch.tool.workflow.bpmn;

import java.io.File;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.tool.workflow.WorkflowDefinitionException;
import nablarch.tool.workflow.WorkflowDefinitionFile;
import nablarch.tool.workflow.WorkflowDefinitionGeneratorSequenceFlow;
import nablarch.tool.workflow.WorkflowDefinitionGeneratorTask;

import nablarch.integration.workflow.definition.BoundaryEvent;
import nablarch.integration.workflow.definition.Event;
import nablarch.integration.workflow.definition.Gateway;
import nablarch.integration.workflow.definition.Lane;
import nablarch.integration.workflow.definition.SequenceFlow;
import nablarch.integration.workflow.definition.Task;
import nablarch.integration.workflow.definition.WorkflowDefinition;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * {@link BpmnWorkflowDefinitionReader}のテストクラス。
 *
 * @author Satoshi Taromaru
 * @since 1.4.2
 */
public class BpmnWorkflowDefinitionReaderTest {

    /**
     * テスト対象クラス。
     */
    private BpmnWorkflowDefinitionReader sut = new BpmnWorkflowDefinitionReader();

    /**
     * 事前処理。テスト用コンポーネント定義をロードする。
     */
    @BeforeClass
    public static void setUp() throws Exception {
        SystemRepository.load(new DiContainer(new XmlComponentDefinitionLoader("classpath:nablarch/tool/workflow/WorkflowDefinitionGeneratorTest.xml")));
    }

    /**
     * 事後処理。ロードしたリポジトリを初期化する。
     */
    @AfterClass
    public static void tearDown() {
        SystemRepository.clear();
    }

    /**
     * ロードした結果、WorkflowDefinitionにワークフロー定義が格納されていることを確認。<br>
     * （シーケンスフローの数が多いため、シーケンスフローのassertは別ケースで実施する）
     */
    @Test
    public void loadWorkflowDefinition() throws Exception {

        File file = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/read/normal/normalWorkflow/WP0001_normalWorkflow_ver1_20140804.bpmn");
        WorkflowDefinition workflowDefinition = sut.load(new WorkflowDefinitionFile(file.getName(), file.getPath(), "WP0001", "normalWorkflow", "1", "20140804"));
        assertThat("ファイル名に指定したワークフローIDがWorkflowDefinitionに保持されていること。", workflowDefinition.getWorkflowId(), is("WP0001"));
        assertThat("ファイル名に指定したバージョンがWorkflowDefinitionに保持されていること。", workflowDefinition.getVersion(), is(1));
        assertThat("ファイル名に指定したワークフロー名がWorkflowDefinitionに保持されていること。", workflowDefinition.getWorkflowName(), is("normalWorkflow"));
        assertThat("ファイル名に指定した適用日がWorkflowDefinitionに保持されていること。", workflowDefinition.getEffectiveDate(), is("20140804"));
        assertLanes(workflowDefinition.getLanes());
        assertTasks(workflowDefinition.getTasks());
        assertEvents(workflowDefinition.getEvents());
        assertGateways(workflowDefinition.getGateways());
        assertBoundaryEvents(workflowDefinition.getBoundaryEvents());
    }

    /**
     * ロードした結果、SequenceFlowにシーケンスフロー情報が格納されていることを確認。
     */
    @Test
    public void loadSequenceFlow() throws Exception {
        File file = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/read/normal/sequenceFlow/WP0002_sequenceFlowReadTest_ver1_20140814.custom");
        WorkflowDefinition workflowDefinition = sut.load(new WorkflowDefinitionFile(file.getName(), file.getPath(), "WP0002", "sequenceFlowReadTest", "1", "20140814"));
        List<SequenceFlow> sequenceFlows = workflowDefinition.getSequenceFlows();

        assertThat(sequenceFlows.get(2).getSequenceFlowId(), is("flow3"));
        assertThat(sequenceFlows.get(2).getSequenceFlowName(), is("to End"));
        assertThat(sequenceFlows.get(2).getSourceFlowNodeId(), is("exclusivegateway12"));
        assertThat(sequenceFlows.get(2).getTargetFlowNodeId(), is("terminateEndEvent1"));
        assertThat(((WorkflowDefinitionGeneratorSequenceFlow) sequenceFlows.get(2)).getCondition(), is("nablarch.tool.workflow.SampleFlowProceedCondition"));

        assertThat("シーケンスフロー(id:flow1)の[Id]に指定したシーケンスフローIDがSequenceFlowに保持されていること。", sequenceFlows.get(0).getSequenceFlowId(), is("flow1"));
        assertThat("シーケンスフロー(id:flow1)の[Name]に指定したシーケンスフロー名がSequenceFlowに保持されていること。", sequenceFlows.get(0).getSequenceFlowName(), is("to UserTask"));
        assertThat("シーケンスフロー(id:flow1)の接続元フローノードIDがSequenceFlowに保持されていること。", sequenceFlows.get(0).getSourceFlowNodeId(), is("startevent12345678"));
        assertThat("シーケンスフロー(id:flow1)の接続先フローノードIDがSequenceFlowに保持されていること。", sequenceFlows.get(0).getTargetFlowNodeId(), is("usertask1234567890"));
        assertThat("シーケンスフロー(id:flow1)の[Condition]に指定したフロー進行条件がSequenceFlowに保持されていること。",
                ((WorkflowDefinitionGeneratorSequenceFlow) sequenceFlows.get(0)).getCondition(), nullValue());

        assertThat("シーケンスフロー(id:flow2)の[Id]に指定したシーケンスフローIDがSequenceFlowに保持されていること。", sequenceFlows.get(1).getSequenceFlowId(), is("flow2"));
        assertThat("シーケンスフロー(id:flow2)の[Name]に指定したシーケンスフロー名がSequenceFlowに保持されていること。", sequenceFlows.get(1).getSequenceFlowName(), is("to ExclusiveGateway"));
        assertThat("シーケンスフロー(id:flow2)の接続元フローノードIDがSequenceFlowに保持されていること。", sequenceFlows.get(1).getSourceFlowNodeId(), is("usertask1234567890"));
        assertThat("シーケンスフロー(id:flow2)の接続先フローノードIDがSequenceFlowに保持されていること。", sequenceFlows.get(1).getTargetFlowNodeId(), is("exclusivegateway12"));
        assertThat("シーケンスフロー(id:flow2)の[Condition]に指定したフロー進行条件がSequenceFlowに保持されていること。",
                ((WorkflowDefinitionGeneratorSequenceFlow) sequenceFlows.get(1)).getCondition(), nullValue());

        assertThat("シーケンスフロー(id:flow3)の[Id]に指定したシーケンスフローIDがSequenceFlowに保持されていること。", sequenceFlows.get(2).getSequenceFlowId(), is("flow3"));
        assertThat("シーケンスフロー(id:flow3)の[Name]に指定したシーケンスフロー名がSequenceFlowに保持されていること。", sequenceFlows.get(2).getSequenceFlowName(), is("to End"));
        assertThat("シーケンスフロー(id:flow3)の接続元フローノードIDがSequenceFlowに保持されていること。", sequenceFlows.get(2).getSourceFlowNodeId(), is("exclusivegateway12"));
        assertThat("シーケンスフロー(id:flow3)の接続先フローノードIDがSequenceFlowに保持されていること。", sequenceFlows.get(2).getTargetFlowNodeId(), is("terminateEndEvent1"));
        assertThat("シーケンスフロー(id:flow3)の[Condition]に指定したフロー進行条件がSequenceFlowに保持されていること。",
                ((WorkflowDefinitionGeneratorSequenceFlow) sequenceFlows.get(2)).getCondition(), is("nablarch.tool.workflow.SampleFlowProceedCondition"));

        assertThat("期待しないSequenceFlowを保持しないこと。", sequenceFlows.size(), is(3));
    }

    /**
     * ロードした結果、WorkflowDefinitionGeneratorSequenceFlowにフロー進行条件が格納されていることを確認。<br>
     * また、WorkflowDefinitionGeneratorTaskに完了条件が格納されていることを確認。
     */
    @Test
    public void convertCondition() throws Exception {
        File file = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/read/normal/condition/WP0046_readCondition_ver1_20140917.bpmn");
        WorkflowDefinition workflowDefinition = sut.load(new WorkflowDefinitionFile(file.getName(), file.getPath(), "WP0046", "readCondition", "1", "20140917"));
        List<SequenceFlow> sequenceFlows = workflowDefinition.getSequenceFlows();
        assertThat("シーケンスフロー(id:SequenceFlow_1)の[Condition]にフロー進行条件を指定しない場合、SequenceFlowに保持されないこと。",
                ((WorkflowDefinitionGeneratorSequenceFlow) sequenceFlows.get(0)).getCondition(), nullValue());
        assertThat("シーケンスフロー(id:SequenceFlow_2)の[Condition]にフロー進行条件の省略記法を指定した場合、対応する完全修飾名で展開されSequenceFlowに保持されること。",
                ((WorkflowDefinitionGeneratorSequenceFlow) sequenceFlows.get(1)).getCondition(), is("nablarch.tool.workflow.SampleFlowProceedCondition(result, 1)"));
        assertThat("シーケンスフロー(id:SequenceFlow_3)の[Condition]にフロー進行条件の省略記法を指定した場合、対応する完全修飾名で展開されSequenceFlowに保持されること。",
                ((WorkflowDefinitionGeneratorSequenceFlow) sequenceFlows.get(2)).getCondition(),
                is("nablarch.tool.workflow.GeFlowProceedCondition(result, 1)"));
        assertThat("シーケンスフロー(id:SequenceFlow_4)の[Condition]にフロー進行条件を指定しない場合、SequenceFlowに保持されないこと。",
                ((WorkflowDefinitionGeneratorSequenceFlow) sequenceFlows.get(3)).getCondition(), nullValue());
        assertThat("シーケンスフロー(id:SequenceFlow_5)の[Condition]にフロー進行条件を指定しない場合、SequenceFlowに保持されないこと。",
                ((WorkflowDefinitionGeneratorSequenceFlow) sequenceFlows.get(4)).getCondition(), nullValue());
        assertThat("シーケンスフロー(id:SequenceFlow_6)の[Condition]にフロー進行条件の省略記法と完全一致しない文字列（後方一致）を指定した場合、入力値がSequenceFlowに保持されること。",
                ((WorkflowDefinitionGeneratorSequenceFlow) sequenceFlows.get(5)).getCondition(), is("seq(result, 1)"));
        assertThat("シーケンスフロー(id:SequenceFlow_7)の[Condition]にフロー進行条件の省略記法と完全一致しない文字列（前方一致）を指定した場合、入力値がSequenceFlowに保持されること。",
                ((WorkflowDefinitionGeneratorSequenceFlow) sequenceFlows.get(6)).getCondition(), is("equals(result, 1)"));
        assertThat("シーケンスフロー(id:SequenceFlow_8)の[Condition]にフロー進行条件を指定しない場合、SequenceFlowに保持されないこと。",
                ((WorkflowDefinitionGeneratorSequenceFlow) sequenceFlows.get(7)).getCondition(), nullValue());
        assertThat("シーケンスフロー(id:SequenceFlow_9)の[Condition]にフロー進行条件を指定しない場合、SequenceFlowに保持されないこと。",
                ((WorkflowDefinitionGeneratorSequenceFlow) sequenceFlows.get(8)).getCondition(), nullValue());
        assertThat("シーケンスフロー(id:SequenceFlow_10)の[Condition]にフロー進行条件の省略記法と完全一致しない文字列（部分一致）を指定した場合、入力値がSequenceFlowに保持されること。",
                ((WorkflowDefinitionGeneratorSequenceFlow) sequenceFlows.get(9)).getCondition(), is("notequals(result, 1)"));
        assertThat("シーケンスフロー(id:SequenceFlow_11)の[Condition]にフロー進行条件を指定しない場合、SequenceFlowに保持されないこと。",
                ((WorkflowDefinitionGeneratorSequenceFlow) sequenceFlows.get(10)).getCondition(), nullValue());
        assertThat("期待しないSequenceFlowを保持しないこと。", sequenceFlows.size(), is(11));

        List<Task> tasks = workflowDefinition.getTasks();
        assertThat("タスク(id:usertask1234567890)の[Completion Condition]に完了条件の省略記法を指定した場合、対応する完全修飾名で展開されTaskに保持されること。",
                ((WorkflowDefinitionGeneratorTask) tasks.get(0)).getCondition(), is("nablarch.tool.workflow.SampleCompletionCondition(1)"));
        assertThat("タスク(id:usertask2345678901)の[Completion Condition]に完了条件の省略記法と完全一致しない文字列（前方一致）を指定した場合、入力値がTaskに保持されること。",
                ((WorkflowDefinitionGeneratorTask) tasks.get(1)).getCondition(), is("order(1)"));
        assertThat("タスク(id:usertask3456789012)の[Completion Condition]に完了条件の省略記法と完全一致しない文字列（後方一致）を指定した場合、入力値がTaskに保持されること。",
                ((WorkflowDefinitionGeneratorTask) tasks.get(2)).getCondition(), is("for(1)"));
        assertThat("タスク(id:usertask4567890123)の[Completion Condition]に完了条件の省略記法を指定した場合、対応する完全修飾名で展開されTaskに保持されること。",
                ((WorkflowDefinitionGeneratorTask) tasks.get(3)).getCondition(), is("nablarch.tool.workflow.AllCompletionCondition"));
        assertThat("タスク(id:usertask5678901234)の[Completion Condition]に完了条件の省略記法と完全一致しない文字列（部分一致）を指定した場合、入力値がTaskに保持されること。",
                ((WorkflowDefinitionGeneratorTask) tasks.get(4)).getCondition(), is("norn(1)"));
        assertThat("期待しないTaskを保持しないこと。", tasks.size(), is(5));
    }

    /**
     * ロードした結果、Laneにレーン情報が格納されていることを確認。
     *
     * @param lanes ロード結果のレーン定義
     */
    private void assertLanes(List<Lane> lanes) {
        assertThat("レーン(id:laneId1)の[Id]に指定したレーンIDがLaneに保持されていること。", lanes.get(0).getLaneId(), is("laneId1"));
        assertThat("レーン(id:laneId1)の[Name]に指定したレーン名がLaneに保持されていること。", lanes.get(0).getLaneName(), is("laneName1"));

        assertThat("レーン(id:laneId2)の[Id]に指定したレーンIDがLaneに保持されていること。", lanes.get(1).getLaneId(), is("laneId2"));
        assertThat("レーン(id:laneId2)の[Name]に指定したレーン名がLaneに保持されていること。", lanes.get(1).getLaneName(), is("laneName2"));

        assertThat("期待しないLaneを保持しないこと。", lanes.size(), is(2));
    }

    /**
     * ロードした結果、Taskにタスク情報が格納されていること。
     *
     * @param tasks ロード結果のタスク定義
     */
    private void assertTasks(List<Task> tasks) {
        assertThat("タスク(id:usertask1234567890)の[Id]に指定したフローノードIDがTaskに保持されていること。", tasks.get(0).getFlowNodeId(), is("usertask1234567890"));
        assertThat("タスク(id:usertask1234567890)の[Name]に指定したフローノード名がTaskに保持されていること。", tasks.get(0).getFlowNodeName(), is("User Task1"));
        assertThat("タスク(id:usertask1234567890)が属するレーンIDがTaskに保持されていること。", tasks.get(0).getLaneId(), is("laneId1"));
        assertThat("タスク(id:usertask1234567890)の[Is Multi Instance]がチェックOFFの場合、マルチインスタンス種別：非マルチインスタンスがTaskに保持されていること。",
                tasks.get(0).getMultiInstanceType(), is(Task.MultiInstanceType.NONE));
        assertThat("タスク(id:usertask1234567890)の[Completion Condition]に指定した完了条件がWorkflowDefinitionGeneratorTaskに保持されていること。",
                ((WorkflowDefinitionGeneratorTask) tasks.get(0)).getCondition(), nullValue());

        assertThat("タスク(id:usertask2345678901)の[Id]に指定したフローノードIDがTaskに保持されていること。", tasks.get(1).getFlowNodeId(), is("usertask2345678901"));
        assertThat("タスク(id:usertask2345678901)の[Name]に指定したフローノード名がTaskに保持されていること。", tasks.get(1).getFlowNodeName(), is("User Task2"));
        assertThat("タスク(id:usertask2345678901)が属するレーンIDがTaskに保持されていること。", tasks.get(1).getLaneId(), is("laneId2"));
        assertThat("タスク(id:usertask2345678901)の[Is Multi Instance]がチェックON、かつ[Is Sequential]がチェックOFFの場合、"
                + "マルチインスタンス種別：並行実行型マルチインスタンスがTaskに保持されていること。", tasks.get(1).getMultiInstanceType(), is(Task.MultiInstanceType.PARALLEL));
        assertThat("タスク(id:usertask2345678901)の[Completion Condition]に指定した完了条件がWorkflowDefinitionGeneratorTaskに保持されていること。",
                ((WorkflowDefinitionGeneratorTask) tasks.get(1)).getCondition(), is("nablarch.tool.workflow.SampleCompletionCondition"));

        assertThat("タスク(id:usertask3456789012)の[Id]に指定したフローノードIDがTaskに保持されていること。", tasks.get(2).getFlowNodeId(), is("usertask3456789012"));
        assertThat("タスク(id:usertask3456789012)の[Name]に指定したフローノード名がTaskに保持されていること。", tasks.get(2).getFlowNodeName(), is("User Task3"));
        assertThat("タスク(id:usertask3456789012)が属するレーンIDがTaskに保持されていること。", tasks.get(2).getLaneId(), is("laneId2"));
        assertThat("タスク(id:usertask3456789012)の[Is Multi Instance]がチェックON、かつ[Is Sequential]がチェックONの場合、"
                + "マルチインスタンス種別：順次実行型マルチインスタンスがTaskに保持されていること。", tasks.get(2).getMultiInstanceType(), is(Task.MultiInstanceType.SEQUENTIAL));
        assertThat("タスク(id:usertask3456789012)の[Completion Condition]に指定した完了条件がWorkflowDefinitionGeneratorTaskに保持されていること。",
                ((WorkflowDefinitionGeneratorTask) tasks.get(2)).getCondition(), is("nablarch.tool.workflow.SampleCompletionCondition(1)"));

        assertThat("期待しないTaskを保持しないこと。", tasks.size(), is(3));
    }

    /**
     * ロードした結果、Eventにイベント情報が格納されていること。
     *
     * @param events ロード結果のイベント定義
     */
    private void assertEvents(List<Event> events) {
        assertThat("イベント(id:startevent12345678)の[Id]に指定したフローノードIDがEventに保持されていること。", events.get(0).getFlowNodeId(), is("startevent12345678"));
        assertThat("イベント(id:startevent12345678)の[Name]に指定したフローノード名がEventに保持されていること。", events.get(0).getFlowNodeName(), is("Start"));
        assertThat("イベント(id:startevent12345678)が属するレーンIDがEventに保持されていること。", events.get(0).getLaneId(), is("laneId1"));
        assertThat("イベント(id:startevent12345678)の種別：開始イベントがEventに保持されていること。", events.get(0).getEventType(), is(Event.EventType.START));

        assertThat("イベント(id:terminateendevent1)の[Id]に指定したフローノードIDがEventに保持されていること。", events.get(1).getFlowNodeId(), is("terminateendevent1"));
        assertThat("イベント(id:terminateendevent1)の[Name]に指定したフローノード名がEventに保持されていること。", events.get(1).getFlowNodeName(), is("TerminateEndEvent"));
        assertThat("イベント(id:terminateendevent1)が属するレーンIDがEventに保持されていること。", events.get(1).getLaneId(), is("laneId1"));
        assertThat("イベント(id:terminateendevent1)の種別：停止イベントがEventに保持されていること。", events.get(1).getEventType(), is(Event.EventType.TERMINATE));

        assertThat("イベント(id:endevent1234567890)の[Id]に指定したフローノードIDがEventに保持されていること。", events.get(2).getFlowNodeId(), is("endevent1234567890"));
        assertThat("イベント(id:endevent1234567890)の[Name]に指定したフローノード名がEventに保持されていること。", events.get(2).getFlowNodeName(), is("TerminateEndEvent"));
        assertThat("イベント(id:endevent1234567890)が属するレーンIDがEventに保持されていること。", events.get(2).getLaneId(), is("laneId2"));
        assertThat("イベント(id:endevent1234567890)の種別：停止イベントがEventに保持されていること。", events.get(2).getEventType(), is(Event.EventType.TERMINATE));

        assertThat("期待しないEventを保持しないこと。", events.size(), is(3));
    }

    /**
     * ロードした結果、Gatewayにゲートウェイ情報が格納されていること。
     *
     * @param gateways ロード結果のゲートウェイ定義
     */
    private void assertGateways(List<Gateway> gateways) {
        assertThat("ゲートウェイ(id:exclusivegateway12)の[Id]に指定したフローノードIDがGatewayに保持されていること。", gateways.get(0).getFlowNodeId(), is("exclusivegateway12"));
        assertThat("ゲートウェイ(id:exclusivegateway12)の[Name]に指定したフローノード名がGatewayに保持されていること。", gateways.get(0).getFlowNodeName(), is("Exclusive Gateway1"));
        assertThat("ゲートウェイ(id:exclusivegateway12)が属するレーンIDがGatewayに保持されていること。", gateways.get(0).getLaneId(), is("laneId2"));
        assertThat("ゲートウェイ(id:exclusivegateway12)の種別：XORゲートウェイがGatewayに保持されていること。", gateways.get(0).getGatewayType(), is(Gateway.GatewayType.EXCLUSIVE));

        assertThat("ゲートウェイ(id:exclusivegateway23)の[Id]に指定したフローノードIDがGatewayに保持されていること。", gateways.get(1).getFlowNodeId(), is("exclusivegateway23"));
        assertThat("ゲートウェイ(id:exclusivegateway23)の[Name]に指定したフローノード名がGatewayに保持されていること。", gateways.get(1).getFlowNodeName(), is("Exclusive Gateway2"));
        assertThat("ゲートウェイ(id:exclusivegateway23)が属するレーンIDがGatewayに保持されていること。", gateways.get(1).getLaneId(), is("laneId2"));
        assertThat("ゲートウェイ(id:exclusivegateway23)の種別：XORゲートウェイがGatewayに保持されていること。", gateways.get(1).getGatewayType(), is(Gateway.GatewayType.EXCLUSIVE));

        assertThat("期待しないGatewayを保持しないこと。", gateways.size(), is(2));
    }

    /**
     * ロードした結果、BoundaryEventに境界イベント情報が格納されていること。
     *
     * @param boundaryEvents ロード結果の境界イベント定義
     */
    private void assertBoundaryEvents(List<BoundaryEvent> boundaryEvents) {
        assertThat("境界イベント(id:boundarymessage123)の[Id]に指定したフローノードIDがBoundaryEventに保持されていること。", boundaryEvents.get(0).getFlowNodeId(), is("boundarymessage123"));
        assertThat("境界イベント(id:boundarymessage123)の[Name]に指定したフローノード名がBoundaryEventに保持されていること。", boundaryEvents.get(0).getFlowNodeName(), is("Message"));
        assertThat("境界イベント(id:boundarymessage123)が属するレーン(id:laneId2)のIDがBoundaryEventに保持されていること。", boundaryEvents.get(0).getLaneId(), is("laneId2"));
        assertThat("境界イベント(id:boundarymessage123)の[Message]に指定した境界イベントトリガーIDがBoundaryEventに保持されていること。",
                boundaryEvents.get(0).getBoundaryEventTriggerId(), is("MG001"));
        assertThat("境界イベント(id:boundarymessage123)の[Message]に指定した境界イベントトリガー名がBoundaryEventに保持されていること。",
                boundaryEvents.get(0).getBoundaryEventTriggerName(), is("MG001"));
        assertThat("境界イベント(id:boundarymessage123)の接続先タスクIDがBoundaryEventに保持されていること。", boundaryEvents.get(0).getAttachedTaskId(), is("usertask2345678901"));

        assertThat("期待しないBoundaryEventを保持しないこと。", boundaryEvents.size(), is(1));
    }

    /**
     * unmarshal時、xml不正を検出した場合、例外を送出すること。
     *
     * @throws Exception 想定外エラー
     */
    @Test(expected = WorkflowDefinitionException.class)
    public void unmarshalErrorAnomalyDetection() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/read/invalid/unmarshalAnomalyDetection/WP0004_duplicateId_ver1_20140804.bpmn");
        sut.load(new WorkflowDefinitionFile(inputFile.getName(), inputFile.getPath(), "WP0004", "duplicateId", "1", "20140804"));
    }

    /**
     * unmarshal時、xml不正で例外が発生した場合、例外を上位に送出すること。
     *
     * @throws Exception 想定外エラー
     */
    @Test(expected = RuntimeException.class)
    public void unmarshalError() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/read/invalid/unmarshalError/WP0005_unmarshalError_ver1_20140804.bpmn");
        sut.load(new WorkflowDefinitionFile(inputFile.getName(), inputFile.getPath(), "WP0005", "unmarshalError", "1", "20140804"));
    }

    /**
     * 精査エラーが発生した場合、例外を送出すること。
     *
     * @throws Exception 想定外エラー
     */
    @Test(expected = WorkflowDefinitionException.class)
    public void validationError() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/read/invalid/validateError/WP0006_validateError_ver1_20140804.bpmn");
        sut.load(new WorkflowDefinitionFile(inputFile.getName(), inputFile.getPath(), "WP0006", "validateError", "1", "20140804"));
    }
}
