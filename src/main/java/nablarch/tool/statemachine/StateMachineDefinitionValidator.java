package nablarch.tool.statemachine;

import org.omg.spec.bpmn._20100524.model.*;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;

/**
 * ステートマシン定義のバリデーションを行う。
 *
 * @author Naoki Yamamoto
 */
public class StateMachineDefinitionValidator {

    /**
     * 指定されたステートマシン定義をバリデーションする。
     * @param definitions ステートマシン定義
     * @throws InvalidStateMachineModelException 不正なステートマシン定義の場合
     */
    public void validate(final TDefinitions definitions) throws InvalidStateMachineModelException {
        List<TProcess> processes = getProcesses(definitions);
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
