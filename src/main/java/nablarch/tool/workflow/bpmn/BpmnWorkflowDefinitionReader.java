package nablarch.tool.workflow.bpmn;

import java.io.File;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.SchemaFactory;

import org.omg.spec.bpmn._20100524.model.TDefinitions;

import nablarch.core.repository.SystemRepository;
import nablarch.core.util.FileUtil;
import nablarch.tool.workflow.WorkflowDefinitionException;
import nablarch.tool.workflow.WorkflowDefinitionFile;
import nablarch.tool.workflow.WorkflowDefinitionReader;

import nablarch.integration.workflow.definition.WorkflowDefinition;

/**
 * BPMN2.0に準拠したファイルからワークフロー定義情報を読み込むクラス。
 *
 * @author Satoshi Taromaru
 * @since 1.4.2
 */
public class BpmnWorkflowDefinitionReader implements WorkflowDefinitionReader {

    /**
     * ワークフロー定義ファイルからワークフロー定義情報へ変換するオブジェクト。
     */
    private final Unmarshaller unmarshaller;

    /**
     * コンストラクタ。
     */
    public BpmnWorkflowDefinitionReader() {
        unmarshaller = createUnmarshaller();
    }

    @Override
    @SuppressWarnings("unchecked")
    public WorkflowDefinition load(WorkflowDefinitionFile definitionFile) {
        JAXBElement<TDefinitions> obj;
        try {
            BpmnWorkflowDefinitionValidationEventHandler eventHandler = new BpmnWorkflowDefinitionValidationEventHandler();
            unmarshaller.setEventHandler(eventHandler);
            obj = (JAXBElement<TDefinitions>) unmarshaller.unmarshal(new File(definitionFile.getPath()));
            if (eventHandler.hasAnomalyDetection()) {
                throw new WorkflowDefinitionException(eventHandler.getErrorMessages());
            }
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
        TDefinitions definition = obj.getValue();
        BpmnWorkflowDefinitionValidator validator = SystemRepository.get("workflowDefinitionValidator");
        validator.validateWorkflowDefinition(definition);
        return new BpmnWorkflowDefinitionConverter().convertToWorkflowDefinition(definition, definitionFile.getWorkflowId(),
                definitionFile.getWorkflowName(), Integer.parseInt(definitionFile.getVersion()), definitionFile.getEffectiveDate());
    }

    /**
     * ワークフロー定義情報へ変換するためのオブジェクトを生成する。
     *
     * @return 変換用オブジェクト
     */
    private Unmarshaller createUnmarshaller() {
        Unmarshaller unmarshaller;
        try {
            JAXBContext ctx = JAXBContext.newInstance("org.omg.spec.bpmn._20100524.di"
                    + ":org.omg.spec.bpmn._20100524.model"
                    + ":org.omg.spec.dd._20100524.dc"
                    + ":org.omg.spec.dd._20100524.di");
            unmarshaller = ctx.createUnmarshaller();

            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            URL resource = FileUtil.getClasspathResourceURL("workflow/bpmn/BPMN20.xsd");
            unmarshaller.setSchema(factory.newSchema(resource));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Unmarshaller.", e);
        }
        return unmarshaller;
    }
}
