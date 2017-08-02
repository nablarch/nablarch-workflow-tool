package nablarch.tool.workflow;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import nablarch.integration.workflow.definition.WorkflowDefinition;
import nablarch.tool.BpmnLoader;
import nablarch.tool.ErrorPrinter;
import nablarch.tool.Utils;
import nablarch.tool.workflow.bpmn.BpmnWorkflowDefinitionReader;

/**
 * ワークフローの定義情報をロードするクラス。
 * @author siosio
 */
public class WorkflowLoader implements BpmnLoader{

    @Override
    public List<WorkflowDefinition> load(final String loadDir) {

        final List<WorkflowDefinitionFile> definitionFiles = Utils.findDefinitionFiles(new File(loadDir));

        final List<WorkflowDefinition> definitions = new ArrayList<WorkflowDefinition>();

        final BpmnWorkflowDefinitionReader reader = new BpmnWorkflowDefinitionReader();
        for (WorkflowDefinitionFile definitionFile : definitionFiles) {
            try {
                definitions.add(reader.load(definitionFile));
            } catch (WorkflowDefinitionException e) {
                ErrorPrinter.print(definitionFile.getName(), e.getMessages());
            }
        }
        return definitions;
    }
}
