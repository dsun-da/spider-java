package com.daml.asx.transfer;

import com.daml.ledger.javaapi.data.Unit;
import com.daml.ledger.javaapi.data.Value;
import da.internal.prelude.optional.None;
import da.internal.prelude.optional.Some;
import da.asx.main.integration.bmw.messages.bah.generated.*;
import da.asx.main.integration.bmw.messages.hold201.Hold201;
import da.asx.main.integration.bmw.messages.hold201.generated.*;
import da.asx.main.integration.bmw.messages.hold201.generated.deliveryreceipttype2code__1.DeliveryReceiptType2Code__1z_FREE;
import da.asx.main.integration.bmw.messages.hold201.generated.depositorybic_asx_1.DepositoryBIC_ASX_1z_XASXAU2S;
import da.asx.main.integration.bmw.messages.hold201.generated.instrumentidentificationtype_asx_1.InstrumentIdentificationType_ASX_1z_INFO;
import da.asx.main.integration.bmw.messages.hold201.generated.issuer_asx_1.Issuer_ASX_1z_XASX;
import da.asx.main.integration.bmw.messages.hold201.generated.partyidentification71choice__1.PartyIdentification71Choice__1z_PrtryId;
import da.asx.main.integration.bmw.messages.hold201.generated.transactioncondition_asx_1.TransactionCondition_ASX_1z_BDTR;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

public class MessageBuilder {
    public static Value buildMsg(String bahFrIdUIC, String hin, ReceiveDelivery1Code receiveDelivery1Code) {
        return new Hold201(
                new BusinessApplicationHeaderV01(
                        new Party9Choice__1(
                                new PartyIdentification42__1(
                                        new Party10Choice__1(
                                                new OrganisationIdentification7__1(
                                                        new None(Unit.getInstance()),
                                                        new Some(new GenericOrganisationIdentification1__1(bahFrIdUIC))
                                                )
                                        )
                                )
                        ),
                        new Party9Choice__1(
                                new PartyIdentification42__1(
                                        new Party10Choice__1(
                                                new OrganisationIdentification7__1(
                                                        new None(Unit.getInstance()),
                                                        new Some(new GenericOrganisationIdentification1__1("00001"))
                                                )
                                        )
                                )
                        ),
                        bahFrIdUIC + "|2222222222",
                        "sese.023.001.07",
                        "hold_201_001_03_!p",
                        Instant.now(),
                        new None(Unit.getInstance()),
                        "",
                        new None(Unit.getInstance())
                ),
                new Document(
                        new SecuritiesSettlementTransactionInstructionV07(
                                bahFrIdUIC + "|B2222",
                                new SettlementTypeAndAdditionalParameters19__1(
                                        receiveDelivery1Code,
                                        new DeliveryReceiptType2Code__1z_FREE(Unit.getInstance()),
                                        new Some("Z004")),  // "Z003"
                                new SecuritiesTradeDetails51__1(
                                        Arrays.asList("/PRTY/B001", "/UNDR/B002"),  // Arrays.asList("/PRTY/A001", "/UNDR/A002"),
                                        new SettlementDate9Choice__1(
                                                new DateAndDateTimeChoice__1("2018-08-02")),
                                        Collections.singletonList(
                                                new TradeTransactionCondition5Choice__1(
                                                        new GenericIdentification30__1(
                                                                "CDIV",
                                                                new Issuer_ASX_1z_XASX(Unit.getInstance())))),
                                        new None(Unit.getInstance()),
                                        new None(Unit.getInstance())),
                                new SecurityIdentification19__1(
                                        new Some("AU0000000123"),
                                        new Some(
                                                new OtherIdentification1__1(
                                                        "CBA",
                                                        new IdentificationSource3Choice__1(
                                                                new InstrumentIdentificationType_ASX_1z_INFO(Unit.getInstance()))))),
                                new QuantityAndAccount39__1(
                                        new Quantity6Choice__1(
                                                new FinancialInstrumentQuantity1Choice__1(
                                                        new BigDecimal(500.0))),
                                        new SecuritiesAccount19__1(hin)),  //
                                new SettlementDetails119__1(
                                        new SecuritiesTransactionType32Choice__1(
                                                new GenericIdentification30__3(
                                                        "MRKT",
                                                        new Issuer_ASX_1z_XASX(Unit.getInstance()))),
                                        new SettlementTransactionCondition16Choice__1(
                                                new GenericIdentification30__4(
                                                        new TransactionCondition_ASX_1z_BDTR(Unit.getInstance()),
                                                        new Issuer_ASX_1z_XASX(Unit.getInstance())))),
                                new SettlementParties36__1(
                                        new PartyIdentification75__1(new PartyIdentification44Choice__1(new DepositoryBIC_ASX_1z_XASXAU2S(Unit.getInstance()))),
                                        new PartyIdentificationAndAccount106__1(
                                                new PartyIdentification71Choice__1z_PrtryId(
                                                        new GenericIdentification36__1(
                                                                "01442",
                                                                new Issuer_ASX_1z_XASX(Unit.getInstance()))))),
                                new SettlementParties36__2(
                                        new PartyIdentification75__1(
                                                new PartyIdentification44Choice__1(
                                                        new DepositoryBIC_ASX_1z_XASXAU2S(Unit.getInstance()))),
                                        new PartyIdentificationAndAccount106__2(
                                                new PartyIdentification71Choice__1z_PrtryId(
                                                        new GenericIdentification36__1(
                                                                "01443",
                                                                new Issuer_ASX_1z_XASX(Unit.getInstance()))),
                                                new None(Unit.getInstance())
                                        )
                                )
                        )
                )
        ).toValue();
    }

}
