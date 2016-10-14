package nablarch.tool.workflow.integration.helper;

import nablarch.common.idgenerator.IdFormatter;
import nablarch.common.idgenerator.IdGenerator;

/**
 * インスタンスIDの採番で使用する{@link nablarch.common.idgenerator.IdGenerator}実装クラス。
 *
 * @author Satoshi Taromaru
 * @since 1.4.2
 */
public class IdGeneratorImpl implements IdGenerator{

    /**
     * インスタンスID用採番メソッド。
     * 本テストではインスタンスを一つしか生成しないため固定値を返却する。
     *
     * @param id 採番対象を識別するID
     * @return インスタンスID用採番結果（固定値）
     */
    @Override
    public String generateId(String id) {
        return id + "00000000";
    }

    /**
     * フォーマット指定の採番メソッド。
     * 本テストでは使用しないため、UnsupportedOperationExceptionを送出する。
     *
     * @param id 採番対象を識別するID
     * @param formatter 採番したIDをフォーマットするIdFormatter
     * @return 採番対象ID内でユニークな採番結果のID
     */
    @Override
    public String generateId(String id, IdFormatter formatter) {
        throw new UnsupportedOperationException("本テストでは使用しない。");
    }
}

