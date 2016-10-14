package nablarch.tool.workflow;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nablarch.core.repository.SystemRepository;
import nablarch.test.Assertion;
import nablarch.tool.util.poi.SimpleTableReader;
import nablarch.tool.workflow.helper.ErrorLogReader;

import nablarch.integration.workflow.definition.loader.WorkflowDefinitionSchema;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsIn.isIn;
import static org.junit.Assert.assertThat;

/**
 * {@link WorkflowDefinitionGenerator}のテストクラス。
 *
 * @author Satoshi Taromaru
 * @since 1.4.2
 */
public class WorkflowDefinitionGeneratorTest {

    /**
     * デフォルトの標準エラー出力先。
     */
    private PrintStream nativeErr = null;

    /**
     * 変更後の標準エラー出力先。
     */
    private final ByteArrayOutputStream snatchedErr = new ByteArrayOutputStream();

    /**
     * 事前処理。
     * 標準エラー出力先を変更する。
     */
    @Before
    public void setUp() {
        nativeErr = System.err;
        System.setErr((new PrintStream(new BufferedOutputStream(snatchedErr))));
    }

    /**
     * 事後処理。
     * ロードしたリポジトリ、プロパティを初期化し標準エラー出力先を元に戻す。
     */
    @After
    public void tearDown() {
        System.setErr(nativeErr);
        SystemRepository.clear();
        System.clearProperty("inputFileDir");
        System.clearProperty("outputFilePath");
        System.clearProperty("logFilePath");
    }

    /**
     * 複数ファイル読み込み。（エラー無）
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void testNormalExit() throws Exception {
        System.setProperty("inputFileDir", "src/test/java/nablarch/tool/workflow/bpmn/xml/read/normal/multiInput/");
        WorkflowDefinitionGenerator.main("classpath:nablarch/tool/workflow/WorkflowDefinitionGeneratorTest.xml");

        // 出力先のブックを取得
        HSSFWorkbook outBook = new HSSFWorkbook(new FileInputStream("src/test/work/MASTER_DATA_WF.xls"));
        SimpleTableReader tableReader = new SimpleTableReader().setHeaderRowNum(1);

        WorkflowDefinitionSchema schema = SystemRepository.get("workflowDefinitionSchema");
        Assertion.assertEqualsIgnoringOrder(getNormalExitWorkflowDefinitionData(schema),
                tableReader.read(outBook.getSheet(schema.getWorkflowDefinitionTableName())));
        Assertion.assertEqualsIgnoringOrder(getNormalExitLaneData(schema), tableReader.read(outBook.getSheet(schema.getLaneTableName())));
        Assertion.assertEqualsIgnoringOrder(getNormalExitFlowNodeData(schema), tableReader.read(outBook.getSheet(schema.getFlowNodeTableName())));
        Assertion.assertEqualsIgnoringOrder(getNormalExitTaskData(schema), tableReader.read(outBook.getSheet(schema.getTaskTableName())));
        Assertion.assertEqualsIgnoringOrder(getNormalExitBoundaryEventData(schema), tableReader.read(outBook.getSheet(schema.getBoundaryEventTableName())));
        Assertion.assertEqualsIgnoringOrder(getNormalExitEventData(schema), tableReader.read(outBook.getSheet(schema.getEventTableName())));
        Assertion.assertEqualsIgnoringOrder(getNormalExitGatewayData(schema), tableReader.read(outBook.getSheet(schema.getGatewayTableName())));
        Assertion.assertEqualsIgnoringOrder(getNormalExitBoundaryEventTriggerData(schema),
                tableReader.read(outBook.getSheet(schema.getEventTriggerTableName())));
        Assertion.assertEqualsIgnoringOrder(getNormalExitSequenceFlowData(schema), tableReader.read(outBook.getSheet(schema.getSequenceFlowTableName())));
    }

    /**
     * 複数ファイル読み込み。（エラー有、エラー無混合）
     * ワークフローID、バージョンの組み合わせが重複した場合、精査エラーとなることを確認。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void duplicateWorkflow() throws Exception {
        String outputFilePath = "src/test/work/MASTER_DATA_WF_HAS_ERROR.xls";
        String logFilePath = "src/test/work/duplicateWorkflow.log";
        System.setProperty("inputFileDir", "src/test/java/nablarch/tool/workflow/bpmn/xml/validate/duplicateWorkflow/");
        System.setProperty("outputFilePath", outputFilePath);
        System.setProperty("logFilePath", logFilePath);

        // テスト実施
        WorkflowDefinitionGenerator.main("classpath:nablarch/tool/workflow/WorkflowDefinitionGeneratorTest.xml");

        // 検証
        System.err.flush();
        assertThat("精査エラーが発生した場合、標準エラー出力にメッセージが表示されること。", snatchedErr.toString(),
                is("精査エラーが発生しました。詳細は次のファイルを確認してください。 [src/test/work/duplicateWorkflow.log]" + System.getProperty("line.separator")));

        List<String> log = new ErrorLogReader(logFilePath).getLog();
        List<String> expectedErrorMessage = new ArrayList<String>();
        expectedErrorMessage.add("fileName = [WP0003_duplicateWorkflow_ver1_20140804.bpmn] ワークフローID、バージョンの組み合わせが重複しています。");
        expectedErrorMessage.add("fileName = [WP0003_duplicateWorkflow_ver1_20140805.bpmn] ワークフローID、バージョンの組み合わせが重複しています。");
        assertThat("ワークフローID、バージョンの組み合わせが重複した場合、精査エラー情報をファイルに出力すること", log.get(0), isIn(expectedErrorMessage));
        assertThat("期待しないメッセージが出力されていないこと。", log.size(), is(1));
        assertThat("入力ファイルの内、一つでも正常な入力ファイルが有る場合、Excelファイルを出力すること。", new File(outputFilePath).exists(), is(true));
    }

    /**
     * 引数が無い場合、標準エラー出力にメッセージ出力されること。
     */
    @Test
    public void noArg() throws IOException {
        String outputFilePath = "test/work/MASTER_DATA_WF_NO_ARG.xls";
        System.setProperty("outputFilePath", outputFilePath);
        WorkflowDefinitionGenerator.main();

        System.err.flush();
        assertThat("引数が無い場合、標準エラー出力にメッセージが表示されること。", snatchedErr.toString(),
                is("引数には、コンポーネント定義ファイルのパスを一つだけ指定してください。（例：java nablarch.tool.workflow.WorkflowDefinitionGenerator WorkflowDefinitionGenerator.xml）"
                        + " 実際の引数 = []" + System.getProperty("line.separator")));
        assertThat("全ての入力ファイルが不正な場合、Excelファイルを出力しないこと。", new File(outputFilePath).exists(), is(false));
    }


    /**
     * 引数の数が多い場合、標準エラー出力にメッセージ出力されること。
     */
    @Test
    public void tooManyArgs() {
        String outputFilePath = "test/work/MASTER_DATA_WF_TOO_MANY_ARG.xls";
        System.setProperty("outputFilePath", outputFilePath);
        WorkflowDefinitionGenerator.main("arg1", "arg2");

        System.err.flush();
        assertThat("引数の数が多い場合、標準エラー出力にメッセージが表示されること。", snatchedErr.toString(),
                is("引数には、コンポーネント定義ファイルのパスを一つだけ指定してください。（例：java nablarch.tool.workflow.WorkflowDefinitionGenerator WorkflowDefinitionGenerator.xml）"
                        + " 実際の引数 = [arg1, arg2]" + System.getProperty("line.separator")));
        assertThat("全ての入力ファイルが不正な場合、Excelファイルを出力しないこと。", new File(outputFilePath).exists(), is(false));
    }

    /**
     * ファイル名精査。
     * 適用日が存在しない日付の場合。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void invalidEffectiveDate() throws Exception {
        String outputFilePath = "src/test/work/MASTER_DATA_WF_INVALID_EFFECTIVE_DATE.xls";
        String logFilePath = "src/test/work/invalidEffectiveDate.log";
        System.setProperty("inputFileDir", "src/test/java/nablarch/tool/workflow/bpmn/xml/validate/invalidFileName/invalidEffectiveDate/");
        System.setProperty("outputFilePath", outputFilePath);
        System.setProperty("logFilePath", logFilePath);
        WorkflowDefinitionGenerator.main("classpath:nablarch/tool/workflow/WorkflowDefinitionGeneratorTest.xml");

        System.err.flush();
        assertThat("適用日に存在しない日付を指定した場合、標準エラー出力にメッセージが表示されること。", snatchedErr.toString(),
                is("精査エラーが発生しました。詳細は次のファイルを確認してください。 [" + logFilePath + "]" + System.getProperty("line.separator")));

        List<String> expectedList = new ArrayList<String>();
        expectedList.add("fileName = [WP1042_invalidEffectiveDate_ver1_20140832.bpmn] 適用日が不正です。有効な日付をYYYYMMDD形式でファイル名に含めてください。 (例: WP0001_workflowName_ver1_20140804.bpmn)");
        assertThat("適用日に存在しない日付を指定した場合、精査エラーとなること。", new ErrorLogReader(logFilePath).getLog(), is(expectedList));
        assertThat("全ての入力ファイルが不正な場合、Excelファイルを出力しないこと。", new File(outputFilePath).exists(), is(false));
    }

    /**
     * ワークフローID桁数精査。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void tooManyWorkflowIdLength() throws Exception {
        String outputFilePath = "src/test/work/MASTER_DATA_WF_TOO_MANY_ID_LENGTH.xls";
        String logFilePath = "src/test/work/tooManyWorkflowIdLength.log";
        System.setProperty("inputFileDir", "src/test/java/nablarch/tool/workflow/bpmn/xml/validate/invalidFileName/tooManyWorkflowIdLength/");
        System.setProperty("outputFilePath", outputFilePath);
        System.setProperty("logFilePath", logFilePath);
        WorkflowDefinitionGenerator.main("classpath:nablarch/tool/workflow/WorkflowDefinitionGeneratorTest.xml");

        System.err.flush();
        assertThat("ワークフローIDがID体系で定められた桁数と異なる場合、標準エラー出力にメッセージが表示されること。", snatchedErr.toString(),
                is("精査エラーが発生しました。詳細は次のファイルを確認してください。 [" + logFilePath + "]" + System.getProperty("line.separator")));

        List<String> expectedList = new ArrayList<String>();
        expectedList.add("fileName = [WP0000001_tooManyWorkflowIdLength_ver1_20140804.bpmn] ワークフローIDの桁数がID体系で定められた桁数と異なります。ファイル名を修正してください。 （設定値:6 実際:9）");
        assertThat("ワークフローIDがID体系で定められた桁数と異なる場合、精査エラーとなること。", new ErrorLogReader(logFilePath).getLog(), is(expectedList));
        assertThat("全ての入力ファイルが不正な場合、Excelファイルを出力しないこと。", new File(outputFilePath).exists(), is(false));
    }

    /**
     * ファイル名精査。
     * 適用日がファイル名に含まれていない場合。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void noEffectiveDate() throws Exception {
        String outputFilePath = "src/test/work/MASTER_DATA_WF_NO_EFFECTIVE_DATE.xls";
        String logFilePath = "src/test/work/noEffectiveDate.log";
        System.setProperty("inputFileDir", "src/test/java/nablarch/tool/workflow/bpmn/xml/validate/invalidFileName/noEffectiveDate/");
        System.setProperty("outputFilePath", outputFilePath);
        System.setProperty("logFilePath", logFilePath);
        WorkflowDefinitionGenerator.main("classpath:nablarch/tool/workflow/WorkflowDefinitionGeneratorTest.xml");

        System.err.flush();
        assertThat("適用日がファイル名に含まれていない場合、標準エラー出力にメッセージが表示されること。", snatchedErr.toString(),
                is("精査エラーが発生しました。詳細は次のファイルを確認してください。 [" + logFilePath + "]" + System.getProperty("line.separator")));

        List<String> expectedList = new ArrayList<String>();
        expectedList.add("fileName = [WP1043_noEffectiveDate_ver1.bpmn] ファイル名が不正です。"
                + "ファイル名を[ワークフローID]_[ワークフロー名]_ver[バージョン]_[YYYYMMDD].[拡張子]に修正してください。 (例: WP0001_workflowName_ver1_20140804.bpmn)");
        assertThat("適用日がファイル名に含まれていない場合、精査エラーとなること。", new ErrorLogReader(logFilePath).getLog(), is(expectedList));
        assertThat("全ての入力ファイルが不正な場合、Excelファイルを出力しないこと。", new File(outputFilePath).exists(), is(false));
    }

    /**
     * ファイル名精査。
     * 適用日がファイル名に含まれていない場合。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void emptyEffectiveDate() throws Exception {
        String outputFilePath = "src/test/work/MASTER_DATA_WF_EMPTY_EFFECTIVE_DATE.xls";
        String logFilePath = "src/test/work/emptyEffectiveDate.log";
        System.setProperty("inputFileDir", "src/test/java/nablarch/tool/workflow/bpmn/xml/validate/invalidFileName/emptyEffectiveDate/");
        System.setProperty("outputFilePath", outputFilePath);
        System.setProperty("logFilePath", logFilePath);
        WorkflowDefinitionGenerator.main("classpath:nablarch/tool/workflow/WorkflowDefinitionGeneratorTest.xml");

        System.err.flush();
        assertThat("適用日がファイル名に含まれていない場合、標準エラー出力にメッセージが表示されること。", snatchedErr.toString(),
                is("精査エラーが発生しました。詳細は次のファイルを確認してください。 [" + logFilePath + "]" + System.getProperty("line.separator")));

        List<String> expectedList = new ArrayList<String>();
        expectedList.add("fileName = [WP1044_emptyEffectiveDate_ver1_.bpmn] ファイル名が不正です。"
                + "ファイル名を[ワークフローID]_[ワークフロー名]_ver[バージョン]_[YYYYMMDD].[拡張子]に修正してください。 (例: WP0001_workflowName_ver1_20140804.bpmn)");
        assertThat("適用日がファイル名に含まれていない場合、精査エラーとなること。", new ErrorLogReader(logFilePath).getLog(), is(expectedList));
        assertThat("全ての入力ファイルが不正な場合、Excelファイルを出力しないこと。", new File(outputFilePath).exists(), is(false));
    }

    /**
     * ファイル名精査。
     * バージョンがファイル名に含まれていない場合。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void noVersion() throws Exception {
        String outputFilePath = "src/test/work/MASTER_DATA_WF_NO_VERSION.xls";
        String logFilePath = "src/test/work/noVersion.log";
        System.setProperty("inputFileDir", "src/test/java/nablarch/tool/workflow/bpmn/xml/validate/invalidFileName/noVersion/");
        System.setProperty("outputFilePath", outputFilePath);
        System.setProperty("logFilePath", logFilePath);
        WorkflowDefinitionGenerator.main("classpath:nablarch/tool/workflow/WorkflowDefinitionGeneratorTest.xml");

        System.err.flush();
        assertThat("バージョンがファイル名に含まれていない場合、標準エラー出力にメッセージが表示されること。",
                snatchedErr.toString(), is("精査エラーが発生しました。詳細は次のファイルを確認してください。 [" + logFilePath + "]" + System.getProperty("line.separator")));

        List<String> expectedList = new ArrayList<String>();
        expectedList.add("fileName = [WP1045_noVersion_20140811.bpmn] ファイル名が不正です。"
                + "ファイル名を[ワークフローID]_[ワークフロー名]_ver[バージョン]_[YYYYMMDD].[拡張子]に修正してください。 (例: WP0001_workflowName_ver1_20140804.bpmn)");
        assertThat("バージョンがファイル名に含まれていない場合、精査エラーとなること。", new ErrorLogReader(logFilePath).getLog(), is(expectedList));
        assertThat("全ての入力ファイルが不正な場合、Excelファイルを出力しないこと。", new File(outputFilePath).exists(), is(false));
    }

    /**
     * ファイル名精査。
     * バージョンがファイル名に含まれていない場合。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void emptyVersion() throws Exception {
        String outputFilePath = "src/test/work/MASTER_DATA_WF_NO_VERSION2.xls";
        String logFilePath = "src/test/work/emptyVersion.log";
        System.setProperty("inputFileDir", "src/test/java/nablarch/tool/workflow/bpmn/xml/validate/invalidFileName/emptyVersion/");
        System.setProperty("outputFilePath", outputFilePath);
        System.setProperty("logFilePath", logFilePath);
        WorkflowDefinitionGenerator.main("classpath:nablarch/tool/workflow/WorkflowDefinitionGeneratorTest.xml");

        System.err.flush();
        assertThat("バージョンがファイル名に含まれていない場合、標準エラー出力にメッセージが表示されること。", snatchedErr.toString(),
                is("精査エラーが発生しました。詳細は次のファイルを確認してください。 [" + logFilePath + "]" + System.getProperty("line.separator")));

        List<String> expectedList = new ArrayList<String>();
        expectedList.add("fileName = [WP1046_emptyVersion_ver_20140811.bpmn] ファイル名が不正です。"
                + "ファイル名を[ワークフローID]_[ワークフロー名]_ver[バージョン]_[YYYYMMDD].[拡張子]に修正してください。 (例: WP0001_workflowName_ver1_20140804.bpmn)");
        assertThat("バージョンがファイル名に含まれていない場合、精査エラーとなること。", new ErrorLogReader(logFilePath).getLog(), is(expectedList));
        assertThat("全ての入力ファイルが不正な場合、Excelファイルを出力しないこと。", new File(outputFilePath).exists(), is(false));
    }

    /**
     * Validator内で精査エラーが発生した場合。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void validateError() throws Exception {
        String outputFilePath = "src/test/work/MASTER_DATA_WF_VALIDATE_ERROR.xls";
        String logFilePath = "src/test/work/validateError.log";
        System.setProperty("inputFileDir", "src/test/java/nablarch/tool/workflow/bpmn/xml/read/invalid/validateError/");
        System.setProperty("outputFilePath", outputFilePath);
        System.setProperty("logFilePath", logFilePath);
        WorkflowDefinitionGenerator.main("classpath:nablarch/tool/workflow/WorkflowDefinitionGeneratorTest.xml");

        System.err.flush();
        assertThat("Validator内で精査エラーが発生した場合、標準エラー出力にメッセージが表示されること。", snatchedErr.toString(),
                is("精査エラーが発生しました。詳細は次のファイルを確認してください。 [" + logFilePath + "]" + System.getProperty("line.separator")));

        List<String> expectedList = new ArrayList<String>();
        expectedList.add("fileName = [WP0006_validateError_ver1_20140804.bpmn] ワークフローには、必ず一つのプールを配置してください。");
        assertThat(new ErrorLogReader(logFilePath).getLog(), is(expectedList));

        HSSFWorkbook outBook = new HSSFWorkbook(new FileInputStream(outputFilePath));
        SimpleTableReader tableReader = new SimpleTableReader().setHeaderRowNum(1);
        WorkflowDefinitionSchema schema = SystemRepository.get("workflowDefinitionSchema");
        assertThat(tableReader.read(outBook.getSheet(schema.getWorkflowDefinitionTableName())), is(getFailExitWorkflowDefinitionData(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getLaneTableName())), is(getFailExitLaneData(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getFlowNodeTableName())), is(getFailExitFlowNodeData(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getTaskTableName())), is(getFailExitTaskData(schema)));
        List<Map<String, String>> emptyList = new ArrayList<Map<String, String>>();
        assertThat(tableReader.read(outBook.getSheet(schema.getBoundaryEventTableName())), is(emptyList));
        assertThat(tableReader.read(outBook.getSheet(schema.getEventTableName())), is(getFailExitEventData(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getGatewayTableName())), is(getFailExitGatewayData(schema)));
        assertThat(tableReader.read(outBook.getSheet(schema.getEventTriggerTableName())), is(emptyList));
        assertThat(tableReader.read(outBook.getSheet(schema.getSequenceFlowTableName())), is(getFailExitSequenceFlowData(schema)));
        assertThat("入力ファイルの内、一つでも正常な入力ファイルが有る場合、Excelファイルを出力すること。", new File(outputFilePath).exists(), is(true));
    }

    /**
     * unmarshal時、xml不正を検知した場合。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void unmarshalErrorAnomalyDetection() throws Exception {
        String outputFilePath = "src/test/work/MASTER_DATA_WF_ANOMALY_DETECTION.xls";
        String logFilePath = "src/test/work/unmarshalErrorAnomalyDetection.log";
        System.setProperty("inputFileDir", "src/test/java/nablarch/tool/workflow/bpmn/xml/read/invalid/unmarshalAnomalyDetection/");
        System.setProperty("outputFilePath", outputFilePath);
        System.setProperty("logFilePath", logFilePath);
        WorkflowDefinitionGenerator.main("classpath:nablarch/tool/workflow/WorkflowDefinitionGeneratorTest.xml");

        System.err.flush();
        assertThat("unmarshal時、xml不正を検知した場合、標準エラー出力にメッセージが表示されること。", snatchedErr.toString(),
                is("精査エラーが発生しました。詳細は次のファイルを確認してください。 [" + logFilePath + "]" + System.getProperty("line.separator")));

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFilePath), "UTF-8"));
        assertThat(Pattern.compile("fileName = \\[WP0004_duplicateId_ver1_20140804.bpmn\\] bpmn定義ファイル読み込み時にデータ不正を検知しました。bpmn定義ファイルが、モデリングツールで正常に開けるか確認してください。"
                + " lineNumber = \\[22\\] errorLevel = \\[FATAL_ERROR\\] errorMessage = \\[.*\\]").matcher(reader.readLine()).matches(), is(true));
        assertThat(Pattern.compile("fileName = \\[WP0004_duplicateId_ver1_20140804.bpmn\\] bpmn定義ファイル読み込み時にデータ不正を検知しました。bpmn定義ファイルが、モデリングツールで正常に開けるか確認してください。"
                + " lineNumber = \\[22\\] errorLevel = \\[ERROR\\] errorMessage = \\[.*\\]").matcher(reader.readLine()).matches(), is(true));
        assertThat("全ての入力ファイルが不正な場合、Excelファイルを出力しないこと。", new File(outputFilePath).exists(), is(false));
    }

    /**
     * ワークフロー定義ファイル格納ディレクトリの指定が、存在しないディレクトリを指している場合
     * 標準エラー出力にメッセージを出力すること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void invalidInputFileDir() throws Exception {
        String invalidInputFileDir = "invalidDir";
        String outputFilePath = "src/test/work/MASTER_DATA_WF_INVALID_INPUT_DIR.xls";
        System.setProperty("inputFileDir", invalidInputFileDir);
        System.setProperty("outputFilePath", outputFilePath);
        WorkflowDefinitionGenerator.main("classpath:nablarch/tool/workflow/WorkflowDefinitionGeneratorTest.xml");
        System.err.flush();
        assertThat("ワークフロー定義ファイル格納ディレクトリの指定が不正な場合、標準エラー出力にメッセージが表示されること。",
                snatchedErr.toString(), is("ワークフロー定義ファイル格納ディレクトリが不正です。存在するディレクトリを指定してください。 設定値 = [" + invalidInputFileDir + "]" + System.getProperty("line.separator")));
        assertThat("全ての入力ファイルが不正な場合、Excelファイルを出力しないこと。", new File(outputFilePath).exists(), is(false));
    }

    /**
     * ワークフロー定義ファイル格納ディレクトリの指定が、ファイルパスを指している場合
     * 標準エラー出力にメッセージを出力すること。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void invalidInputFilePath() throws Exception {
        String invalidInputFileDir = "src/test/java/nablarch/tool/workflow/bpmn/xml/read/normal/normalWorkflow/WP0001_normalWorkflow_ver1_20140804.bpmn";
        System.setProperty("inputFileDir", invalidInputFileDir);
        String outputFilePath = "src/test/work/MASTER_DATA_WF_INVALID_INPUT_Path.xls";
        System.setProperty("outputFilePath", outputFilePath);
        WorkflowDefinitionGenerator.main("classpath:nablarch/tool/workflow/WorkflowDefinitionGeneratorTest.xml");
        System.err.flush();
        assertThat("ワークフロー定義ファイル格納ディレクトリの指定が、ファイルパスを指している場合、標準エラー出力にメッセージが表示されること。", snatchedErr.toString(),
                is("ワークフロー定義ファイル格納ディレクトリが不正です。ディレクトリを指定してください。 設定値 = [" + invalidInputFileDir + "]" + System.getProperty("line.separator")));
        assertThat("全ての入力ファイルが不正な場合、Excelファイルを出力しないこと。", new File(outputFilePath).exists(), is(false));
    }

    /**
     * ワークフロー定義の期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return 期待値
     */
    private List<Map<String, String>> getNormalExitWorkflowDefinitionData(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"WP0001", "1", "normalWorkflow", "20140804"},
                {"WP0002", "1", "sequenceFlowReadTest", "20140814"}
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
     * タスクの期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return 期待値
     */
    private List<Map<String, String>> getNormalExitTaskData(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"WP0001", "1", "usertask1234567890", "NONE", ""},
                {"WP0001", "1", "usertask2345678901", "PARALLEL", "nablarch.tool.workflow.SampleCompletionCondition"},
                {"WP0001", "1", "usertask3456789012", "SEQUENTIAL", "nablarch.tool.workflow.SampleCompletionCondition(1)"},
                {"WP0002", "1", "usertask1234567890", "NONE", ""}
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
     * レーンの期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return 期待値
     */
    private List<Map<String, String>> getNormalExitLaneData(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"WP0001", "1", "laneId1", "laneName1"},
                {"WP0001", "1", "laneId2", "laneName2"},
                {"WP0002", "1", "laneId1", "Lane 1"}
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
    private List<Map<String, String>> getNormalExitFlowNodeData(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"WP0001", "1", "endevent1234567890", "laneId2", "TerminateEndEvent"},
                {"WP0001", "1", "startevent12345678", "laneId1", "Start"},
                {"WP0001", "1", "terminateendevent1", "laneId1", "TerminateEndEvent"},
                {"WP0001", "1", "boundarymessage123", "laneId2", "Message"},
                {"WP0001", "1", "usertask1234567890", "laneId1", "User Task1"},
                {"WP0001", "1", "usertask2345678901", "laneId2", "User Task2"},
                {"WP0001", "1", "usertask3456789012", "laneId2", "User Task3"},
                {"WP0001", "1", "exclusivegateway12", "laneId2", "Exclusive Gateway1"},
                {"WP0001", "1", "exclusivegateway23", "laneId2", "Exclusive Gateway2"},
                {"WP0002", "1", "startevent12345678", "laneId1", "startevent_name"},
                {"WP0002", "1", "terminateEndEvent1", "laneId1", "terminateEndEvent_name"},
                {"WP0002", "1", "usertask1234567890", "laneId1", "usertask_name"},
                {"WP0002", "1", "exclusivegateway12", "laneId1", "exclusivegateway_name"}
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
     * 境界イベントの期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return 期待値
     */
    private List<Map<String, String>> getNormalExitBoundaryEventData(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"WP0001", "1", "boundarymessage123", "MG001", "usertask2345678901"}
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
    private List<Map<String, String>> getNormalExitEventData(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"WP0001", "1", "endevent1234567890", "TERMINATE"},
                {"WP0001", "1", "startevent12345678", "START"},
                {"WP0001", "1", "terminateendevent1", "TERMINATE"},
                {"WP0002", "1", "startevent12345678", "START"},
                {"WP0002", "1", "terminateEndEvent1", "TERMINATE"}
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
    private List<Map<String, String>> getNormalExitGatewayData(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"WP0001", "1", "exclusivegateway12", "EXCLUSIVE"},
                {"WP0001", "1", "exclusivegateway23", "EXCLUSIVE"},
                {"WP0002", "1", "exclusivegateway12", "EXCLUSIVE"}
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
    private List<Map<String, String>> getNormalExitBoundaryEventTriggerData(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"WP0001", "1", "MG001", "MG001"}
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
    private List<Map<String, String>> getNormalExitSequenceFlowData(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"WP0001", "1", "flow1", "startevent12345678", "usertask1234567890", "", "to UserTask1"},
                {"WP0001", "1", "flow2", "usertask1234567890", "usertask2345678901", "", "to User Task2"},
                {"WP0001", "1", "flow3", "boundarymessage123", "usertask1234567890", "", "to User Task1"},
                {"WP0001", "1", "flow4", "usertask2345678901", "exclusivegateway12", "", "to Exclusive Gateway1"},
                {"WP0001", "1", "flow5", "exclusivegateway12", "usertask1234567890", "nablarch.tool.workflow.SampleFlowProceedCondition(condition,1)", "to User Task1"},
                {"WP0001", "1", "flow6", "exclusivegateway12", "exclusivegateway23", "nablarch.tool.workflow.SampleFlowProceedCondition", "to Exclusive Gateway2"},
                {"WP0001", "1", "flow7", "exclusivegateway23", "usertask3456789012", "jp.co.tis.workflow.CustomCondition(result, 1)", "to User Task3"},
                {"WP0001", "1", "flow8", "exclusivegateway23", "terminateendevent1", "nablarch.tool.workflow.SampleFlowProceedCondition", "to TerminateEndEvent"},
                {"WP0001", "1", "flow9", "usertask3456789012", "endevent1234567890", "", "to TerminateEndEvent"},
                {"WP0002", "1", "flow1", "startevent12345678", "usertask1234567890", "", "to UserTask"},
                {"WP0002", "1", "flow2", "usertask1234567890", "exclusivegateway12", "", "to ExclusiveGateway"},
                {"WP0002", "1", "flow3", "exclusivegateway12", "terminateEndEvent1", "nablarch.tool.workflow.SampleFlowProceedCondition", "to End"}
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
     * ワークフロー定義の期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return 期待値
     */
    private List<Map<String, String>> getFailExitWorkflowDefinitionData(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"WP0003", "1", "normalWorkflow", "20140804"}
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
     * タスクの期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return 期待値
     */
    private List<Map<String, String>> getFailExitTaskData(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"WP0003", "1", "usertask1234567890", "NONE", ""}
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
     * レーンの期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return 期待値
     */
    private List<Map<String, String>> getFailExitLaneData(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"WP0003", "1", "laneId1", "Lane 1"}
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
    private List<Map<String, String>> getFailExitFlowNodeData(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"WP0003", "1", "startevent12345678", "laneId1", "startevent_name"},
                {"WP0003", "1", "terminateEndEvent1", "laneId1", "terminateEndEvent_name"},
                {"WP0003", "1", "usertask1234567890", "laneId1", "usertask_name"},
                {"WP0003", "1", "exclusivegateway12", "laneId1", "exclusivegateway_name"}
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
     * イベントの期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return 期待値
     */
    private List<Map<String, String>> getFailExitEventData(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"WP0003", "1", "startevent12345678", "START"},
                {"WP0003", "1", "terminateEndEvent1", "TERMINATE"}
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
    private List<Map<String, String>> getFailExitGatewayData(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"WP0003", "1", "exclusivegateway12", "EXCLUSIVE"}
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
     * シーケンスフローの期待値を取得する。
     *
     * @param schema スキーマ情報
     * @return 期待値
     */
    private List<Map<String, String>> getFailExitSequenceFlowData(WorkflowDefinitionSchema schema) {
        String[][] exp = {
                {"WP0003", "1", "flow1", "startevent12345678", "usertask1234567890", "", "to UserTask"},
                {"WP0003", "1", "flow2", "usertask1234567890", "exclusivegateway12", "", "to ExclusiveGateway"},
                {"WP0003", "1", "flow3", "exclusivegateway12", "terminateEndEvent1", "nablarch.tool.workflow.SampleFlowProceedCondition", "to End"}
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
