BitcoinAuthenticator - Wallet
====================

###IMPORTANT - This project is under developement.

####What Is This ?
Bitcoin Authenticator is a P2P bitcoin wallet and [android application](https://github.com/cpacia/BitcoinAuthenticator) for creating a 2 factor transaction authentication and authorization. Bitcoin Authenticator is composed of a desktop wallet and an  [android application](https://github.com/cpacia/BitcoinAuthenticator), by pairing the two together they create an P2SH bitcoin address. Any bitcoins that are transferred to that address will require the authentication and digital signature of both the desktop wallet and the android Authenticator app.

####How do i get the android application ? 
[Click here](https://github.com/cpacia/BitcoinAuthenticator) 

#### Building requirements for the BitcoinAuthenticator wallet
1. Install Java 8
2. [Eclipse Luna, Support for java 8](http://www.eclipse.org/home/index.php) (Obviously, only if you work with eclipse)



#### Importing Into Eclipse 
1. Get a copy of the project 
   ```
   git clone https://github.com/negedzuregal/BitcoinAuthWallet.git
   ```
2. In Eclipse:
  ```
  File -> Import -> existing maven project
   ``` 
3. Import pom.xml

#### Building

#####With Maven
1. Clone the project:
 ```
 $ git clone https://github.com/negedzuregal/BitcoinAuthWallet.git
 ```
2. 
 ```
 $ cd BitcoinAuthWallet
 ```
3. build: 
 ```
$mvn clean package
 ```

##### Protobuf class
From the Protobuf folder(/src/main/java/authenticator/protobuf):
 ```
$ protoc <Proto File>.proto --java_out=../../
 ```

##### Executable Jar
 ```
$ mvn clean compile assembly:single
 ```

#### TODO
1. Testing Testing Testing !
2. Transaction list Pane
3. Currency conversions from bitcoin
4. Smart input selection
5. Smart fee calculation
6. Improve command line parameters for various functionalities
7. Basic RPC
8. mixing
9. Integrate with [OneName](https://onename.io)
10. Discover scheme for the key hierarchy
