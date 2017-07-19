package nablarch.tool.statemachine;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.integration.workflow.definition.WorkflowDefinition;
import nablarch.tool.DefinitionCreator;
import nablarch.tool.DefinitionWriter;
import nablarch.tool.workflow.WorkflowDefinitionFile;

/**
 * ステートマシンのバリデーションを実行するクラス。
 *
 * @author Naoki Yamamoto
 */
public class Main {

    /** BPMNファイル名のパターン */
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("(.+)_(.+)_ver(.+)_(.+)\\.(.+)");

    /**
     * 隠蔽コンストラクタ。
     */
    private Main() {
    }

    /**
     * 指定されたフォルダ内の全てのBPMNファイルに対応するステートマシン定義データのCSVファイルを生成する。
     *
     * @param args [0]:BPMNファイルの配置フォルダ [1]:CSVファイルの出力先フォルダ [2]:コンポーネント定義ファイルパス
     */
    public static void main(final String... args) {
        final File inputDir = new File(args[0]);
        final File[] bpmnFiles = inputDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.endsWith(".bpmn");
            }
        });
        final File outputDir = new File(args[1]);
        SystemRepository.load(new DiContainer(new XmlComponentDefinitionLoader("file:" + args[2])));

        final StateMachineDefinitionValidator validator = new StateMachineDefinitionValidator();
        final DefinitionCreator creator = new StateMachineDefinitionCreator();
        final List<WorkflowDefinition> workflowDefinitions = new ArrayList<WorkflowDefinition>();
        for (File bpmnFile : bpmnFiles) {
            try {
                final WorkflowDefinitionFile definitionFile = createWorkflowDefinitionFile(bpmnFile);
                validator.validate(definitionFile);
                workflowDefinitions.add(creator.create(definitionFile));
            } catch (InvalidStateMachineModelException e) {
                System.out.println(bpmnFile.getName());
                for (String message : e.getMessages()) {
                    System.out.println('\t' + message);
                }
            }
        }
        final DefinitionWriter writer = new StateMachineDefinitionWriter();
        writer.write(workflowDefinitions, outputDir);
    }

    /**
     * ワークフロー定義ファイルのインスタンスを生成する
     * @param file ファイル
     * @return ワークフロー定義ファイル情報
     */
    private static WorkflowDefinitionFile createWorkflowDefinitionFile(final File file) {
        final Matcher matcher = FILE_NAME_PATTERN.matcher(file.getName());
        if (matcher.matches()) {
            return new WorkflowDefinitionFile(
                    file.getName(),
                    file.getPath(),
                    matcher.group(1),
                    matcher.group(2),
                    matcher.group(3),
                    matcher.group(4));
        } else {
            throw new IllegalStateException("ワークフロー定義ファイルのファイル名が不正です。" +
                    "ファイル名を[ワークフローID]_[ワークフロー名]_ver[バージョン]_[YYYYMMDD].bpmnに修正してください。");
        }

    }
}
