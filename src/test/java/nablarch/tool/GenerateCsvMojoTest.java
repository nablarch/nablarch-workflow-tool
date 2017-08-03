package nablarch.tool;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.hamcrest.Matchers;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * {@link GenerateCsvMojo}のテスト。
 */
public class GenerateCsvMojoTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void ステートマシンのみを指定した場合その情報が出力できること() throws Exception {
        final GenerateCsvMojo sut = new GenerateCsvMojo();
        sut.stateMachineBpmnDir = new File("src/test/java/nablarch/tool/mojodata/state");
        final File root = temporaryFolder.getRoot();
        sut.outputPath = root;

        sut.configurationFilePath = new File("src/test/resources/nablarch/tool/workflow/default-definition.xml");

        assertThat(root.listFiles(), Matchers.<File>emptyArray());

        sut.execute();

        assertThat(new File(root, "WF_WORKFLOW_DEFINITION.csv").exists(), is(true));

        assertThat(readFile(new File(root, "WF_WORKFLOW_DEFINITION.csv")), allOf(
                containsString("simple"),
                containsString("ステートマシン")
        ));
        // 以降は、ファイルが出力されていることのみアサート
        assertThat(new File(root, "WF_LANE.csv").exists(), is(true));
        assertThat(new File(root, "WF_FLOW_NODE.csv").exists(), is(true));
        assertThat(new File(root, "WF_EVENT.csv").exists(), is(true));
        assertThat(new File(root, "WF_TASK.csv").exists(), is(true));
        assertThat(new File(root, "WF_GATEWAY.csv").exists(), is(true));
        assertThat(new File(root, "WF_BOUNDARY_EVENT.csv").exists(), is(true));
        assertThat(new File(root, "WF_BOUNDARY_EVENT_TRIGGER.csv").exists(), is(true));
        assertThat(new File(root, "WF_SEQUENCE_FLOW.csv").exists(), is(true));
    }

    @Test
    public void ワークフローのみを指定した場合その情報が出力できること() throws Exception {
        final GenerateCsvMojo sut = new GenerateCsvMojo();
        sut.workflowBpmnDir = new File("src/test/java/nablarch/tool/mojodata/workflow");
        final File root = temporaryFolder.getRoot();
        sut.outputPath = root;

        sut.configurationFilePath = new File("src/test/resources/nablarch/tool/workflow/default-definition.xml");

        assertThat(root.listFiles(), Matchers.<File>emptyArray());

        sut.execute();

        assertThat(new File(root, "WF_WORKFLOW_DEFINITION.csv").exists(), is(true));
        assertThat(readFile(new File(root, "WF_WORKFLOW_DEFINITION.csv")), allOf(
                containsString("workflow"),
                containsString("ワークフロー")
        ));
        // 以降はファイルが出力されていることのみアサート
        assertThat(new File(root, "WF_LANE.csv").exists(), is(true));
        assertThat(new File(root, "WF_FLOW_NODE.csv").exists(), is(true));
        assertThat(new File(root, "WF_EVENT.csv").exists(), is(true));
        assertThat(new File(root, "WF_TASK.csv").exists(), is(true));
        assertThat(new File(root, "WF_GATEWAY.csv").exists(), is(true));
        assertThat(new File(root, "WF_BOUNDARY_EVENT.csv").exists(), is(true));
        assertThat(new File(root, "WF_BOUNDARY_EVENT_TRIGGER.csv").exists(), is(true));
        assertThat(new File(root, "WF_SEQUENCE_FLOW.csv").exists(), is(true));
    }

    @Test
    public void ステートマシンとワークフローを指定した場合両方の情報が出力できること() throws Exception {
        final GenerateCsvMojo sut = new GenerateCsvMojo();
        sut.stateMachineBpmnDir = new File("src/test/java/nablarch/tool/mojodata/state");
        sut.workflowBpmnDir = new File("src/test/java/nablarch/tool/mojodata/workflow");
        final File root = temporaryFolder.getRoot();
        sut.outputPath = root;

        sut.configurationFilePath = new File("src/test/resources/nablarch/tool/workflow/default-definition.xml");

        assertThat(root.listFiles(), Matchers.<File>emptyArray());

        sut.execute();

        assertThat(new File(root, "WF_WORKFLOW_DEFINITION.csv").exists(), is(true));
        assertThat(readFile(new File(root, "WF_WORKFLOW_DEFINITION.csv")), allOf(
                containsString("simple"),
                containsString("ステートマシン"),
                containsString("workflow"),
                containsString("ワークフロー")
        ));
        // 以降はファイルが出力されていることのみアサート
        assertThat(new File(root, "WF_LANE.csv").exists(), is(true));
        assertThat(new File(root, "WF_FLOW_NODE.csv").exists(), is(true));
        assertThat(new File(root, "WF_EVENT.csv").exists(), is(true));
        assertThat(new File(root, "WF_TASK.csv").exists(), is(true));
        assertThat(new File(root, "WF_GATEWAY.csv").exists(), is(true));
        assertThat(new File(root, "WF_BOUNDARY_EVENT.csv").exists(), is(true));
        assertThat(new File(root, "WF_BOUNDARY_EVENT_TRIGGER.csv").exists(), is(true));
        assertThat(new File(root, "WF_SEQUENCE_FLOW.csv").exists(), is(true));
    }

    private String readFile(final File file) throws Exception {
        final BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), "Windows-31J"));

        final StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }
}