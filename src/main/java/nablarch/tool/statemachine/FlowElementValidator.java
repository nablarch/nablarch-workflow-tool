package nablarch.tool.statemachine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.omg.spec.bpmn._20100524.model.TBoundaryEvent;
import org.omg.spec.bpmn._20100524.model.TEndEvent;
import org.omg.spec.bpmn._20100524.model.TFlowElement;
import org.omg.spec.bpmn._20100524.model.TFlowNode;
import org.omg.spec.bpmn._20100524.model.TGateway;
import org.omg.spec.bpmn._20100524.model.TProcess;
import org.omg.spec.bpmn._20100524.model.TSequenceFlow;
import org.omg.spec.bpmn._20100524.model.TStartEvent;
import org.omg.spec.bpmn._20100524.model.TSubProcess;
import org.omg.spec.bpmn._20100524.model.TTask;

/**
 * 利用可能なFlowNodeのみが利用されていることをバリデーションする。
 *
 * @author siosio
 */
public class FlowElementValidator implements Validator {

    /** バリデーション対象 */
    private final List<TFlowElement> flowElements;

    /** 許可している要素 */
    private static final List<Class<? extends TFlowElement>> SUPPORTED_NODE = Arrays.asList(
            TStartEvent.class,
            TEndEvent.class,
            TGateway.class,
            TSequenceFlow.class,
            TSubProcess.class,
            TTask.class,
            TBoundaryEvent.class
    );

    /**
     * バリデータを構築する。
     *
     * @param flowElements バリデーション対象
     */
    private FlowElementValidator(final List<TFlowElement> flowElements) {
        this.flowElements = flowElements;
    }

    /**
     * FlowElementのバリデータを生成する
     *
     * @param process バリデーション対象のプロセス
     * @return バリデータ
     */
    public static FlowElementValidator create(final TProcess process) {
        return new FlowElementValidator(filter(process.getFlowElement()));
    }

    /**
     * FlowElementのバリデータを生成する
     *
     * @param subProcess バリデーション対象のサブプロセス
     * @return バリデータ
     */
    public static FlowElementValidator create(final TSubProcess subProcess) {
        return new FlowElementValidator(filter(subProcess.getFlowElement()));
    }

    /**
     * FlowElementのみにする。
     *
     * @param elements FlowElementのJAXB要素
     * @return FlowElementにリスト
     */
    private static List<TFlowElement> filter(final List<JAXBElement<? extends TFlowElement>> elements) {
        final List<TFlowElement> flowElements = new ArrayList<TFlowElement>();
        for (final JAXBElement<? extends TFlowElement> element : elements) {
            flowElements.add(element.getValue());
        }
        return flowElements;
    }

    @Override
    public void validate(final ValidateContext context) {
        for (final TFlowElement node : flowElements) {
            if (!SUPPORTED_NODE.contains(node.getClass())) {
                context.addMessage(MessageUtil.getMessage("invalid.element", node.getId(), node.getName()));
            }
        }
    }
}
