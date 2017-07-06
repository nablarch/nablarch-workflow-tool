package nablarch.tool.statemachine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.omg.spec.bpmn._20100524.model.TBoundaryEvent;
import org.omg.spec.bpmn._20100524.model.TEndEvent;
import org.omg.spec.bpmn._20100524.model.TFlowElement;
import org.omg.spec.bpmn._20100524.model.TGateway;
import org.omg.spec.bpmn._20100524.model.TProcess;
import org.omg.spec.bpmn._20100524.model.TStartEvent;
import org.omg.spec.bpmn._20100524.model.TSubProcess;
import org.omg.spec.bpmn._20100524.model.TTask;

/**
 * 親プロセスのバリデーションを行う{@link Validator}。
 */
public class ParentProcessValidator implements Validator {

    /** プロセス */
    private final TProcess process;

    /**
     * インスタンスを生成する。
     * @param process プロセス
     */
    public ParentProcessValidator(final TProcess process) {
        this.process = process;
    }

    @Override
    public void validate(final ValidateContext context) {
        for (final Validator validator : createValidator()) {
            validator.validate(context);
        }
        for (final SubProcessValidator subProcessValidator : getSubProcessValidator()) {
            subProcessValidator.validate(context);
        }
    }

    /**
     * プロセスに定義されている各要素をバリデーションする{@link Validator}を生成する。
     * @return バリデータリスト
     */
    private List<Validator> createValidator() {
        return Arrays.asList(
                new StartEventValidator(getStartEvent()),
                new EndEventValidator(getEndEvent()),
                new TaskValidator(getTask(), getBoundaryEvent()),
                new BoundaryEventValidator(getBoundaryEvent()),
                new GatewayValidator(getGateway()),
                FlowElementValidator.create(process)
        );
    }

    /**
     * 開始イベントリストを取得する。
     * @return 開始イベントリスト
     */
    private List<TStartEvent> getStartEvent() {
        final List<TStartEvent> startEvents = new ArrayList<TStartEvent>();
        for (final JAXBElement<? extends TFlowElement> element : process.getFlowElement()) {
            if (element.getValue() instanceof TStartEvent) {
                startEvents.add((TStartEvent) element.getValue());
            }
        }
        return startEvents;
    }

    /**
     * 停止イベントリストを取得する。
     * @return 停止イベントリスト
     */
    private List<TEndEvent> getEndEvent() {
        final List<TEndEvent> endEvents = new ArrayList<TEndEvent>();
        for (final JAXBElement<? extends TFlowElement> element : process.getFlowElement()) {
            if (element.getValue() instanceof TEndEvent) {
                endEvents.add((TEndEvent) element.getValue());
            }
        }
        return endEvents;
    }

    /**
     * タスクリストを取得する。
     * @return タスクリスト
     */
    private List<TTask> getTask() {
        final List<TTask> tasks = new ArrayList<TTask>();
        for (JAXBElement<? extends TFlowElement> element : process.getFlowElement()) {
            if (element.getValue() instanceof TTask) {
                tasks.add((TTask) element.getValue());
            }
        }
        return tasks;
    }

    /**
     * 境界イベントリストを取得する。
     * @return 境界イベントリスト
     */
    private List<TBoundaryEvent> getBoundaryEvent() {
        final List<TBoundaryEvent> boundaryEvents = new ArrayList<TBoundaryEvent>();
        for (JAXBElement<? extends TFlowElement> element : process.getFlowElement()) {
            if (element.getValue() instanceof TBoundaryEvent) {
                boundaryEvents.add((TBoundaryEvent) element.getValue());
            }
        }
        return boundaryEvents;
    }

    /**
     * サブプロセスのバリデータリストを取得する。
     * @return サブプロセスのバリデータリスト
     */
    private List<SubProcessValidator> getSubProcessValidator() {
        final List<SubProcessValidator> subProcess = new ArrayList<SubProcessValidator>();
        for (final JAXBElement<? extends TFlowElement> element : process.getFlowElement()) {
            if (element.getValue() instanceof TSubProcess) {
                subProcess.add(new SubProcessValidator((TSubProcess) element.getValue()));
            }
        }
        return subProcess;
    }

    /**
     * ゲートウェイリストを取得する。
     * @return ゲートウェイリスト
     */
    private List<TGateway> getGateway() {
        final List<TGateway> gateways = new ArrayList<TGateway>();
        for (final JAXBElement<? extends TFlowElement> element : process.getFlowElement()) {
            if (element.getValue() instanceof TGateway) {
                gateways.add((TGateway) element.getValue());
            }
        }
        return gateways;
    }
    
}
