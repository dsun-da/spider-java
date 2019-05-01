// Copyright (c) 2019, Digital Asset (Switzerland) GmbH and/or its affiliates.
// All rights reserved.

package com.daml.spider;

import com.daml.ledger.javaapi.data.GetPackageResponse;
import com.daml.ledger.javaapi.data.Unit;
import com.daml.ledger.rxjava.DamlLedgerClient;
import com.daml.ledger.rxjava.LedgerClient;
import com.daml.ledger.rxjava.PackageClient;
import com.daml.spider.transfer.BilateralTransferProcessor;
import com.daml.spider.transfer.MessageBuilder;
import com.digitalasset.daml_lf.DamlLf;
import com.digitalasset.daml_lf.DamlLf1;
import com.google.protobuf.CodedInputStream;
import da.spider.main.integration.bmw.messages.hold201.generated.receivedelivery1code.ReceiveDelivery1Codez_DELI;
import da.spider.main.integration.bmw.messages.hold201.generated.receivedelivery1code.ReceiveDelivery1Codez_RECE;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.reactivex.Flowable;

import java.io.IOException;
import java.util.Optional;

public class Main {

    // application id used for sending commands
    public static final String APP_ID = "App";

    // constants for referring to the parties
    public static final String OPERATOR = "00001";

    public static void main(String[] args) {
        // Extract host and port from arguments
        if (args.length < 3) {
            System.err.println("Usage: HOST PORT [DelivererRequest|ReceiverRequest|QueryHoldings]");
            System.exit(-1);
        }
        String host = args[0];
        int port = Integer.valueOf(args[1]);
        Demo mode = Demo.valueOf(args[2]);

        //         create a client object to access services on the ledger
    //        DamlLedgerClient client = DamlLedgerClient.forHostWithLedgerIdDiscovery(host, port, Optional.empty());
        DamlLedgerClient client = getDamlLedgerClient(host, port);

        // Connects to the ledger and runs initial validation
        client.connect();

        // inspect the packages on the ledger and extract the package id of the package containing the Ingress module
        // this is helpful during development when the package id changes a lot due to frequent changes to the DAML code
        String packageId = detectIngressPackageId(client);

        // initialize the processor for OPERATOR
        BilateralTransferProcessor processor = new BilateralTransferProcessor(OPERATOR, client, packageId);

        // start the processors asynchronously to log transactions
//        processor.runIndefinitely();

        switch (mode) {
            case QueryNacks:
                processor.queryNacks();
                break;
            case QueryHoldings:
                processor.queryHoldginsFromAcs();
                break;
            case DelivererRequest:
                processor.requestTransfer("01442", MessageBuilder.buildMsg("01442", "0010000023", new ReceiveDelivery1Codez_DELI(Unit.getInstance())));
                break;
            case ReceiverRequest:
                processor.requestTransfer("01443", MessageBuilder.buildMsg("01443", "0010000031", new ReceiveDelivery1Codez_RECE(Unit.getInstance())));
                break;
            case ReceiverRequestNack:
                processor.requestTransfer("01443", MessageBuilder.buildMsg("01443", "0010000023", new ReceiveDelivery1Codez_RECE(Unit.getInstance())));
                break;
            default:
                break;
        }

        try {
            // wait a couple of seconds for the processing to finish
    //            Thread.sleep(60 * 1000);
            Thread.sleep(3 * 1000);
            System.exit(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static DamlLedgerClient getDamlLedgerClient(String host, int port) {
        // Initialize a plaintext gRPC channel with max size enlarged
        // Otherwise you may get RESOURCE_EXHAUSTED error when fetch packages
        int MAX_MESSAGE_SIZE = 64 * 1024 * 1024; // 64 MiB
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .maxInboundMessageSize(MAX_MESSAGE_SIZE)
                .build();

    //         create a client object to access services on the ledger
        return new DamlLedgerClient(Optional.empty(), channel);
    }

    private static String detectIngressPackageId(LedgerClient client) {
        PackageClient packageService = client.getPackageClient();

        // fetch a list of all package ids available on the ledger
        Flowable<String> packagesIds = packageService.listPackages();

        // fetch all packages and find the package that contains the DA.Spider.Main.Integration.BMW.Ingress module
        String packageId = packagesIds
                .flatMap(p -> packageService.getPackage(p).toFlowable())
                .filter(Main::containsIngressModule)
                .map(GetPackageResponse::getHash)
                .firstElement().blockingGet();

        if (packageId == null) {
            // No package on the ledger contained the Ingress module
            throw new RuntimeException("Module Ingress is not available on the ledger");
        }
        return packageId;
    }

    private static boolean containsIngressModule(GetPackageResponse getPackageResponse) {
        try {
    //            DamlLf.ArchivePayload payload = DamlLf.ArchivePayload.parseFrom(getPackageResponse.getArchivePayload());
            // This will fix the InvalidProtoBufferEx from above line
            // com.google.protobuf.InvalidProtocolBufferException: Protocol message had too many levels of nesting.  May be malicious.  Use CodedInputStream.setRecursionLimit() to increase the depth limit.
            CodedInputStream codedInputStream = CodedInputStream.newInstance(
                    getPackageResponse.getArchivePayload());
            int ProtobufRecursionLevel = 1000;
            codedInputStream.setRecursionLimit(ProtobufRecursionLevel);

            // parse the archive payload
            DamlLf.ArchivePayload payload = DamlLf.ArchivePayload.parseFrom(codedInputStream);

            // get the DAML LF package
            DamlLf1.Package lfPackage = payload.getDamlLf1();
            // check if the Ingress module is in the current package
            Optional<DamlLf1.Module> ingressModule = lfPackage.getModulesList().stream()
                    .filter(m -> m.getName().getSegmentsList().contains("Ingress")).findFirst();

            if (ingressModule.isPresent())
                return true;

        } catch (IOException  e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return false;
    }
}
