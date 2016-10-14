
package org.omg.spec.bpmn._20100524.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;


/**
 * <p>Java class for tGroup complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="tGroup">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/BPMN/20100524/MODEL}tArtifact">
 *       &lt;attribute name="categoryValueRef" type="{http://www.w3.org/2001/XMLSchema}QName" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tGroup")
public class TGroup
    extends TArtifact
{

    @XmlAttribute
    protected QName categoryValueRef;

    /**
     * Gets the value of the categoryValueRef property.
     * 
     * @return
     *     possible object is
     *     {@link QName }
     *     
     */
    public QName getCategoryValueRef() {
        return categoryValueRef;
    }

    /**
     * Sets the value of the categoryValueRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link QName }
     *     
     */
    public void setCategoryValueRef(QName value) {
        this.categoryValueRef = value;
    }

}
