package nablarch.tool.statemachine;

/**
 * ステートマシンの要素をバリデーションするインタフェース。
 */
public interface Validator {

    /**
     * ステートマシンの要素をバリデーションする。
     * @param context バリデーションコンテキスト
     */
    void validate(ValidateContext context);
}
