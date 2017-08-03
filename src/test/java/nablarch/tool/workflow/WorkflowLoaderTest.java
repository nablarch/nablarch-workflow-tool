package nablarch.tool.workflow;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.maven.plugin.logging.SystemStreamLog;

import nablarch.integration.workflow.definition.WorkflowDefinition;

import org.junit.Test;

/**
 * {@link WorkflowLoader}のテスト。
 */
public class WorkflowLoaderTest {

    @Test
    public void ロード出来ること() throws Exception {
        final WorkflowLoader sut = new WorkflowLoader();
        final List<WorkflowDefinition> actual = sort(
                sut.load(new File("src/test/java/nablarch/tool/workflow/bpmn/xml/read/normal"), new SystemStreamLog()));

        assertThat(actual, hasSize(3));

        // -------------------------------------- 1つめ
        assertThat(actual.get(0), allOf(
                hasProperty("workflowId", is("WP0001")),
                hasProperty("workflowName", is("normalWorkflow")),
                hasProperty("tasks", hasSize(3)),               // タスク定義は3つ
                hasProperty("gateways", hasSize(2)),            // ゲートウェイ定義は2つ
                hasProperty("events", hasSize(3)),              // イベント定義は3つ
                hasProperty("lanes", hasSize(2)),               // レーン定義は2つ
                hasProperty("boundaryEvents", hasSize(1)),      // 境界イベント定義は1つ
                hasProperty("sequenceFlows", hasSize(9))        // シーケンスフロー定義は9
        ));

        // -------------------------------------- 2つめ
        assertThat(actual.get(1), hasProperty("workflowId", is("WP0001")));

        // -------------------------------------- 3つめ
        assertThat(actual.get(2), hasProperty("workflowId", is("WP0046")));

    }

    private static List<WorkflowDefinition> sort(List<WorkflowDefinition> src) {
        final List<WorkflowDefinition> definitions = new ArrayList<WorkflowDefinition>(src);
        Collections.sort(definitions, new Comparator<WorkflowDefinition>() {
            @Override
            public int compare(final WorkflowDefinition o1, final WorkflowDefinition o2) {
                return o1.getWorkflowId()
                         .compareTo(o2.getWorkflowId());
            }
        });
        return definitions;
    }
}