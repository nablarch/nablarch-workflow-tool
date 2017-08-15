package nablarch.tool;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.logging.Log;

import nablarch.core.util.StringUtil;
import nablarch.tool.workflow.WorkflowDefinitionException;
import nablarch.tool.workflow.WorkflowDefinitionFile;

/**
 * ユーティリティ群。
 *
 * @author siosio
 */
public class Utils {
    
    /**
     * ファイル名Pattern。
     */
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("(.+)_(.+)_ver(.+)_(.+)\\.(.+)");

    /**
     * フロー進行条件、完了条件のクラス名、パラメータ抽出用正規表現。
     */
    private static final Pattern CONDITION_PATTERN = Pattern.compile("([^　\\s\\(]+)(?:\\(([^\\)]+)\\))?");

    /**
     * ワークフロー定義ファイル格納ディレクトリ内の、読み取り対象ワークフロー定義ファイルを取得する。
     *
     * @param inputDir ワークフロー定義ファイル格納ディレクトリ
     * @param log logger
     * @return 読み取り対象ワークフロー定義ファイル
     */
    public static List<WorkflowDefinitionFile> findDefinitionFiles(final File inputDir,
            final Log log) {

        final File[] filteredFiles = inputDir.listFiles(new WorkflowDefinitionFileFilter());
        if (filteredFiles == null) {
            return Collections.emptyList();
        }
        
        final List<WorkflowDefinitionFile> files = new ArrayList<WorkflowDefinitionFile>();
        for (File file : filteredFiles) {
            if (file.isDirectory()) {
                files.addAll(findDefinitionFiles(file, log));
                continue;
            }

            String fileName = file.getName();
            try {
                WorkflowDefinitionFile definitionFile = parseDefinitionFileName(file);
                if (files.contains(definitionFile)) {
                    throw new WorkflowDefinitionException("ワークフローID、バージョンの組み合わせが重複しています。");
                }
                files.add(definitionFile);

            } catch (WorkflowDefinitionException e) {
                ErrorPrinter.print(fileName, e.getMessages(), log);
            }
        }

        return files;
    }
    
    /**
     * ファイル名を精査し、ワークフロー定義ファイル情報を返却する。
     *
     * @param file bpmnファイル
     * @return ワークフロー定義ファイル情報
     */
    private static WorkflowDefinitionFile parseDefinitionFileName(final File file) {
        final String fileName = file.getName();
        final Matcher m = FILE_NAME_PATTERN.matcher(fileName);
        if (!m.matches()) {
            throw new WorkflowDefinitionException("ファイル名が不正です。ファイル名を[ワークフローID]_[ワークフロー名]_ver[バージョン]_[YYYYMMDD].[拡張子]に修正してください。"
                    + " (例: WP0001_workflowName_ver1_20140804.bpmn)");
        }
        final String workflowId = m.group(1);
        final String effectiveDate = m.group(4);
        return new WorkflowDefinitionFile(fileName, file.getPath(), workflowId, m.group(2), m.group(3), effectiveDate);
    }

    /**
     * フロー進行条件、完了条件の入力値に対し、フォーマット精査を行う。
     * 入力値に対し、以下の精査を行う。
     * <pre>
     *     クラス名（省略記法） + (パラメータ1, パラメータ2, ....)の形式であること。※パラメータが無い場合、括弧を書かないこと
     *     クラス名（省略記法）部分に、空白文字（全角空白を含む）を含まないこと。
     *     クラス名（省略記法）部分が、.（ピリオド）で開始、終了していないこと。
     *     クラス名（省略記法）部分に、連続する.（ピリオド）を含まないこと。
     *     空白文字のみのパラメータが指定されていないこと。
     *
     *     例）
     *     "Condition" ※パラメータが無い場合、括弧は不要
     *     "test.Condition(param , 1)" ※ピリオドを含む場合、ピリオド区切りであること
     *     "Condition( param , 1 )" ※括弧内の半角スペースは許容する
     * </pre>
     *
     * @param condition フロー進行条件または完了条件
     * @param id        シーケンスフローIDまたはフローノードID
     * @param name      シーケンスフロー名またはフローノード名
     * @return 精査エラーメッセージ
     */
    public static List<String> validateConditionFormat(String condition, String id, String name) {
        final List<String> errorList = new ArrayList<String>();

        final Matcher matcher = CONDITION_PATTERN.matcher(condition);
        if (!matcher.matches()) {
            errorList.add(String.format("条件の形式が不正です。「完全修飾名(パラメータ1, パラメータ2, ....)」の形式で入力してください。"
                    + "完全修飾名は空白を含んではいけません。パラメータが無い場合、括弧を含んではいけません。"
                    + "完全修飾名の代わりに、あらかじめ登録された省略記法を利用することも出来ます。入力値 = [%s] id = [%s] name = [%s]", condition, id, name));
            return errorList;
        }

        String className = matcher.group(1);
        if (className.startsWith(".") || className.endsWith(".")) {
            errorList.add(String.format("クラス名、省略記法はピリオドで開始、終了してはいけません。入力値 = [%s] id = [%s] name = [%s]", condition, id, name));
        }
        if (className.contains("..")) {
            errorList.add(String.format("クラス名、省略記法は連続したピリオドを含んではいけません。入力値 = [%s] id = [%s] name = [%s]", condition, id, name));
        }

        String params = matcher.group(2);
        if (params == null) {
            return errorList;
        }
        for (String param : params.split(",", -1)) {
            if (StringUtil.isNullOrEmpty(param.trim())) {
                errorList.add(String.format("空文字となるパラメータを含んではいけません。入力値 = [%s] id = [%s] name = [%s]", condition, id, name));
                return errorList;
            }
        }

        return errorList;
    }

    /**
     * ワークフロー定義ファイルの抽出フィルタ。
     */
    private static class WorkflowDefinitionFileFilter implements FileFilter {

        @Override
        public boolean accept(File file) {
            if (file.isDirectory()) {
                return true;
            }
            return file.getName()
                       .endsWith(".bpmn");
        }
    }
}
