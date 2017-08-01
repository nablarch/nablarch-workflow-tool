package nablarch.tool.workflow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nablarch.test.support.SystemRepositoryResource;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;

import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.tool.util.poi.PoiUtil;
import nablarch.tool.util.poi.SimpleTableReader;

import nablarch.integration.workflow.definition.BoundaryEvent;
import nablarch.integration.workflow.definition.Event;
import nablarch.integration.workflow.definition.Gateway;
import nablarch.integration.workflow.definition.Lane;
import nablarch.integration.workflow.definition.SequenceFlow;
import nablarch.integration.workflow.definition.Task;
import nablarch.integration.workflow.definition.WorkflowDefinition;
import nablarch.integration.workflow.definition.loader.WorkflowDefinitionSchema;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * {@link ExcelDataWriter}のテストクラス。
 *
 * @author Satoshi Taromaru
 * @since 1.4.2
 */
public class ExcelDataWriterTest {

    @Rule
    public SystemRepositoryResource systemRepositoryResource =
            new SystemRepositoryResource("nablarch/tool/workflow/WorkflowDefinitionGeneratorTest.xml");

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /** テスト対象 */
    private ExcelDataWriter sut = new ExcelDataWriter();

    /**
     * 出力ファイルパスが不正な場合、RuntimeExceptionが送出されること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void testOpen() throws Exception {
        expectedException.expect(RuntimeException.class);
        expectedException.expectCause(Matchers.<Throwable>instanceOf(FileNotFoundException.class));
        sut.write(new ArrayList<WorkflowDefinition>(), new File("invalid"));
    }

    /**
     * 全てのサポート対象要素を含む入力値をインプットにした場合、正しく出力されること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void testAllSupportElement() throws Exception {
        WorkflowDefinition workflowDefinition = new WorkflowDefinition("testWorkFlow1", 1, "process_poolName1", "20140804");
        List<Task> tasks = new ArrayList<Task>();
        tasks.add(new WorkflowDefinitionGeneratorTask("usertask1", "flowNodeName1", "laneId1", "NONE", ""));
        tasks.add(new WorkflowDefinitionGeneratorTask("usertask2", "flowNodeName2", "laneId1", "SEQUENTIAL", "nablarch.tool.workflow.Condition1"));
        tasks.add(new WorkflowDefinitionGeneratorTask("usertask3", "flowNodeName3", "laneId1", "PARALLEL", "nablarch.tool.workflow.Condition2"));
        workflowDefinition.setTasks(tasks);

        List<Lane> lanes = new ArrayList<Lane>();
        lanes.add(new Lane("laneId1", "laneName1"));
        workflowDefinition.setLanes(lanes);

        List<Event> events = new ArrayList<Event>();
        events.add(new Event("startevent1", "flowNodeName4", "laneId1", "START", new ArrayList<SequenceFlow>()));
        events.add(new Event("endevent1", "flowNodeName5", "laneId1", "TERMINATE", new ArrayList<SequenceFlow>()));
        workflowDefinition.setEvents(events);

        List<Gateway> gateways = new ArrayList<Gateway>();
        gateways.add(new Gateway("exclusivegateway1", "flowNodeName6", "laneId1", "EXCLUSIVE", new ArrayList<SequenceFlow>()));
        workflowDefinition.setGateways(gateways);

        List<BoundaryEvent> boundaryEvents = new ArrayList<BoundaryEvent>();
        boundaryEvents.add(new BoundaryEvent("boundarymessage1", "flowNodeName7", "laneId1", "MG001", "MG001", "usertask2", new ArrayList<SequenceFlow>()));
        boundaryEvents.add(new BoundaryEvent("boundarymessage2", "flowNodeName8", "laneId1", "MG001", "MG001", "usertask3", new ArrayList<SequenceFlow>()));
        workflowDefinition.setBoundaryEvents(boundaryEvents);

        List<SequenceFlow> sequenceFlows = new ArrayList<SequenceFlow>();
        sequenceFlows.add(new WorkflowDefinitionGeneratorSequenceFlow("flow1", "to UserTask1", "startevent1", "usertask1", ""));
        sequenceFlows.add(new WorkflowDefinitionGeneratorSequenceFlow("flow2", "to UserTask2", "usertask1", "usertask2", ""));
        sequenceFlows.add(new WorkflowDefinitionGeneratorSequenceFlow("flow3", "to UserTask3", "usertask2", "usertask3", ""));
        sequenceFlows.add(new WorkflowDefinitionGeneratorSequenceFlow("flow4", "to Exclusive Gateway1", "usertask3", "exclusivegateway1", ""));
        sequenceFlows.add(
                new WorkflowDefinitionGeneratorSequenceFlow("flow5", "to End1", "exclusivegateway1", "endevent1", "nablarch.tool.workflow.Condition3"));
        sequenceFlows.add(new WorkflowDefinitionGeneratorSequenceFlow("flow6", "to UserTask1 from Boundary Event", "boundarymessage1", "usertask1", ""));
        sequenceFlows.add(
                new WorkflowDefinitionGeneratorSequenceFlow("flow7", "to UserTask2 from Gateway", "exclusivegateway1", "usertask2", "nablarch.tool.workflow.Condition3"));
        sequenceFlows.add(new WorkflowDefinitionGeneratorSequenceFlow("flow8", "to UserTask2 from Boundary Event", "boundarymessage2", "usertask2", ""));
        workflowDefinition.setSequenceFlows(sequenceFlows);
        List<WorkflowDefinition> definitions = new ArrayList<WorkflowDefinition>();
        definitions.add(workflowDefinition);

        // テスト実施
        sut.write(definitions, temporaryFolder.getRoot());

        // 検証
        HSSFWorkbook outBook = new HSSFWorkbook(new FileInputStream(temporaryFolder.getRoot().getPath() + "/workflowDefinitionData.xls"));
        SimpleTableReader tableReader = new SimpleTableReader().setHeaderRowNum(1);

        WorkflowDefinitionSchema schema = systemRepositoryResource.getComponent("workflowDefinitionSchema");
        assertThat(tableReader.read(outBook.getSheet(schema.getWorkflowDefinitionTableName())), is(getExpectedWorkflowDefinitionData(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getLaneTableName())), is(getExpectedLaneData(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getFlowNodeTableName())), is(getExpectedFlowNodeData(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getTaskTableName())), is(getExpectedTaskData(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getBoundaryEventTableName())), is(getExpectedBoundaryEventData(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getEventTableName())), is(getExpectedEventData(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getGatewayTableName())), is(getExpectedGatewayData(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getEventTriggerTableName())), is(getExpectedBoundaryEventTriggerData(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getSequenceFlowTableName())), is(getExpectedSequenceFlowData(schema)));
    }

    /**
     * ゲートウェイが存在しない場合、正しく出力されること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void testNoGateway() throws Exception {
        WorkflowDefinition workflowDefinition = new WorkflowDefinition("testWorkFlow1", 1, "process_poolName1", "20140804");
        List<Task> tasks = new ArrayList<Task>();
        tasks.add(new WorkflowDefinitionGeneratorTask("usertask1", "flowNodeName1", "laneId1", "NONE", ""));
        tasks.add(new WorkflowDefinitionGeneratorTask("usertask2", "flowNodeName2", "laneId1", "SEQUENTIAL", "nablarch.tool.workflow.Condition1"));
        tasks.add(new WorkflowDefinitionGeneratorTask("usertask3", "flowNodeName3", "laneId1", "PARALLEL", "nablarch.tool.workflow.Condition2"));
        workflowDefinition.setTasks(tasks);

        List<Lane> lanes = new ArrayList<Lane>();
        lanes.add(new Lane("laneId1", "laneName1"));
        workflowDefinition.setLanes(lanes);

        List<Event> events = new ArrayList<Event>();
        events.add(new Event("startevent1", "flowNodeName4", "laneId1", "START", new ArrayList<SequenceFlow>()));
        events.add(new Event("endevent1", "flowNodeName5", "laneId1", "TERMINATE", new ArrayList<SequenceFlow>()));
        workflowDefinition.setEvents(events);

        workflowDefinition.setGateways(new ArrayList<Gateway>());

        List<BoundaryEvent> boundaryEvents = new ArrayList<BoundaryEvent>();
        boundaryEvents.add(new BoundaryEvent("boundarymessage1", "flowNodeName7", "laneId1", "MG001", "MG001", "usertask2", new ArrayList<SequenceFlow>()));
        boundaryEvents.add(new BoundaryEvent("boundarymessage2", "flowNodeName8", "laneId1", "MG001", "MG001", "usertask3", new ArrayList<SequenceFlow>()));
        workflowDefinition.setBoundaryEvents(boundaryEvents);

        List<SequenceFlow> sequenceFlows = new ArrayList<SequenceFlow>();
        sequenceFlows.add(new WorkflowDefinitionGeneratorSequenceFlow("flow1", "to UserTask1", "startevent1", "usertask1", ""));
        sequenceFlows.add(new WorkflowDefinitionGeneratorSequenceFlow("flow2", "to UserTask2", "usertask1", "usertask2", ""));
        sequenceFlows.add(new WorkflowDefinitionGeneratorSequenceFlow("flow3", "to UserTask3", "usertask2", "usertask3", ""));
        sequenceFlows.add(
                new WorkflowDefinitionGeneratorSequenceFlow("flow5", "to End1", "usertask3", "endevent1", ""));
        sequenceFlows.add(new WorkflowDefinitionGeneratorSequenceFlow("flow6", "to UserTask1 from Boundary Event", "boundarymessage1", "usertask1", ""));
        sequenceFlows.add(new WorkflowDefinitionGeneratorSequenceFlow("flow8", "to UserTask2 from Boundary Event", "boundarymessage2", "usertask2", ""));
        workflowDefinition.setSequenceFlows(sequenceFlows);
        List<WorkflowDefinition> definitions = new ArrayList<WorkflowDefinition>();
        definitions.add(workflowDefinition);

        // テスト実施
        sut.write(definitions, temporaryFolder.getRoot());

        // 検証
        HSSFWorkbook outBook = new HSSFWorkbook(new FileInputStream(temporaryFolder.getRoot().getPath() + "/workflowDefinitionData.xls"));
        SimpleTableReader tableReader = new SimpleTableReader().setHeaderRowNum(1);

        WorkflowDefinitionSchema schema = systemRepositoryResource.getComponent("workflowDefinitionSchema");
        assertThat(tableReader.read(outBook.getSheet(schema.getWorkflowDefinitionTableName())), is(getExpectedWorkflowDefinitionData(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getLaneTableName())), is(getExpectedLaneData(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getFlowNodeTableName())), is(getExpectedFlowNodeDataNoGateway(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getTaskTableName())), is(getExpectedTaskData(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getBoundaryEventTableName())), is(getExpectedBoundaryEventData(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getEventTableName())), is(getExpectedEventData(schema)));
        List<Map<String, String>> emptyList = new ArrayList<Map<String, String>>();
        assertThat(tableReader.read(outBook.getSheet(schema.getGatewayTableName())), is(emptyList));
        assertThat(tableReader.read(outBook.getSheet(schema.getEventTriggerTableName())), is(getExpectedBoundaryEventTriggerData(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getSequenceFlowTableName())), is(getExpectedSequenceFlowDataNoGateway(schema)));
    }

    /**
     * 境界イベントが存在しない場合、正しく出力されること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void testNoBoundaryEvent() throws Exception {
        WorkflowDefinition workflowDefinition = new WorkflowDefinition("testWorkFlow1", 1, "process_poolName1", "20140804");
        List<Task> tasks = new ArrayList<Task>();
        tasks.add(new WorkflowDefinitionGeneratorTask("usertask1", "flowNodeName1", "laneId1", "NONE", ""));
        tasks.add(new WorkflowDefinitionGeneratorTask("usertask2", "flowNodeName2", "laneId1", "SEQUENTIAL", "nablarch.tool.workflow.Condition1"));
        tasks.add(new WorkflowDefinitionGeneratorTask("usertask3", "flowNodeName3", "laneId1", "PARALLEL", "nablarch.tool.workflow.Condition2"));
        workflowDefinition.setTasks(tasks);

        List<Lane> lanes = new ArrayList<Lane>();
        lanes.add(new Lane("laneId1", "laneName1"));
        workflowDefinition.setLanes(lanes);

        List<Event> events = new ArrayList<Event>();
        events.add(new Event("startevent1", "flowNodeName4", "laneId1", "START", new ArrayList<SequenceFlow>()));
        events.add(new Event("endevent1", "flowNodeName5", "laneId1", "TERMINATE", new ArrayList<SequenceFlow>()));
        workflowDefinition.setEvents(events);

        List<Gateway> gateways = new ArrayList<Gateway>();
        gateways.add(new Gateway("exclusivegateway1", "flowNodeName6", "laneId1", "EXCLUSIVE", new ArrayList<SequenceFlow>()));
        workflowDefinition.setGateways(gateways);

        workflowDefinition.setBoundaryEvents(new ArrayList<BoundaryEvent>());

        List<SequenceFlow> sequenceFlows = new ArrayList<SequenceFlow>();
        sequenceFlows.add(new WorkflowDefinitionGeneratorSequenceFlow("flow1", "to UserTask1", "startevent1", "usertask1", ""));
        sequenceFlows.add(new WorkflowDefinitionGeneratorSequenceFlow("flow2", "to UserTask2", "usertask1", "usertask2", ""));
        sequenceFlows.add(new WorkflowDefinitionGeneratorSequenceFlow("flow3", "to UserTask3", "usertask2", "usertask3", ""));
        sequenceFlows.add(new WorkflowDefinitionGeneratorSequenceFlow("flow4", "to Exclusive Gateway1", "usertask3", "exclusivegateway1", ""));
        sequenceFlows.add(
                new WorkflowDefinitionGeneratorSequenceFlow("flow5", "to End1", "exclusivegateway1", "endevent1", "nablarch.tool.workflow.Condition3"));
        sequenceFlows.add(
                new WorkflowDefinitionGeneratorSequenceFlow("flow7", "to UserTask2 from Gateway", "exclusivegateway1", "usertask2", "nablarch.tool.workflow.Condition3"));
        workflowDefinition.setSequenceFlows(sequenceFlows);
        List<WorkflowDefinition> definitions = new ArrayList<WorkflowDefinition>();
        definitions.add(workflowDefinition);

        // テスト実施
        sut.write(definitions, temporaryFolder.getRoot());

        // 検証
        HSSFWorkbook outBook = new HSSFWorkbook(new FileInputStream(temporaryFolder.getRoot().getPath() + "/workflowDefinitionData.xls"));
        SimpleTableReader tableReader = new SimpleTableReader().setHeaderRowNum(1);

        WorkflowDefinitionSchema schema = systemRepositoryResource.getComponent("workflowDefinitionSchema");
        assertThat(tableReader.read(outBook.getSheet(schema.getWorkflowDefinitionTableName())), is(getExpectedWorkflowDefinitionData(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getLaneTableName())), is(getExpectedLaneData(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getFlowNodeTableName())), is(getExpectedFlowNodeDataNoBoundaryEvent(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getTaskTableName())), is(getExpectedTaskData(schema)));
        List<Map<String, String>> emptyList = new ArrayList<Map<String, String>>();
        assertThat(tableReader.read(outBook.getSheet(schema.getBoundaryEventTableName())), is(emptyList));
        assertThat(tableReader.read(outBook.getSheet(schema.getEventTableName())), is(getExpectedEventData(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getGatewayTableName())), is(getExpectedGatewayData(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getEventTriggerTableName())), is(emptyList));
        assertThat(tableReader.read(outBook.getSheet(schema.getSequenceFlowTableName())), is(getExpectedSequenceFlowDataNoBoundaryEvent(schema)));
    }

    /**
     * ワークフロー定義の期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return 期待値
     */
    private List<Map<String, String>> getExpectedWorkflowDefinitionData(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"testWorkFlow1", "1", "process_poolName1", "20140804"}
        };
        List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
        for (String[] e : exp) {
            Map<String, String> row = new HashMap<String, String>();
            row.put(schema.getWorkflowIdColumnName(), e[0]);
            row.put(schema.getVersionColumnName(), e[1]);
            row.put(schema.getWorkflowNameColumnName(), e[2]);
            row.put(schema.getEffectiveDateColumnName(), e[3]);
            ret.add(row);
        }
        return ret;
    }

    /**
     * レーンの期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return 期待値
     */
    private List<Map<String, String>> getExpectedLaneData(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"testWorkFlow1", "1", "laneId1", "laneName1"}
        };
        List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
        for (String[] e : exp) {
            Map<String, String> row = new HashMap<String, String>();
            row.put(schema.getWorkflowIdColumnName(), e[0]);
            row.put(schema.getVersionColumnName(), e[1]);
            row.put(schema.getLaneIdColumnName(), e[2]);
            row.put(schema.getLaneNameColumnName(), e[3]);
            ret.add(row);
        }
        return ret;
    }

    /**
     * フローノードの期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return 期待値
     */
    private List<Map<String, String>> getExpectedFlowNodeData(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"testWorkFlow1", "1", "endevent1", "laneId1", "flowNodeName5"},
                {"testWorkFlow1", "1", "startevent1", "laneId1", "flowNodeName4"},
                {"testWorkFlow1", "1", "boundarymessage1", "laneId1", "flowNodeName7"},
                {"testWorkFlow1", "1", "boundarymessage2", "laneId1", "flowNodeName8"},
                {"testWorkFlow1", "1", "usertask1", "laneId1", "flowNodeName1"},
                {"testWorkFlow1", "1", "usertask2", "laneId1", "flowNodeName2"},
                {"testWorkFlow1", "1", "usertask3", "laneId1", "flowNodeName3"},
                {"testWorkFlow1", "1", "exclusivegateway1", "laneId1", "flowNodeName6"}
        };
        List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
        for (String[] e : exp) {
            Map<String, String> row = new HashMap<String, String>();
            row.put(schema.getWorkflowIdColumnName(), e[0]);
            row.put(schema.getVersionColumnName(), e[1]);
            row.put(schema.getFlowNodeIdColumnName(), e[2]);
            row.put(schema.getLaneIdColumnName(), e[3]);
            row.put(schema.getFlowNodeNameColumnName(), e[4]);
            ret.add(row);
        }
        return ret;
    }

    /**
     * フローノードの期待値を取得する。
     * （ゲートウェイが存在しないケース）
     *
     * @param schema スキーマ情報
     * @return 期待値
     */
    private List<Map<String, String>> getExpectedFlowNodeDataNoGateway(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"testWorkFlow1", "1", "endevent1", "laneId1", "flowNodeName5"},
                {"testWorkFlow1", "1", "startevent1", "laneId1", "flowNodeName4"},
                {"testWorkFlow1", "1", "boundarymessage1", "laneId1", "flowNodeName7"},
                {"testWorkFlow1", "1", "boundarymessage2", "laneId1", "flowNodeName8"},
                {"testWorkFlow1", "1", "usertask1", "laneId1", "flowNodeName1"},
                {"testWorkFlow1", "1", "usertask2", "laneId1", "flowNodeName2"},
                {"testWorkFlow1", "1", "usertask3", "laneId1", "flowNodeName3"}
        };
        List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
        for (String[] e : exp) {
            Map<String, String> row = new HashMap<String, String>();
            row.put(schema.getWorkflowIdColumnName(), e[0]);
            row.put(schema.getVersionColumnName(), e[1]);
            row.put(schema.getFlowNodeIdColumnName(), e[2]);
            row.put(schema.getLaneIdColumnName(), e[3]);
            row.put(schema.getFlowNodeNameColumnName(), e[4]);
            ret.add(row);
        }
        return ret;
    }

    /**
     * フローノードの期待値を取得する。
     * （境界イベントが存在しないケース）
     *
     * @param schema スキーマ情報
     * @return 期待値
     */
    private List<Map<String, String>> getExpectedFlowNodeDataNoBoundaryEvent(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"testWorkFlow1", "1", "endevent1", "laneId1", "flowNodeName5"},
                {"testWorkFlow1", "1", "startevent1", "laneId1", "flowNodeName4"},
                {"testWorkFlow1", "1", "usertask1", "laneId1", "flowNodeName1"},
                {"testWorkFlow1", "1", "usertask2", "laneId1", "flowNodeName2"},
                {"testWorkFlow1", "1", "usertask3", "laneId1", "flowNodeName3"},
                {"testWorkFlow1", "1", "exclusivegateway1", "laneId1", "flowNodeName6"}
        };
        List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
        for (String[] e : exp) {
            Map<String, String> row = new HashMap<String, String>();
            row.put(schema.getWorkflowIdColumnName(), e[0]);
            row.put(schema.getVersionColumnName(), e[1]);
            row.put(schema.getFlowNodeIdColumnName(), e[2]);
            row.put(schema.getLaneIdColumnName(), e[3]);
            row.put(schema.getFlowNodeNameColumnName(), e[4]);
            ret.add(row);
        }
        return ret;
    }

    /**
     * タスクの期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return 期待値
     */
    private List<Map<String, String>> getExpectedTaskData(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"testWorkFlow1", "1", "usertask1", "NONE", ""},
                {"testWorkFlow1", "1", "usertask2", "SEQUENTIAL", "nablarch.tool.workflow.Condition1"},
                {"testWorkFlow1", "1", "usertask3", "PARALLEL", "nablarch.tool.workflow.Condition2"}
        };
        List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
        for (String[] e : exp) {
            Map<String, String> row = new HashMap<String, String>();
            row.put(schema.getWorkflowIdColumnName(), e[0]);
            row.put(schema.getVersionColumnName(), e[1]);
            row.put(schema.getFlowNodeIdColumnName(), e[2]);
            row.put(schema.getMultiInstanceTypeColumnName(), e[3]);
            row.put(schema.getCompletionConditionColumnName(), e[4]);
            ret.add(row);
        }
        return ret;
    }

    /**
     * 境界イベントの期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return 期待値
     */
    private List<Map<String, String>> getExpectedBoundaryEventData(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"testWorkFlow1", "1", "boundarymessage1", "MG001", "usertask2"},
                {"testWorkFlow1", "1", "boundarymessage2", "MG001", "usertask3"}
        };
        List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
        for (String[] e : exp) {
            Map<String, String> row = new HashMap<String, String>();
            row.put(schema.getWorkflowIdColumnName(), e[0]);
            row.put(schema.getVersionColumnName(), e[1]);
            row.put(schema.getFlowNodeIdColumnName(), e[2]);
            row.put(schema.getBoundaryEventTriggerIdColumnName(), e[3]);
            row.put(schema.getAttachedTaskIdColumnName(), e[4]);
            ret.add(row);
        }
        return ret;
    }

    /**
     * イベントの期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return 期待値
     */
    private List<Map<String, String>> getExpectedEventData(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"testWorkFlow1", "1", "endevent1", "TERMINATE"},
                {"testWorkFlow1", "1", "startevent1", "START"}
        };
        List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
        for (String[] e : exp) {
            Map<String, String> row = new HashMap<String, String>();
            row.put(schema.getWorkflowIdColumnName(), e[0]);
            row.put(schema.getVersionColumnName(), e[1]);
            row.put(schema.getFlowNodeIdColumnName(), e[2]);
            row.put(schema.getEventTypeColumnName(), e[3]);
            ret.add(row);
        }
        return ret;
    }

    /**
     * ゲートウェイの期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return 期待値
     */
    private List<Map<String, String>> getExpectedGatewayData(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"testWorkFlow1", "1", "exclusivegateway1", "EXCLUSIVE"}
        };
        List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
        for (String[] e : exp) {
            Map<String, String> row = new HashMap<String, String>();
            row.put(schema.getWorkflowIdColumnName(), e[0]);
            row.put(schema.getVersionColumnName(), e[1]);
            row.put(schema.getFlowNodeIdColumnName(), e[2]);
            row.put(schema.getGatewayTypeColumnName(), e[3]);
            ret.add(row);
        }
        return ret;
    }

    /**
     * 境界イベントトリガーの期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return 期待値
     */
    private List<Map<String, String>> getExpectedBoundaryEventTriggerData(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"testWorkFlow1", "1", "MG001", "MG001"}
        };
        List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
        for (String[] e : exp) {
            Map<String, String> row = new HashMap<String, String>();
            row.put(schema.getWorkflowIdColumnName(), e[0]);
            row.put(schema.getVersionColumnName(), e[1]);
            row.put(schema.getBoundaryEventTriggerIdColumnName(), e[2]);
            row.put(schema.getBoundaryEventTriggerNameColumnName(), e[3]);
            ret.add(row);
        }
        return ret;
    }

    /**
     * シーケンスフローの期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return 期待値
     */
    private List<Map<String, String>> getExpectedSequenceFlowData(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"testWorkFlow1", "1", "flow1", "startevent1", "usertask1", "", "to UserTask1"},
                {"testWorkFlow1", "1", "flow2", "usertask1", "usertask2", "", "to UserTask2"},
                {"testWorkFlow1", "1", "flow3", "usertask2", "usertask3", "", "to UserTask3"},
                {"testWorkFlow1", "1", "flow4", "usertask3", "exclusivegateway1", "", "to Exclusive Gateway1"},
                {"testWorkFlow1", "1", "flow5", "exclusivegateway1", "endevent1", "nablarch.tool.workflow.Condition3", "to End1"},
                {"testWorkFlow1", "1", "flow6", "boundarymessage1", "usertask1", "", "to UserTask1 from Boundary Event"},
                {"testWorkFlow1", "1", "flow7", "exclusivegateway1", "usertask2", "nablarch.tool.workflow.Condition3", "to UserTask2 from Gateway"},
                {"testWorkFlow1", "1", "flow8", "boundarymessage2", "usertask2", "", "to UserTask2 from Boundary Event"}
        };
        List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
        for (String[] e : exp) {
            Map<String, String> row = new HashMap<String, String>();
            row.put(schema.getWorkflowIdColumnName(), e[0]);
            row.put(schema.getVersionColumnName(), e[1]);
            row.put(schema.getSequenceFlowIdColumnName(), e[2]);
            row.put(schema.getSourceFlowNodeIdColumnName(), e[3]);
            row.put(schema.getTargetFlowNodeIdColumnName(), e[4]);
            row.put(schema.getFlowProceedConditionColumnName(), e[5]);
            row.put(schema.getSequenceFlowNameColumnName(), e[6]);
            ret.add(row);
        }
        return ret;
    }

    /**
     * シーケンスフローの期待値を取得する。
     * （ゲートウェイが存在しないケース）
     *
     * @param schema スキーマ情報
     * @return 期待値
     */
    private List<Map<String, String>> getExpectedSequenceFlowDataNoGateway(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"testWorkFlow1", "1", "flow1", "startevent1", "usertask1", "", "to UserTask1"},
                {"testWorkFlow1", "1", "flow2", "usertask1", "usertask2", "", "to UserTask2"},
                {"testWorkFlow1", "1", "flow3", "usertask2", "usertask3", "", "to UserTask3"},
                {"testWorkFlow1", "1", "flow5", "usertask3", "endevent1", "", "to End1"},
                {"testWorkFlow1", "1", "flow6", "boundarymessage1", "usertask1", "", "to UserTask1 from Boundary Event"},
                {"testWorkFlow1", "1", "flow8", "boundarymessage2", "usertask2", "", "to UserTask2 from Boundary Event"}
        };
        List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
        for (String[] e : exp) {
            Map<String, String> row = new HashMap<String, String>();
            row.put(schema.getWorkflowIdColumnName(), e[0]);
            row.put(schema.getVersionColumnName(), e[1]);
            row.put(schema.getSequenceFlowIdColumnName(), e[2]);
            row.put(schema.getSourceFlowNodeIdColumnName(), e[3]);
            row.put(schema.getTargetFlowNodeIdColumnName(), e[4]);
            row.put(schema.getFlowProceedConditionColumnName(), e[5]);
            row.put(schema.getSequenceFlowNameColumnName(), e[6]);
            ret.add(row);
        }
        return ret;
    }

    /**
     * シーケンスフローの期待値を取得する。
     * （境界イベントが存在しないケース）
     *
     * @param schema スキーマ情報
     * @return 期待値
     */
    private List<Map<String, String>> getExpectedSequenceFlowDataNoBoundaryEvent(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"testWorkFlow1", "1", "flow1", "startevent1", "usertask1", "", "to UserTask1"},
                {"testWorkFlow1", "1", "flow2", "usertask1", "usertask2", "", "to UserTask2"},
                {"testWorkFlow1", "1", "flow3", "usertask2", "usertask3", "", "to UserTask3"},
                {"testWorkFlow1", "1", "flow4", "usertask3", "exclusivegateway1", "", "to Exclusive Gateway1"},
                {"testWorkFlow1", "1", "flow5", "exclusivegateway1", "endevent1", "nablarch.tool.workflow.Condition3", "to End1"},
                {"testWorkFlow1", "1", "flow7", "exclusivegateway1", "usertask2", "nablarch.tool.workflow.Condition3", "to UserTask2 from Gateway"}
        };
        List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
        for (String[] e : exp) {
            Map<String, String> row = new HashMap<String, String>();
            row.put(schema.getWorkflowIdColumnName(), e[0]);
            row.put(schema.getVersionColumnName(), e[1]);
            row.put(schema.getSequenceFlowIdColumnName(), e[2]);
            row.put(schema.getSourceFlowNodeIdColumnName(), e[3]);
            row.put(schema.getTargetFlowNodeIdColumnName(), e[4]);
            row.put(schema.getFlowProceedConditionColumnName(), e[5]);
            row.put(schema.getSequenceFlowNameColumnName(), e[6]);
            ret.add(row);
        }
        return ret;
    }

    /**
     * 環境設定ファイルで設定したパスにブックが存在する場合、
     * 既存ファイルを削除し、ブックが新規作成されることを確認。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void testOverrideAlreadyExistBook() throws Exception {
        // 事前処理
        String outputFilePath = "src/test/work/MASTER_DATA_WF_ALREADY_EXIST.xls";
        System.setProperty("outputFilePath", outputFilePath);
        String uri = "classpath:nablarch/tool/workflow/WorkflowDefinitionGeneratorTest.xml";
        DiContainer container = new DiContainer(new XmlComponentDefinitionLoader(uri));

        // 出力ファイル名と同じ名前のブックを作成
        HSSFWorkbook book = new HSSFWorkbook();
        book.createSheet("alreadySheet").createRow(0).createCell(0).setCellValue("cellValue");
        PoiUtil.save(book, temporaryFolder.getRoot().getPath() + '/' + "workflowDefinitionData.xml");

        // テスト実施
        sut.write(createInputForSortTest(), temporaryFolder.getRoot());

        // 検証
        HSSFWorkbook outBook = new HSSFWorkbook(new FileInputStream(temporaryFolder.getRoot().getPath() + "/workflowDefinitionData.xls"));
        SimpleTableReader tableReader = new SimpleTableReader().setHeaderRowNum(1);

        WorkflowDefinitionSchema schema = systemRepositoryResource.getComponent("workflowDefinitionSchema");
        assertThat(tableReader.read(outBook.getSheet(schema.getWorkflowDefinitionTableName())), is(getExpectedWorkflowDefinitionDataForSort(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getLaneTableName())), is(getExpectedLaneDataForSort(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getFlowNodeTableName())), is(getExpectedFlowNodeDataForSort(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getTaskTableName())), is(getExpectedTaskDataForSort(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getBoundaryEventTableName())), is(getExpectedBoundaryEventDataForSort(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getEventTableName())), is(getExpectedEventDataForSort(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getGatewayTableName())), is(getExpectedGatewayDataForSort(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getEventTriggerTableName())), is(getExpectedBoundaryEventTriggerDataForSort(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getSequenceFlowTableName())), is(getExpectedSequenceFlowDataForSort(schema)));
    }

    /**
     * 生成されたプロセス定義データがソートされていることを確認。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void testSort() throws Exception {
        // テスト実施
        sut.write(createInputForSortTest(), temporaryFolder.getRoot());

        // 検証
        HSSFWorkbook outBook = new HSSFWorkbook(new FileInputStream(temporaryFolder.getRoot().getPath() + "/workflowDefinitionData.xls"));
        SimpleTableReader tableReader = new SimpleTableReader().setHeaderRowNum(1);

        WorkflowDefinitionSchema schema = systemRepositoryResource.getComponent("workflowDefinitionSchema");
        assertThat(tableReader.read(outBook.getSheet(schema.getWorkflowDefinitionTableName())), is(getExpectedWorkflowDefinitionDataForSort(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getLaneTableName())), is(getExpectedLaneDataForSort(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getFlowNodeTableName())), is(getExpectedFlowNodeDataForSort(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getTaskTableName())), is(getExpectedTaskDataForSort(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getBoundaryEventTableName())), is(getExpectedBoundaryEventDataForSort(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getEventTableName())), is(getExpectedEventDataForSort(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getGatewayTableName())), is(getExpectedGatewayDataForSort(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getEventTriggerTableName())), is(getExpectedBoundaryEventTriggerDataForSort(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getSequenceFlowTableName())), is(getExpectedSequenceFlowDataForSort(schema)));
    }

    /**
     * ソートテスト用入力値を生成する。
     *
     * @return 入力値
     */
    private List<WorkflowDefinition> createInputForSortTest() {
        List<WorkflowDefinition> list = new ArrayList<WorkflowDefinition>();
        list.add(createInputForSortTest3());
        list.add(createInputForSortTest1());
        list.add(createInputForSortTest2ver3());
        list.add(createInputForSortTest2ver1());
        list.add(createInputForSortTest2ver2());
        return list;
    }

    /**
     * ソートテスト用WorkflowDefinitionを生成する。
     *
     * @return ソートテスト用WorkflowDefinition
     */
    private WorkflowDefinition createInputForSortTest1() {
        WorkflowDefinition workflowDefinition = new WorkflowDefinition("testWorkFlow1", 1, "process_poolName2", "20140804");

        List<Task> tasks = new ArrayList<Task>();
        tasks.add(new WorkflowDefinitionGeneratorTask("usertask1", "flowNodeName2", "laneId2", "NONE", "nablarch.tool.workflow2"));
        workflowDefinition.setTasks(tasks);

        List<Lane> lanes = new ArrayList<Lane>();
        lanes.add(new Lane("laneId1", "laneName1"));
        workflowDefinition.setLanes(lanes);

        List<Event> events = new ArrayList<Event>();
        events.add(new Event("startevent1", "flowNodeName3", "laneId3", "START", new ArrayList<SequenceFlow>()));
        workflowDefinition.setEvents(events);

        List<Gateway> gateways = new ArrayList<Gateway>();
        gateways.add(new Gateway("exclusivegateway1", "flowNodeName6", "laneId5", "EXCLUSIVE", new ArrayList<SequenceFlow>()));
        workflowDefinition.setGateways(gateways);

        List<BoundaryEvent> boundaryEvents = new ArrayList<BoundaryEvent>();
        boundaryEvents.add(new BoundaryEvent("boundarymessage1", "flowNodeName4", "laneId4", "MG001", "MG001", "usertask2", new ArrayList<SequenceFlow>()));
        workflowDefinition.setBoundaryEvents(boundaryEvents);

        List<SequenceFlow> sequenceFlows = new ArrayList<SequenceFlow>();
        sequenceFlows.add(new WorkflowDefinitionGeneratorSequenceFlow("flow1", "to UserTask1", "startevent1", "usertask1", "nablarch.tool.workflow2"));
        workflowDefinition.setSequenceFlows(sequenceFlows);
        return workflowDefinition;
    }

    /**
     * ソートテスト用WorkflowDefinitionを生成する。
     *
     * @return ソートテスト用WorkflowDefinition
     */
    private WorkflowDefinition createInputForSortTest2ver1() {
        WorkflowDefinition workflowDefinition = new WorkflowDefinition("testWorkFlow2", 1, "process_poolName3", "20140805");

        List<Task> tasks = new ArrayList<Task>();
        tasks.add(new WorkflowDefinitionGeneratorTask("usertask1", "flowNodeName6", "laneId6", "NONE", "nablarch.tool.workflow3"));
        workflowDefinition.setTasks(tasks);

        List<Lane> lanes = new ArrayList<Lane>();
        lanes.add(new Lane("laneId1", "laneName2"));
        workflowDefinition.setLanes(lanes);

        List<Event> events = new ArrayList<Event>();
        events.add(new Event("startevent1", "flowNodeName16", "laneId16", "START", new ArrayList<SequenceFlow>()));
        workflowDefinition.setEvents(events);

        List<Gateway> gateways = new ArrayList<Gateway>();
        gateways.add(new Gateway("exclusivegateway1", "flowNodeName21", "laneId21", "EXCLUSIVE", new ArrayList<SequenceFlow>()));
        workflowDefinition.setGateways(gateways);

        List<BoundaryEvent> boundaryEvents = new ArrayList<BoundaryEvent>();
        boundaryEvents.add(new BoundaryEvent("boundarymessage1", "flowNodeName11", "laneId11", "MG001", "MG001", "usertask3", new ArrayList<SequenceFlow>()));
        workflowDefinition.setBoundaryEvents(boundaryEvents);

        List<SequenceFlow> sequenceFlows = new ArrayList<SequenceFlow>();
        sequenceFlows.add(new WorkflowDefinitionGeneratorSequenceFlow("flow1", "to UserTask2", "startevent2", "usertask2", "nablarch.tool.workflow3"));
        workflowDefinition.setSequenceFlows(sequenceFlows);
        return workflowDefinition;
    }

    /**
     * ソートテスト用WorkflowDefinitionを生成する。
     *
     * @return ソートテスト用WorkflowDefinition
     */
    private WorkflowDefinition createInputForSortTest2ver2() {
        WorkflowDefinition workflowDefinition = new WorkflowDefinition("testWorkFlow2", 2, "process_poolName4", "20140806");

        List<Task> tasks = new ArrayList<Task>();
        tasks.add(new WorkflowDefinitionGeneratorTask("usertask3", "flowNodeName9", "laneId9", "NONE", "nablarch.tool.workflow6"));
        tasks.add(new WorkflowDefinitionGeneratorTask("usertask1", "flowNodeName7", "laneId7", "NONE", "nablarch.tool.workflow4"));
        tasks.add(new WorkflowDefinitionGeneratorTask("usertask2", "flowNodeName8", "laneId8", "NONE", "nablarch.tool.workflow5"));
        workflowDefinition.setTasks(tasks);

        List<Lane> lanes = new ArrayList<Lane>();
        lanes.add(new Lane("laneId3", "laneName5"));
        lanes.add(new Lane("laneId1", "laneName3"));
        lanes.add(new Lane("laneId2", "laneName4"));
        workflowDefinition.setLanes(lanes);

        List<Event> events = new ArrayList<Event>();
        events.add(new Event("startevent3", "flowNodeName19", "laneId19", "START", new ArrayList<SequenceFlow>()));
        events.add(new Event("startevent1", "flowNodeName17", "laneId17", "START", new ArrayList<SequenceFlow>()));
        events.add(new Event("startevent2", "flowNodeName18", "laneId18", "START", new ArrayList<SequenceFlow>()));
        workflowDefinition.setEvents(events);

        List<Gateway> gateways = new ArrayList<Gateway>();
        gateways.add(new Gateway("exclusivegateway3", "flowNodeName24", "laneId24", "EXCLUSIVE", new ArrayList<SequenceFlow>()));
        gateways.add(new Gateway("exclusivegateway1", "flowNodeName22", "laneId22", "EXCLUSIVE", new ArrayList<SequenceFlow>()));
        gateways.add(new Gateway("exclusivegateway2", "flowNodeName23", "laneId23", "EXCLUSIVE", new ArrayList<SequenceFlow>()));
        workflowDefinition.setGateways(gateways);

        List<BoundaryEvent> boundaryEvents = new ArrayList<BoundaryEvent>();
        boundaryEvents.add(new BoundaryEvent("boundarymessage3", "flowNodeName14", "laneId14", "MG003", "MG003", "usertask6", new ArrayList<SequenceFlow>()));
        boundaryEvents.add(new BoundaryEvent("boundarymessage1", "flowNodeName12", "laneId12", "MG001", "MG001", "usertask4", new ArrayList<SequenceFlow>()));
        boundaryEvents.add(new BoundaryEvent("boundarymessage2", "flowNodeName13", "laneId13", "MG002", "MG002", "usertask5", new ArrayList<SequenceFlow>()));
        workflowDefinition.setBoundaryEvents(boundaryEvents);

        List<SequenceFlow> sequenceFlows = new ArrayList<SequenceFlow>();
        sequenceFlows.add(new WorkflowDefinitionGeneratorSequenceFlow("flow3", "to UserTask5", "startevent5", "usertask5", "nablarch.tool.workflow6"));
        sequenceFlows.add(new WorkflowDefinitionGeneratorSequenceFlow("flow1", "to UserTask3", "startevent3", "usertask3", "nablarch.tool.workflow4"));
        sequenceFlows.add(new WorkflowDefinitionGeneratorSequenceFlow("flow2", "to UserTask4", "startevent4", "usertask4", "nablarch.tool.workflow5"));
        workflowDefinition.setSequenceFlows(sequenceFlows);
        return workflowDefinition;
    }

    /**
     * ソートテスト用WorkflowDefinitionを生成する。
     *
     * @return ソートテスト用WorkflowDefinition
     */
    private WorkflowDefinition createInputForSortTest2ver3() {
        WorkflowDefinition workflowDefinition = new WorkflowDefinition("testWorkFlow2", 3, "process_poolName5", "20140807");

        List<Task> tasks = new ArrayList<Task>();
        tasks.add(new WorkflowDefinitionGeneratorTask("usertask1", "flowNodeName10", "laneId10", "NONE", "nablarch.tool.workflow7"));
        workflowDefinition.setTasks(tasks);

        List<Lane> lanes = new ArrayList<Lane>();
        lanes.add(new Lane("laneId1", "laneName6"));
        workflowDefinition.setLanes(lanes);

        List<Event> events = new ArrayList<Event>();
        events.add(new Event("startevent1", "flowNodeName20", "laneId20", "START", new ArrayList<SequenceFlow>()));
        workflowDefinition.setEvents(events);

        List<Gateway> gateways = new ArrayList<Gateway>();
        gateways.add(new Gateway("exclusivegateway1", "flowNodeName25", "laneId25", "EXCLUSIVE", new ArrayList<SequenceFlow>()));
        workflowDefinition.setGateways(gateways);

        List<BoundaryEvent> boundaryEvents = new ArrayList<BoundaryEvent>();
        boundaryEvents.add(new BoundaryEvent("boundarymessage1", "flowNodeName15", "laneId15", "MG001", "MG001", "usertask7", new ArrayList<SequenceFlow>()));
        workflowDefinition.setBoundaryEvents(boundaryEvents);

        List<SequenceFlow> sequenceFlows = new ArrayList<SequenceFlow>();
        sequenceFlows.add(new WorkflowDefinitionGeneratorSequenceFlow("flow1", "to UserTask6", "startevent6", "usertask6", "nablarch.tool.workflow7"));
        workflowDefinition.setSequenceFlows(sequenceFlows);
        return workflowDefinition;
    }

    /**
     * ソートテスト用WorkflowDefinitionを生成する。
     *
     * @return ソートテスト用WorkflowDefinition
     */
    private WorkflowDefinition createInputForSortTest3() {
        WorkflowDefinition workflowDefinition = new WorkflowDefinition("testWorkFlow3", 1, "process_poolName1", "20140803");

        List<Task> tasks = new ArrayList<Task>();
        tasks.add(new WorkflowDefinitionGeneratorTask("usertask1", "flowNodeName26", "laneId26", "NONE", "nablarch.tool.workflow1"));
        workflowDefinition.setTasks(tasks);

        List<Lane> lanes = new ArrayList<Lane>();
        lanes.add(new Lane("laneId1", "laneName0"));
        workflowDefinition.setLanes(lanes);

        List<Event> events = new ArrayList<Event>();
        events.add(new Event("startevent1", "flowNodeName28", "laneId28", "START", new ArrayList<SequenceFlow>()));
        workflowDefinition.setEvents(events);

        List<Gateway> gateways = new ArrayList<Gateway>();
        gateways.add(new Gateway("exclusivegateway1", "flowNodeName1", "laneId1", "EXCLUSIVE", new ArrayList<SequenceFlow>()));
        workflowDefinition.setGateways(gateways);

        List<BoundaryEvent> boundaryEvents = new ArrayList<BoundaryEvent>();
        boundaryEvents.add(new BoundaryEvent("boundarymessage1", "flowNodeName27", "laneId27", "MG001", "MG001", "usertask1", new ArrayList<SequenceFlow>()));
        workflowDefinition.setBoundaryEvents(boundaryEvents);

        List<SequenceFlow> sequenceFlows = new ArrayList<SequenceFlow>();
        sequenceFlows.add(new WorkflowDefinitionGeneratorSequenceFlow("flow1", "to UserTask0", "startevent0", "usertask0", "nablarch.tool.workflow1"));
        workflowDefinition.setSequenceFlows(sequenceFlows);
        return workflowDefinition;
    }

    /**
     * ワークフロー定義のソートテスト用期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return ワークフロー定義のソートテスト用期待値
     */
    private List<Map<String, String>> getExpectedWorkflowDefinitionDataForSort(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"testWorkFlow3", "1", "process_poolName1", "20140803"},
                {"testWorkFlow1", "1", "process_poolName2", "20140804"},
                {"testWorkFlow2", "3", "process_poolName5", "20140807"},
                {"testWorkFlow2", "1", "process_poolName3", "20140805"},
                {"testWorkFlow2", "2", "process_poolName4", "20140806"}
        };
        List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
        for (String[] e : exp) {
            Map<String, String> row = new HashMap<String, String>();
            row.put(schema.getWorkflowIdColumnName(), e[0]);
            row.put(schema.getVersionColumnName(), e[1]);
            row.put(schema.getWorkflowNameColumnName(), e[2]);
            row.put(schema.getEffectiveDateColumnName(), e[3]);
            ret.add(row);
        }
        return ret;
    }

    /**
     * タスクのソートテスト用期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return ソートテスト用期待値
     */
    private List<Map<String, String>> getExpectedTaskDataForSort(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"testWorkFlow3", "1", "usertask1", "NONE", "nablarch.tool.workflow1"},
                {"testWorkFlow1", "1", "usertask1", "NONE", "nablarch.tool.workflow2"},
                {"testWorkFlow2", "3", "usertask1", "NONE", "nablarch.tool.workflow7"},
                {"testWorkFlow2", "1", "usertask1", "NONE", "nablarch.tool.workflow3"},
                {"testWorkFlow2", "2", "usertask1", "NONE", "nablarch.tool.workflow4"},
                {"testWorkFlow2", "2", "usertask2", "NONE", "nablarch.tool.workflow5"},
                {"testWorkFlow2", "2", "usertask3", "NONE", "nablarch.tool.workflow6"}
        };
        List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
        for (String[] e : exp) {
            Map<String, String> row = new HashMap<String, String>();
            row.put(schema.getWorkflowIdColumnName(), e[0]);
            row.put(schema.getVersionColumnName(), e[1]);
            row.put(schema.getFlowNodeIdColumnName(), e[2]);
            row.put(schema.getMultiInstanceTypeColumnName(), e[3]);
            row.put(schema.getCompletionConditionColumnName(), e[4]);
            ret.add(row);
        }
        return ret;
    }

    /**
     * レーンのソートテスト用期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return ソートテスト用期待値
     */
    private List<Map<String, String>> getExpectedLaneDataForSort(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"testWorkFlow3", "1", "laneId1", "laneName0"},
                {"testWorkFlow1", "1", "laneId1", "laneName1"},
                {"testWorkFlow2", "3", "laneId1", "laneName6"},
                {"testWorkFlow2", "1", "laneId1", "laneName2"},
                {"testWorkFlow2", "2", "laneId1", "laneName3"},
                {"testWorkFlow2", "2", "laneId2", "laneName4"},
                {"testWorkFlow2", "2", "laneId3", "laneName5"}
        };
        List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
        for (String[] e : exp) {
            Map<String, String> row = new HashMap<String, String>();
            row.put(schema.getWorkflowIdColumnName(), e[0]);
            row.put(schema.getVersionColumnName(), e[1]);
            row.put(schema.getLaneIdColumnName(), e[2]);
            row.put(schema.getLaneNameColumnName(), e[3]);
            ret.add(row);
        }
        return ret;
    }

    /**
     * フローノードのソートテスト用期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return ソートテスト用期待値
     */
    private List<Map<String, String>> getExpectedFlowNodeDataForSort(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"testWorkFlow3", "1", "startevent1", "laneId28", "flowNodeName28"},
                {"testWorkFlow3", "1", "boundarymessage1", "laneId27", "flowNodeName27"},
                {"testWorkFlow3", "1", "usertask1", "laneId26", "flowNodeName26"},
                {"testWorkFlow3", "1", "exclusivegateway1", "laneId1", "flowNodeName1"},
                {"testWorkFlow1", "1", "startevent1", "laneId3", "flowNodeName3"},
                {"testWorkFlow1", "1", "boundarymessage1", "laneId4", "flowNodeName4"},
                {"testWorkFlow1", "1", "usertask1", "laneId2", "flowNodeName2"},
                {"testWorkFlow1", "1", "exclusivegateway1", "laneId5", "flowNodeName6"},
                {"testWorkFlow2", "3", "startevent1", "laneId20", "flowNodeName20"},
                {"testWorkFlow2", "3", "boundarymessage1", "laneId15", "flowNodeName15"},
                {"testWorkFlow2", "3", "usertask1", "laneId10", "flowNodeName10"},
                {"testWorkFlow2", "3", "exclusivegateway1", "laneId25", "flowNodeName25"},
                {"testWorkFlow2", "1", "startevent1", "laneId16", "flowNodeName16"},
                {"testWorkFlow2", "1", "boundarymessage1", "laneId11", "flowNodeName11"},
                {"testWorkFlow2", "1", "usertask1", "laneId6", "flowNodeName6"},
                {"testWorkFlow2", "1", "exclusivegateway1", "laneId21", "flowNodeName21"},
                {"testWorkFlow2", "2", "startevent1", "laneId17", "flowNodeName17"},
                {"testWorkFlow2", "2", "startevent2", "laneId18", "flowNodeName18"},
                {"testWorkFlow2", "2", "startevent3", "laneId19", "flowNodeName19"},
                {"testWorkFlow2", "2", "boundarymessage1", "laneId12", "flowNodeName12"},
                {"testWorkFlow2", "2", "boundarymessage2", "laneId13", "flowNodeName13"},
                {"testWorkFlow2", "2", "boundarymessage3", "laneId14", "flowNodeName14"},
                {"testWorkFlow2", "2", "usertask1", "laneId7", "flowNodeName7"},
                {"testWorkFlow2", "2", "usertask2", "laneId8", "flowNodeName8"},
                {"testWorkFlow2", "2", "usertask3", "laneId9", "flowNodeName9"},
                {"testWorkFlow2", "2", "exclusivegateway1", "laneId22", "flowNodeName22"},
                {"testWorkFlow2", "2", "exclusivegateway2", "laneId23", "flowNodeName23"},
                {"testWorkFlow2", "2", "exclusivegateway3", "laneId24", "flowNodeName24"}
        };
        List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
        for (String[] e : exp) {
            Map<String, String> row = new HashMap<String, String>();
            row.put(schema.getWorkflowIdColumnName(), e[0]);
            row.put(schema.getVersionColumnName(), e[1]);
            row.put(schema.getFlowNodeIdColumnName(), e[2]);
            row.put(schema.getLaneIdColumnName(), e[3]);
            row.put(schema.getFlowNodeNameColumnName(), e[4]);
            ret.add(row);
        }
        return ret;
    }

    /**
     * 境界イベントのソートテスト用期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return ソートテスト用期待値
     */
    private List<Map<String, String>> getExpectedBoundaryEventDataForSort(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"testWorkFlow3", "1", "boundarymessage1", "MG001", "usertask1"},
                {"testWorkFlow1", "1", "boundarymessage1", "MG001", "usertask2"},
                {"testWorkFlow2", "3", "boundarymessage1", "MG001", "usertask7"},
                {"testWorkFlow2", "1", "boundarymessage1", "MG001", "usertask3"},
                {"testWorkFlow2", "2", "boundarymessage1", "MG001", "usertask4"},
                {"testWorkFlow2", "2", "boundarymessage2", "MG002", "usertask5"},
                {"testWorkFlow2", "2", "boundarymessage3", "MG003", "usertask6"}
        };
        List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
        for (String[] e : exp) {
            Map<String, String> row = new HashMap<String, String>();
            row.put(schema.getWorkflowIdColumnName(), e[0]);
            row.put(schema.getVersionColumnName(), e[1]);
            row.put(schema.getFlowNodeIdColumnName(), e[2]);
            row.put(schema.getBoundaryEventTriggerIdColumnName(), e[3]);
            row.put(schema.getAttachedTaskIdColumnName(), e[4]);
            ret.add(row);
        }
        return ret;
    }

    /**
     * イベントのソートテスト用期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return ソートテスト用期待値
     */
    private List<Map<String, String>> getExpectedEventDataForSort(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"testWorkFlow3", "1", "startevent1", "START"},
                {"testWorkFlow1", "1", "startevent1", "START"},
                {"testWorkFlow2", "3", "startevent1", "START"},
                {"testWorkFlow2", "1", "startevent1", "START"},
                {"testWorkFlow2", "2", "startevent1", "START"},
                {"testWorkFlow2", "2", "startevent2", "START"},
                {"testWorkFlow2", "2", "startevent3", "START"}
        };
        List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
        for (String[] e : exp) {
            Map<String, String> row = new HashMap<String, String>();
            row.put(schema.getWorkflowIdColumnName(), e[0]);
            row.put(schema.getVersionColumnName(), e[1]);
            row.put(schema.getFlowNodeIdColumnName(), e[2]);
            row.put(schema.getEventTypeColumnName(), e[3]);
            ret.add(row);
        }
        return ret;
    }

    /**
     * ゲートウェイのソートテスト用期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return ソートテスト用期待値
     */
    private List<Map<String, String>> getExpectedGatewayDataForSort(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"testWorkFlow3", "1", "exclusivegateway1", "EXCLUSIVE"},
                {"testWorkFlow1", "1", "exclusivegateway1", "EXCLUSIVE"},
                {"testWorkFlow2", "3", "exclusivegateway1", "EXCLUSIVE"},
                {"testWorkFlow2", "1", "exclusivegateway1", "EXCLUSIVE"},
                {"testWorkFlow2", "2", "exclusivegateway1", "EXCLUSIVE"},
                {"testWorkFlow2", "2", "exclusivegateway2", "EXCLUSIVE"},
                {"testWorkFlow2", "2", "exclusivegateway3", "EXCLUSIVE"}
        };
        List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
        for (String[] e : exp) {
            Map<String, String> row = new HashMap<String, String>();
            row.put(schema.getWorkflowIdColumnName(), e[0]);
            row.put(schema.getVersionColumnName(), e[1]);
            row.put(schema.getFlowNodeIdColumnName(), e[2]);
            row.put(schema.getGatewayTypeColumnName(), e[3]);
            ret.add(row);
        }
        return ret;
    }

    /**
     * 境界イベントトリガーのソートテスト用期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return ソートテスト用期待値
     */
    private List<Map<String, String>> getExpectedBoundaryEventTriggerDataForSort(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"testWorkFlow3", "1", "MG001", "MG001"},
                {"testWorkFlow1", "1", "MG001", "MG001"},
                {"testWorkFlow2", "3", "MG001", "MG001"},
                {"testWorkFlow2", "1", "MG001", "MG001"},
                {"testWorkFlow2", "2", "MG001", "MG001"},
                {"testWorkFlow2", "2", "MG002", "MG002"},
                {"testWorkFlow2", "2", "MG003", "MG003"}
        };
        List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
        for (String[] e : exp) {
            Map<String, String> row = new HashMap<String, String>();
            row.put(schema.getWorkflowIdColumnName(), e[0]);
            row.put(schema.getVersionColumnName(), e[1]);
            row.put(schema.getBoundaryEventTriggerIdColumnName(), e[2]);
            row.put(schema.getBoundaryEventTriggerNameColumnName(), e[3]);
            ret.add(row);
        }
        return ret;
    }

    /**
     * シーケンスフローのソートテスト用期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return ソートテスト用期待値
     */
    private List<Map<String, String>> getExpectedSequenceFlowDataForSort(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"testWorkFlow3", "1", "flow1", "startevent0", "usertask0", "nablarch.tool.workflow1", "to UserTask0"},
                {"testWorkFlow1", "1", "flow1", "startevent1", "usertask1", "nablarch.tool.workflow2", "to UserTask1"},
                {"testWorkFlow2", "3", "flow1", "startevent6", "usertask6", "nablarch.tool.workflow7", "to UserTask6"},
                {"testWorkFlow2", "1", "flow1", "startevent2", "usertask2", "nablarch.tool.workflow3", "to UserTask2"},
                {"testWorkFlow2", "2", "flow1", "startevent3", "usertask3", "nablarch.tool.workflow4", "to UserTask3"},
                {"testWorkFlow2", "2", "flow2", "startevent4", "usertask4", "nablarch.tool.workflow5", "to UserTask4"},
                {"testWorkFlow2", "2", "flow3", "startevent5", "usertask5", "nablarch.tool.workflow6", "to UserTask5"}
        };
        List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
        for (String[] e : exp) {
            Map<String, String> row = new HashMap<String, String>();
            row.put(schema.getWorkflowIdColumnName(), e[0]);
            row.put(schema.getVersionColumnName(), e[1]);
            row.put(schema.getSequenceFlowIdColumnName(), e[2]);
            row.put(schema.getSourceFlowNodeIdColumnName(), e[3]);
            row.put(schema.getTargetFlowNodeIdColumnName(), e[4]);
            row.put(schema.getFlowProceedConditionColumnName(), e[5]);
            row.put(schema.getSequenceFlowNameColumnName(), e[6]);
            ret.add(row);
        }
        return ret;
    }
}
