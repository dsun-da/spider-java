// Copyright (c) 2019, Digital Asset (Switzerland) GmbH and/or its affiliates.
// All rights reserved.

package com.daml.spider.transfer;

import com.daml.ledger.javaapi.data.*;
import com.daml.ledger.rxjava.LedgerClient;
import com.google.protobuf.Empty;
import da.internal.prelude.optional.Some;
import da.spider.main.bizprocess.holding.model.Holding;
import da.spider.main.integration.bmw.egress.comm808out.Comm808Out;
import da.spider.main.integration.bmw.ingress.BmwIngressMaster;
import da.spider.main.integration.bmw.messages.hold202.Hold202;
import io.reactivex.Flowable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.daml.spider.Main.APP_ID;


public class BilateralTransferProcessor {

    private static final String HOLD_201_CHOICE_NAME = "Hold_201_001_03_sese_023_001_07";

    private final String operator;
    private final Identifier comm807OutMsgIdentifier;
    private final Identifier comm808OutMsgIdentifier;
    private final Identifier hold201ContextIdentifier;
    private final Identifier hold202OutMsgIdentifier;
    private final Identifier hold207OutMsgIdentifier;
    private final Identifier hold208OutMsgIdentifier;
    private LedgerClient client;
    private String packageId;
    private Identifier bmwIngressMasterIdentifier;

    public BilateralTransferProcessor(String operator, LedgerClient client, String packageId) {
        this.bmwIngressMasterIdentifier = new Identifier(packageId, "DA.ASX.Main.Integration.BMW.Ingress", "BmwIngressMaster");
        this.hold201ContextIdentifier = new Identifier(packageId, "DA.ASX.Main.Controllers.Hold201 where", "LookupContextForDemandTransfer");
        this.hold202OutMsgIdentifier = new Identifier(packageId, "DA.ASX.Main.Integration.BMW.Egress.Hold202Out", "Hold202Out");
        this.hold207OutMsgIdentifier = new Identifier(packageId, "DA.ASX.Main.Integration.BMW.Egress.Hold207Out", "Hold207Out");
        this.hold208OutMsgIdentifier = new Identifier(packageId, "DA.ASX.Main.Integration.BMW.Egress.Hold208Out", "Hold208Out");
        this.comm807OutMsgIdentifier = new Identifier(packageId, "DA.ASX.Main.Integration.BMW.Egress.Comm807Out", "Comm807Out");
        this.comm808OutMsgIdentifier = new Identifier(packageId, "DA.ASX.Main.Integration.BMW.Egress.Comm808Out", "Comm808Out");
        this.operator = operator;
        this.client = client;
        this.packageId = packageId;
    }

    public void requestTransfer(String participant, Value msg) {
        bmwIngressMasterIdentifier = new Identifier(
                packageId,
                "DA.ASX.Main.Integration.BMW.Ingress",
                "BmwIngressMaster");
        Filter ingressContractFilter = new InclusiveFilter(new HashSet<>(Collections.singletonList(
                bmwIngressMasterIdentifier
        )));
        Flowable<GetActiveContractsResponse> activeContracts = this.client.getActiveContractSetClient()
                .getActiveContracts(
                        new FiltersByParty(Collections.singletonMap(participant, ingressContractFilter)),
                        true);
        CreatedEvent x = activeContracts.flatMap(ac -> Flowable.fromIterable(ac.getCreatedEvents()))
                .filter(e -> e.getTemplateId().equals(bmwIngressMasterIdentifier)
                        && BmwIngressMaster.fromValue(e.getArguments()).participant.equalsIgnoreCase(participant))
                .blockingFirst();
        String workflowId = UUID.randomUUID().toString();
        Command exerciseCommand = makeHold201Cmd(workflowId, x, participant, msg);
//        submitCommandAndWait(workflowId, participant, Collections.singletonList(exerciseCommand));
        submitCommandAsync(workflowId, participant, Collections.singletonList(exerciseCommand));
    }

    public void runIndefinitely() {
        // assemble the request for the transaction stream
        System.out.printf("%s starts reading transactions.\n", operator);
        Flowable<Transaction> transactions = client.getTransactionsClient().getTransactions(
//                LedgerOffset.LedgerEnd.getInstance(),
                LedgerOffset.LedgerBegin.getInstance(),
                new FiltersByParty(
                        Collections.singletonMap(operator, NoFilter.instance)),
                true);
        transactions.forEach(this::processTransaction);

        Flowable<CompletionStreamResponse> completionStream = client.getCommandCompletionClient().completionStream(
                APP_ID,
                LedgerOffset.LedgerEnd.getInstance(),
                new HashSet<>(Collections.singleton(operator)));
        completionStream.forEach(this::processCmdCompletion);
    }

    private void processCmdCompletion(CompletionStreamResponse completionStreamResponse) {
        completionStreamResponse.getCompletions().stream()
                .forEach(x -> {
                    System.out.println(String.format("Completion: %s", x));
                });
    }

    private void processTransaction(Transaction tx) {
        tx.getEvents()
                .forEach(e -> {
                    if (e instanceof CreatedEvent) {
                        logCreatedEvent((CreatedEvent)e);
                    } else {
                        logEvent(e);
                    }
//                    Note: with transaction stream, we won't get any exercised event. the archived event is logged
//                    if (e instanceof ExercisedEvent) {
//                        logExercisedEvent((ExercisedEvent) e);
//                    }
                });
    }

    private void submitCommandAsync(String workflowId, String party, List<Command> exerciseCommands) {
        String commandId = UUID.randomUUID().toString();
        System.out.println(String.format("CommandId: %s", commandId));
        client.getCommandSubmissionClient().submit(
                workflowId,
                APP_ID,
                commandId,
                party,
                Instant.now(),
                Instant.now().plusSeconds(10),
                exerciseCommands);
    }

    private void submitCommandAndWait(String workflowId, String party, List<Command> exerciseCommands) {
        String commandId = UUID.randomUUID().toString();
        System.out.println(String.format("CommandId: %s", commandId));
        Empty x = client.getCommandClient().submitAndWait(
                workflowId,
                APP_ID,
                commandId,
                party,
                Instant.now(),
                Instant.now().plusSeconds(10),
                exerciseCommands)
                .blockingGet();
        System.out.println(String.format("Completed: %s", x));
    }

    private Command makeHold201Cmd(String workflowId, CreatedEvent event, String bahFrIdUIC, Value msg) {
        Identifier template = event.getTemplateId();

//        BmwIngressMaster ingressMaster = BmwIngressMaster.fromValue(event.getArguments());
//        String p = ingressMaster.participant;
//        System.out.println("IngressMaster.participant: " + p);
//
//        if (!p.equalsIgnoreCase(bahFrIdUIC)) {
//            System.out.printf("%s is not expected participant %s \n", operator, p);
//            return null;
//        }

        String contractId = event.getContractId();

        System.out.printf("%s is %sing on %s in workflow %s\n", bahFrIdUIC, HOLD_201_CHOICE_NAME, contractId, workflowId);

        // assemble the exercise command
        Command cmd = new ExerciseCommand(
                template,
                contractId,
                HOLD_201_CHOICE_NAME,
                new Record(Collections.singletonList(new Record.Field("message", msg)))
        );

        return cmd;
    }

    private void logEvent(Event event) {
        Identifier template = event.getTemplateId();

        System.out.println(String.format("event %s occurred for template id: %s", event.getClass(), template));
//        System.out.println(String.format("%s#%s(%s): %s", event.getContractId(), event.getChoice(), event.getChoiceArgument(), event.getContractCreatingEventId()));
    }

    private void logExercisedEvent(ExercisedEvent event) {
        Identifier template = event.getTemplateId();
//        if (!template.equals(bmwIngressMasterIdentifier)) {
//            return;
//        }

        System.out.println(String.format("exercisedEvent occurred for template id: %s", template));

        System.out.println(String.format("%s#%s(%s): %s", event.getContractId(), event.getChoice(), event.getChoiceArgument(), event.getContractCreatingEventId()));
    }

    private void logCreatedEvent(CreatedEvent event) {
        Identifier template = event.getTemplateId();

        if (!(template.equals(bmwIngressMasterIdentifier) ||
                template.equals(hold201ContextIdentifier) ||
                template.equals(hold202OutMsgIdentifier) ||
                template.equals(hold207OutMsgIdentifier) ||
                template.equals(hold208OutMsgIdentifier) ||
                template.equals(comm807OutMsgIdentifier) ||
                template.equals(comm808OutMsgIdentifier))) {
            return;
        }

        System.out.println(String.format("createdEvent occurred for template id: %s", template));

        System.out.println(String.format("cid: %s, args: %s", event.getContractId(), event.getArguments()));

//        Map<String, Value> fields = event.getArguments().getFieldsMap();

//        TOOD extract Balance
//        if (template.equals(hold202OutMsgIdentifier)) {
//            Record arguments = event.getArguments();
//            String hin = extractHoldingFromHold202(arguments).get();
//            System.out.println(String.format("HIN: %s!", hin));
//        }
    }

    private Optional<BigDecimal> extractHoldingFromHold202(Record arguments) {
        Optional<BigDecimal> hin = arguments.getFields().stream()
                .filter(f -> f.getLabel().isPresent() && f.getLabel().get().equalsIgnoreCase("message"))
                .map(Record.Field::getValue)
                .map(Hold202::fromValue)
//                Note: dlvrg or rcvg might be None
                .map(x -> x.zuDocument.zuSctiesSttlmTxConf.zuSplmtryData.zuEnvlp.zuDocument.hold202SuplDataV01.zuHldgBal.zuDlvrgHldgBal)
                .map(b -> ((Some<BigDecimal>)b).body)
                .findFirst();
        return hin;
    }

    private Function<Record.Field, Stream<Value>> getFieldValueByName(String fieldName) {
        return f -> (f.getLabel().isPresent() &&
                fieldName.equalsIgnoreCase(f.getLabel().get())) ?
                Stream.of(f.getValue()) :
                Stream.empty();
    }

    private Function<Record.Field, Stream<Record.Field>> getRecordFieldByName(String fieldName) {
        return f -> (f.getLabel().isPresent() &&
                fieldName.equalsIgnoreCase(f.getLabel().get()) &&
                f.getValue() instanceof Record) ?
                ((Record)f.getValue()).getFields().stream() :
                Stream.empty();
    }

    public void queryNacks() {
        Identifier id = new Identifier(
                packageId,
                "DA.ASX.Main.Integration.BMW.Egress.Comm808Out",
                "Comm808Out");
        Filter filter = new InclusiveFilter(new HashSet<>(Collections.singletonList(id)));

        Flowable<GetActiveContractsResponse> activeContracts = this.client.getActiveContractSetClient()
                .getActiveContracts(
                        new FiltersByParty(Collections.singletonMap(operator, filter)),
                        true);
        activeContracts.forEach(x -> {
            System.out.println("activeContract: " + x);
            x.getCreatedEvents().forEach(e -> {
                Comm808Out comm808Out = Comm808Out.fromValue(e.getArguments());
                System.out.println(String.format("Comm808Out: %s", comm808Out));
            });
        });
    }

    public void queryHoldginsFromAcs() {
        Identifier holdingId = new Identifier(
                packageId,
                "DA.ASX.Main.BizProcess.Holding.Model",
                "Holding");
        Filter filter = new InclusiveFilter(new HashSet<>(Collections.singletonList(
                holdingId
        )));

        Flowable<GetActiveContractsResponse> activeContracts = this.client.getActiveContractSetClient()
                .getActiveContracts(
                        new FiltersByParty(Collections.singletonMap(operator, filter)),
                        true);
        activeContracts.forEach(x -> {
            System.out.println("activeContract: " + x);
            x.getCreatedEvents().forEach(e -> {
                Holding holding = Holding.fromValue(e.getArguments());
                System.out.println(String.format("Holing: %s", holding));
            });
        });
    }

}
