package nablarch.tool.statemachine;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.integration.workflow.definition.Event.EventType;
import nablarch.integration.workflow.definition.WorkflowDefinition;
import nablarch.tool.DefinitionCreator;
import nablarch.tool.workflow.WorkflowDefinitionFile;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * {@link StateMachineDefinitionCreator}のテストクラス
 */
public class StateMachineDefinitionCreatorTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    /** テスト対象 */
    private DefinitionCreator sut = new StateMachineDefinitionCreator();

    @Before
    public void setUp() throws Exception {
        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                final Map<String, String> flowProceedCondition = new HashMap<String, String>();
                flowProceedCondition.put("eq", "nablarch.tool.workflow.EqFlowProceedCondition");

                final HashMap<String, Object> map = new HashMap<String, Object>();
                map.put("flowProceedCondition", flowProceedCondition);
                return map;
            }
        });
    }

    @Test
    public void 必須要素のみのシンプルなステートマシン定義を生成できること() throws Exception {

        final WorkflowDefinitionFile workflowDefinitionFile =
                createWorkflowDefinitionFile("src/test/testbpmn/statemachine/creator/WP0001_simple_ver1_20170101.bpmn");

        final WorkflowDefinition workflowDefinition = sut.create(workflowDefinitionFile);

        // レーン
        assertThat(workflowDefinition.getLanes(), contains(
                allOf(
                        hasProperty("laneId", is("L1")),
                        hasProperty("laneName", is("レーン"))
                )
        ));

        // イベント
        assertThat(workflowDefinition.getEvents(), containsInAnyOrder(
                allOf(
                        hasProperty("flowNodeId", is("start")),
                        hasProperty("flowNodeName", is("開始イベント")),
                        hasProperty("eventType", is(EventType.START)),
                        hasProperty("laneId", is("L1"))
                ),
                allOf(
                        hasProperty("flowNodeId", is("end")),
                        hasProperty("flowNodeName", is("停止イベント")),
                        hasProperty("eventType", is(EventType.TERMINATE)),
                        hasProperty("laneId", is("L1"))
                )
        ));

        // 境界イベント
        assertThat(workflowDefinition.getBoundaryEvents(), contains(
                allOf(
                        hasProperty("flowNodeId", is("boundary")),
                        hasProperty("flowNodeName", is("境界イベント")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("boundaryEventTriggerId", is("M01")),
                        hasProperty("boundaryEventTriggerName", is("メッセージ")),
                        hasProperty("attachedTaskId", is("task"))
                )
        ));

        // シーケンスフロー
        assertThat(workflowDefinition.getSequenceFlows(), containsInAnyOrder(
                allOf(
                        hasProperty("sequenceFlowId", is("start_seq")),
                        hasProperty("sequenceFlowName", is("開始イベントのシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("start")),
                        hasProperty("targetFlowNodeId", is("task"))
                ),
                allOf(
                        hasProperty("sequenceFlowId", is("boundary_seq")),
                        hasProperty("sequenceFlowName", is("境界イベントのシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("boundary")),
                        hasProperty("targetFlowNodeId", is("end"))
                )
        ));

        // タスク
        assertThat(workflowDefinition.getTasks(), contains(
                allOf(
                        hasProperty("flowNodeId", is("task")),
                        hasProperty("flowNodeName", is("タスク")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("multiInstanceType", is(false))
                )
        ));
    }

    @Test
    public void ゲートウェイを使用したステートマシン定義を生成できること() throws Exception {
        final WorkflowDefinitionFile workflowDefinitionFile =
                createWorkflowDefinitionFile("src/test/testbpmn/statemachine/creator/WP0002_gateway_ver1_20170101.bpmn");

        final WorkflowDefinition workflowDefinition = sut.create(workflowDefinitionFile);

        // レーン
        assertThat(workflowDefinition.getLanes(), contains(
                allOf(
                        hasProperty("laneId", is("L1")),
                        hasProperty("laneName", is("レーン"))
                )
        ));

        // イベント
        assertThat(workflowDefinition.getEvents(), containsInAnyOrder(
                allOf(
                        hasProperty("flowNodeId", is("start")),
                        hasProperty("flowNodeName", is("開始イベント")),
                        hasProperty("eventType", is(EventType.START)),
                        hasProperty("laneId", is("L1"))
                ),
                allOf(
                        hasProperty("flowNodeId", is("end")),
                        hasProperty("flowNodeName", is("停止イベント")),
                        hasProperty("eventType", is(EventType.TERMINATE)),
                        hasProperty("laneId", is("L1"))
                )
        ));

        // 境界イベント
        assertThat(workflowDefinition.getBoundaryEvents(), contains(
                allOf(
                        hasProperty("flowNodeId", is("boundary")),
                        hasProperty("flowNodeName", is("境界イベント")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("boundaryEventTriggerId", is("M01")),
                        hasProperty("boundaryEventTriggerName", is("メッセージ")),
                        hasProperty("attachedTaskId", is("task"))
                )
        ));

        // ゲートウェイ
        assertThat(workflowDefinition.getGateways(), contains(
                allOf(
                        hasProperty("flowNodeId", is("gateway")),
                        hasProperty("flowNodeName", is("ゲートウェイ")),
                        hasProperty("laneId", is("L1"))
                )
        ));

        // シーケンスフロー
        assertThat(workflowDefinition.getSequenceFlows(), containsInAnyOrder(
                allOf(
                        hasProperty("sequenceFlowId", is("start_seq")),
                        hasProperty("sequenceFlowName", is("開始イベントのシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("start")),
                        hasProperty("targetFlowNodeId", is("task"))
                ),
                allOf(
                        hasProperty("sequenceFlowId", is("boundary_seq")),
                        hasProperty("sequenceFlowName", is("境界イベントのシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("boundary")),
                        hasProperty("targetFlowNodeId", is("gateway"))
                ),
                allOf(
                        hasProperty("sequenceFlowId", is("gateway_end_seq")),
                        hasProperty("sequenceFlowName", is("ゲートウェイから停止イベントへのシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("gateway")),
                        hasProperty("targetFlowNodeId", is("end")),
                        hasProperty("condition", is("nablarch.tool.workflow.EqFlowProceedCondition(condition, 1)"))

                ),
                allOf(
                        hasProperty("sequenceFlowId", is("gateway_task_seq")),
                        hasProperty("sequenceFlowName", is("ゲートウェイからタスクへのシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("gateway")),
                        hasProperty("targetFlowNodeId", is("task")),
                        hasProperty("condition", is("nablarch.tool.workflow.EqFlowProceedCondition(condition, 0)"))
                )
        ));

        // タスク
        assertThat(workflowDefinition.getTasks(), contains(
                allOf(
                        hasProperty("flowNodeId", is("task")),
                        hasProperty("flowNodeName", is("タスク")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("multiInstanceType", is(false))
                )
        ));

    }

    @Test
    public void サブプロセスを使用したステートマシン定義を生成できること() throws Exception {

        final WorkflowDefinitionFile workflowDefinitionFile =
                createWorkflowDefinitionFile("src/test/testbpmn/statemachine/creator/WP0003_subprocess_ver1_20170101.bpmn");

        final WorkflowDefinition workflowDefinition = sut.create(workflowDefinitionFile);

        // レーン
        assertThat(workflowDefinition.getLanes(), contains(
                allOf(
                        hasProperty("laneId", is("L1")),
                        hasProperty("laneName", is("レーン"))
                )
        ));

        // イベント
        assertThat(workflowDefinition.getEvents(), containsInAnyOrder(
                allOf(
                        hasProperty("flowNodeId", is("start")),
                        hasProperty("flowNodeName", is("開始イベント")),
                        hasProperty("eventType", is(EventType.START)),
                        hasProperty("laneId", is("L1"))
                ),
                allOf(
                        hasProperty("flowNodeId", is("end")),
                        hasProperty("flowNodeName", is("停止イベント")),
                        hasProperty("eventType", is(EventType.TERMINATE)),
                        hasProperty("laneId", is("L1"))
                )
        ));

        // 境界イベント
        assertThat(workflowDefinition.getBoundaryEvents(), containsInAnyOrder(
                allOf(
                        hasProperty("flowNodeId", is("boundary")),
                        hasProperty("flowNodeName", is("境界イベント")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("boundaryEventTriggerId", is("M01")),
                        hasProperty("boundaryEventTriggerName", is("メッセージ")),
                        hasProperty("attachedTaskId", is("task"))
                ),
                allOf(
                        hasProperty("flowNodeId", is("sub_boundary")),
                        hasProperty("flowNodeName", is("サブプロセスの境界イベント")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("boundaryEventTriggerId", is("M02")),
                        hasProperty("boundaryEventTriggerName", is("サブプロセスのメッセージ")),
                        hasProperty("attachedTaskId", is("sub_task"))
                )
        ));

        // シーケンスフロー
        assertThat(workflowDefinition.getSequenceFlows(), containsInAnyOrder(
                allOf(
                        hasProperty("sequenceFlowId", is("start_seq")),
                        hasProperty("sequenceFlowName", is("開始イベントのシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("start")),
                        hasProperty("targetFlowNodeId", is("task"))
                ),
                allOf(
                        hasProperty("sequenceFlowId", is("boundary_seq")),
                        hasProperty("sequenceFlowName", is("境界イベントのシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("boundary")),
                        hasProperty("targetFlowNodeId", is("sub_task"))
                ),
                allOf(
                        hasProperty("sequenceFlowId", is("sub_boundary_seq")),
                        hasProperty("sequenceFlowName", is("サブプロセスの境界イベントのシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("sub_boundary")),
                        hasProperty("targetFlowNodeId", is("end"))
                )
        ));

        // タスク
        assertThat(workflowDefinition.getTasks(), containsInAnyOrder(
                allOf(
                        hasProperty("flowNodeId", is("task")),
                        hasProperty("flowNodeName", is("タスク")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("multiInstanceType", is(false))
                ),
                allOf(
                        hasProperty("flowNodeId", is("sub_task")),
                        hasProperty("flowNodeName", is("サブプロセスのタスク")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("multiInstanceType", is(false))
                )
        ));
    }

    @Test
    public void 複数の停止イベントがあるサブプロセスを使用したステートマシン定義を生成できること() throws Exception {

        final WorkflowDefinitionFile workflowDefinitionFile =
                createWorkflowDefinitionFile("src/test/testbpmn/statemachine/creator/WP0004_subprocess-end_ver1_20170101.bpmn");

        final WorkflowDefinition workflowDefinition = sut.create(workflowDefinitionFile);

        // レーン
        assertThat(workflowDefinition.getLanes(), contains(
                allOf(
                        hasProperty("laneId", is("L1")),
                        hasProperty("laneName", is("レーン"))
                )
        ));

        // イベント
        assertThat(workflowDefinition.getEvents(), containsInAnyOrder(
                allOf(
                        hasProperty("flowNodeId", is("start")),
                        hasProperty("flowNodeName", is("開始イベント")),
                        hasProperty("eventType", is(EventType.START)),
                        hasProperty("laneId", is("L1"))
                ),
                allOf(
                        hasProperty("flowNodeId", is("end")),
                        hasProperty("flowNodeName", is("停止イベント")),
                        hasProperty("eventType", is(EventType.TERMINATE)),
                        hasProperty("laneId", is("L1"))
                )
        ));

        // 境界イベント
        assertThat(workflowDefinition.getBoundaryEvents(), containsInAnyOrder(
                allOf(
                        hasProperty("flowNodeId", is("boundary")),
                        hasProperty("flowNodeName", is("境界イベント")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("boundaryEventTriggerId", is("M01")),
                        hasProperty("boundaryEventTriggerName", is("メッセージ")),
                        hasProperty("attachedTaskId", is("task"))
                ),
                allOf(
                        hasProperty("flowNodeId", is("sub_boundary")),
                        hasProperty("flowNodeName", is("サブプロセスの境界イベント")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("boundaryEventTriggerId", is("M02")),
                        hasProperty("boundaryEventTriggerName", is("サブプロセスのメッセージ")),
                        hasProperty("attachedTaskId", is("sub_task"))
                )
        ));

        // ゲートウェイ
        assertThat(workflowDefinition.getGateways(), contains(
                allOf(
                        hasProperty("flowNodeId", is("sub_gateway")),
                        hasProperty("flowNodeName", is("サブプロセスのゲートウェイ")),
                        hasProperty("laneId", is("L1"))
                )
        ));

        // シーケンスフロー
        assertThat(workflowDefinition.getSequenceFlows(), containsInAnyOrder(
                allOf(
                        hasProperty("sequenceFlowId", is("start_seq")),
                        hasProperty("sequenceFlowName", is("開始イベントのシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("start")),
                        hasProperty("targetFlowNodeId", is("task"))
                ),
                allOf(
                        hasProperty("sequenceFlowId", is("boundary_seq")),
                        hasProperty("sequenceFlowName", is("境界イベントのシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("boundary")),
                        hasProperty("targetFlowNodeId", is("sub_task"))
                ),
                allOf(
                        hasProperty("sequenceFlowId", is("sub_boundary_seq")),
                        hasProperty("sequenceFlowName", is("サブプロセスの境界イベントのシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("sub_boundary")),
                        hasProperty("targetFlowNodeId", is("sub_gateway"))
                ),
                allOf(
                        hasProperty("sequenceFlowId", is("sub_gateway_seq1")),
                        hasProperty("sequenceFlowName", is("サブプロセスのゲートウェイのシーケンスフロー１")),
                        hasProperty("sourceFlowNodeId", is("sub_gateway")),
                        hasProperty("targetFlowNodeId", is("end"))
                ),
                allOf(
                        hasProperty("sequenceFlowId", is("sub_gateway_seq2")),
                        hasProperty("sequenceFlowName", is("サブプロセスのゲートウェイのシーケンスフロー２")),
                        hasProperty("sourceFlowNodeId", is("sub_gateway")),
                        hasProperty("targetFlowNodeId", is("end"))
                )
        ));

        // タスク
        assertThat(workflowDefinition.getTasks(), containsInAnyOrder(
                allOf(
                        hasProperty("flowNodeId", is("task")),
                        hasProperty("flowNodeName", is("タスク")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("multiInstanceType", is(false))
                ),
                allOf(
                        hasProperty("flowNodeId", is("sub_task")),
                        hasProperty("flowNodeName", is("サブプロセスのタスク")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("multiInstanceType", is(false))
                )
        ));
    }

    @Test
    public void 複数のサブプロセスを使用したステートマシン定義を生成できること() throws Exception {

        final WorkflowDefinitionFile workflowDefinitionFile =
                createWorkflowDefinitionFile("src/test/testbpmn/statemachine/creator/WP0005_subprocess-multi_ver1_20170101.bpmn");

        final WorkflowDefinition workflowDefinition = sut.create(workflowDefinitionFile);

        // レーン
        assertThat(workflowDefinition.getLanes(), contains(
                allOf(
                        hasProperty("laneId", is("L1")),
                        hasProperty("laneName", is("レーン"))
                )
        ));

        // イベント
        assertThat(workflowDefinition.getEvents(), containsInAnyOrder(
                allOf(
                        hasProperty("flowNodeId", is("start")),
                        hasProperty("flowNodeName", is("開始イベント")),
                        hasProperty("eventType", is(EventType.START)),
                        hasProperty("laneId", is("L1"))
                ),
                allOf(
                        hasProperty("flowNodeId", is("end1")),
                        hasProperty("flowNodeName", is("停止イベント１")),
                        hasProperty("eventType", is(EventType.TERMINATE)),
                        hasProperty("laneId", is("L1"))
                ),
                allOf(
                        hasProperty("flowNodeId", is("end2")),
                        hasProperty("flowNodeName", is("停止イベント２")),
                        hasProperty("eventType", is(EventType.TERMINATE)),
                        hasProperty("laneId", is("L1"))
                )
        ));

        // 境界イベント
        assertThat(workflowDefinition.getBoundaryEvents(), containsInAnyOrder(
                allOf(
                        hasProperty("flowNodeId", is("boundary1")),
                        hasProperty("flowNodeName", is("境界イベント１")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("boundaryEventTriggerId", is("M01")),
                        hasProperty("boundaryEventTriggerName", is("メッセージ１")),
                        hasProperty("attachedTaskId", is("task"))
                ),
                allOf(
                        hasProperty("flowNodeId", is("boundary2")),
                        hasProperty("flowNodeName", is("境界イベント２")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("boundaryEventTriggerId", is("M02")),
                        hasProperty("boundaryEventTriggerName", is("メッセージ２")),
                        hasProperty("attachedTaskId", is("task"))
                ),
                allOf(
                        hasProperty("flowNodeId", is("sub1_boundary")),
                        hasProperty("flowNodeName", is("サブプロセス１の境界イベント")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("boundaryEventTriggerId", is("M03")),
                        hasProperty("boundaryEventTriggerName", is("サブプロセス１のメッセージ")),
                        hasProperty("attachedTaskId", is("sub1_task"))
                ),
                allOf(
                        hasProperty("flowNodeId", is("sub2_boundary")),
                        hasProperty("flowNodeName", is("サブプロセス２の境界イベント")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("boundaryEventTriggerId", is("M04")),
                        hasProperty("boundaryEventTriggerName", is("サブプロセス２のメッセージ")),
                        hasProperty("attachedTaskId", is("sub2_task"))
                )
        ));

        // シーケンスフロー
        assertThat(workflowDefinition.getSequenceFlows(), containsInAnyOrder(
                allOf(
                        hasProperty("sequenceFlowId", is("start_seq")),
                        hasProperty("sequenceFlowName", is("開始イベントのシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("start")),
                        hasProperty("targetFlowNodeId", is("task"))
                ),
                allOf(
                        hasProperty("sequenceFlowId", is("boundary1_seq")),
                        hasProperty("sequenceFlowName", is("境界イベント１のシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("boundary1")),
                        hasProperty("targetFlowNodeId", is("sub1_task"))
                ),
                allOf(
                        hasProperty("sequenceFlowId", is("sub1_boundary_seq")),
                        hasProperty("sequenceFlowName", is("サブプロセス１の境界イベントのシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("sub1_boundary")),
                        hasProperty("targetFlowNodeId", is("end1"))
                ),
                allOf(
                        hasProperty("sequenceFlowId", is("boundary2_seq")),
                        hasProperty("sequenceFlowName", is("境界イベント２のシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("boundary2")),
                        hasProperty("targetFlowNodeId", is("sub2_task"))
                ),
                allOf(
                        hasProperty("sequenceFlowId", is("sub2_boundary_seq")),
                        hasProperty("sequenceFlowName", is("サブプロセス２の境界イベントのシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("sub2_boundary")),
                        hasProperty("targetFlowNodeId", is("end2"))
                )
        ));

        // タスク
        assertThat(workflowDefinition.getTasks(), containsInAnyOrder(
                allOf(
                        hasProperty("flowNodeId", is("task")),
                        hasProperty("flowNodeName", is("タスク")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("multiInstanceType", is(false))
                ),
                allOf(
                        hasProperty("flowNodeId", is("sub1_task")),
                        hasProperty("flowNodeName", is("サブプロセス１のタスク")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("multiInstanceType", is(false))
                ),
                allOf(
                        hasProperty("flowNodeId", is("sub2_task")),
                        hasProperty("flowNodeName", is("サブプロセス２のタスク")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("multiInstanceType", is(false))
                )
        ));
    }

    @Test
    public void ネストしたサブプロセスを使用したステートマシン定義を生成できること() throws Exception {

        final WorkflowDefinitionFile workflowDefinitionFile =
                createWorkflowDefinitionFile("src/test/testbpmn/statemachine/creator/WP0006_subprocess-nest_ver1_20170101.bpmn");

        final WorkflowDefinition workflowDefinition = sut.create(workflowDefinitionFile);

        // レーン
        assertThat(workflowDefinition.getLanes(), contains(
                allOf(
                        hasProperty("laneId", is("L1")),
                        hasProperty("laneName", is("レーン"))
                )
        ));

        // イベント
        assertThat(workflowDefinition.getEvents(), containsInAnyOrder(
                allOf(
                        hasProperty("flowNodeId", is("start")),
                        hasProperty("flowNodeName", is("開始イベント")),
                        hasProperty("eventType", is(EventType.START)),
                        hasProperty("laneId", is("L1"))
                ),
                allOf(
                        hasProperty("flowNodeId", is("end")),
                        hasProperty("flowNodeName", is("停止イベント")),
                        hasProperty("eventType", is(EventType.TERMINATE)),
                        hasProperty("laneId", is("L1"))
                )
        ));

        // 境界イベント
        assertThat(workflowDefinition.getBoundaryEvents(), containsInAnyOrder(
                allOf(
                        hasProperty("flowNodeId", is("boundary")),
                        hasProperty("flowNodeName", is("境界イベント")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("boundaryEventTriggerId", is("M01")),
                        hasProperty("boundaryEventTriggerName", is("メッセージ")),
                        hasProperty("attachedTaskId", is("task"))
                ),
                allOf(
                        hasProperty("flowNodeId", is("sub_boundary")),
                        hasProperty("flowNodeName", is("サブプロセスの境界イベント")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("boundaryEventTriggerId", is("M02")),
                        hasProperty("boundaryEventTriggerName", is("サブプロセスのメッセージ")),
                        hasProperty("attachedTaskId", is("sub_task"))
                ),
                allOf(
                        hasProperty("flowNodeId", is("nest_sub_boundary")),
                        hasProperty("flowNodeName", is("ネストしたサブプロセスの境界イベント")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("boundaryEventTriggerId", is("M03")),
                        hasProperty("boundaryEventTriggerName", is("ネストしたサブプロセスのメッセージ")),
                        hasProperty("attachedTaskId", is("nest_sub_task"))
                )

        ));

        // シーケンスフロー
        assertThat(workflowDefinition.getSequenceFlows(), containsInAnyOrder(
                allOf(
                        hasProperty("sequenceFlowId", is("start_seq")),
                        hasProperty("sequenceFlowName", is("開始イベントのシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("start")),
                        hasProperty("targetFlowNodeId", is("task"))
                ),
                allOf(
                        hasProperty("sequenceFlowId", is("boundary_seq")),
                        hasProperty("sequenceFlowName", is("境界イベントのシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("boundary")),
                        hasProperty("targetFlowNodeId", is("sub_task"))
                ),
                allOf(
                        hasProperty("sequenceFlowId", is("sub_boundary_seq")),
                        hasProperty("sequenceFlowName", is("サブプロセスの境界イベントのシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("sub_boundary")),
                        hasProperty("targetFlowNodeId", is("nest_sub_task"))
                ),
                allOf(
                        hasProperty("sequenceFlowId", is("nest_sub_bound_seq")),
                        hasProperty("sequenceFlowName", is("ネストしたサブプロセスの境界イベントのシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("nest_sub_boundary")),
                        hasProperty("targetFlowNodeId", is("end"))
                )
        ));

        // タスク
        assertThat(workflowDefinition.getTasks(), containsInAnyOrder(
                allOf(
                        hasProperty("flowNodeId", is("task")),
                        hasProperty("flowNodeName", is("タスク")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("multiInstanceType", is(false))
                ),
                allOf(
                        hasProperty("flowNodeId", is("sub_task")),
                        hasProperty("flowNodeName", is("サブプロセスのタスク")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("multiInstanceType", is(false))
                ),
                allOf(
                        hasProperty("flowNodeId", is("nest_sub_task")),
                        hasProperty("flowNodeName", is("ネストしたサブプロセスのタスク")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("multiInstanceType", is(false))
                )
        ));
    }

    @Test
    public void レーンが複数存在するステートマシン定義を生成できること() throws Exception {

        final WorkflowDefinitionFile workflowDefinitionFile =
                createWorkflowDefinitionFile("src/test/testbpmn/statemachine/creator/WP0007_lane_ver1_20170101.bpmn");

        final WorkflowDefinition workflowDefinition = sut.create(workflowDefinitionFile);

        // レーン
        assertThat(workflowDefinition.getLanes(), contains(
                allOf(
                        hasProperty("laneId", is("L1")),
                        hasProperty("laneName", is("レーン１"))
                ),
                allOf(
                        hasProperty("laneId", is("L2")),
                        hasProperty("laneName", is("レーン２"))
                )
        ));

        // イベント
        assertThat(workflowDefinition.getEvents(), containsInAnyOrder(
                allOf(
                        hasProperty("flowNodeId", is("start")),
                        hasProperty("flowNodeName", is("開始イベント")),
                        hasProperty("eventType", is(EventType.START)),
                        hasProperty("laneId", is("L1"))
                ),
                allOf(
                        hasProperty("flowNodeId", is("end1")),
                        hasProperty("flowNodeName", is("停止イベント１")),
                        hasProperty("eventType", is(EventType.TERMINATE)),
                        hasProperty("laneId", is("L1"))
                ),
                allOf(
                        hasProperty("flowNodeId", is("end2")),
                        hasProperty("flowNodeName", is("停止イベント２")),
                        hasProperty("eventType", is(EventType.TERMINATE)),
                        hasProperty("laneId", is("L2"))
                )
        ));

        // 境界イベント
        assertThat(workflowDefinition.getBoundaryEvents(), contains(
                allOf(
                        hasProperty("flowNodeId", is("boundary1")),
                        hasProperty("flowNodeName", is("境界イベント１")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("boundaryEventTriggerId", is("M01")),
                        hasProperty("boundaryEventTriggerName", is("メッセージ１")),
                        hasProperty("attachedTaskId", is("task1"))
                ),
                allOf(
                        hasProperty("flowNodeId", is("boundary2")),
                        hasProperty("flowNodeName", is("境界イベント２")),
                        hasProperty("laneId", is("L2")),
                        hasProperty("boundaryEventTriggerId", is("M02")),
                        hasProperty("boundaryEventTriggerName", is("メッセージ２")),
                        hasProperty("attachedTaskId", is("task2"))
                )
        ));

        // ゲートウェイ
        assertThat(workflowDefinition.getGateways(), contains(
                allOf(
                        hasProperty("flowNodeId", is("gateway")),
                        hasProperty("flowNodeName", is("ゲートウェイ")),
                        hasProperty("laneId", is("L1"))
                )
        ));

        // シーケンスフロー
        assertThat(workflowDefinition.getSequenceFlows(), containsInAnyOrder(
                allOf(
                        hasProperty("sequenceFlowId", is("start_seq")),
                        hasProperty("sequenceFlowName", is("開始イベントのシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("start")),
                        hasProperty("targetFlowNodeId", is("task1"))
                ),
                allOf(
                        hasProperty("sequenceFlowId", is("boundary1_seq")),
                        hasProperty("sequenceFlowName", is("境界イベント１のシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("boundary1")),
                        hasProperty("targetFlowNodeId", is("gateway"))
                ),
                allOf(
                        hasProperty("sequenceFlowId", is("gateway_end1_seq")),
                        hasProperty("sequenceFlowName", is("ゲートウェイから停止イベント１へのシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("gateway")),
                        hasProperty("targetFlowNodeId", is("end1")),
                        hasProperty("condition", is("nablarch.tool.workflow.EqFlowProceedCondition(condition, 1)"))

                ),
                allOf(
                        hasProperty("sequenceFlowId", is("gateway_task2_seq")),
                        hasProperty("sequenceFlowName", is("ゲートウェイからタスク２へのシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("gateway")),
                        hasProperty("targetFlowNodeId", is("task2")),
                        hasProperty("condition", is("nablarch.tool.workflow.EqFlowProceedCondition(condition, 2)"))
                ),
                allOf(
                        hasProperty("sequenceFlowId", is("boundary2_seq")),
                        hasProperty("sequenceFlowName", is("境界イベント２のシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("boundary2")),
                        hasProperty("targetFlowNodeId", is("end2"))
                )
        ));

        // タスク
        assertThat(workflowDefinition.getTasks(), contains(
                allOf(
                        hasProperty("flowNodeId", is("task1")),
                        hasProperty("flowNodeName", is("タスク１")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("multiInstanceType", is(false))
                ),
                allOf(
                        hasProperty("flowNodeId", is("task2")),
                        hasProperty("flowNodeName", is("タスク２")),
                        hasProperty("laneId", is("L2")),
                        hasProperty("multiInstanceType", is(false))
                )
        ));
    }

    @Test
    public void flowProceedConditionがコンポーネント定義されていない場合はシーケンスフローのconditionが変換されないこと() throws Exception {

        SystemRepository.clear();

        final WorkflowDefinitionFile workflowDefinitionFile =
                createWorkflowDefinitionFile("src/test/testbpmn/statemachine/creator/WP0008_condition_ver1_20170101.bpmn");

        final WorkflowDefinition workflowDefinition = sut.create(workflowDefinitionFile);

        // レーン
        assertThat(workflowDefinition.getLanes(), contains(
                allOf(
                        hasProperty("laneId", is("L1")),
                        hasProperty("laneName", is("レーン"))
                )
        ));

        // イベント
        assertThat(workflowDefinition.getEvents(), containsInAnyOrder(
                allOf(
                        hasProperty("flowNodeId", is("start")),
                        hasProperty("flowNodeName", is("開始イベント")),
                        hasProperty("eventType", is(EventType.START)),
                        hasProperty("laneId", is("L1"))
                ),
                allOf(
                        hasProperty("flowNodeId", is("end")),
                        hasProperty("flowNodeName", is("停止イベント")),
                        hasProperty("eventType", is(EventType.TERMINATE)),
                        hasProperty("laneId", is("L1"))
                )
        ));

        // 境界イベント
        assertThat(workflowDefinition.getBoundaryEvents(), contains(
                allOf(
                        hasProperty("flowNodeId", is("boundary")),
                        hasProperty("flowNodeName", is("境界イベント")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("boundaryEventTriggerId", is("M01")),
                        hasProperty("boundaryEventTriggerName", is("メッセージ")),
                        hasProperty("attachedTaskId", is("task"))
                )
        ));

        // ゲートウェイ
        assertThat(workflowDefinition.getGateways(), contains(
                allOf(
                        hasProperty("flowNodeId", is("gateway")),
                        hasProperty("flowNodeName", is("ゲートウェイ")),
                        hasProperty("laneId", is("L1"))
                )
        ));

        // シーケンスフロー
        assertThat(workflowDefinition.getSequenceFlows(), containsInAnyOrder(
                allOf(
                        hasProperty("sequenceFlowId", is("start_seq")),
                        hasProperty("sequenceFlowName", is("開始イベントのシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("start")),
                        hasProperty("targetFlowNodeId", is("task"))
                ),
                allOf(
                        hasProperty("sequenceFlowId", is("boundary_seq")),
                        hasProperty("sequenceFlowName", is("境界イベントのシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("boundary")),
                        hasProperty("targetFlowNodeId", is("gateway"))
                ),
                allOf(
                        hasProperty("sequenceFlowId", is("gateway_end_seq")),
                        hasProperty("sequenceFlowName", is("ゲートウェイから停止イベントへのシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("gateway")),
                        hasProperty("targetFlowNodeId", is("end")),
                        hasProperty("condition", is("eq(condition, 1)"))

                ),
                allOf(
                        hasProperty("sequenceFlowId", is("gateway_task_seq")),
                        hasProperty("sequenceFlowName", is("ゲートウェイからタスクへのシーケンスフロー")),
                        hasProperty("sourceFlowNodeId", is("gateway")),
                        hasProperty("targetFlowNodeId", is("task")),
                        hasProperty("condition", is("eq(condition, 0)"))
                )
        ));

        // タスク
        assertThat(workflowDefinition.getTasks(), contains(
                allOf(
                        hasProperty("flowNodeId", is("task")),
                        hasProperty("flowNodeName", is("タスク")),
                        hasProperty("laneId", is("L1")),
                        hasProperty("multiInstanceType", is(false))
                )
        ));

    }

    @Test
    public void プロセスが存在しない場合に例外が送出されること() throws Exception {

        final WorkflowDefinitionFile workflowDefinitionFile =
                createWorkflowDefinitionFile("src/test/testbpmn/statemachine/creator/WP0009_process-nothing_ver1_20170101.bpmn");

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("ステートマシンにプロセスが定義されていません。");
        sut.create(workflowDefinitionFile);
    }

    /**
     * {@link WorkflowDefinitionFile}のインスタンスを生成する
     * @param path ファイルパス
     * @return {@link WorkflowDefinitionFile}のインスタンス
     */
    private WorkflowDefinitionFile createWorkflowDefinitionFile(String path) {
        final File file = new File(path);
        return new WorkflowDefinitionFile(
                file.getName(),
                file.getPath(),
                "WP0001",
                "test",
                "1",
                "20170101");
    }
}