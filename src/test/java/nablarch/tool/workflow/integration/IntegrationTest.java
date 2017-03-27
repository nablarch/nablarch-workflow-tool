package nablarch.tool.workflow.integration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nablarch.core.ThreadContext;
import nablarch.core.db.connection.AppDbConnection;
import nablarch.core.db.connection.ConnectionFactory;
import nablarch.core.db.connection.DbConnectionContext;
import nablarch.core.db.connection.TransactionManagerConnection;
import nablarch.core.db.statement.SqlPStatement;
import nablarch.core.db.statement.exception.SqlStatementException;
import nablarch.core.db.transaction.SimpleDbTransactionExecutor;
import nablarch.core.db.transaction.SimpleDbTransactionManager;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.transaction.TransactionContext;
import nablarch.core.util.FileUtil;
import nablarch.test.core.db.MasterDataSetUpper;
import nablarch.tool.workflow.WorkflowDefinitionGenerator;

import nablarch.integration.workflow.WorkflowInstance;
import nablarch.integration.workflow.WorkflowManager;
import nablarch.integration.workflow.definition.WorkflowDefinitionHolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;

/**
 * ワークフロー定義生成ツールとワークフローエンジン機能の機能間テストを行うクラス。
 *
 * @author Satoshi Taromaru
 * @since 1.4.2
 */
public class IntegrationTest {
    /**
     * DDLパス
     */
    private static final String DDL_PATH = "nablarch/tool/workflow/integration/forward_with_drop.sql";

    /**
     * テスト用ワークフローID。
     */
    private static final String TEST_WORKFLOW_ID = "WF1101";

    /**
     * 申請者ユーザID。
     */
    public static final String APPLICATION_USER = "0000000001";

    /**
     * 庶務担当者ユーザID。
     */
    public static final String GENERAL_USER = "0000000002";

    /**
     * 庶務グループ
     */
    private static final String APPROVAL_SECTION = "L3";

    /**
     * 承認者1ユーザID。
     */
    public static final String APPROVAL_USER_1 = "0000000003";

    /**
     * 承認者2ユーザID。
     */
    public static final String APPROVAL_USER_2 = "0000000004";

    @Before
    public void setUp() throws Exception {

        // テーブルセットアップ
        SystemRepository.clear();
        SystemRepository.load(new DiContainer(new XmlComponentDefinitionLoader("nablarch/tool/workflow/integration/default-definition.xml")));
        createWorkflowProcessDefinitionTable(loadSql());

        // 業務トランザクション用コネクションの登録
        ConnectionFactory connectionFactory = SystemRepository.get("connectionFactory");
        TransactionManagerConnection connection = connectionFactory.getConnection(TransactionContext.DEFAULT_TRANSACTION_CONTEXT_KEY);
        DbConnectionContext.setConnection(connection);

        // ワークフロー定義生成ツール実行
        WorkflowDefinitionGenerator.main("classpath:nablarch/tool/workflow/integration/IntegrationTest.xml");

        // DBセットアップ
        MasterDataSetUpper.main("classpath:nablarch/tool/workflow/integration/master-data.xml", "src/test/work/MASTER_DATA_WF_IT.xls");

        SystemRepository.load(new DiContainer(new XmlComponentDefinitionLoader("nablarch/tool/workflow/integration/default-definition.xml")));
        WorkflowDefinitionHolder workflowDefinitionHolder = SystemRepository.get("workflowDefinitionHolder");
        workflowDefinitionHolder.initialize();
    }

    @After
    public void tearDown() throws Exception {
        ThreadContext.clear();
        SystemRepository.clear();
    }

    /**
     * 開始イベントから終了イベントまで進み、
     * 各タスク終了後のワークフローインスタンスの状態を確認する。
     *
     * @throws Exception 想定外エラー
     */
    @Test
    public void testStatus() throws Exception {
        // 開始
        WorkflowInstance workflow = startWorkflow();
        assertStartWorkflow(workflow);

        // 引戻し
        cancelWorkflow(workflow);
        assertThat(workflow.isActive("T002"), is(true));

        // 再申請
        completeTask(workflow, APPLICATION_USER);
        assertThat(workflow.isActive("T001"), is(true));

        // 確認
        confirmWorkflow(workflow);
        assertThat(workflow.isActive("T003"), is(true));
        assertThat(workflow.hasActiveUserTask(APPROVAL_USER_1), is(true));
        assertThat(workflow.hasActiveUserTask(APPROVAL_USER_2), is(true));

        // 承認
        completeTask(workflow, APPROVAL_USER_1);
        assertThat(workflow.isActive("T004"), is(true));
        assertThat(workflow.hasActiveUserTask(APPROVAL_USER_1), is(true));
        assertThat(workflow.hasActiveUserTask(APPROVAL_USER_2), is(false));

        // 最終承認（一人目）
        completeTask(workflow, APPROVAL_USER_1);
        assertThat(workflow.isActive("T004"), is(true));
        assertThat(workflow.hasActiveUserTask(APPROVAL_USER_1), is(false));
        assertThat(workflow.hasActiveUserTask(APPROVAL_USER_2), is(true));

        // 最終承認（二人目）
        completeTask(workflow, APPROVAL_USER_2);
        assertThat(workflow.isCompleted(), is(true));
    }

    /**
     * ワークフローを開始させる。</br>
     * ユーザ：申請者
     *
     * @return ワークフローインスタンス
     */
    private WorkflowInstance startWorkflow() {
        ThreadContext.setUserId(APPLICATION_USER);
        WorkflowInstance workflow = WorkflowManager.startInstance(TEST_WORKFLOW_ID);
        workflow.assignUser("T001", GENERAL_USER);
        workflow.assignUser("T002", APPLICATION_USER);
        List<String> users = new ArrayList<String>();
        users.add(APPROVAL_USER_1);
        users.add(APPROVAL_USER_2);
        workflow.assignUsersToLane(APPROVAL_SECTION, users);
        commit();
        return workflow;
    }

    /**
     * 引戻しを行う。
     * ユーザ：申請者
     *
     * @param workflow ワークフローインスタンス
     */
    private void cancelWorkflow(WorkflowInstance workflow) {
        ThreadContext.setUserId(APPLICATION_USER);
        workflow.triggerEvent("BT01");
        commit();
    }

    /**
     * 確認タスクを完了させる。
     * ユーザ：申請者
     *
     * @param workflow ワークフローインスタンス
     */
    private void confirmWorkflow(WorkflowInstance workflow) {
        ThreadContext.setUserId(GENERAL_USER);
        Map<String, String> conditionParam = new HashMap<String, String>();
        conditionParam.put("condition", "0");
        workflow.completeUserTask(conditionParam);
        commit();
    }

    /**
     * 指定したユーザでタスクを完了させる。
     *
     * @param workflow ワークフローインスタンス
     * @param userId   ユーザID
     */
    private void completeTask(WorkflowInstance workflow, String userId) {
        ThreadContext.setUserId(userId);
        workflow.completeUserTask();
        commit();
    }

    /**
     * スタート直後のワークフローインスタンスの状態をアサートする。
     * <pre>
     * ワークフローを開始時、モデリングツールで設定した開始イベントの遷移先ノードがアクティブになっていることを確認する。
     *
     * レーン割り当ての結果、モデリングツールで作成したレーン上のタスクに正しく割り当てられていることを確認する。
     * ・「承認」のアサインユーザがT003、T004であること（並列のため、順番は考慮しない）
     * ・「最終承認」のアサインユーザがT003、T004であること（順列のため、アサイン順と等しいことを確認する）
     * </pre>
     *
     * @param workflow ワークフローインスタンス
     */
    private void assertStartWorkflow(WorkflowInstance workflow) {
        assertThat(workflow.isActive("T001"), is(true));
        // 並列の場合、順番は考慮しない
        assertThat(workflow.getAssignedUsers("T003"), is(containsInAnyOrder(APPROVAL_USER_1, APPROVAL_USER_2)));
        // 順列の場合、アサインした順番であること
        assertThat(workflow.getAssignedUsers("T004"), is(contains(APPROVAL_USER_1, APPROVAL_USER_2)));
    }

    /**
     * コミットを行う。
     */
    public void commit() {
        TransactionManagerConnection connection = (TransactionManagerConnection) DbConnectionContext.getConnection();
        connection.commit();
    }

    /**
     * ワークフロープロセス定義テーブルを構築する。
     */
    public void createWorkflowProcessDefinitionTable(final List<String> sqlList) {
        SimpleDbTransactionManager transactionManager = SystemRepository.get("testFwTran");
        new SimpleDbTransactionExecutor<Void>(transactionManager) {
            @Override
            public Void execute(AppDbConnection connection) {
                for (String sql : sqlList) {
                    SqlPStatement statement = connection.prepareStatement(sql);
                    try {
                        statement.execute();
                    } catch (SqlStatementException ignored) {
                        ignored.printStackTrace();
                    }
                }
                return null;
            }
        }.doTransaction();
    }

    /**
     * SQLファイルからSQL文をロードする。
     *
     * @return ワークフロープロセス定義テーブルセットアップ用SQL文
     */
    private List<String> loadSql() throws Exception {
        List<String> sqlList = new ArrayList<String>();
        InputStream resource = FileUtil.getClasspathResource(DDL_PATH);
        if (resource == null) {
            throw new IllegalStateException("forward_with_drop.sqlが見つかりません.");
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(resource, "UTF-8"));
        while (true) {
            String sql = reader.readLine();
            if (sql == null) {
                break;
            }
            sqlList.add(sql);
        }
        return sqlList;
    }
}
