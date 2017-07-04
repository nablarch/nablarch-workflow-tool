package nablarch.tool.statemachine;

import java.io.File;
import java.io.FilenameFilter;
import javax.xml.bind.JAXB;

import org.omg.spec.bpmn._20100524.model.TDefinitions;

/**
 * ステートマシンのバリデーションを実行するクラス。
 *
 * @author Naoki Yamamoto
 */
public class Main {

    /**
     * 隠蔽コンストラクタ。
     */
    private Main() {
    }

    /**
     * 指定されたフォルダ内の全てのBPMNファイルに対し、ステートマシンとして適切か否かをバリデーションする。
     *
     * @param args BPMNファイルの配置フォルダ
     */
    public static void main(final String... args) {
        final File directory = new File(args[0]);
        final File[] bpmnFiles = directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.endsWith(".bpmn");
            }
        });

        final StateMachineDefinitionValidator validator = new StateMachineDefinitionValidator();
        for (File bpmnFile : bpmnFiles) {
            final TDefinitions definitions = JAXB.unmarshal(bpmnFile, TDefinitions.class);
            try {
                validator.validate(definitions);
            } catch (InvalidStateMachineModelException e) {
                System.out.println(bpmnFile.getName());
                for (String message : e.getMessages()) {
                    System.out.println('\t' + message);
                }
            }

        }

    }

}
