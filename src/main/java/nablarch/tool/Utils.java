package nablarch.tool;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * ワークフロー定義ファイル格納ディレクトリ内の、読み取り対象ワークフロー定義ファイルを取得する。
     *
     * @param inputDir ワークフロー定義ファイル格納ディレクトリ
     * @return 読み取り対象ワークフロー定義ファイル
     */
    public static List<WorkflowDefinitionFile> findDefinitionFiles(final File inputDir) {

        final File[] filteredFiles = inputDir.listFiles(new WorkflowDefinitionFileFilter());
        if (filteredFiles == null) {
            return Collections.emptyList();
        }
        
        final List<WorkflowDefinitionFile> files = new ArrayList<WorkflowDefinitionFile>();
        for (File file : filteredFiles) {
            if (file.isDirectory()) {
                files.addAll(findDefinitionFiles(file));
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
                ErrorPrinter.print(fileName, e.getMessages());
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
