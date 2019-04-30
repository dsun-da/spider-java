# How to run this app to create an Account
1. Start and setup the ledger server

    You can achieve this by running the func test (solution-spider) for ASX CDE1 Training setup
    
    ```
    git clone git@github.com:DACH-NY/solution-spider.git
    git co chore/ASX_CDE1_Training
    make run-func-test ftags=@SPD-3804-BilateralTransfer-0
    ```

2. Run the application - Simulate Deliverer submit Bilateral Demand Transfer Request

    You can setup the IntelliJ with the following settings base on an "Application" template
    
    * Main class: com.daml.asx.Main
    * Program arguments: localhost 8080 DelivererRequest
    * Use classpath of module: example-spider-java

3. Run the application - Simulate Receiver submit Bilateral Demand Transfer Request

    You can setup the IntelliJ with the following settings base on an "Application" template

    * Main class: com.daml.asx.Main
    * Program arguments: localhost 8080 ReceiverRequest
    * Use classpath of module: example-spider-java

4. Run the application - Simulate Operator query the Holdings of Deliver and Receiver for the Bilateral Demand Transfer Request

    You can setup the IntelliJ with the following settings base on an "Application" template
    
    * Main class: com.daml.asx.Main
    * Program arguments: localhost 8080 QueryHoldings
    * Use classpath of module: example-spider-java

