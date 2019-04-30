# How to run this app to create an Account
1. Start and setup the ledger server

    You can achieve this by running the func test for SPD-3804
    
    ```
    make run-func-test ftags=@SPD-3804-BilateralTransfer-Setup
    ```

2. Run the application - Simulate Deliverer submit Bilateral Demand Transfer Request

    You can setup the IntelliJ with the following settings base on an "Application" template
    
    * Main class: com.daml.asx.Main
    * Program arguments: localhost 8080 DelivererRequest
    * Use classpath of module: example-spider-java

3. Run the application - Simulate Operator query the Holdings of Deliver and Receiver for the Bilateral Demand Transfer Request

    You can setup the IntelliJ with the following settings base on an "Application" template
    
    * Main class: com.daml.asx.Main
    * Program arguments: localhost 8080 QueryHoldings
    * Use classpath of module: example-spider-java

