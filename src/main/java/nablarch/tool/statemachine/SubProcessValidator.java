package nablarch.tool.statemachine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.omg.spec.bpmn._20100524.model.TBoundaryEvent;
import org.omg.spec.bpmn._20100524.model.TEndEvent;
import org.omg.spec.bpmn._20100524.model.TFlowElement;
import org.omg.spec.bpmn._20100524.model.TGateway;
import org.omg.spec.bpmn._20100524.model.TSequenceFlow;
import org.omg.spec.bpmn._20100524.model.TStartEvent;
import org.omg.spec.bpmn._20100524.model.TSubProcess;
import org.omg.spec.bpmn._20100524.model.TTask;

/**
 * サブプロセスのバリデーションを行う{@link Validator}。
 *
 * @author Naoki Yamamoto
 */
public class SubProcessValidator implements Validator {

    /** サブプロセス */
    private final TSubProcess subProcess;

    /**
     * インスタンスを生成する。
     * @param subProcess サブプロセス
     */
    public SubProcessValidator(final TSubProcess subProcess) {
        this.subProcess = subProcess;
    }

    @Override
    public void validate(final ValidateContext context) {
        for (final Validator validator : createValidator()) {
            validator.validate(context);
        }
        final List<TSubProcess> nestSubProcess = getNestSubProcess();
        if (!nestSubProcess.isEmpty()) {
            for (TSubProcess process : nestSubProcess) {
                new SubProcessValidator(process).validate(context);
            }
        }
    }

    /**
     * サブプロセスに定義された各要素をバリデーションする{@link Validator}を生成する。
     * @return バリデータリスト
     */
    private List<Validator> createValidator() {
        return Arrays.asList(
                new StartEventValidatorInSubProcess(subProcess, getStartEvent()),
                new EndEventValidatorInSubProcess(subProcess, getEndEvent()),
                new TaskValidatorInSubProcess(subProcess, getTask(), getBoundaryEvent()),
                new BoundaryEventValidator(getBoundaryEvent()),
                new GatewayValidator(getGateway(), getSequenceFlowList()),
                FlowElementValidator.create(subProcess)
        );
    }

    /**
     * 開始イベントリストを取得する。
     * @return 開始イベント
     */
    private List<TStartEvent> getStartEvent() {
        final List<TStartEvent> startEvents = new ArrayList<TStartEvent>();
        for (final JAXBElement<? extends TFlowElement> element : subProcess.getFlowElement()) {
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
        for (final JAXBElement<? extends TFlowElement> element : subProcess.getFlowElement()) {
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
        for (JAXBElement<? extends TFlowElement> element : subProcess.getFlowElement()) {
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
        for (JAXBElement<? extends TFlowElement> element : subProcess.getFlowElement()) {
            if (element.getValue() instanceof TBoundaryEvent) {
                boundaryEvents.add((TBoundaryEvent) element.getValue());
            }
        }
        return boundaryEvents;
    }

    /**
     * ネストしたサブプロセスリストを取得する。
     * @return ネストしたサブプロセスリスト
     */
    private List<TSubProcess> getNestSubProcess() {
        final List<TSubProcess> nestSubProcesses = new ArrayList<TSubProcess>();
        for (JAXBElement<? extends TFlowElement> element : subProcess.getFlowElement()) {
            if (element.getValue() instanceof TSubProcess) {
                nestSubProcesses.add(((TSubProcess) element.getValue()));
            }
        }
        return nestSubProcesses;
    }

    /**
     * ゲートウェイリストを取得する。
     * @return ゲートウェイリスト
     */
    private List<TGateway> getGateway() {
        final List<TGateway> gateways = new ArrayList<TGateway>();
        for (final JAXBElement<? extends TFlowElement> element : subProcess.getFlowElement()) {
            if (element.getValue() instanceof TGateway) {
                gateways.add((TGateway) element.getValue());
            }
        }
        return gateways;
    }
    
    /**
     * シーケンスフローのリストを取得する。
     * @return シーケンスフローのリスト
     */
    private List<TSequenceFlow> getSequenceFlowList() {
        final List<TSequenceFlow> flows = new ArrayList<TSequenceFlow>();
        for (final JAXBElement<? extends TFlowElement> element : subProcess.getFlowElement()) {
            if (element.getValue() instanceof TSequenceFlow) {
                flows.add((TSequenceFlow) element.getValue());
            }
        }
        return flows;
    }

    /**
     * サブプロセスの開始イベントのバリデーションを行う{@link Validator}。
     */
    private static class StartEventValidatorInSubProcess extends StartEventValidator {

        /** サブプロセス */
        private final TSubProcess subProcess;

        /**
         * インスタンスを生成する。
         * @param subProcess サブプロセス
         * @param startEvents 開始イベント
         */
        private StartEventValidatorInSubProcess(
                final TSubProcess subProcess,
                final List<TStartEvent> startEvents) {
            super(startEvents);
            this.subProcess = subProcess;
        }

        @Override
        void validateCount(final ValidateContext context) {
            if (startEvents.size() != 1) {
                context.addMessage(
                        MessageUtil.getMessage("subprocess.start.event.count", subProcess.getId(),
                                subProcess.getName()));
            }
        }
    }

    /**
     * サブプロセスの停止イベントのバリデーションを行う{@link Validator}。
     */
    private static class EndEventValidatorInSubProcess extends EndEventValidator {

        /** サブプロセス */
        private final TSubProcess subProcess;

        /**
         * インスタンスを生成する。
         * @param subProcess サブプロセス
         * @param endEvents 停止イベント
         */
        private EndEventValidatorInSubProcess(
                final TSubProcess subProcess,
                final List<TEndEvent> endEvents) {
            super(endEvents);
            this.subProcess = subProcess;
        }

        @Override
        void validateCount(final ValidateContext context) {
            if (endEvents.isEmpty()) {
                context.addMessage(
                        MessageUtil.getMessage("subprocess.end.event.count", subProcess.getId(), subProcess.getName()));
            }
        }
    }

    /**
     * サブプロセスのタスクのバリデーションを行う{@link Validator}。
     */
    private static class TaskValidatorInSubProcess extends TaskValidator {

        /** サブプロセス */
        private final TSubProcess subProcess;

        /**
         * インスタンスを生成する。
         * @param subProcess サブプロセス
         * @param tasks タスクリスト
         * @param boundaryEvents 境界イベント
         */
        public TaskValidatorInSubProcess(final TSubProcess subProcess, final List<TTask> tasks, final List<TBoundaryEvent> boundaryEvents) {
            super(tasks, boundaryEvents);
            this.subProcess = subProcess;
        }

        @Override
        void validateCount(final ValidateContext context) {
            if (tasks.isEmpty()) {
                context.addMessage(MessageUtil.getMessage("subprocess.invalid.task.count", subProcess.getId(), subProcess.getName()));
            }
        }
    }
}
