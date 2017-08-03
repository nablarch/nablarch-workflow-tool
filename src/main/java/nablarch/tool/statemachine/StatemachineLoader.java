package nablarch.tool.statemachine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.logging.Log;

import nablarch.integration.workflow.definition.WorkflowDefinition;
import nablarch.tool.BpmnLoader;
import nablarch.tool.DefinitionCreator;
import nablarch.tool.ErrorPrinter;
import nablarch.tool.Utils;
import nablarch.tool.workflow.WorkflowDefinitionFile;

/**
 * ステートマシンの定義情報をロードするクラス。
 * @author siosio
 */
public class StatemachineLoader implements BpmnLoader{

    @Override
    public List<WorkflowDefinition> load(final File loadDir, final Log log) {
        final List<WorkflowDefinitionFile> definitionFiles = Utils.findDefinitionFiles(loadDir, log);

        final DefinitionCreator creator = new StateMachineDefinitionCreator();
        
        final StateMachineDefinitionValidator validator = new StateMachineDefinitionValidator();
        
        final List<WorkflowDefinition> workflowDefinitions = new ArrayList<WorkflowDefinition>();
        for (final WorkflowDefinitionFile definitionFile : definitionFiles) {
            log.info("load_file:" + definitionFile.getPath());
            try {
                validator.validate(definitionFile);
                workflowDefinitions.add(creator.create(definitionFile));
            } catch (InvalidStateMachineModelException e) {
                ErrorPrinter.print(definitionFile.getName(), e.getMessages(), log);
            }
        }
        return workflowDefinitions;
    }
}
