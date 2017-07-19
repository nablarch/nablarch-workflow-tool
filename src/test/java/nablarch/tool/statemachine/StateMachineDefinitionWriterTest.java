package nablarch.tool.statemachine;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;

import nablarch.integration.workflow.definition.BoundaryEvent;
import nablarch.integration.workflow.definition.Event;
import nablarch.integration.workflow.definition.Event.EventType;
import nablarch.integration.workflow.definition.Gateway;
import nablarch.integration.workflow.definition.Gateway.GatewayType;
import nablarch.integration.workflow.definition.Lane;
import nablarch.integration.workflow.definition.SequenceFlow;
import nablarch.integration.workflow.definition.Task;
import nablarch.integration.workflow.definition.WorkflowDefinition;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.tool.workflow.WorkflowDefinitionGeneratorSequenceFlow;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * {@link StateMachineDefinitionWriter}のテストクラス。
 */
public class StateMachineDefinitionWriterTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("nablarch/tool/workflow/default-definition.xml");

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /** テスト対象 */
    private final StateMachineDefinitionWriter sut = new StateMachineDefinitionWriter();

    @Test
    public void ステートマシン定義をもとにCSVファイルを出力できること() throws Exception {

        final WorkflowDefinition workflowDefinition = new WorkflowDefinition("WP0001", 1, "test", "20170101");
        workflowDefinition.setLanes(new ArrayList<Lane>() {{
            add(new Lane("01", "レーン１"));
        }});
        workflowDefinition.setEvents(new ArrayList<Event>() {{
            add(new Event("start", "開始イベント", "01", EventType.START.toString(), null));
            add(new Event("end", "停止イベント", "01", EventType.TERMINATE.toString(), null));
        }});
        workflowDefinition.setGateways(new ArrayList<Gateway>() {{
            add(new Gateway("gateway", "ゲートウェイ", "01", GatewayType.EXCLUSIVE.toString(), null));
        }});
        workflowDefinition.setTasks(new ArrayList<Task>() {{
            add(new Task("task", "タスク", "01", Task.MultiInstanceType.NONE.toString(), null, null));
        }});
        workflowDefinition.setBoundaryEvents(new ArrayList<BoundaryEvent>() {{
            add(new BoundaryEvent("boundary", "境界イベント", "01",
                    "M01", "メッセージ", "task", null));
        }});
        workflowDefinition.setSequenceFlows(new ArrayList<SequenceFlow>() {{
            add(new WorkflowDefinitionGeneratorSequenceFlow("start_sequence", "開始イベントのシーケンスフロー",
                    "start", "task", null));
            add(new WorkflowDefinitionGeneratorSequenceFlow("boundary_sequence", "境界イベントのシーケンスフロー",
                    "boundary", "gateway", null));
            add(new WorkflowDefinitionGeneratorSequenceFlow("gateway_sequence1", "ゲートウェイのシーケンスフロー１",
                    "gateway", "end", "nablarch.tool.workflow.EqFlowProceedCondition(condition, 1)"));
            add(new WorkflowDefinitionGeneratorSequenceFlow("gateway_sequence2", "ゲートウェイのシーケンスフロー２",
                    "gateway", "task", "nablarch.tool.workflow.EqFlowProceedCondition(condition, 2)"));
        }});

        sut.write(Arrays.asList(workflowDefinition), temporaryFolder.getRoot());

        // ワークフロー定義
        File file = new File(temporaryFolder.getRoot(), "WF_WORKFLOW_DEFINITION.csv");
        assertThat(file.exists(), is(true));
        BufferedReader reader = new BufferedReader(new FileReader(file));
        assertThat(reader.readLine(), is("WORKFLOW_ID,DEF_VERSION,WORKFLOW_NAME,EFFECTIVE_DATE"));
        assertThat(reader.readLine(), is("WP0001,1,test,20170101"));
        assertThat(reader.readLine(), is(nullValue()));
        reader.close();

        // レーン
        file = new File(temporaryFolder.getRoot(), "WF_LANE.csv");
        assertThat(file.exists(), is(true));
        reader = new BufferedReader(new FileReader(file));
        assertThat(reader.readLine(), is("WORKFLOW_ID,DEF_VERSION,LANE_ID,LANE_NAME"));
        assertThat(reader.readLine(), is("WP0001,1,01,レーン１"));
        assertThat(reader.readLine(), is(nullValue()));
        reader.close();

        // フローノード
        file = new File(temporaryFolder.getRoot(), "WF_FLOW_NODE.csv");
        assertThat(file.exists(), is(true));
        reader = new BufferedReader(new FileReader(file));
        assertThat(reader.readLine(), is("WORKFLOW_ID,DEF_VERSION,FLOW_NODE_ID,LANE_ID,FLOW_NODE_NAME"));
        assertThat(reader.readLine(), is("WP0001,1,start,01,開始イベント"));
        assertThat(reader.readLine(), is("WP0001,1,end,01,停止イベント"));
        assertThat(reader.readLine(), is("WP0001,1,gateway,01,ゲートウェイ"));
        assertThat(reader.readLine(), is("WP0001,1,task,01,タスク"));
        assertThat(reader.readLine(), is("WP0001,1,boundary,01,境界イベント"));
        assertThat(reader.readLine(), is(nullValue()));
        reader.close();

        // イベント
        file = new File(temporaryFolder.getRoot(), "WF_EVENT.csv");
        assertThat(file.exists(), is(true));
        reader = new BufferedReader(new FileReader(file));
        assertThat(reader.readLine(), is("WORKFLOW_ID,DEF_VERSION,FLOW_NODE_ID,EVENT_TYPE"));
        assertThat(reader.readLine(), is("WP0001,1,start,START"));
        assertThat(reader.readLine(), is("WP0001,1,end,TERMINATE"));
        assertThat(reader.readLine(), is(nullValue()));
        reader.close();

        // ゲートウェイ
        file = new File(temporaryFolder.getRoot(), "WF_GATEWAY.csv");
        assertThat(file.exists(), is(true));
        reader = new BufferedReader(new FileReader(file));
        assertThat(reader.readLine(), is("WORKFLOW_ID,DEF_VERSION,FLOW_NODE_ID,GATEWAY_TYPE"));
        assertThat(reader.readLine(), is("WP0001,1,gateway,EXCLUSIVE"));
        assertThat(reader.readLine(), is(nullValue()));
        reader.close();

        // タスク
        file = new File(temporaryFolder.getRoot(), "WF_TASK.csv");
        assertThat(file.exists(), is(true));
        reader = new BufferedReader(new FileReader(file));
        assertThat(reader.readLine(), is("WORKFLOW_ID,DEF_VERSION,FLOW_NODE_ID,MULTI_INSTANCE_TYPE,COMPLETION_CONDITION"));
        assertThat(reader.readLine(), is("WP0001,1,task,NONE,"));
        assertThat(reader.readLine(), is(nullValue()));
        reader.close();

        // 境界イベント
        file = new File(temporaryFolder.getRoot(), "WF_BOUNDARY_EVENT.csv");
        assertThat(file.exists(), is(true));
        reader = new BufferedReader(new FileReader(file));
        assertThat(reader.readLine(), is("WORKFLOW_ID,DEF_VERSION,FLOW_NODE_ID,BOUNDARY_EVENT_TRIGGER_ID,ATTACHED_TASK_ID"));
        assertThat(reader.readLine(), is("WP0001,1,boundary,M01,task"));
        assertThat(reader.readLine(), is(nullValue()));
        reader.close();

        // 境界イベントトリガー
        file = new File(temporaryFolder.getRoot(), "WF_BOUNDARY_EVENT_TRIGGER.csv");
        assertThat(file.exists(), is(true));
        reader = new BufferedReader(new FileReader(file));
        assertThat(reader.readLine(), is("WORKFLOW_ID,DEF_VERSION,BOUNDARY_EVENT_TRIGGER_ID,BOUNDARY_EVENT_TRIGGER_NAME"));
        assertThat(reader.readLine(), is("WP0001,1,M01,メッセージ"));
        assertThat(reader.readLine(), is(nullValue()));
        reader.close();

        // シーケンスフロー
        file = new File(temporaryFolder.getRoot(), "WF_SEQUENCE_FLOW.csv");
        assertThat(file.exists(), is(true));
        reader = new BufferedReader(new FileReader(file));
        assertThat(reader.readLine(), is("WORKFLOW_ID,DEF_VERSION,SEQUENCE_FLOW_ID,SOURCE_FLOW_NODE_ID,TARGET_FLOW_NODE_ID,FLOW_PROCEED_CONDITION,SEQUENCE_FLOW_NAME"));
        assertThat(reader.readLine(), is("WP0001,1,start_sequence,start,task,,開始イベントのシーケンスフロー"));
        assertThat(reader.readLine(), is("WP0001,1,boundary_sequence,boundary,gateway,,境界イベントのシーケンスフロー"));
        assertThat(reader.readLine(), is("WP0001,1,gateway_sequence1,gateway,end,\"nablarch.tool.workflow.EqFlowProceedCondition(condition, 1)\",ゲートウェイのシーケンスフロー１"));
        assertThat(reader.readLine(), is("WP0001,1,gateway_sequence2,gateway,task,\"nablarch.tool.workflow.EqFlowProceedCondition(condition, 2)\",ゲートウェイのシーケンスフロー２"));
        assertThat(reader.readLine(), is(nullValue()));
        reader.close();
    }

    @Test
    public void 複数のステートマシン定義をもとにCSVファイルを出力できること() throws Exception {

        final WorkflowDefinition workflowDefinition1 = new WorkflowDefinition("WP0001", 1, "test1", "20170101");
        workflowDefinition1.setLanes(new ArrayList<Lane>() {{
            add(new Lane("01", "レーン"));
        }});
        workflowDefinition1.setEvents(new ArrayList<Event>() {{
            add(new Event("start", "開始イベント", "01", EventType.START.toString(), null));
            add(new Event("end", "停止イベント", "01", EventType.TERMINATE.toString(), null));
        }});
        workflowDefinition1.setGateways(new ArrayList<Gateway>() {{
            add(new Gateway("gateway", "ゲートウェイ", "01", GatewayType.EXCLUSIVE.toString(), null));
        }});
        workflowDefinition1.setTasks(new ArrayList<Task>() {{
            add(new Task("task", "タスク", "01", Task.MultiInstanceType.NONE.toString(), null, null));
        }});
        workflowDefinition1.setBoundaryEvents(new ArrayList<BoundaryEvent>() {{
            add(new BoundaryEvent("boundary", "境界イベント", "01",
                    "M01", "メッセージ", "task", null));
        }});
        workflowDefinition1.setSequenceFlows(new ArrayList<SequenceFlow>() {{
            add(new WorkflowDefinitionGeneratorSequenceFlow("start_sequence", "開始イベントのシーケンスフロー",
                    "start", "task", null));
            add(new WorkflowDefinitionGeneratorSequenceFlow("boundary_sequence", "境界イベントのシーケンスフロー",
                    "boundary", "gateway", null));
            add(new WorkflowDefinitionGeneratorSequenceFlow("gateway_sequence1", "ゲートウェイのシーケンスフロー１",
                    "gateway", "end", "nablarch.tool.workflow.EqFlowProceedCondition(condition, 1)"));
            add(new WorkflowDefinitionGeneratorSequenceFlow("gateway_sequence2", "ゲートウェイのシーケンスフロー２",
                    "gateway", "task", "nablarch.tool.workflow.EqFlowProceedCondition(condition, 2)"));
        }});

        final WorkflowDefinition workflowDefinition2 = new WorkflowDefinition("WP0002", 1, "test2", "20170101");
        workflowDefinition2.setLanes(new ArrayList<Lane>() {{
            add(new Lane("01", "レーン１"));
            add(new Lane("02", "レーン２"));
        }});
        workflowDefinition2.setEvents(new ArrayList<Event>() {{
            add(new Event("start", "開始イベント", "01", EventType.START.toString(), null));
            add(new Event("end1", "停止イベント１", "01", EventType.TERMINATE.toString(), null));
            add(new Event("end2", "停止イベント２", "02", EventType.TERMINATE.toString(), null));
        }});
        workflowDefinition2.setGateways(new ArrayList<Gateway>() {{
            add(new Gateway("gateway", "ゲートウェイ", "01", GatewayType.EXCLUSIVE.toString(), null));
        }});
        workflowDefinition2.setTasks(new ArrayList<Task>() {{
            add(new Task("task1", "タスク１", "01", Task.MultiInstanceType.NONE.toString(), null, null));
            add(new Task("task2", "タスク２", "02", Task.MultiInstanceType.NONE.toString(), null, null));
        }});
        workflowDefinition2.setBoundaryEvents(new ArrayList<BoundaryEvent>() {{
            add(new BoundaryEvent("boundary1", "境界イベント１", "01",
                    "M01", "メッセージ１", "task1", null));
            add(new BoundaryEvent("boundary2", "境界イベント２", "02",
                    "M02", "メッセージ２", "task2", null));
        }});
        workflowDefinition2.setSequenceFlows(new ArrayList<SequenceFlow>() {{
            add(new WorkflowDefinitionGeneratorSequenceFlow("start_sequence", "開始イベントのシーケンスフロー",
                    "start", "task1", null));
            add(new WorkflowDefinitionGeneratorSequenceFlow("boundary1_sequence", "境界イベント１のシーケンスフロー",
                    "boundary1", "gateway", null));
            add(new WorkflowDefinitionGeneratorSequenceFlow("boundary2_sequence", "境界イベント２のシーケンスフロー",
                    "boundary2", "end2", null));
            add(new WorkflowDefinitionGeneratorSequenceFlow("gateway_sequence1", "ゲートウェイのシーケンスフロー１",
                    "gateway", "end1", "nablarch.tool.workflow.EqFlowProceedCondition(condition, 1)"));
            add(new WorkflowDefinitionGeneratorSequenceFlow("gateway_sequence2", "ゲートウェイのシーケンスフロー２",
                    "gateway", "task2", "nablarch.tool.workflow.EqFlowProceedCondition(condition, 2)"));
        }});

        sut.write(Arrays.asList(workflowDefinition1, workflowDefinition2), temporaryFolder.getRoot());

        // ワークフロー定義
        File file = new File(temporaryFolder.getRoot(), "WF_WORKFLOW_DEFINITION.csv");
        assertThat(file.exists(), is(true));
        BufferedReader reader = new BufferedReader(new FileReader(file));
        assertThat(reader.readLine(), is("WORKFLOW_ID,DEF_VERSION,WORKFLOW_NAME,EFFECTIVE_DATE"));
        assertThat(reader.readLine(), is("WP0001,1,test1,20170101"));
        assertThat(reader.readLine(), is("WP0002,1,test2,20170101"));
        assertThat(reader.readLine(), is(nullValue()));
        reader.close();

        // レーン
        file = new File(temporaryFolder.getRoot(), "WF_LANE.csv");
        assertThat(file.exists(), is(true));
        reader = new BufferedReader(new FileReader(file));
        assertThat(reader.readLine(), is("WORKFLOW_ID,DEF_VERSION,LANE_ID,LANE_NAME"));
        assertThat(reader.readLine(), is("WP0001,1,01,レーン"));
        assertThat(reader.readLine(), is("WP0002,1,01,レーン１"));
        assertThat(reader.readLine(), is("WP0002,1,02,レーン２"));
        assertThat(reader.readLine(), is(nullValue()));
        reader.close();

        // フローノード
        file = new File(temporaryFolder.getRoot(), "WF_FLOW_NODE.csv");
        assertThat(file.exists(), is(true));
        reader = new BufferedReader(new FileReader(file));
        assertThat(reader.readLine(), is("WORKFLOW_ID,DEF_VERSION,FLOW_NODE_ID,LANE_ID,FLOW_NODE_NAME"));
        assertThat(reader.readLine(), is("WP0001,1,start,01,開始イベント"));
        assertThat(reader.readLine(), is("WP0001,1,end,01,停止イベント"));
        assertThat(reader.readLine(), is("WP0001,1,gateway,01,ゲートウェイ"));
        assertThat(reader.readLine(), is("WP0001,1,task,01,タスク"));
        assertThat(reader.readLine(), is("WP0001,1,boundary,01,境界イベント"));
        assertThat(reader.readLine(), is("WP0002,1,start,01,開始イベント"));
        assertThat(reader.readLine(), is("WP0002,1,end1,01,停止イベント１"));
        assertThat(reader.readLine(), is("WP0002,1,end2,02,停止イベント２"));
        assertThat(reader.readLine(), is("WP0002,1,gateway,01,ゲートウェイ"));
        assertThat(reader.readLine(), is("WP0002,1,task1,01,タスク１"));
        assertThat(reader.readLine(), is("WP0002,1,task2,02,タスク２"));
        assertThat(reader.readLine(), is("WP0002,1,boundary1,01,境界イベント１"));
        assertThat(reader.readLine(), is("WP0002,1,boundary2,02,境界イベント２"));
        assertThat(reader.readLine(), is(nullValue()));
        reader.close();

        // イベント
        file = new File(temporaryFolder.getRoot(), "WF_EVENT.csv");
        assertThat(file.exists(), is(true));
        reader = new BufferedReader(new FileReader(file));
        assertThat(reader.readLine(), is("WORKFLOW_ID,DEF_VERSION,FLOW_NODE_ID,EVENT_TYPE"));
        assertThat(reader.readLine(), is("WP0001,1,start,START"));
        assertThat(reader.readLine(), is("WP0001,1,end,TERMINATE"));
        assertThat(reader.readLine(), is("WP0002,1,start,START"));
        assertThat(reader.readLine(), is("WP0002,1,end1,TERMINATE"));
        assertThat(reader.readLine(), is("WP0002,1,end2,TERMINATE"));
        assertThat(reader.readLine(), is(nullValue()));
        reader.close();

        // ゲートウェイ
        file = new File(temporaryFolder.getRoot(), "WF_GATEWAY.csv");
        assertThat(file.exists(), is(true));
        reader = new BufferedReader(new FileReader(file));
        assertThat(reader.readLine(), is("WORKFLOW_ID,DEF_VERSION,FLOW_NODE_ID,GATEWAY_TYPE"));
        assertThat(reader.readLine(), is("WP0001,1,gateway,EXCLUSIVE"));
        assertThat(reader.readLine(), is("WP0002,1,gateway,EXCLUSIVE"));
        assertThat(reader.readLine(), is(nullValue()));
        reader.close();

        // タスク
        file = new File(temporaryFolder.getRoot(), "WF_TASK.csv");
        assertThat(file.exists(), is(true));
        reader = new BufferedReader(new FileReader(file));
        assertThat(reader.readLine(), is("WORKFLOW_ID,DEF_VERSION,FLOW_NODE_ID,MULTI_INSTANCE_TYPE,COMPLETION_CONDITION"));
        assertThat(reader.readLine(), is("WP0001,1,task,NONE,"));
        assertThat(reader.readLine(), is("WP0002,1,task1,NONE,"));
        assertThat(reader.readLine(), is("WP0002,1,task2,NONE,"));
        assertThat(reader.readLine(), is(nullValue()));
        reader.close();

        // 境界イベント
        file = new File(temporaryFolder.getRoot(), "WF_BOUNDARY_EVENT.csv");
        assertThat(file.exists(), is(true));
        reader = new BufferedReader(new FileReader(file));
        assertThat(reader.readLine(), is("WORKFLOW_ID,DEF_VERSION,FLOW_NODE_ID,BOUNDARY_EVENT_TRIGGER_ID,ATTACHED_TASK_ID"));
        assertThat(reader.readLine(), is("WP0001,1,boundary,M01,task"));
        assertThat(reader.readLine(), is("WP0002,1,boundary1,M01,task1"));
        assertThat(reader.readLine(), is("WP0002,1,boundary2,M02,task2"));
        assertThat(reader.readLine(), is(nullValue()));
        reader.close();

        // 境界イベントトリガー
        file = new File(temporaryFolder.getRoot(), "WF_BOUNDARY_EVENT_TRIGGER.csv");
        assertThat(file.exists(), is(true));
        reader = new BufferedReader(new FileReader(file));
        assertThat(reader.readLine(), is("WORKFLOW_ID,DEF_VERSION,BOUNDARY_EVENT_TRIGGER_ID,BOUNDARY_EVENT_TRIGGER_NAME"));
        assertThat(reader.readLine(), is("WP0001,1,M01,メッセージ"));
        assertThat(reader.readLine(), is("WP0002,1,M01,メッセージ１"));
        assertThat(reader.readLine(), is("WP0002,1,M02,メッセージ２"));
        assertThat(reader.readLine(), is(nullValue()));
        reader.close();

        // シーケンスフロー
        file = new File(temporaryFolder.getRoot(), "WF_SEQUENCE_FLOW.csv");
        assertThat(file.exists(), is(true));
        reader = new BufferedReader(new FileReader(file));
        assertThat(reader.readLine(), is("WORKFLOW_ID,DEF_VERSION,SEQUENCE_FLOW_ID,SOURCE_FLOW_NODE_ID,TARGET_FLOW_NODE_ID,FLOW_PROCEED_CONDITION,SEQUENCE_FLOW_NAME"));
        assertThat(reader.readLine(), is("WP0001,1,start_sequence,start,task,,開始イベントのシーケンスフロー"));
        assertThat(reader.readLine(), is("WP0001,1,boundary_sequence,boundary,gateway,,境界イベントのシーケンスフロー"));
        assertThat(reader.readLine(), is("WP0001,1,gateway_sequence1,gateway,end,\"nablarch.tool.workflow.EqFlowProceedCondition(condition, 1)\",ゲートウェイのシーケンスフロー１"));
        assertThat(reader.readLine(), is("WP0001,1,gateway_sequence2,gateway,task,\"nablarch.tool.workflow.EqFlowProceedCondition(condition, 2)\",ゲートウェイのシーケンスフロー２"));
        assertThat(reader.readLine(), is("WP0002,1,start_sequence,start,task1,,開始イベントのシーケンスフロー"));
        assertThat(reader.readLine(), is("WP0002,1,boundary1_sequence,boundary1,gateway,,境界イベント１のシーケンスフロー"));
        assertThat(reader.readLine(), is("WP0002,1,boundary2_sequence,boundary2,end2,,境界イベント２のシーケンスフロー"));
        assertThat(reader.readLine(), is("WP0002,1,gateway_sequence1,gateway,end1,\"nablarch.tool.workflow.EqFlowProceedCondition(condition, 1)\",ゲートウェイのシーケンスフロー１"));
        assertThat(reader.readLine(), is("WP0002,1,gateway_sequence2,gateway,task2,\"nablarch.tool.workflow.EqFlowProceedCondition(condition, 2)\",ゲートウェイのシーケンスフロー２"));
        assertThat(reader.readLine(), is(nullValue()));
        reader.close();
    }
}
