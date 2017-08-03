package nablarch.tool;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.integration.workflow.definition.WorkflowDefinition;
import nablarch.tool.statemachine.StatemachineLoader;
import nablarch.tool.workflow.WorkflowLoader;

/**
 * BPMNからデータベースへロードするためのCSVデータを出力するMavenプラグイン。
 * @author siosio
 */
@Mojo(name = "generate-csv", requiresDependencyResolution = ResolutionScope.RUNTIME)
public class GenerateCsvMojo extends AbstractMojo {

    /** ステートマシン用のBPMNが置かれたディレクトリ*/
    @Parameter
    File stateMachineBpmnDir;
    
    /** ワークフロー用のBPMNが置かれたディレクトリ */
    @Parameter
    File workflowBpmnDir;

    /** CSVの出力先ディレクトリ */
    @Parameter(required = true)
    File outputPath;
    
    /** ツール実行に必要な設定値を持ったコンポーネント定義のパス */
    @Parameter(required = true)
    File configurationFilePath;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Log log = getLog();
        if (!outputPath.exists() && outputPath.mkdirs()) {
            log.error("出力先ディレクトリが作成出来ません。");
            throw new MojoFailureException("出力先ディレクトリの作成に失敗しました。");
        }

        SystemRepository.load(new DiContainer(new XmlComponentDefinitionLoader(configurationFilePath.toURI().toString())));

        final List<WorkflowDefinition> workflowDefinitions = new ArrayList<WorkflowDefinition>();
        if (stateMachineBpmnDir != null) {
            final StatemachineLoader statemachineLoader = new StatemachineLoader();
            workflowDefinitions.addAll(statemachineLoader.load(stateMachineBpmnDir, log));
        }

        if (workflowBpmnDir != null) {
            final WorkflowLoader workflowLoader = new WorkflowLoader();
            workflowDefinitions.addAll(workflowLoader.load(workflowBpmnDir, log));
        }

        final CsvDataWriter writer = new CsvDataWriter();
        writer.write(workflowDefinitions, outputPath);
    }
}
