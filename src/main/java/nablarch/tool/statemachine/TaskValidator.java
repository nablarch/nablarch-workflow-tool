package nablarch.tool.statemachine;

import java.util.List;

import nablarch.integration.workflow.util.WorkflowUtil;
import nablarch.integration.workflow.util.WorkflowUtil.ListFilter;

import org.omg.spec.bpmn._20100524.model.TBoundaryEvent;
import org.omg.spec.bpmn._20100524.model.TTask;
import org.omg.spec.bpmn._20100524.model.TUserTask;

/**
 * タスクのバリデーションを行う{@link Validator}。
 * @author Naoki Yamamoto
 */
public class TaskValidator implements Validator {

    /** タスクリスト */
    final List<TTask> tasks;

    /** 境界イベント */
    final List<TBoundaryEvent> boundaryEvents;

    /**
     * インスタンスを生成する。
     * @param tasks タスクリスト
     * @param boundaryEvents 境界イベントリスト
     */
    public TaskValidator(final List<TTask> tasks, final List<TBoundaryEvent> boundaryEvents) {
        this.tasks = tasks;
        this.boundaryEvents = boundaryEvents;
    }

    @Override
    public void validate(final ValidateContext context) {
        validateCount(context);
        if (tasks.isEmpty()) {
            return;
        }

        for (final TTask task : tasks) {
            validateType(context, task);
            validateBoundary(context, task);
            validateOutgoing(context, task);
            validateIncoming(context, task);
        }
    }

    /**
     * タスクタイプをバリデーションする。
     * @param context バリデーションコンテキスト
     * @param task タスク
     */
    void validateType(final ValidateContext context, final TTask task) {
        if (task instanceof TUserTask) {
            final String message = MessageUtil.getMessage("unsupported.task", task.getId(), task.getName());
            context.addMessage(message);
        }
    }

    /**
     * タスク数をバリデーションする。
     * @param context バリデーションコンテキスト
     */
    void validateCount(final ValidateContext context) {
        if (tasks.isEmpty()) {
            context.addMessage(MessageUtil.getMessage("invalid.task.count"));
        }
    }

    /**
     * タスクの境界イベントをバリデーションする。
     * @param context バリデーションコンテキスト
     * @param task タスク
     */
    void validateBoundary(final ValidateContext context, final TTask task) {
        final boolean existsBoundary = WorkflowUtil.contains(boundaryEvents, new ListFilter<TBoundaryEvent>() {
            @Override
            public boolean isMatch(final TBoundaryEvent other) {
                return other.getAttachedToRef()
                            .getLocalPart()
                            .equals(task.getId());
            }
        });

        if (!existsBoundary) {
            context.addMessage(MessageUtil.getMessage("task.boundary.notfound", task.getId(), task.getName()));
        }
    }

    /**
     * タスクの遷移先をバリデーションする。
     * @param context バリデーションコンテキスト
     * @param task タスク
     */
    void validateOutgoing(final ValidateContext context, final TTask task) {
        if (!task.getOutgoing()
                 .isEmpty()) {
            context.addMessage(MessageUtil.getMessage("task.outgoing.found", task.getId(), task.getName()));
        }
    }

    /**
     * タスクの遷移元をバリデーションする。
     * @param context バリデーションコンテキスト
     * @param task タスク
     */
    void validateIncoming(final ValidateContext context, final TTask task) {
        if (task.getIncoming()
                .isEmpty()) {
            context.addMessage(MessageUtil.getMessage("task.incoming.notfound", task.getId(), task.getName()));
        }
    }
}
