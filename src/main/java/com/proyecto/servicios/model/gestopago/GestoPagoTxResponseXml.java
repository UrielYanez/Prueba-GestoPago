package com.proyecto.servicios.model.gestopago;

import jakarta.xml.bind.annotation.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@XmlRootElement(name = "RESPONSE")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
public class GestoPagoTxResponseXml {

    @XmlElement(name = "ID_TX")
    private String idTx;

    @XmlElement(name = "NUM_AUTORIZACION")
    private String numAutorizacion;

    @XmlElement(name = "SALDO")
    private BigDecimal saldo;

    @XmlElement(name = "PIN")
    private String pinRoot;

    @XmlElement(name = "MENSAJE")
    private MensajeWrapper mensaje;

    public String getPinGiftCard() {
        if (mensaje != null && mensaje.getPin() != null && !mensaje.getPin().isBlank()) {
            return mensaje.getPin().trim();
        }
        return pinRoot != null ? pinRoot.trim() : null;
    }

    public String getLegend() {
        return mensaje != null ? mensaje.getLegend() : null;
    }

    public String getCodigo() {
        return mensaje != null ? mensaje.getCodigo() : null;
    }

    public String getTexto() {
        return mensaje != null ? mensaje.getTexto() : null;
    }

    public boolean isExitoso() {
        return "01".equals(getCodigo());
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter
    @Setter
    public static class MensajeWrapper {
        @XmlElement(name = "CODIGO")
        private String codigo;

        @XmlElement(name = "PIN")
        private String pin;

        @XmlElement(name = "legend")
        private String legend;

        @XmlElement(name = "TEXTO")
        private String texto;

        @XmlElement(name = "REFERENCIA")
        private String referencia;
    }
}
