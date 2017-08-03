package nablarch.tool.statemachine;

import static org.hamcrest.Matchers.*;
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
 * {@link StatemachineLoader}のテスト。
 */
public class StatemachineLoaderTest {

    @Test
    public void ロード出来ること() throws Exception {
        final StatemachineLoader sut = new StatemachineLoader();
        final List<WorkflowDefinition> actual = sort(sut.load(new File("src/test/testbpmn/statemachine/creator"), new SystemStreamLog()));

        assertThat(actual, hasSize(8));         // WP0009はバリデーションエラーなので、有効なのは8

        // -------------------------------------- 1つめ(1つめだけ詳細にアサート)
        assertThat(actual.get(0), allOf(
                hasProperty("workflowId", is("WP0001")),
                hasProperty("workflowName", is("simple")),
                hasProperty("events", hasSize(2)),          // イベントは1つ
                hasProperty("tasks", hasSize(1)),           // タスクは2つ
                hasProperty("lanes", hasSize(1)),           // レーンは1つ
                hasProperty("gateways", empty()),           // ゲートウェイはなし
                hasProperty("boundaryEvents", hasSize(1))   // 境界イベントは1つ
        ));

        // -------------------------------------- 2つめ
        assertThat(actual.get(1), hasProperty("workflowId", is("WP0002")));

        // -------------------------------------- 3つめ(サブプロセスありなので詳細にアサート)
        assertThat(actual.get(2), allOf(
                hasProperty("workflowId", is("WP0003")),
                hasProperty("events", hasSize(2)),
                hasProperty("tasks", hasSize(2)),
                hasProperty("lanes", hasSize(1)),
                hasProperty("gateways", empty()),
                hasProperty("boundaryEvents", hasSize(2))
        ));

        // -------------------------------------- 4つめ
        assertThat(actual.get(3), hasProperty("workflowId", is("WP0004")));
        
        // -------------------------------------- 5つめ
        assertThat(actual.get(4), hasProperty("workflowId", is("WP0005")));
        
        // -------------------------------------- 6つめ
        assertThat(actual.get(5), hasProperty("workflowId", is("WP0006")));
        
        // -------------------------------------- 7つめ
        assertThat(actual.get(6), hasProperty("workflowId", is("WP0007")));
        
        // -------------------------------------- 8つめ
        assertThat(actual.get(7), hasProperty("workflowId", is("WP0008")));
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