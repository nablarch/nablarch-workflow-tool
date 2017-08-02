package nablarch.tool.workflow.bpmn;

import java.io.File;
import java.net.URL;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.SchemaFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.spec.bpmn._20100524.model.TDefinitions;

import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.util.FileUtil;
import nablarch.tool.workflow.WorkflowDefinitionException;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * {@link BpmnWorkflowDefinitionValidator}のテストクラス。
 *
 * @author Satoshi Taromaru
 * @since 1.4.2
 */
public class BpmnWorkflowDefinitionValidatorTest {

    /**
     * テスト対象クラス。
     */
    private static BpmnWorkflowDefinitionValidator sut;

    /**
     * ワークフロー定義ファイルからワークフロー定義情報へ変換するオブジェクト。
     */
    private static Unmarshaller unmarshaller;

    /**
     * 事前処理。テスト用コンポーネント定義をロードし、unmarshallerを生成する。
     */
    @BeforeClass
    public static void setUp() throws Exception {
        SystemRepository.load(new DiContainer(new XmlComponentDefinitionLoader("classpath:nablarch/tool/workflow/WorkflowDefinitionGeneratorTest.xml")));
        sut = SystemRepository.get("workflowDefinitionValidator");
        unmarshaller = createUnmarshaller();
    }

    /**
     * 事後処理。ロードしたリポジトリを初期化する。
     */
    @AfterClass
    public static void tearDown() {
        SystemRepository.clear();
    }


    /**
     * ワークフロー定義ファイル内にprocessタグが複数存在する場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void tooManyProcess() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/WP1007_tooManyProcess_ver1_20140804.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("processタグが複数存在する場合、精査エラーとなること。", messages, hasItem("ワークフローには、必ず一つのプールを配置してください。"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * ワークフロー定義ファイル内にprocessタグが存在しない場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void noProcess() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/requiredItem/WP1005_noProcess_ver1_20140804.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("processタグが無い場合、精査エラーとなること。", messages, hasItem("ワークフローには、必ず一つのプールを配置してください。"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * ワークフロー定義ファイル内にlaneSetタグが存在しない場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void noLane() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/requiredItem/WP1009_noLane_ver1_20140821.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("laneSetタグが存在しない場合、精査エラーとなること。", messages, hasItem("ワークフローにレーンは必須です。レーンを配置してください。"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * ワークフロー定義ファイル内にlaneSetタグが存在しない場合、精査エラーとなること。
     * （一度配置したフロー図を削除した場合）
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void noProcessAfterDelete() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/requiredItem/WP1006_noLane_ver2_20140804.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("laneSetタグが存在しない場合、精査エラーとなること。", messages, hasItem("ワークフローにレーンは必須です。レーンを配置してください。"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * startEventタグが子要素（EventDefinitionタグ）を持つ場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void unsupportedStartEvent() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/unsupported/WP1016_unsupportedStartEvent_ver1_20140811.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("startEventタグが子要素（EventDefinitionタグ）を持つ場合、精査エラーとなること。",
                    messages, hasItem("サポート対象外の開始イベントです。 id = [startevent12345678] name = [StartEvent_name]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * ワークフロー定義ファイル内にstartEventタグが存在しない場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void noStartEvent() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/requiredItem/WP1008_noStartEvent_ver1_20140804.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("startEventタグが存在しない場合、精査エラーとなること。", messages, hasItem("ワークフローに開始イベントは必須です。開始イベントを配置してください。"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * ワークフロー定義ファイル内にstartEventタグが複数存在する場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void tooManyStartEvent() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/WP1012_tooManyStartEvent_ver1_20140804.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("startEventタグが複数存在する場合、精査エラーとなること。", messages, hasItem("ワークフローに配置できる開始イベントは一つのみです。開始イベントを配置し直してください。"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * 以下の条件を満たすsequenceFlowタグが複数存在する場合、精査エラーとなること。</br>
     * ・startEventタグのid属性の値が、sourceRef属性に設定してある
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void tooManyTransitionFromStartEvent() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/WP1022_tooManyTransitionFromStartEvent_ver1_20140811.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("startEventタグのid属性の値が、sourceRef属性に設定してあるsequenceFlowタグが複数存在する場合、精査エラーとなること。",
                    messages, hasItem("開始イベントが複数の遷移元に設定されています。開始イベントから伸びるシーケンスフローが1つになるように配置してください。 id = [startevent12345678] name = [StartEvent_name]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * 以下の条件を満たすsequenceFlowタグが存在しない場合、精査エラーとなること。</br>
     * ・startEventタグのid属性の値が、sourceRef属性に設定してある
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void noTransitionFromStartEvent() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/sequenceFlow/WP1036_noTransitionFromStartEvent_ver1_20140820.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("startEventタグのid属性の値が、sourceRef属性に設定してあるsequenceFlowタグが存在しない場合、精査エラーとなること。",
                    messages, hasItem("開始イベントが遷移元に設定されていません。開始イベントから伸びるようにシーケンスフローを配置してください。 id = [startevent12345678] name = [startevent1_name]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * endEventタグの子要素がterminateEventDefinitionタグ以外のEventDefinitionタグである場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void unsupportedEndEvent() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/unsupported/WP1016_unsupportedEndEvent_ver1_20140811.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("endEventタグの子要素（EventDefinitionタグ）が存在しない場合、精査エラーとなること。",
                    messages, hasItem("サポート対象外の終了イベントです。 id = [endEvent] name = [EndEvent_name]"));
            assertThat("endEventタグの子要素（EventDefinitionタグ）がterminateEventDefinitionタグ以外の場合、精査エラーとなること。",
                    messages, hasItem("サポート対象外の終了イベントです。 id = [unsupportedEndEvent1] name = [unsupportedEndEvent_name]"));
            assertThat("endEventタグの子要素（EventDefinitionタグ）が複数存在する場合、精査エラーとなること。",
                    messages, hasItem("サポート対象外の終了イベントです。 id = [unsupportedEndEvent2] name = [tooManyEventDefinition_name]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(3));
        }
    }

    /**
     * ワークフロー定義ファイル内にendEventタグが存在しない場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void noEndEvent() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/requiredItem/WP1010_noEndEvent_ver1_20140804.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("endEventタグが存在しない場合、精査エラーとなること。", messages, hasItem("ワークフローに停止イベントは必須です。停止イベントを配置してください。"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * 以下の条件を満たすsequenceFlowタグが存在しない場合、精査エラーとなること。</br>
     * ・endEventタグのid属性の値が、targetRef属性に設定してある
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void noTransitionToEndEvent() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/sequenceFlow/WP1037_noTransitionToEndEvent_ver1_20140820.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("endEventタグのid属性の値が、targetRef属性に設定してあるsequenceFlowタグが存在しない場合、精査エラーとなること。",
                    messages, hasItem("停止イベントが遷移先に設定されていません。停止イベントに向かうようにシーケンスフローを配置してください。 id = [terminateEndEvent1] name = [TerminateEndEvent]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * boundaryEventタグの子要素がmessageEventDefinitionタグ以外のEventDefinitionタグである場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void unsupportedBoundaryEvent() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/unsupported/WP1016_unsupportedBoundaryEvent_ver1_20140811.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("boundaryEventタグの子要素（EventDefinitionタグ）が存在しない場合、精査エラーとなること。",
                    messages, hasItem("サポート対象外の境界イベントです。 id = [boundarymessage123] name = [BoundaryEvent_name]"));
            assertThat("boundaryEventタグの子要素（EventDefinitionタグ）がterminateEventDefinitionタグ以外の場合、精査エラーとなること。",
                    messages, hasItem("サポート対象外の境界イベントです。 id = [boundarymessage345] name = [BoundaryEvent3_name]"));
            assertThat("boundaryEventタグの子要素（EventDefinitionタグ）が複数存在する場合、精査エラーとなること。",
                    messages, hasItem("サポート対象外の境界イベントです。 id = [boundarymessage234] name = [BoundaryEvent2_name]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(3));
        }
    }

    /**
     * 以下の条件を満たすsequenceFlowタグが複数存在する場合、精査エラーとなること。</br>
     * ・boundaryEventタグのid属性の値が、sourceRef属性に設定してある
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void tooManyTransitionFromBoundaryEvent() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/WP1023_tooManyTransitionFromBoundaryEvent_ver1_20140811.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("boundaryEventタグのid属性の値が、sourceRef属性に設定してあるsequenceFlowタグが複数存在する場合、精査エラーとなること。",
                    messages, hasItem("境界イベントが複数の遷移元に設定されています。境界イベントから伸びるシーケンスフローが1つになるように配置してください。 id = [boundarymessage123] name = [BoundaryEvent_name]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * 以下の条件を満たすsequenceFlowタグが存在しない場合、精査エラーとなること。</br>
     * ・boundaryEventタグのid属性の値が、sourceRef属性に設定してある
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void noTransitionFromBoundaryEvent() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/sequenceFlow/WP1040_noTransitionFromBoundaryEvent_ver1_20140820.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("boundaryEventタグのid属性の値が、sourceRef属性に設定してあるsequenceFlowタグが存在しない場合、精査エラーとなること。",
                    messages, hasItem("境界イベントが遷移元に設定されていません。境界イベントから伸びるようにシーケンスフローを配置してください。 id = [boundarymessage123] name = [boundarymessage_name]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * boundaryEventタグの子要素（messageEventDefinition）のmessageRefがmessageタグと紐付いてない場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void noBoundaryEventTriggerId() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/requiredItem/WP1003_noBoundaryEventTriggerId_ver1_20140804.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("messageRef属性が無い場合、精査エラーとなること。",
                    messages, hasItem("境界イベントの境界イベントトリガーIDは必須です。[Message]を設定してください。 id = [boundarymessage345] name = [boundarymessage_name]"));
            assertThat("messageRef属性の値がmessageタグのid属性と紐付かない場合、精査エラーとなること。",
                    messages, hasItem("境界イベントの境界イベントトリガーIDは必須です。[Message]を設定してください。 id = [boundarymessage456] name = [boundarymessage2_name]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(2));
        }
    }

    /**
     * boundaryEventタグのcancelActivity属性がtrueでない場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void cancelActivityNotTrue() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/WP1019_cancelActivityNotTrue_ver1_20140813.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("boundaryEventタグのcancelActivity属性がtrueでない場合、精査エラーとなること。",
                    messages, hasItem("境界イベントのCancelActivityがtrueではありません。CancelActivityをtrueにしてください。 id = [boundarymessage123] name = [boundarymessage123_name]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * userTaskタグ以外のTaskタグ（serviceTaskタグ）が存在する場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void unsupportedTask() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/unsupported/WP1016_unsupportedTask_ver1_20140811.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("userTaskタグ以外のTaskタグ（serviceTaskタグ）が存在する場合、精査エラーとなること。",
                    messages, hasItem("サポート対象外の要素です。 id = [ServiceTask_123456] name = [ServiceTask_name]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * ワークフロー定義ファイル内にuserTaskタグが存在しない場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void noUserTask() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/requiredItem/WP1011_noUserTask_ver1_20140804.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("userTaskタグが存在しない場合、精査エラーとなること。", messages, hasItem("ワークフローにタスクは必須です。タスクを配置してください。"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * 以下の条件を満たすsequenceFlowタグが複数存在する場合、精査エラーとなること。</br>
     * ・userTaskタグのid属性の値が、sourceRef属性に設定してある
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void tooManyTransitionFromUserTask() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/sequenceFlow/WP1041_tooManyTransitionFromUserTask_ver1_20140820.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("userTaskタグのid属性の値が、sourceRef属性に設定してあるsequenceFlowタグが複数存在する場合、精査エラーとなること。",
                    messages, hasItem("タスクが複数の遷移元に設定されています。タスクから伸びるシーケンスフローが1つになるように配置してください。 id = [usertask1234567890] name = [usertask1_name]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * 以下の条件を満たすsequenceFlowタグが存在しない場合、精査エラーとなること。</br>
     * ・userTaskタグのid属性の値が、sourceRef属性に設定してある
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void noTransitionFromUserTask() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/sequenceFlow/WP1038_noTransitionFromUserTask_ver1_20140820.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("userTaskタグのid属性の値が、sourceRef属性に設定してあるsequenceFlowタグが存在しない場合、精査エラーとなること。",
                    messages, hasItem("タスクが遷移元に設定されていません。タスクから伸びるようにシーケンスフローを配置してください。 id = [usertask2345678901] name = [usertask2_name]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * 以下の条件を満たすsequenceFlowタグが存在しない場合、精査エラーとなること。</br>
     * ・userTaskタグのid属性の値が、targetRef属性に設定してある
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void noTransitionToUserTask() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/sequenceFlow/WP1039_noTransitionToUserTask_ver1_20140820.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("userTaskタグのid属性の値が、targetRef属性に設定してあるsequenceFlowタグが存在しない場合、精査エラーとなること。",
                    messages, hasItem("タスクが遷移先に設定されていません。タスクに向かうようにシーケンスフローを配置してください。 id = [usertask2345678901] name = [usertask2_name]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * multiInstanceLoopCharacteristicsタグの子要素（completionCondition）が無い場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void noSeqCompletionCondition() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/requiredItem/WP1001_noSeqCompletionCondition_ver1_20140804.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            SystemRepository.load(new DiContainer(new XmlComponentDefinitionLoader("classpath:nablarch/tool/workflow/WorkflowDefinitionGeneratorTest.xml")));
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("multiInstanceLoopCharacteristicsタグの子要素（completionCondition）が無い場合、精査エラーとなること。",
                    messages, hasItem("マルチインスタンス・アクティビティの完了条件は必須です。[Completion condition]を設定してください。 id = [usertask1234567890] name = [usertask1_name]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * completionConditionタグの子要素が無い場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void noParaCompletionCondition() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/requiredItem/WP1002_noParaCompletionCondition_ver1_20140804.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("completionConditionタグの子要素が無い場合、精査エラーとなること。",
                    messages, hasItem("マルチインスタンス・アクティビティの完了条件は必須です。[Completion condition]を設定してください。 id = [usertask2345678901] name = [usertask2_name]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * exclusiveGatewayタグ以外のGatewayタグ（parallelGatewayタグ）が存在する場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void unsupportedGateway() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/unsupported/WP1016_unsupportedGateway_ver1_20140811.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("exclusiveGatewayタグ以外のGatewayタグ（parallelGatewayタグ）が存在する場合、精査エラーとなること。",
                    messages, hasItem("サポート対象外の要素です。 id = [ParallelGateway_12] name = [ParallelGateway_name]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * sequenceFlowタグのsourceRef属性にgatewayのidが設定されている、</br>
     * かつsequenceFlowタグの子要素（multiInstanceLoopCharacteristicsタグ）に値が設定されていない場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void noFlowProceedCondition() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/requiredItem/WP1004_noFlowProceedCondition_ver1_20140804.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("sequenceFlowタグの子要素（conditionExpression）が無い場合、精査エラーとなること。",
                    messages, hasItem("ゲートウェイから伸びるシーケンスフローの場合、フロー進行条件は必須です。[条件]を設定してください。 id = [flow3] name = [to TerminateEndEvent]"));
            assertThat("conditionExpressionタグの子要素が無い場合、精査エラーとなること。",
                    messages, hasItem("ゲートウェイから伸びるシーケンスフローの場合、フロー進行条件は必須です。[条件]を設定してください。 id = [flow4] name = [to User Task2]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(2));
        }
    }

    /**
     * 以下の条件を満たすsequenceFlowタグが存在しない場合、精査エラーとなること。</br>
     * ・exclusiveGatewayタグのid属性の値が、sourceRef属性に設定してある
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void noTransitionFromGateway() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/sequenceFlow/WP1034_noTransitionFromGateway_ver1_20140820.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("exclusiveGatewayタグのid属性の値が、sourceRef属性に設定してあるsequenceFlowタグが存在しない場合、精査エラーとなること。",
                    messages, hasItem("ゲートウェイが遷移元に設定されていません。ゲートウェイから伸びるようにシーケンスフローを配置してください。 id = [exclusivegateway23] name = [exclusivegateway2_name]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * 以下の条件を満たすsequenceFlowタグが複数存在する場合、精査エラーとなること。</br>
     * ・exclusiveGatewayタグのid属性の値が、targetRef属性に設定してある
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void tooManyTransitionToGateway() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/WP1013_tooManyTransitionToGateway_ver1_20140811.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("exclusiveGatewayタグのid属性の値が、targetRef属性に設定してあるsequenceFlowタグが複数存在する場合、精査エラーとなること。",
                    messages, hasItem("ゲートウェイが複数の遷移先に設定されています。ゲートウェイに向かうシーケンスフローが1つになるように配置してください。 id = [ExclusiveGateway_1] name = [ExclusiveGateway_name]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * 以下の条件を満たすsequenceFlowタグが存在しない場合、精査エラーとなること。</br>
     * ・exclusiveGatewayタグのid属性の値が、targetRef属性に設定してある
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void noTransitionToGateway() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/sequenceFlow/WP1035_noTransitionToGateway_ver1_20140820.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("exclusiveGatewayタグのid属性の値が、targetRef属性に設定してあるsequenceFlowタグが存在しない場合、精査エラーとなること。",
                    messages, hasItem("ゲートウェイが遷移先に設定されていません。ゲートウェイに向かうようにシーケンスフローを配置してください。 id = [exclusivegateway23] name = [exclusivegateway2_name]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * processタグの子要素に、サポート対象外のFlowElementタグ（subProcessタグ）が存在する場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void unsupportedFlowElement() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/unsupported/WP1016_unsupportedFlowElement_ver1_20140811.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("サポート対象外のflowElementタグ（subProcessタグ）が存在する場合、精査エラーとなること。",
                    messages, hasItem("サポート対象外の要素です。 id = [SubProcess_1] name = [SubProcess_name]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * definitionsタグの子要素に、サポート対象外のRootElementタグ（dataStoreタグ）が存在する場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void unsupportedRootElement() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/unsupported/WP1017_unsupportedRootElement_ver1_20140821.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("サポート対象外のrootElementタグ（dataStoreタグ）が存在する場合、精査エラーとなること。",
                    messages, hasItem("サポート対象外の要素です。 id = [DataStore_1]"));
            assertThat("サポート対象外のflowElementタグ（dataStoreReferenceタグ）が存在する場合、精査エラーとなること。",
                    messages, hasItem("サポート対象外の要素です。 id = [_DataStoreReference_6] name = [Data Store 1]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(2));
        }
    }

    /**
     * collaborationタグの子要素にparticipantタグ以外のタグ（conversationタグ）が存在する場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void unsupportedCollaboration() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/unsupported/WP1018_unsupportedCollaboration_ver1_20140821.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("collaborationタグの子要素にparticipantタグ以外のタグ（conversationタグ）が存在する場合、精査エラーとなること。",
                    messages, hasItem("サポート対象外の要素です。 id = [Conversation_5] name = [Conversation]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(1));
        }
    }

    /**
     * 以下のようにendEventタグからフロー要素タグを辿った際、辿り付かないフロー要素タグが存在する場合、精査エラーとなること。<br>
     * フロー要素タグ（endEventタグ）のid属性の値がtargetRef属性に設定されているsequenceFlowタグ→<br>
     * sequenceFlowタグの、sourceRef属性で設定されているidを持つフロー要素タグ→<br>
     * フロー要素タグのid属性の値がtargetRef属性に設定されているsequenceFlowタグ...
     * ※endEventタグからstartEventタグに辿りつけないケース
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void unreachableFromEndEventToStartEvent() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/WP1020_unreachableFromEndEventToStartEvent_ver1_20140813.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("endEventタグからフロー要素を辿った際、startEventタグに辿りつかない場合、精査エラーとなること。",
                    messages, hasItem("フローノードが停止イベントから辿れません。停止イベントへ辿れるようにシーケンスフローを配置してください。 id = [startevent12345678] name = [StartEvent_name] "));
            assertThat("endEventタグからフロー要素を辿った際、userTaskタグに辿りつかない場合、精査エラーとなること。",
                    messages, hasItem("フローノードが停止イベントから辿れません。停止イベントへ辿れるようにシーケンスフローを配置してください。 id = [usertask1234567890] name = [UserTask_1_name] "));
            assertThat("endEventタグからフロー要素を辿った際、exclusiveGatewayタグに辿りつかない場合、精査エラーとなること。",
                    messages, hasItem("フローノードが停止イベントから辿れません。停止イベントへ辿れるようにシーケンスフローを配置してください。 id = [ExclusiveGateway_1] name = [ExclusiveGateway_1_name] "));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(3));
        }
    }

    /**
     * 以下のようにendEventタグからフロー要素タグを辿った際、辿り付かないフロー要素タグが存在する場合、精査エラーとなること。<br>
     * フロー要素タグ（endEventタグ）のid属性の値がtargetRef属性に設定されているsequenceFlowタグ→<br>
     * sequenceFlowタグの、sourceRef属性で設定されているidを持つフロー要素タグ→<br>
     * フロー要素タグのid属性の値がtargetRef属性に設定されているsequenceFlowタグ...<br>
     * ※endEventタグからstartEventタグに辿りつけるケース
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void unreachableFlowElement() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/WP1021_unreachableFlowElement_ver1_20140813.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("endEventタグからフロー要素を辿った際、userTaskタグに辿りつかない場合、精査エラーとなること。",
                    messages, hasItem("フローノードが停止イベントから辿れません。停止イベントへ辿れるようにシーケンスフローを配置してください。 id = [usertask2345678901] name = [usertask2345678901_name] "));
            assertThat("endEventタグからフロー要素を辿った際、userTaskタグに辿りつかない場合、精査エラーとなること。",
                    messages, hasItem("フローノードが停止イベントから辿れません。停止イベントへ辿れるようにシーケンスフローを配置してください。 id = [usertask3456789012] name = [usertask3456789012_name] "));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(2));
        }
    }

    /**
     * completionConditionタグの子要素について、引数の後ろに文字列が存在する場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void charAfterCompletionConditionParam() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/invalidCondition/"
                + "WP1022_charAfterCompletionConditionParam_ver1_20140813.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("completionConditionタグの子要素「e(result, 1)q」について、引数の後ろに文字列が存在する場合、精査エラーとなること。",
                    messages, hasItem("条件の形式が不正です。「完全修飾名(パラメータ1, パラメータ2, ....)」の形式で入力してください。"
                    + "完全修飾名は空白を含んではいけません。パラメータが無い場合、括弧を含んではいけません。"
                    + "完全修飾名の代わりに、あらかじめ登録された省略記法を利用することも出来ます。入力値 = [e(result, 1)q] id = [usertask2345678901] name = [User Task2]"));
            assertThat("completionConditionタグの子要素「nablarch.tool.work(result, 1)flow.bpmn.Condition」について、引数の後ろに文字列が存在する場合、精査エラーとなること。",
                    messages, hasItem("条件の形式が不正です。「完全修飾名(パラメータ1, パラメータ2, ....)」の形式で入力してください。"
                    + "完全修飾名は空白を含んではいけません。パラメータが無い場合、括弧を含んではいけません。"
                    + "完全修飾名の代わりに、あらかじめ登録された省略記法を利用することも出来ます。入力値 = [nablarch.tool.work(result, 1)flow.bpmn.Condition] id = [usertask5678901234] name = [User Task5]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(2));
        }
    }

    /**
     * completionConditionタグの子要素について、引数の括弧内が空の場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void noParamOnCompletionConditionParentheses() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/invalidCondition/WP1024_noParamOnCompletionConditionParentheses_ver1_20140813.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("completionConditionタグの子要素「eq()」について、引数の括弧内が空の場合、精査エラーとなること。",
                    messages, hasItem("条件の形式が不正です。「完全修飾名(パラメータ1, パラメータ2, ....)」の形式で入力してください。"
                    + "完全修飾名は空白を含んではいけません。パラメータが無い場合、括弧を含んではいけません。"
                    + "完全修飾名の代わりに、あらかじめ登録された省略記法を利用することも出来ます。入力値 = [eq()] id = [usertask3456789012] name = [User Task3]"));
            assertThat("completionConditionタグの子要素「nablarch.tool.workflow.bpmn.Condition()」について、引数の括弧内が空の場合、精査エラーとなること。",
                    messages, hasItem("条件の形式が不正です。「完全修飾名(パラメータ1, パラメータ2, ....)」の形式で入力してください。"
                    + "完全修飾名は空白を含んではいけません。パラメータが無い場合、括弧を含んではいけません。"
                    + "完全修飾名の代わりに、あらかじめ登録された省略記法を利用することも出来ます。入力値 = [nablarch.tool.workflow.bpmn.Condition()] id = [usertask6789012345] name = [User Task6]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(2));
        }
    }

    /**
     * conditionExpressionタグの子要素について、引数の後ろに文字列が存在する場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void charAfterFlowProceedConditionParam() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/invalidCondition/WP1029_charAfterFlowProceedConditionParam_ver1_20140813.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("conditionExpressionタグの子要素「e(result, 1)q」について、引数の後ろに文字列が存在する場合、精査エラーとなること。",
                    messages, hasItem("条件の形式が不正です。「完全修飾名(パラメータ1, パラメータ2, ....)」の形式で入力してください。"
                    + "完全修飾名は空白を含んではいけません。パラメータが無い場合、括弧を含んではいけません。"
                    + "完全修飾名の代わりに、あらかじめ登録された省略記法を利用することも出来ます。入力値 = [e(result, 1)q] id = [SequenceFlow_6] name = [SequenceFlow 6]"));
            assertThat("conditionExpressionタグの子要素「nablarch.tool.work(result, 1)flow.bpmn.Condition」について、引数の後ろに文字列が存在する場合、精査エラーとなること。",
                    messages, hasItem("条件の形式が不正です。「完全修飾名(パラメータ1, パラメータ2, ....)」の形式で入力してください。"
                    + "完全修飾名は空白を含んではいけません。パラメータが無い場合、括弧を含んではいけません。"
                    + "完全修飾名の代わりに、あらかじめ登録された省略記法を利用することも出来ます。入力値 = [nablarch.tool.work(result, 1)flow.bpmn.Condition] id = [SequenceFlow_9] name = [SequenceFlow 9]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(2));
        }
    }

    /**
     * conditionExpressionタグの子要素について、引数の括弧内が空の場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void noParamOnFlowProceedConditionParentheses() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/invalidCondition/WP1030_noParamOnFlowProceedConditionParentheses_ver1_20140813.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("conditionExpressionタグの子要素「eq()」について、引数の括弧内が空の場合、精査エラーとなること。",
                    messages, hasItem("条件の形式が不正です。「完全修飾名(パラメータ1, パラメータ2, ....)」の形式で入力してください。"
                    + "完全修飾名は空白を含んではいけません。パラメータが無い場合、括弧を含んではいけません。"
                    + "完全修飾名の代わりに、あらかじめ登録された省略記法を利用することも出来ます。入力値 = [eq()] id = [SequenceFlow_7] name = [SequenceFlow 7]"));
            assertThat("conditionExpressionタグの子要素「nablarch.tool.workflow.bpmn.Condition()」について、引数の括弧内が空の場合、精査エラーとなること。",
                    messages, hasItem("条件の形式が不正です。「完全修飾名(パラメータ1, パラメータ2, ....)」の形式で入力してください。"
                    + "完全修飾名は空白を含んではいけません。パラメータが無い場合、括弧を含んではいけません。"
                    + "完全修飾名の代わりに、あらかじめ登録された省略記法を利用することも出来ます。入力値 = [nablarch.tool.workflow.bpmn.Condition()] id = [SequenceFlow_10] name = [SequenceFlow 10]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(2));
        }
    }

    /**
     * completionConditionタグの子要素について、引数に空文字のパラメータを含む場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void containEmptyParamOnCompletionCondition() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/invalidCondition/WP1042_containEmptyParamOnCompletionCondition_ver1_20140813.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("completionConditionタグの子要素「eq( ,result, 1)」について、引数に空文字のパラメータを含む場合、精査エラーとなること。",
                    messages, hasItem("空文字となるパラメータを含んではいけません。入力値 = [eq( ,result, 1)] id = [usertask1234567890] name = [User Task1]"));
            assertThat("completionConditionタグの子要素「eq(result, 1, )」について、引数に空文字のパラメータを含む場合、精査エラーとなること。",
                    messages, hasItem("空文字となるパラメータを含んではいけません。入力値 = [eq(result, 1, )] id = [usertask2345678901] name = [User Task2]"));
            assertThat("completionConditionタグの子要素「eq(result, ,1)」について、引数に空文字のパラメータを含む場合、精査エラーとなること。",
                    messages, hasItem("空文字となるパラメータを含んではいけません。入力値 = [eq(result, ,1)] id = [usertask3456789012] name = [User Task3]"));
            assertThat("completionConditionタグの子要素「nablarch.tool.workflow.bpmn.Condition( ,result, 1)」について、引数に空文字のパラメータを含む場合、精査エラーとなること。",
                    messages, hasItem("空文字となるパラメータを含んではいけません。入力値 = [nablarch.tool.workflow.bpmn.Condition(,result, 1)] id = [usertask4567890123] name = [User Task4]"));
            assertThat("completionConditionタグの子要素「nablarch.tool.workflow.bpmn.Condition(result, 1,)」について、引数に空文字のパラメータを含む場合、精査エラーとなること。",
                    messages, hasItem("空文字となるパラメータを含んではいけません。入力値 = [nablarch.tool.workflow.bpmn.Condition(result, 1,)] id = [usertask5678901234] name = [User Task5]"));
            assertThat("completionConditionタグの子要素「nablarch.tool.workflow.bpmn.Condition(result,,1)」について、引数に空文字のパラメータを含む場合、精査エラーとなること。",
                    messages, hasItem("空文字となるパラメータを含んではいけません。入力値 = [nablarch.tool.workflow.bpmn.Condition(result,,1)] id = [usertask6789012345] name = [User Task6]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(6));
        }
    }

    /**
     * conditionExpressionタグの子要素について、引数に空文字のパラメータを含む場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void containEmptyParamOnFlowProceedCondition() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/invalidCondition/WP1043_containEmptyParamOnFlowProceedCondition_ver1_20140813.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("conditionExpressionタグの子要素「eq( ,result, 1)」について、引数に空文字のパラメータを含む場合、精査エラーとなること。",
                    messages, hasItem("空文字となるパラメータを含んではいけません。入力値 = [eq( ,result, 1)] id = [SequenceFlow_5] name = [SequenceFlow 5]"));
            assertThat("conditionExpressionタグの子要素「eq(result, 1, )」について、引数に空文字のパラメータを含む場合、精査エラーとなること。",
                    messages, hasItem("空文字となるパラメータを含んではいけません。入力値 = [eq(result, 1, )] id = [SequenceFlow_6] name = [SequenceFlow 6]"));
            assertThat("conditionExpressionタグの子要素「eq(result, , 1)」について、引数に空文字のパラメータを含む場合、精査エラーとなること。",
                    messages, hasItem("空文字となるパラメータを含んではいけません。入力値 = [eq(result, , 1)] id = [SequenceFlow_7] name = [SequenceFlow 7]"));
            assertThat("conditionExpressionタグの子要素「nablarch.tool.workflow.bpmn.Condition(,result, 1)」について、引数に空文字のパラメータを含む場合、精査エラーとなること。",
                    messages, hasItem("空文字となるパラメータを含んではいけません。入力値 = [nablarch.tool.workflow.bpmn.Condition(,result, 1)] id = [SequenceFlow_8] name = [SequenceFlow 8]"));
            assertThat("conditionExpressionタグの子要素「nablarch.tool.workflow.bpmn.Condition(result, 1,)」について、引数に空文字のパラメータを含む場合、精査エラーとなること。",
                    messages, hasItem("空文字となるパラメータを含んではいけません。入力値 = [nablarch.tool.workflow.bpmn.Condition(result, 1,)] id = [SequenceFlow_9] name = [SequenceFlow 9]"));
            assertThat("conditionExpressionタグの子要素「nablarch.tool.workflow.bpmn.Condition(result,, 1)」について、引数に空文字のパラメータを含む場合、精査エラーとなること。",
                    messages, hasItem("空文字となるパラメータを含んではいけません。入力値 = [nablarch.tool.workflow.bpmn.Condition(result,, 1)] id = [SequenceFlow_10] name = [SequenceFlow 10]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(6));
        }
    }

    /**
     * completionConditionタグの子要素について、クラス名に空文字を含む場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void containEmptyCharOnCompletionCondition() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/invalidCondition/WP1046_containEmptyCharOnCompletionCondition_ver1_20140813.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("completionConditionタグの子要素「e q」について、引数の括弧内が空の場合、精査エラーとなること。",
                    messages, hasItem("条件の形式が不正です。「完全修飾名(パラメータ1, パラメータ2, ....)」の形式で入力してください。"
                    + "完全修飾名は空白を含んではいけません。パラメータが無い場合、括弧を含んではいけません。"
                    + "完全修飾名の代わりに、あらかじめ登録された省略記法を利用することも出来ます。入力値 = [e q] id = [usertask1234567890] name = [User Task1]"));
            assertThat("completionConditionタグの子要素「e　q」について、引数の括弧内が空の場合、精査エラーとなること。",
                    messages, hasItem("条件の形式が不正です。「完全修飾名(パラメータ1, パラメータ2, ....)」の形式で入力してください。"
                    + "完全修飾名は空白を含んではいけません。パラメータが無い場合、括弧を含んではいけません。"
                    + "完全修飾名の代わりに、あらかじめ登録された省略記法を利用することも出来ます。入力値 = [e　q] id = [usertask2345678901] name = [User Task2]"));
            assertThat("completionConditionタグの子要素「nablarch .tool.workflow.bpmn.Condition」について、引数の括弧内が空の場合、精査エラーとなること。",
                    messages, hasItem("条件の形式が不正です。「完全修飾名(パラメータ1, パラメータ2, ....)」の形式で入力してください。"
                    + "完全修飾名は空白を含んではいけません。パラメータが無い場合、括弧を含んではいけません。"
                    + "完全修飾名の代わりに、あらかじめ登録された省略記法を利用することも出来ます。入力値 = [nablarch .tool.workflow.bpmn.Condition] id = [usertask3456789012] name = [User Task3]"));
            assertThat("completionConditionタグの子要素「nablarch　.tool.workflow.bpmn.Condition」について、引数の括弧内が空の場合、精査エラーとなること。",
                    messages, hasItem("条件の形式が不正です。「完全修飾名(パラメータ1, パラメータ2, ....)」の形式で入力してください。"
                    + "完全修飾名は空白を含んではいけません。パラメータが無い場合、括弧を含んではいけません。"
                    + "完全修飾名の代わりに、あらかじめ登録された省略記法を利用することも出来ます。入力値 = [nablarch　.tool.workflow.bpmn.Condition] id = [usertask4567890123] name = [User Task4]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(4));
        }
    }

    /**
     * conditionExpressionタグの子要素について、クラス名に空文字を含む場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void containEmptyCharOnFlowProceedCondition() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/invalidCondition/WP1046_containEmptyCharOnFlowProceedCondition_ver1_20140813.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("conditionExpressionタグの子要素「e q」について、引数の括弧内が空の場合、精査エラーとなること。",
                    messages, hasItem("条件の形式が不正です。「完全修飾名(パラメータ1, パラメータ2, ....)」の形式で入力してください。"
                    + "完全修飾名は空白を含んではいけません。パラメータが無い場合、括弧を含んではいけません。"
                    + "完全修飾名の代わりに、あらかじめ登録された省略記法を利用することも出来ます。入力値 = [e q] id = [SequenceFlow_5] name = [SequenceFlow 5]"));
            assertThat("conditionExpressionタグの子要素「e　q」について、引数の括弧内が空の場合、精査エラーとなること。",
                    messages, hasItem("条件の形式が不正です。「完全修飾名(パラメータ1, パラメータ2, ....)」の形式で入力してください。"
                    + "完全修飾名は空白を含んではいけません。パラメータが無い場合、括弧を含んではいけません。"
                    + "完全修飾名の代わりに、あらかじめ登録された省略記法を利用することも出来ます。入力値 = [e　q] id = [SequenceFlow_6] name = [SequenceFlow 6]"));
            assertThat("conditionExpressionタグの子要素「nablarch .tool.workflow.bpmn.Condition」について、引数の括弧内が空の場合、精査エラーとなること。",
                    messages, hasItem("条件の形式が不正です。「完全修飾名(パラメータ1, パラメータ2, ....)」の形式で入力してください。"
                    + "完全修飾名は空白を含んではいけません。パラメータが無い場合、括弧を含んではいけません。"
                    + "完全修飾名の代わりに、あらかじめ登録された省略記法を利用することも出来ます。入力値 = [nablarch .tool.workflow.bpmn.Condition] id = [SequenceFlow_8] name = [SequenceFlow 8]"));
            assertThat("conditionExpressionタグの子要素「nablarch　.tool.workflow.bpmn.Condition」について、引数の括弧内が空の場合、精査エラーとなること。",
                    messages, hasItem("条件の形式が不正です。「完全修飾名(パラメータ1, パラメータ2, ....)」の形式で入力してください。"
                    + "完全修飾名は空白を含んではいけません。パラメータが無い場合、括弧を含んではいけません。"
                    + "完全修飾名の代わりに、あらかじめ登録された省略記法を利用することも出来ます。入力値 = [nablarch　.tool.workflow.bpmn.Condition] id = [SequenceFlow_9] name = [SequenceFlow 9]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(4));
        }
    }

    /**
     * completionConditionタグの子要素について、パッケージ名に空文字を含む場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void containEmptyPackageOnCompletionCondition() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/invalidCondition/WP1048_containEmptyPackageOnCompletionCondition_ver1_20140813.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("completionConditionタグの子要素「.nablarch.tool.workflow.bpmn.Condition(result, 1)」について、パッケージ名の先頭が空文字の場合、精査エラーとなること。",
                    messages, hasItem("クラス名、省略記法はピリオドで開始、終了してはいけません。入力値 = [.nablarch.tool.workflow.bpmn.Condition(result, 1)] id = [usertask4567890123] name = [User Task4]"));
            assertThat("completionConditionタグの子要素「nablarch..tool.workflow.bpmn.Condition(result, 1)」について、パッケージ名の途中が空文字の場合、精査エラーとなること。",
                    messages, hasItem("クラス名、省略記法は連続したピリオドを含んではいけません。入力値 = [nablarch..tool.workflow.bpmn.Condition(result, 1)] id = [usertask5678901234] name = [User Task5]"));
            assertThat("completionConditionタグの子要素「nablarch.tool.workflow.bpmn.Condition.(result, 1)」について、パッケージ名の最後が空文字の場合、精査エラーとなること。",
                    messages, hasItem("クラス名、省略記法はピリオドで開始、終了してはいけません。入力値 = [nablarch.tool.workflow.bpmn.Condition.(result, 1)] id = [usertask6789012345] name = [User Task6]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(3));
        }
    }

    /**
     * conditionExpressionタグの子要素について、パッケージ名に空文字を含む場合、精査エラーとなること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void containEmptyPackageOnFlowProceedCondition() throws Exception {
        File inputFile = new File("src/test/java/nablarch/tool/workflow/bpmn/xml/validate/invalidCondition/WP1048_containEmptyPackageOnFlowProceedCondition_ver1_20140813.bpmn");
        @SuppressWarnings("unchecked")
        JAXBElement<TDefinitions> obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(inputFile);
        TDefinitions tDefinitions = obj.getValue();
        try {
            sut.validateWorkflowDefinition(tDefinitions);
            fail();
        } catch (WorkflowDefinitionException e) {
            List<String> messages = e.getMessages();
            assertThat("completionConditionタグの子要素「.nablarch.tool.workflow.bpmn.Condition(result, 1)」について、パッケージ名の先頭が空文字の場合、精査エラーとなること。",
                    messages, hasItem("クラス名、省略記法はピリオドで開始、終了してはいけません。"
                    + "入力値 = [.nablarch.tool.workflow.bpmn.Condition(result, 1)] id = [SequenceFlow_5] name = [SequenceFlow 5]"));
            assertThat("completionConditionタグの子要素「nablarch..tool.workflow.bpmn.Condition(result, 1)」について、パッケージ名の途中が空文字の場合、精査エラーとなること。",
                    messages, hasItem("クラス名、省略記法は連続したピリオドを含んではいけません。"
                    + "入力値 = [nablarch..tool.workflow.bpmn.Condition(result, 1)] id = [SequenceFlow_6] name = [SequenceFlow 6]"));
            assertThat("completionConditionタグの子要素「nablarch.tool.workflow.bpmn.Condition.(result, 1)」について、パッケージ名の最後が空文字の場合、精査エラーとなること。",
                    messages, hasItem("クラス名、省略記法はピリオドで開始、終了してはいけません。"
                    + "入力値 = [nablarch.tool.workflow.bpmn.Condition.(result, 1)] id = [SequenceFlow_7] name = [SequenceFlow 7]"));
            assertThat("期待しない精査エラーが発生していないこと。", messages.size(), is(3));
        }
    }

    /**
     * ワークフロー定義情報へ変換するためのオブジェクトを生成する。
     *
     * @return 変換用オブジェクト
     */
    private static Unmarshaller createUnmarshaller() {
        Unmarshaller unmarshaller;
        try {
            JAXBContext ctx = JAXBContext.newInstance("org.omg.spec.bpmn._20100524.di"
                    + ":org.omg.spec.bpmn._20100524.model"
                    + ":org.omg.spec.dd._20100524.dc"
                    + ":org.omg.spec.dd._20100524.di");
            unmarshaller = ctx.createUnmarshaller();

            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            URL resource = FileUtil.getClasspathResourceURL("workflow/bpmn/BPMN20.xsd");
            unmarshaller.setSchema(factory.newSchema(resource));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Unmarshaller.", e);
        }
        return unmarshaller;
    }
}
