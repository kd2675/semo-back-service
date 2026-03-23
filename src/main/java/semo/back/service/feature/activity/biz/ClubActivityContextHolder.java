package semo.back.service.feature.activity.biz;

import org.springframework.util.StringUtils;

import java.util.ArrayDeque;
import java.util.Deque;

public final class ClubActivityContextHolder {
    private static final ThreadLocal<Deque<ActivityDraft>> HOLDER = ThreadLocal.withInitial(ArrayDeque::new);

    private ClubActivityContextHolder() {
    }

    public static void push() {
        HOLDER.get().push(new ActivityDraft());
    }

    public static Snapshot currentSnapshot() {
        ActivityDraft draft = currentDraft();
        if (draft == null) {
            return new Snapshot(null, null);
        }
        return new Snapshot(draft.successDetail, draft.failureDetail);
    }

    public static void pop() {
        Deque<ActivityDraft> stack = HOLDER.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
        if (stack.isEmpty()) {
            HOLDER.remove();
        }
    }

    public static void setSuccessDetail(String detail) {
        ActivityDraft draft = currentDraft();
        if (draft != null && StringUtils.hasText(detail)) {
            draft.successDetail = detail.trim();
        }
    }

    public static void setFailureDetail(String detail) {
        ActivityDraft draft = currentDraft();
        if (draft != null && StringUtils.hasText(detail)) {
            draft.failureDetail = detail.trim();
        }
    }

    public static void setDetails(String successDetail, String failureDetail) {
        setSuccessDetail(successDetail);
        setFailureDetail(failureDetail);
    }

    private static ActivityDraft currentDraft() {
        Deque<ActivityDraft> stack = HOLDER.get();
        if (stack.isEmpty()) {
            return null;
        }
        return stack.peek();
    }

    public record Snapshot(String successDetail, String failureDetail) {
    }

    private static final class ActivityDraft {
        private String successDetail;
        private String failureDetail;
    }
}
