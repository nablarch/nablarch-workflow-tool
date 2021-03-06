
package org.omg.spec.bpmn._20100524.model;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tEventBasedGatewayType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="tEventBasedGatewayType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Exclusive"/>
 *     &lt;enumeration value="Parallel"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "tEventBasedGatewayType")
@XmlEnum
public enum TEventBasedGatewayType {

    @XmlEnumValue("Exclusive")
    EXCLUSIVE("Exclusive"),
    @XmlEnumValue("Parallel")
    PARALLEL("Parallel");
    private final String value;

    TEventBasedGatewayType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static TEventBasedGatewayType fromValue(String v) {
        for (TEventBasedGatewayType c: TEventBasedGatewayType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
