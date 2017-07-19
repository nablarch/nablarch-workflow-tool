package nablarch.tool.statemachine;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.io.File;

import nablarch.tool.workflow.WorkflowDefinitionFile;
import org.hamcrest.beans.HasPropertyWithValue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * {@link StateMachineDefinitionValidator}のテストクラス。
 */
public class StateMachineDefinitionValidatorTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    /** テスト対象 */
    private StateMachineDefinitionValidator sut = new StateMachineDefinitionValidator();

    @Test
    public void プールが未定義の時は例外が発生すること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/process-nothing.bpmn");

        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage(is("配置できるプールは1つのみです。プールを配置しなおしてください。"));
        sut.validate(file);
    }

    @Test
    public void プールが複数定義されている時は例外が発生すること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/process-duplicate.bpmn");

        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage(is("配置できるプールは1つのみです。プールを配置しなおしてください。"));
        sut.validate(file);
    }

    @Test
    public void レーンが未定義の場合は例外が発生すること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/lane-nothing.bpmn");
        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage(is("レーンは必須です。レーンを配置してください。"));
        sut.validate(file);
    }

    @Test
    public void スタートイベントが存在しない場合例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/start-event-notfound.bpmn");

        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("配置できる開始イベントは1つのみです。開始イベントを配置しなおしてください。");
        sut.validate(file);
    }

    @Test
    public void 複数のスタートイベントが存在した場合は例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/start-event-duplicate.bpmn");

        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("配置できる開始イベントは1つのみです。開始イベントを配置しなおしてください。");
        sut.validate(file);
    }

    @Test
    public void サポートされないシグナルスタートイベントが設定されていた場合は例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile(
                "src/test/testbpmn/statemachine/validator/start-event-unsupported-type.bpmn");

        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("サポート対象外の開始イベントです。 id:start, name:サポートされない開始イベント");
        sut.validate(file);
        
    }

    @Test
    public void 停止イベントが未定義の場合は例外が送出されること () throws  Exception{
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/end-event-notfound.bpmn");

        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("停止イベントは必須です。停止イベントを配置してください。");
        sut.validate(file);
    }

    @Test
    public void サポートされない停止イベントが設定されていた場合は例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/end-event-unsupported-type.bpmn");

        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("サポート対象外の停止イベントです。 id:end, name:サポートされない停止イベント");
        sut.validate(file);
    }

    @Test
    public void タスクが未定義の場合は例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/task-notfound.bpmn");
        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("タスクは必須です。タスクを配置してください。");
        sut.validate(file);
    }

    @Test
    public void サポートされないタスクが設定されていた場合は例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/task-unsupported.bpmn");

        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("サポート対象外のタスクです。 id:task, name:サポートされないタスク");
        sut.validate(file);
    }

    @Test
    public void タスクに境界イベントが指定されていない場合に例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/task-boundary-notfound.bpmn");

        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("タスクに境界イベントが設定されていません。 id:task, name:境界イベントがないタスク");
        sut.validate(file);
    }

    @Test
    public void タスクに遷移先が指定されている場合に例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/task-outgoing-found.bpmn");

        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("タスクに遷移先は設定できません。 id:task, name:遷移先があるタスク");
        sut.validate(file);
    }

    @Test
    public void タスクに遷移元が指定されていない場合に例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/task-incoming-notfound.bpmn");

        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("タスクに遷移元が設定されていません。 id:task, name:遷移元がないタスク");
        sut.validate(file);
    }

    @Test
    public void サポート対象外のゲートウェイが設定されていた場合は例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/gateway-unsupported.bpmn");

        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("サポート対象外のゲートウェイです。 id:gateway, name:サポートされないゲートウェイ");
        sut.validate(file);
    }

    @Test
    public void ゲートウェイに遷移元が設定されていない場合は例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/gateway-incoming-notfound.bpmn");

        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("ゲートウェイに遷移元が設定されていません。 id:gateway, name:遷移元のない境界イベント");
        sut.validate(file);
    }
    
    @Test
    public void ゲートウェイに遷移先が設定されていない場合は例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/gateway-outgoing-notfound.bpmn");

        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("ゲートウェイに遷移先が設定されていません。 id:gateway, name:遷移先のない境界イベント");
        sut.validate(file);
    }
    
    @Test
    public void サポート対象外の境界イベントが設定されていた場合は例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/boundary-unsupported.bpmn");

        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("サポート対象外の境界イベントです。 id:boundary, name:サポートされない境界イベント");
        sut.validate(file);
    }
    
    @Test
    public void 境界イベントに遷移先が設定されていない場合は例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/boundary-outgoing-notfound.bpmn");

        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("境界イベントに遷移先が設定されていません。 id:message, name:遷移先なし");
        sut.validate(file);
    }

    @Test
    public void サブプロセスに開始イベントが設定されていない場合は例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/subprocess-start-event-notfound.bpmn");

        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("サブプロセス内に配置できる開始イベントは1つのみです。開始イベントを配置しなおしてください。 id:subprocess, name:開始イベントがないサブプロセス");
        sut.validate(file);
    }

    @Test
    public void サブプロセスに開始イベントが複数設定されている場合は例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/subprocess-start-event-duplicate.bpmn");
        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("サブプロセス内に配置できる開始イベントは1つのみです。開始イベントを配置しなおしてください。 id:subprocess, name:開始イベントが複数あるサブプロセス");
        sut.validate(file);
    }

    @Test
    public void サブプロセスにサポート対象外の開始イベントが設定されていた場合は例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/subprocess-start-event-unsupported-type.bpmn");
        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("サポート対象外の開始イベントです。 id:start, name:サポートされない開始イベント");
        sut.validate(file);
    }
    
    @Test
    public void サブプロセスの開始イベントに遷移先が設定されていない場合例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile(
                "src/test/testbpmn/statemachine/validator/subprocess-start-event-outgoing-notfound.bpmn");
        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("開始イベントに設定できる遷移先は1つのみです。 id:sub_start, name:サブ内の開始イベント");
        sut.validate(file);
    }
    
    @Test
    public void サブプロセスの開始イベントに遷移先が複数設定されている場合例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile(
                "src/test/testbpmn/statemachine/validator/subprocess-start-event-outgoing-duplicate.bpmn");
        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("開始イベントに設定できる遷移先は1つのみです。 id:sub_start, name:サブ内の開始イベント");
        sut.validate(file);
    }

    @Test
    public void サブプロセスに停止イベントが設定されていない場合は例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/subprocess-end-event-notfound.bpmn");
        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("サブプロセス内に停止イベントは必須です。停止イベントを配置してください。 id:subprocess, name:停止イベントが設定されていないサブプロセス");
        sut.validate(file);
    }

    @Test
    public void サブプロセスにサポート対象外の停止イベントが設定されていた場合は例外が送出されていること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/subprocess-end-event-unsupported.bpmn");
        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("サポート対象外の停止イベントです。 id:endEvent, name:サポートされない停止イベント");
        sut.validate(file);
    }
    
    @Test
    public void サブプロセスの停止イベントに遷移元が指定されていない場合に例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/subprocess-end-event-incoming-notfound.bpmn");
        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("停止イベントに遷移元が設定されていません。 id:sub-end, name:遷移元の内終了イベント");
        sut.validate(file);
    }

    @Test
    public void サブプロセスにタスクが設定されていない場合は例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/subprocess-task-notfound.bpmn");
        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("サブプロセス内にタスクは必須です。タスクを配置してください。 id:subprocess, name:タスクが設定されていないサブプロセス");
        sut.validate(file);
    }

    @Test
    public void サブプロセスにサポートされていないタスクが設定されていた場合は例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/subprocess-task-unsupported.bpmn");
        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("サポート対象外のタスクです。 id:task, name:サポートされていないタスク");
        sut.validate(file);
    }

    @Test
    public void サブプロセスに境界イベントが指定されていないタスクがある場合に例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/subprocess-task-boundary.bpmn");

        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("タスクに境界イベントが設定されていません。 id:task, name:境界イベントがないタスク");
        sut.validate(file);
    }

    @Test
    public void サブプロセスに遷移先が指定されているタスクがある場合に例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/subprocess-task-ountgoing-found.bpmn");

        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("タスクに遷移先は設定できません。 id:task, name:遷移先があるタスク");
        sut.validate(file);
    }

    @Test
    public void サブプロセスに遷移元が指定されていないタスクがある場合に例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/subprocess-task-incoming-notfound.bpmn");

        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("タスクに遷移元が設定されていません。 id:task, name:遷移元がないタスク");
        sut.validate(file);
    }

    @Test
    public void サブプロセスにサポートされていないゲートウェイが設定されていた場合は例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/subprocess-gateway-unsupported.bpmn");
        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("サポート対象外のゲートウェイです。 id:gateway, name:サポートされないゲートウェイ");
        sut.validate(file);
    }
    
    @Test
    public void サブプロセスのゲートウェイに遷移元が設定されていない場合は例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/subprocess-gateway-incoming-notfound.bpmn");

        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("ゲートウェイに遷移元が設定されていません。 id:gateway, name:遷移元のない境界イベント");
        sut.validate(file);
    }
    
    @Test
    public void サブプロセスのゲートウェイに遷移先が設定されていない場合は例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/subprocess-gateway-outgoing-notfound.bpmn");

        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("ゲートウェイに遷移先が設定されていません。 id:gateway, name:遷移先のない境界イベント");
        sut.validate(file);
    }

    @Test
    public void サブプロセスにサポートされていない境界イベントが設定されていた場合は例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/subprocess-boundary-unsupported.bpmn");
        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("サポート対象外の境界イベントです。 id:boundary, name:サポートされない境界イベント");
        sut.validate(file);
    }
    
    @Test
    public void サブプロセスの境界イベントに遷移先が設定されていない場合は例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/subprocess-boundary-outgoing-notfound.bpmn");

        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("境界イベントに遷移先が設定されていません。 id:message, name:遷移先なし");
        sut.validate(file);
    }

    @Test
    public void ネストしたサブプロセスの定義が不正な場合に例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/subprocess-nest-undefined.bpmn");
        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("サブプロセス内に配置できる開始イベントは1つのみです。開始イベントを配置しなおしてください。 id:nest-subprocess, name:開始イベントがないネストしたサブプロセス");
        sut.validate(file);
    }

    @Test
    public void 開始イベントから遷移先が指定されていない場合に例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/start-event-outgoing-notfound.bpmn");
        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("開始イベントに設定できる遷移先は1つのみです。 id:start, name:遷移先がない開始イベント");
        sut.validate(file);
    }

    @Test
    public void 開始イベントから複数の遷移先が指定されている場合に例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/start-event-outgoing-duplicate.bpmn");
        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("開始イベントに設定できる遷移先は1つのみです。 id:start, name:遷移先が複数の開始イベント");
        sut.validate(file);
    }

    @Test
    public void 停止イベントに遷移元が指定されていない場合に例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/end-event-incoming-notfound.bpmn");
        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("停止イベントに遷移元が設定されていません。 id:end, name:遷移元がない停止イベント");
        sut.validate(file);
    }

    @Test
    public void 利用出来ない要素が置かれていた場合例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/invalid-flow-node.bpmn");
        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("サポート対象外の要素が設定されています。 id:invalid, name:サポートされない要素");
        expectedException.expect(HasPropertyWithValue.hasProperty("messages", hasSize(1)));
        sut.validate(file);
    }
    
    @Test
    public void サブプロセスに利用出来ない要素が置かれていた場合例外が送出されること() throws Exception {
        final WorkflowDefinitionFile file = createWorkflowDefinitionFile("src/test/testbpmn/statemachine/validator/subprocess-invalid-flow-node.bpmn");
        expectedException.expect(InvalidStateMachineModelException.class);
        expectedException.expectMessage("サポート対象外の要素が設定されています。 id:invalid, name:サポートされない要素");
        expectedException.expect(HasPropertyWithValue.hasProperty("messages", hasSize(1)));
        sut.validate(file);
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