package nablarch.tool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nablarch.common.databind.ObjectMapper;
import nablarch.common.databind.ObjectMapperFactory;
import nablarch.common.databind.csv.CsvDataBindConfig;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.FileUtil;
import nablarch.integration.workflow.definition.BoundaryEvent;
import nablarch.integration.workflow.definition.Event;
import nablarch.integration.workflow.definition.Gateway;
import nablarch.integration.workflow.definition.Lane;
import nablarch.integration.workflow.definition.SequenceFlow;
import nablarch.integration.workflow.definition.Task;
import nablarch.integration.workflow.definition.WorkflowDefinition;
import nablarch.integration.workflow.definition.loader.WorkflowDefinitionSchema;
import nablarch.tool.workflow.WorkflowDefinitionGeneratorSequenceFlow;
import nablarch.tool.workflow.WorkflowDefinitionGeneratorTask;

/**
 * {@link WorkflowDefinition}に定義されたデータをCSVファイルに出力する{@link DataWriter}実装クラス。
 *
 * @author Naoki Yamamoto
 */
public class CsvDataWriter implements DataWriter {

    @Override
    public void write(final List<WorkflowDefinition> workflowDefinitions, final File outputDir) {
        final WorkflowDefinitionSchema schema = SystemRepository.get("workflowDefinitionSchema");

        try {
            writeWorkflowDefinitionRecord(workflowDefinitions, schema, outputDir);
            writeLaneRecord(workflowDefinitions, schema, outputDir);
            writeFlowNodeRecord(workflowDefinitions, schema, outputDir);
            writeEventRecord(workflowDefinitions, schema, outputDir);
            writeGatewayRecord(workflowDefinitions, schema, outputDir);
            writeTaskRecord(workflowDefinitions, schema, outputDir);
            writeBoundaryEventRecord(workflowDefinitions, schema, outputDir);
            writeSequenceFlowRecord(workflowDefinitions, schema, outputDir);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * ワークフロー定義テーブルのデータを出力する。
     * @param definitions ワークフロー定義リスト
     * @param schema ワークフロー定義情報
     * @param outputDir 出力先ディレクトリ
     * @throws IOException ファイルを開くことができなかった場合
     */
    private void writeWorkflowDefinitionRecord(final List<WorkflowDefinition> definitions, final WorkflowDefinitionSchema schema, final File outputDir) throws IOException {
        final File file = new File(outputDir, schema.getWorkflowDefinitionTableName() + ".csv");
        ObjectMapper<Map> mapper = null;
        try {
            mapper = ObjectMapperFactory.create(Map.class, new FileOutputStream(file), CsvDataBindConfig.DEFAULT
                    .withCharset(Charset.forName("Windows-31j"))
                    .withHeaderTitles(
                            schema.getWorkflowIdColumnName(),
                            schema.getVersionColumnName(),
                            schema.getWorkflowNameColumnName(),
                            schema.getEffectiveDateColumnName()));

            for (WorkflowDefinition definition : definitions) {
                final Map<String, Object> map = new HashMap<String, Object>();
                map.put(schema.getWorkflowIdColumnName(), definition.getWorkflowId());
                map.put(schema.getVersionColumnName(), definition.getVersion());
                map.put(schema.getWorkflowNameColumnName(), definition.getWorkflowName());
                map.put(schema.getEffectiveDateColumnName(), definition.getEffectiveDate());
                mapper.write(map);
            }
        } finally {
            FileUtil.closeQuietly(mapper);
        }
    }

    /**
     * レーンテーブルのデータを出力する。
     * @param definitions ワークフロー定義リスト
     * @param schema ワークフロー定義情報
     * @param outputDir 出力先ディレクトリ
     * @throws IOException ファイルを開くことができなかった場合
     */
    private void writeLaneRecord(final List<WorkflowDefinition> definitions, final WorkflowDefinitionSchema schema, final File outputDir) throws IOException {
        final File file = new File(outputDir, schema.getLaneTableName() + ".csv");
        ObjectMapper<Map> mapper = null;
        try {
            mapper = ObjectMapperFactory.create(Map.class, new FileOutputStream(file), CsvDataBindConfig.DEFAULT
                    .withCharset(Charset.forName("Windows-31j"))
                    .withHeaderTitles(
                            schema.getWorkflowIdColumnName(),
                            schema.getVersionColumnName(),
                            schema.getLaneIdColumnName(),
                            schema.getLaneNameColumnName()));

            for (WorkflowDefinition definition : definitions) {
                for (Lane lane : definition.getLanes()) {
                    final Map<String, Object> map = new HashMap<String, Object>();
                    map.put(schema.getWorkflowIdColumnName(), definition.getWorkflowId());
                    map.put(schema.getVersionColumnName(), definition.getVersion());
                    map.put(schema.getLaneIdColumnName(), lane.getLaneId());
                    map.put(schema.getLaneNameColumnName(), lane.getLaneName());
                    mapper.write(map);
                }
            }
        } finally {
            FileUtil.closeQuietly(mapper);
        }
    }

    /**
     * フローノードテーブルのデータを出力する。
     * @param definitions ワークフロー定義リスト
     * @param schema ワークフロー定義情報
     * @param outputDir 出力先ディレクトリ
     * @throws IOException ファイルを開くことができなかった場合
     */
    private void writeFlowNodeRecord(final List<WorkflowDefinition> definitions, final WorkflowDefinitionSchema schema, final File outputDir) throws IOException {
        final File flowNodeFile = new File(outputDir, schema.getFlowNodeTableName() + ".csv");
        ObjectMapper<Map> flowNodeMapper = null;
        try {
            flowNodeMapper = ObjectMapperFactory.create(Map.class, new FileOutputStream(flowNodeFile),
                    CsvDataBindConfig.DEFAULT
                            .withCharset(Charset.forName("Windows-31j"))
                            .withHeaderTitles(
                                    schema.getWorkflowIdColumnName(),
                                    schema.getVersionColumnName(),
                                    schema.getFlowNodeIdColumnName(),
                                    schema.getLaneIdColumnName(),
                                    schema.getFlowNodeNameColumnName()));

            for (WorkflowDefinition definition : definitions) {
                for (Event event : definition.getEvents()) {
                    flowNodeMapper.write(createFlowNodeData(
                            schema,definition.getWorkflowId(), definition.getVersion(), event.getFlowNodeId(), event.getLaneId(), event.getFlowNodeName()));
                }
                for (Gateway gateway : definition.getGateways()) {
                    flowNodeMapper.write(createFlowNodeData(
                            schema,definition.getWorkflowId(), definition.getVersion(), gateway.getFlowNodeId(), gateway.getLaneId(), gateway.getFlowNodeName()));
                }
                for (Task task : definition.getTasks()) {
                    flowNodeMapper.write(createFlowNodeData(
                            schema,definition.getWorkflowId(), definition.getVersion(), task.getFlowNodeId(), task.getLaneId(), task.getFlowNodeName()));
                }
                for (BoundaryEvent boundary : definition.getBoundaryEvents()) {
                    flowNodeMapper.write(createFlowNodeData(
                            schema,definition.getWorkflowId(), definition.getVersion(), boundary.getFlowNodeId(), boundary.getLaneId(), boundary.getFlowNodeName()));
                }
            }
        } finally {
            FileUtil.closeQuietly(flowNodeMapper);
        }
    }

    /**
     * フローノードテーブルの1レコード分のデータを生成する。
     * @param schema ワークフロー定義情報
     * @param workflowId ワークフローID
     * @param version バージョン
     * @param flowNodeId フローノードID
     * @param laneId レーンID
     * @param flowNodeName フローノード名
     * @return フローノードテーブルのデータ
     */
    private Map<String, Object> createFlowNodeData(final WorkflowDefinitionSchema schema,
                                                   final String workflowId,
                                                   final int version,
                                                   final String flowNodeId,
                                                   final String laneId,
                                                   final String flowNodeName) {
        final Map<String, Object> flowNodeMap = new HashMap<String, Object>();
        flowNodeMap.put(schema.getWorkflowIdColumnName(), workflowId);
        flowNodeMap.put(schema.getVersionColumnName(), version);
        flowNodeMap.put(schema.getFlowNodeIdColumnName(), flowNodeId);
        flowNodeMap.put(schema.getLaneIdColumnName(), laneId);
        flowNodeMap.put(schema.getFlowNodeNameColumnName(), flowNodeName);
        return flowNodeMap;
    }

    /**
     * イベントテーブルのデータを出力する。
     * @param definitions ワークフロー定義リスト
     * @param schema ワークフロー定義情報
     * @param outputDir 出力先ディレクトリ
     * @throws IOException ファイルを開くことができなかった場合
     */
    private void writeEventRecord(final List<WorkflowDefinition> definitions, final WorkflowDefinitionSchema schema, final File outputDir) throws IOException {

        final File eventFile = new File(outputDir, schema.getEventTableName() + ".csv");

        ObjectMapper<Map> eventMapper = null;
        try {
            eventMapper = ObjectMapperFactory.create(Map.class, new FileOutputStream(eventFile), CsvDataBindConfig.DEFAULT
                    .withCharset(Charset.forName("Windows-31j"))
                    .withHeaderTitles(
                            schema.getWorkflowIdColumnName(),
                            schema.getVersionColumnName(),
                            schema.getFlowNodeIdColumnName(),
                            schema.getEventTypeColumnName()));

            for (WorkflowDefinition definition : definitions) {
                for (Event event : definition.getEvents()) {
                    final Map<String, Object> eventMap = new HashMap<String, Object>();
                    eventMap.put(schema.getWorkflowIdColumnName(), definition.getWorkflowId());
                    eventMap.put(schema.getVersionColumnName(), definition.getVersion());
                    eventMap.put(schema.getFlowNodeIdColumnName(), event.getFlowNodeId());
                    eventMap.put(schema.getEventTypeColumnName(), event.getEventType().toString());
                    eventMapper.write(eventMap);
                }
            }

        } finally {
            FileUtil.closeQuietly(eventMapper);
        }
    }

    /**
     * ゲートウェイテーブルのデータを出力する。
     * @param definitions ワークフロー定義リスト
     * @param schema ワークフロー定義情報
     * @param outputDir 出力先ディレクトリ
     * @throws IOException ファイルを開くことができなかった場合
     */
    private void writeGatewayRecord(final List<WorkflowDefinition> definitions, final WorkflowDefinitionSchema schema, final File outputDir) throws IOException {
        final File gatewayFile = new File(outputDir, schema.getGatewayTableName() + ".csv");
        ObjectMapper<Map> gatewayMapper = null;
        try {
            gatewayMapper = ObjectMapperFactory.create(Map.class, new FileOutputStream(gatewayFile), CsvDataBindConfig.DEFAULT
                    .withCharset(Charset.forName("Windows-31j"))
                    .withHeaderTitles(
                            schema.getWorkflowIdColumnName(),
                            schema.getVersionColumnName(),
                            schema.getFlowNodeIdColumnName(),
                            schema.getGatewayTypeColumnName()));

            for (WorkflowDefinition definition : definitions) {
                for (Gateway gateway : definition.getGateways()) {
                    final Map<String, Object> gatewayMap = new HashMap<String, Object>();
                    gatewayMap.put(schema.getWorkflowIdColumnName(), definition.getWorkflowId());
                    gatewayMap.put(schema.getVersionColumnName(), definition.getVersion());
                    gatewayMap.put(schema.getFlowNodeIdColumnName(), gateway.getFlowNodeId());
                    gatewayMap.put(schema.getGatewayTypeColumnName(), gateway.getGatewayType());
                    gatewayMapper.write(gatewayMap);
                }
            }
        } finally {
            FileUtil.closeQuietly(gatewayMapper);
        }
    }

    /**
     * タスクテーブルのデータを出力する。
     * @param definitions ワークフロー定義リスト
     * @param schema ワークフロー定義情報
     * @param outputDir 出力先ディレクトリ
     * @throws IOException ファイルを開くことができなかった場合
     */
    private void writeTaskRecord(final List<WorkflowDefinition> definitions, final WorkflowDefinitionSchema schema, final File outputDir) throws IOException {
        final File taskFile = new File(outputDir, schema.getTaskTableName() + ".csv");
        ObjectMapper<Map> taskMapper = null;
        try {
            taskMapper = ObjectMapperFactory.create(Map.class, new FileOutputStream(taskFile), CsvDataBindConfig.DEFAULT
                    .withCharset(Charset.forName("Windows-31j"))
                    .withHeaderTitles(
                            schema.getWorkflowIdColumnName(),
                            schema.getVersionColumnName(),
                            schema.getFlowNodeIdColumnName(),
                            schema.getMultiInstanceTypeColumnName(),
                            schema.getCompletionConditionColumnName()));

            for (WorkflowDefinition definition : definitions) {
                for (Task task : definition.getTasks()) {
                    final Map<String, Object> taskMap = new HashMap<String, Object>();
                    taskMap.put(schema.getWorkflowIdColumnName(), definition.getWorkflowId());
                    taskMap.put(schema.getVersionColumnName(), definition.getVersion());
                    taskMap.put(schema.getFlowNodeIdColumnName(), task.getFlowNodeId());
                    taskMap.put(schema.getMultiInstanceTypeColumnName(), task.getMultiInstanceType());
                    taskMap.put(schema.getCompletionConditionColumnName(), ((WorkflowDefinitionGeneratorTask) task).getCondition());
                    taskMapper.write(taskMap);
                }
            }
        } finally {
            FileUtil.closeQuietly(taskMapper);
        }
    }

    /**
     * 境界イベント、境界イベントトリガーテーブルのデータを出力する。
     * @param definitions ワークフロー定義リスト
     * @param schema ワークフロー定義情報
     * @param outputDir 出力先ディレクトリ
     * @throws IOException ファイルを開くことができなかった場合
     */
    private void writeBoundaryEventRecord(final List<WorkflowDefinition> definitions, final WorkflowDefinitionSchema schema, final File outputDir) throws IOException {
        final File boundaryFile = new File(outputDir, schema.getBoundaryEventTableName() + ".csv");
        final File triggerFile = new File(outputDir, schema.getEventTriggerTableName() + ".csv");
        ObjectMapper<Map> boundaryMapper = null;
        ObjectMapper<Map> triggerMapper = null;

        try {
            boundaryMapper = ObjectMapperFactory.create(Map.class, new FileOutputStream(boundaryFile),
                    CsvDataBindConfig.DEFAULT
                            .withCharset(Charset.forName("Windows-31j"))
                            .withHeaderTitles(
                                    schema.getWorkflowIdColumnName(),
                                    schema.getVersionColumnName(),
                                    schema.getFlowNodeIdColumnName(),
                                    schema.getBoundaryEventTriggerIdColumnName(),
                                    schema.getAttachedTaskIdColumnName()));

            triggerMapper = ObjectMapperFactory.create(Map.class, new FileOutputStream(triggerFile),
                    CsvDataBindConfig.DEFAULT
                            .withCharset(Charset.forName("Windows-31j"))
                            .withHeaderTitles(
                                    schema.getWorkflowIdColumnName(),
                                    schema.getVersionColumnName(),
                                    schema.getBoundaryEventTriggerIdColumnName(),
                                    schema.getBoundaryEventTriggerNameColumnName()));

            for (WorkflowDefinition definition : definitions) {
                for (BoundaryEvent boundary : definition.getBoundaryEvents()) {
                    final Map<String, Object> boundaryMap = new HashMap<String, Object>();
                    boundaryMap.put(schema.getWorkflowIdColumnName(), definition.getWorkflowId());
                    boundaryMap.put(schema.getVersionColumnName(), definition.getVersion());
                    boundaryMap.put(schema.getFlowNodeIdColumnName(), boundary.getFlowNodeId());
                    boundaryMap.put(schema.getBoundaryEventTriggerIdColumnName(), boundary.getBoundaryEventTriggerId());
                    boundaryMap.put(schema.getAttachedTaskIdColumnName(), boundary.getAttachedTaskId());
                    boundaryMapper.write(boundaryMap);

                    final Map<String, Object> triggerMap = new HashMap<String, Object>();
                    triggerMap.put(schema.getWorkflowIdColumnName(), definition.getWorkflowId());
                    triggerMap.put(schema.getVersionColumnName(), definition.getVersion());
                    triggerMap.put(schema.getBoundaryEventTriggerIdColumnName(), boundary.getBoundaryEventTriggerId());
                    triggerMap.put(schema.getBoundaryEventTriggerNameColumnName(), boundary.getBoundaryEventTriggerName());
                    triggerMapper.write(triggerMap);
                }
            }
        } finally {
            FileUtil.closeQuietly(boundaryMapper, triggerMapper);
        }
    }

    /**
     * シーケンスフローテーブルのデータを出力する。
     * @param definitions ワークフロー定義リスト
     * @param schema ワークフロー定義情報
     * @param outputDir 出力先ディレクトリ
     * @throws IOException ファイルを開くことができなかった場合
     */
    private void writeSequenceFlowRecord(final List<WorkflowDefinition> definitions, final WorkflowDefinitionSchema schema, final File outputDir) throws IOException {
        final File sequenceFlowFile = new File(outputDir, schema.getSequenceFlowTableName() + ".csv");
        ObjectMapper<Map> sequenceFlowMapper = null;

        try {
            sequenceFlowMapper = ObjectMapperFactory.create(Map.class, new FileOutputStream(sequenceFlowFile),
                    CsvDataBindConfig.DEFAULT
                            .withCharset(Charset.forName("Windows-31j"))
                            .withHeaderTitles(
                                    schema.getWorkflowIdColumnName(),
                                    schema.getVersionColumnName(),
                                    schema.getSequenceFlowIdColumnName(),
                                    schema.getSourceFlowNodeIdColumnName(),
                                    schema.getTargetFlowNodeIdColumnName(),
                                    schema.getFlowProceedConditionColumnName(),
                                    schema.getSequenceFlowNameColumnName()));

            for (WorkflowDefinition definition : definitions) {
                for (SequenceFlow sequenceFlow : definition.getSequenceFlows()) {
                    final Map<String, Object> sequenceFlowMap = new HashMap<String, Object>();
                    sequenceFlowMap.put(schema.getWorkflowIdColumnName(), definition.getWorkflowId());
                    sequenceFlowMap.put(schema.getVersionColumnName(), definition.getVersion());
                    sequenceFlowMap.put(schema.getSequenceFlowIdColumnName(), sequenceFlow.getSequenceFlowId());
                    sequenceFlowMap.put(schema.getSourceFlowNodeIdColumnName(), sequenceFlow.getSourceFlowNodeId());
                    sequenceFlowMap.put(schema.getTargetFlowNodeIdColumnName(), sequenceFlow.getTargetFlowNodeId());
                    sequenceFlowMap.put(schema.getFlowProceedConditionColumnName(), ((WorkflowDefinitionGeneratorSequenceFlow) sequenceFlow).getCondition());
                    sequenceFlowMap.put(schema.getSequenceFlowNameColumnName(), sequenceFlow.getSequenceFlowName());
                    sequenceFlowMapper.write(sequenceFlowMap);
                }
            }
        } finally {
            FileUtil.closeQuietly(sequenceFlowMapper);
        }
    }
}
