package nablarch.tool.workflow;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@link WorkflowDefinitionFile}のテストクラス。
 *
 * @author Satoshi Taromaru
 * @since 1.4.2
 */
public class WorkflowDefinitionFileTest {

    /**
     * テスト対象。
     */
    private WorkflowDefinitionFile sut = new WorkflowDefinitionFile("fileName", "path", "WF0001", "workflowName", "1", "20140911");

    /**
     * ワークフローID、バージョンで等価性を判定しているか確認。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void testEquals() throws Exception {
        assertThat(sut.equals(new WorkflowDefinitionFile("", "", "WF0001", "", "1", "")), is(true));
        assertThat(sut.equals(new WorkflowDefinitionFile("", "", "WF0002", "", "1", "")), is(false));
        assertThat(sut.equals(new WorkflowDefinitionFile("", "", "WF0001", "", "2", "")), is(false));
        assertThat(sut.equals(new WorkflowDefinitionFile("", "", "WF0002", "", "2", "")), is(false));
        assertThat(sut.equals(sut), is(true));
        assertThat(sut.equals(""), is(false));
        assertThat(sut.equals(null), is(false));
    }

    /**
     * ワークフローID、バージョンが同じ場合、hashCodeの値が等しいことを確認。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void testHashCode() throws Exception {
        assertThat(sut.hashCode() == new WorkflowDefinitionFile("", "", "WF0001", "", "1", "").hashCode(), is(true));
        assertThat(sut.hashCode() == new WorkflowDefinitionFile("", "", "WF0002", "", "1", "").hashCode(), is(false));
        assertThat(sut.hashCode() == new WorkflowDefinitionFile("", "", "WF0001", "", "2", "").hashCode(), is(false));
        assertThat(sut.hashCode() == new WorkflowDefinitionFile("", "", "WF0002", "", "2", "").hashCode(), is(false));
    }
}
