package nablarch.tool.statemachine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBElement;

import nablarch.tool.workflow.WorkflowDefinitionFile;
import org.omg.spec.bpmn._20100524.model.TDefinitions;
import org.omg.spec.bpmn._20100524.model.TProcess;
import org.omg.spec.bpmn._20100524.model.TRootElement;

/**
 * ステートマシン定義のバリデーションを行う。
 *
 * @author Naoki Yamamoto
 */
public class StateMachineDefinitionValidator {

    /**
     * 指定されたステートマシン定義をバリデーションする。
     * @param workflowDefinitionFile ワークフロー定義ファイル
     * @throws InvalidStateMachineModelException 不正なステートマシン定義の場合
     */
    public void validate(final WorkflowDefinitionFile workflowDefinitionFile) throws InvalidStateMachineModelException {
        final TDefinitions definitions = JAXB.unmarshal(new File(workflowDefinitionFile.getPath()), TDefinitions.class);
        final List<TProcess> processes = getProcesses(definitions);
        if (processes.size() != 1) {
            throw new InvalidStateMachineModelException(MessageUtil.getMessage("invalid.pool.count"));
        }
        final TProcess process = processes.get(0);
        if (process.getLaneSet()
                   .isEmpty()) {
            throw new InvalidStateMachineModelException(MessageUtil.getMessage("invalid.lane.count"));
        }

        final ValidateContext context = new ValidateContext();
        final Validator validator = new ParentProcessValidator(process);
        validator.validate(context);
        context.throwError();
    }

    /**
     * ステートマシンに定義されたプロセスリストを取得する。
     * @param definitions ステートマシン定義
     * @return プロセスリスト
     */
    private List<TProcess> getProcesses(TDefinitions definitions) {
        List<TProcess> processes = new ArrayList<TProcess>();
        for (JAXBElement<? extends TRootElement> jaxbElement : definitions.getRootElement()) {
            if (jaxbElement.getValue() instanceof TProcess) {
                processes.add((TProcess)jaxbElement.getValue());
            }
        }
        return processes;
    }
}
