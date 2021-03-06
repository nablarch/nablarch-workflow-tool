package nablarch.tool.workflow;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import nablarch.core.repository.SystemRepository;
import nablarch.tool.DataWriter;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import nablarch.core.util.FileUtil;

import nablarch.integration.workflow.definition.BoundaryEvent;
import nablarch.integration.workflow.definition.Event;
import nablarch.integration.workflow.definition.FlowNode;
import nablarch.integration.workflow.definition.Gateway;
import nablarch.integration.workflow.definition.Lane;
import nablarch.integration.workflow.definition.SequenceFlow;
import nablarch.integration.workflow.definition.Task;
import nablarch.integration.workflow.definition.WorkflowDefinition;
import nablarch.integration.workflow.definition.loader.WorkflowDefinitionSchema;

/**
 * ワークフロー定義情報をExcelに出力するクラス。
 * <p>
 * マスターデータセットアップで利用可能な形式で出力する。
 *
 * @author Satoshi Taromaru
 * @since 1.4.2
 */
public class ExcelDataWriter implements DataWriter {

    /** 出力するExcelファイル名 */
    private static final String DATA_FILE_NAME = "workflowDefinitionData.xls";

    @Override
    public void write(final List<WorkflowDefinition> workflowDefinitions, final File outputDir) {
        final HSSFWorkbook book = new HSSFWorkbook();
        final WorkflowDefinitionSchema schema = SystemRepository.get("workflowDefinitionSchema");
        for (WorkflowDefinition definition : workflowDefinitions) {
            writeWorkflowDefinition(book, definition, schema);
            writeTask(book, definition, schema);
            writeLane(book, definition, schema);
            writeFlowNode(book, definition, schema);
            writeBoundaryEvent(book, definition, schema);
            writeEvent(book, definition, schema);
            writeGateway(book, definition, schema);
            writeBoundaryEventTrigger(book, definition, schema);
            writeSequenceFlow(book, definition, schema);
        }

        final OutputStream out = createOutputStream(outputDir);
        try {
            book.write(out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            FileUtil.closeQuietly(out);
        }
    }

    /**
     * 出力ストリームを生成する。
     *
     * @param outputDir 出力先ディレクトリ
     * @return 出力ストリーム
     */
    private OutputStream createOutputStream(final File outputDir) {
        try {
            return new FileOutputStream(new File(outputDir, DATA_FILE_NAME));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * ワークフロー定義テーブルの情報をブックに書き込む。
     * @param book               書き込み対象ブック
     * @param workflowDefinition ワークフロー定義
     * @param schema ワークフローテーブル定義
     */
    private void writeWorkflowDefinition(HSSFWorkbook book, WorkflowDefinition workflowDefinition, WorkflowDefinitionSchema schema) {
        String tableName = schema.getWorkflowDefinitionTableName();
        HSSFSheet sheet = getSheet(book, tableName, schema.getWorkflowIdColumnName(), schema.getVersionColumnName(),
                schema.getWorkflowNameColumnName(), schema.getEffectiveDateColumnName());

        HSSFRow dataRow = sheet.createRow(sheet.getLastRowNum() + 1);
        dataRow.createCell(0).setCellValue(workflowDefinition.getWorkflowId());
        dataRow.createCell(1).setCellValue(String.valueOf(workflowDefinition.getVersion()));
        dataRow.createCell(2).setCellValue(workflowDefinition.getWorkflowName());
        dataRow.createCell(3).setCellValue(workflowDefinition.getEffectiveDate());

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
    }

    /**
     * タスクテーブルの情報をブックに書き込む。
     * @param book               書き込み対象ブック
     * @param workflowDefinition ワークフロー定義
     * @param schema ワークフローテーブル定義
     */
    private void writeTask(HSSFWorkbook book, WorkflowDefinition workflowDefinition, WorkflowDefinitionSchema schema) {
        String tableName = schema.getTaskTableName();
        HSSFSheet sheet = getSheet(book, tableName, schema.getWorkflowIdColumnName(), schema.getVersionColumnName(),
                schema.getFlowNodeIdColumnName(), schema.getMultiInstanceTypeColumnName(),
                schema.getCompletionConditionColumnName());

        List<Task> tasks = new ArrayList<Task>(workflowDefinition.getTasks());
        Collections.sort(tasks, new Comparator<FlowNode>() {
            @Override
            public int compare(FlowNode o1, FlowNode o2) {
                return o1.getFlowNodeId().compareTo(o2.getFlowNodeId());
            }
        });
        for (Task task : tasks) {
            HSSFRow dataRow = sheet.createRow(sheet.getLastRowNum() + 1);
            dataRow.createCell(0).setCellValue(workflowDefinition.getWorkflowId());
            dataRow.createCell(1).setCellValue(String.valueOf(workflowDefinition.getVersion()));
            dataRow.createCell(2).setCellValue(task.getFlowNodeId());
            dataRow.createCell(3).setCellValue(task.getMultiInstanceType().toString());
            dataRow.createCell(4).setCellValue(((WorkflowDefinitionGeneratorTask) task).getCondition());
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);
    }

    /**
     * レーンテーブルの情報をブックに書き込む。
     * @param book               書き込み対象ブック
     * @param workflowDefinition ワークフロー定義
     * @param schema ワークフローテーブル定義
     */
    private void writeLane(HSSFWorkbook book, WorkflowDefinition workflowDefinition, WorkflowDefinitionSchema schema) {
        String tableName = schema.getLaneTableName();
        HSSFSheet sheet = getSheet(book, tableName, schema.getWorkflowIdColumnName(), schema.getVersionColumnName(),
                schema.getLaneIdColumnName(), schema.getLaneNameColumnName());

        List<Lane> lanes = new ArrayList<Lane>(workflowDefinition.getLanes());
        Collections.sort(lanes, new Comparator<Lane>() {
            @Override
            public int compare(Lane o1, Lane o2) {
                return o1.getLaneId().compareTo(o2.getLaneId());
            }
        });

        for (Lane lane : lanes) {
            HSSFRow dataRow = sheet.createRow(sheet.getLastRowNum() + 1);
            dataRow.createCell(0).setCellValue(workflowDefinition.getWorkflowId());
            dataRow.createCell(1).setCellValue(String.valueOf(workflowDefinition.getVersion()));
            dataRow.createCell(2).setCellValue(lane.getLaneId());
            dataRow.createCell(3).setCellValue(lane.getLaneName());
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
    }

    /**
     * 境界イベントテーブルの情報をブックに書き込む。
     * @param book               書き込み対象ブック
     * @param workflowDefinition ワークフロー定義
     * @param schema ワークフローテーブル定義
     */
    private void writeBoundaryEvent(HSSFWorkbook book, WorkflowDefinition workflowDefinition, WorkflowDefinitionSchema schema) {
        String tableName = schema.getBoundaryEventTableName();
        HSSFSheet sheet = getSheet(book, tableName, schema.getWorkflowIdColumnName(), schema.getVersionColumnName(),
                schema.getFlowNodeIdColumnName(), schema.getBoundaryEventTriggerIdColumnName(),
                schema.getAttachedTaskIdColumnName());

        List<BoundaryEvent> boundaryEvents = new ArrayList<BoundaryEvent>(workflowDefinition.getBoundaryEvents());
        Collections.sort(boundaryEvents, new Comparator<FlowNode>() {
            @Override
            public int compare(FlowNode o1, FlowNode o2) {
                return o1.getFlowNodeId().compareTo(o2.getFlowNodeId());
            }
        });
        for (BoundaryEvent boundaryEvent : boundaryEvents) {
            HSSFRow dataRow = sheet.createRow(sheet.getLastRowNum() + 1);
            dataRow.createCell(0).setCellValue(workflowDefinition.getWorkflowId());
            dataRow.createCell(1).setCellValue(String.valueOf(workflowDefinition.getVersion()));
            dataRow.createCell(2).setCellValue(boundaryEvent.getFlowNodeId());
            dataRow.createCell(3).setCellValue(boundaryEvent.getBoundaryEventTriggerId());
            dataRow.createCell(4).setCellValue(boundaryEvent.getAttachedTaskId());
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);
    }

    /**
     * イベントテーブルの情報をブックに書き込む。
     * @param book               書き込み対象ブック
     * @param workflowDefinition ワークフロー定義
     * @param schema ワークフローテーブル定義
     */
    private void writeEvent(HSSFWorkbook book, WorkflowDefinition workflowDefinition, WorkflowDefinitionSchema schema) {
        String tableName = schema.getEventTableName();
        HSSFSheet sheet = getSheet(book, tableName, schema.getWorkflowIdColumnName(), schema.getVersionColumnName(),
                schema.getFlowNodeIdColumnName(), schema.getEventTypeColumnName());

        List<Event> events = new ArrayList<Event>(workflowDefinition.getEvents());
        Collections.sort(events, new Comparator<FlowNode>() {
            @Override
            public int compare(FlowNode o1, FlowNode o2) {
                return o1.getFlowNodeId().compareTo(o2.getFlowNodeId());
            }
        });
        for (Event event : events) {
            HSSFRow dataRow = sheet.createRow(sheet.getLastRowNum() + 1);
            dataRow.createCell(0).setCellValue(workflowDefinition.getWorkflowId());
            dataRow.createCell(1).setCellValue(String.valueOf(workflowDefinition.getVersion()));
            dataRow.createCell(2).setCellValue(event.getFlowNodeId());
            dataRow.createCell(3).setCellValue(event.getEventType().toString());
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
    }

    /**
     * ゲートウェイテーブルの情報をブックに書き込む。
     * @param book               書き込み対象ブック
     * @param workflowDefinition ワークフロー定義
     * @param schema ワークフローテーブル定義
     */
    private void writeGateway(HSSFWorkbook book, WorkflowDefinition workflowDefinition, WorkflowDefinitionSchema schema) {
        String tableName = schema.getGatewayTableName();
        HSSFSheet sheet = getSheet(book, tableName, schema.getWorkflowIdColumnName(), schema.getVersionColumnName(),
                schema.getFlowNodeIdColumnName(), schema.getGatewayTypeColumnName());

        List<Gateway> gateways = new ArrayList<Gateway>(workflowDefinition.getGateways());
        Collections.sort(gateways, new Comparator<FlowNode>() {
            @Override
            public int compare(FlowNode o1, FlowNode o2) {
                return o1.getFlowNodeId().compareTo(o2.getFlowNodeId());
            }
        });
        for (Gateway gateway : gateways) {
            HSSFRow dataRow = sheet.createRow(sheet.getLastRowNum() + 1);
            dataRow.createCell(0).setCellValue(workflowDefinition.getWorkflowId());
            dataRow.createCell(1).setCellValue(String.valueOf(workflowDefinition.getVersion()));
            dataRow.createCell(2).setCellValue(gateway.getFlowNodeId());
            dataRow.createCell(3).setCellValue(gateway.getGatewayType().toString());
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
    }

    /**
     * 境界イベントトリガーテーブルの情報をブックに書き込む。
     * @param book               書き込み対象ブック
     * @param workflowDefinition ワークフロー定義
     * @param schema ワークフローテーブル定義
     */
    private void writeBoundaryEventTrigger(HSSFWorkbook book, WorkflowDefinition workflowDefinition, WorkflowDefinitionSchema schema) {
        String tableName = schema.getEventTriggerTableName();
        HSSFSheet sheet = getSheet(book, tableName, schema.getWorkflowIdColumnName(), schema.getVersionColumnName(),
                schema.getBoundaryEventTriggerIdColumnName(), schema.getBoundaryEventTriggerNameColumnName());

        List<String> writeList = new ArrayList<String>();
        List<BoundaryEvent> boundaryEvents = new ArrayList<BoundaryEvent>(workflowDefinition.getBoundaryEvents());
        Collections.sort(boundaryEvents, new Comparator<BoundaryEvent>() {
            @Override
            public int compare(BoundaryEvent o1, BoundaryEvent o2) {
                return o1.getBoundaryEventTriggerId().compareTo(o2.getBoundaryEventTriggerId());
            }
        });

        for (BoundaryEvent boundaryEvent : boundaryEvents) {
            String boundaryEventTriggerId = boundaryEvent.getBoundaryEventTriggerId();
            if (!writeList.contains(boundaryEventTriggerId)) {
                HSSFRow dataRow = sheet.createRow(sheet.getLastRowNum() + 1);
                dataRow.createCell(0).setCellValue(workflowDefinition.getWorkflowId());
                dataRow.createCell(1).setCellValue(String.valueOf(workflowDefinition.getVersion()));
                dataRow.createCell(2).setCellValue(boundaryEventTriggerId);
                dataRow.createCell(3).setCellValue(boundaryEvent.getBoundaryEventTriggerName());
                writeList.add(boundaryEventTriggerId);
            }
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
    }

    /**
     * シーケンスフローテーブルの情報をブックに書き込む。
     * @param book               書き込み対象ブック
     * @param workflowDefinition ワークフロー定義
     * @param schema ワークフローテーブル定義
     */
    private void writeSequenceFlow(HSSFWorkbook book, WorkflowDefinition workflowDefinition, WorkflowDefinitionSchema schema) {
        String tableName = schema.getSequenceFlowTableName();
        HSSFSheet sheet = getSheet(book, tableName, schema.getWorkflowIdColumnName(), schema.getVersionColumnName(),
                schema.getSequenceFlowIdColumnName(), schema.getSourceFlowNodeIdColumnName(),
                schema.getTargetFlowNodeIdColumnName(), schema.getFlowProceedConditionColumnName(),
                schema.getSequenceFlowNameColumnName());

        List<SequenceFlow> sequenceFlows = new ArrayList<SequenceFlow>(workflowDefinition.getSequenceFlows());
        Collections.sort(sequenceFlows, new Comparator<SequenceFlow>() {
            @Override
            public int compare(SequenceFlow o1, SequenceFlow o2) {
                return o1.getSequenceFlowId().compareTo(o2.getSequenceFlowId());
            }
        });

        for (SequenceFlow sequenceFlow : sequenceFlows) {
            HSSFRow dataRow = sheet.createRow(sheet.getLastRowNum() + 1);
            dataRow.createCell(0).setCellValue(workflowDefinition.getWorkflowId());
            dataRow.createCell(1).setCellValue(String.valueOf(workflowDefinition.getVersion()));
            dataRow.createCell(2).setCellValue(sequenceFlow.getSequenceFlowId());
            dataRow.createCell(3).setCellValue(sequenceFlow.getSourceFlowNodeId());
            dataRow.createCell(4).setCellValue(sequenceFlow.getTargetFlowNodeId());
            dataRow.createCell(5).setCellValue(((WorkflowDefinitionGeneratorSequenceFlow) sequenceFlow).getCondition());
            dataRow.createCell(6).setCellValue(sequenceFlow.getSequenceFlowName());
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);
        sheet.autoSizeColumn(5);
        sheet.autoSizeColumn(6);
    }

    /**
     * フローノード定義テーブルの情報をブックに書き込む。
     * @param book               書き込み対象ブック
     * @param workflowDefinition ワークフロー定義
     * @param schema ワークフローテーブル定義
     */
    private void writeFlowNode(HSSFWorkbook book, WorkflowDefinition workflowDefinition, WorkflowDefinitionSchema schema) {
        String tableName = schema.getFlowNodeTableName();
        HSSFSheet sheet = getSheet(book, tableName, schema.getWorkflowIdColumnName(), schema.getVersionColumnName(),
                schema.getFlowNodeIdColumnName(), schema.getLaneIdColumnName(),
                schema.getFlowNodeNameColumnName());

        List<Event> events = new ArrayList<Event>(workflowDefinition.getEvents());
        Collections.sort(events, new Comparator<FlowNode>() {
            @Override
            public int compare(FlowNode o1, FlowNode o2) {
                return o1.getFlowNodeId().compareTo(o2.getFlowNodeId());
            }
        });
        for (Event event : events) {
            HSSFRow dataRow = sheet.createRow(sheet.getLastRowNum() + 1);
            dataRow.createCell(0).setCellValue(workflowDefinition.getWorkflowId());
            dataRow.createCell(1).setCellValue(String.valueOf(workflowDefinition.getVersion()));
            dataRow.createCell(2).setCellValue(event.getFlowNodeId());
            dataRow.createCell(3).setCellValue(event.getLaneId());
            dataRow.createCell(4).setCellValue(event.getFlowNodeName());
        }

        List<BoundaryEvent> boundaryEvents = new ArrayList<BoundaryEvent>(workflowDefinition.getBoundaryEvents());
        Collections.sort(boundaryEvents, new Comparator<FlowNode>() {
            @Override
            public int compare(FlowNode o1, FlowNode o2) {
                return o1.getFlowNodeId().compareTo(o2.getFlowNodeId());
            }
        });
        for (BoundaryEvent boundaryEvent : boundaryEvents) {
            HSSFRow dataRow = sheet.createRow(sheet.getLastRowNum() + 1);
            dataRow.createCell(0).setCellValue(workflowDefinition.getWorkflowId());
            dataRow.createCell(1).setCellValue(String.valueOf(workflowDefinition.getVersion()));
            dataRow.createCell(2).setCellValue(boundaryEvent.getFlowNodeId());
            dataRow.createCell(3).setCellValue(boundaryEvent.getLaneId());
            dataRow.createCell(4).setCellValue(boundaryEvent.getFlowNodeName());
        }

        List<Task> tasks = new ArrayList<Task>(workflowDefinition.getTasks());
        Collections.sort(tasks, new Comparator<FlowNode>() {
            @Override
            public int compare(FlowNode o1, FlowNode o2) {
                return o1.getFlowNodeId().compareTo(o2.getFlowNodeId());
            }
        });
        for (Task task : tasks) {
            HSSFRow dataRow = sheet.createRow(sheet.getLastRowNum() + 1);
            dataRow.createCell(0).setCellValue(workflowDefinition.getWorkflowId());
            dataRow.createCell(1).setCellValue(String.valueOf(workflowDefinition.getVersion()));
            dataRow.createCell(2).setCellValue(task.getFlowNodeId());
            dataRow.createCell(3).setCellValue(task.getLaneId());
            dataRow.createCell(4).setCellValue(task.getFlowNodeName());
        }

        List<Gateway> gateways = new ArrayList<Gateway>(workflowDefinition.getGateways());
        Collections.sort(gateways, new Comparator<FlowNode>() {
            @Override
            public int compare(FlowNode o1, FlowNode o2) {
                return o1.getFlowNodeId().compareTo(o2.getFlowNodeId());
            }
        });
        for (Gateway gateway : gateways) {
            HSSFRow dataRow = sheet.createRow(sheet.getLastRowNum() + 1);
            dataRow.createCell(0).setCellValue(workflowDefinition.getWorkflowId());
            dataRow.createCell(1).setCellValue(String.valueOf(workflowDefinition.getVersion()));
            dataRow.createCell(2).setCellValue(gateway.getFlowNodeId());
            dataRow.createCell(3).setCellValue(gateway.getLaneId());
            dataRow.createCell(4).setCellValue(gateway.getFlowNodeName());
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);
    }

    /**
     * ブックから、指定したテーブル名＝シート名のシートを取得する。
     * シートが存在しない場合、ヘッダ行付きのシートを新規作成する。
     *
     * @param book      ブック
     * @param tableName テーブル名
     * @param columns   カラム名
     * @return シート
     */
    private HSSFSheet getSheet(HSSFWorkbook book, String tableName, String... columns) {
        HSSFSheet sheet = book.getSheet(tableName);
        if (sheet == null) {
            sheet = createSheet(book, tableName, columns);
        }
        return sheet;
    }

    /**
     * シートを新規作成し、ヘッダ行を追加して返却する。
     *
     * @param tableName テーブル名
     * @param columns   カラム名
     * @return シート
     */
    private HSSFSheet createSheet(HSSFWorkbook book, String tableName, String... columns) {
        HSSFSheet sheet;
        sheet = book.createSheet(tableName);
        sheet.createRow(0).createCell(0).setCellValue("SETUP_TABLE=" + tableName);
        HSSFRow row = sheet.createRow(1);
        for (int i = 0; i < columns.length; i++) {
            String column = columns[i];
            row.createCell(i).setCellValue(column);
        }
        return sheet;
    }
}
