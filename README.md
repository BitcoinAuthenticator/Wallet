BitcoinAuthenticator - Wallet
====================

###IMPORTANT - This project is under development.
#####Wallet
[![Coverage Status](https://coveralls.io/repos/BitcoinAuthenticator/Wallet/badge.png?branch=develop)](https://coveralls.io/r/BitcoinAuthenticator/Wallet?branch=develop)


####What Is This ?
![alt tag](https://avatars1.githubusercontent.com/u/10117021?v=3&u=ec1653bfd716817994c43933e25d35bebdf40be5&s=140)

Bitcoin Authenticator is a P2P bitcoin wallet and [android application](https://github.com/cpacia/BitcoinAuthenticator) for creating a 2 factor transaction authentication and authorization. Bitcoin Authenticator is composed of a desktop wallet and an  [android application](https://github.com/cpacia/BitcoinAuthenticator), by pairing the two together they create an P2SH bitcoin address. Any bitcoins that are transferred to that address will require the authentication and digital signature of both the desktop wallet and the android Authenticator app.

####How do i get the android application ? 
[Click here](https://github.com/cpacia/BitcoinAuthenticator) 

#### Building requirements for the BitcoinAuthenticator wallet
1. Install Java 8
2. [Eclipse Luna, Support for java 8](http://www.eclipse.org/home/index.php) (Obviously, only if you work with eclipse)

## Building

#####With Maven
1. Clone the project:<br>
```
$ git clone https://github.com/negedzuregal/BitcoinAuthWallet.git
```
2. 
```
$ cd BitcoinAuthWallet/authenticator-wallet
```
3. build wallet: <br>
```
$mvn clean package
```

#### Protobuf class
From the Protobuf folder(/src/main/java/org/authenticator/protobuf):
```
$ protoc <Proto File>.proto --java_out=../../../ --proto_path=./ --proto_path=<system-include-directory, e.g., OSX is /usr/local/include/>
```

For protobuf-install-directory see [this](http://stackoverflow.com/questions/20069295/importing-google-protobuf-descriptor-proto-in-java-protocol-buffers)

#### Importing Into Eclipse 
1. In Eclipse:
```
File -> Import -> existing maven project
``` 
3. Run with JVM assertion flag:<br>
```
Run -> Run Configurations -> arguments -> add -ea in VM Arguments
```

## Native Installer
1. 
```
$ cd  authenticator-wallet
```
2. build: <br>
```
$mvn clean package
```
3. Set jh  (OSX/ Linux):  
```
$ jh=$JAVA_HOME/bin
```
<br>
Set jh (Windows):  
```
c:\<Path to project> set jh=%JAVA_HOME%/bin
```
4. Build (OSX/ Linux): 
```
$ $jh/javafxpackager -deploy -v -native -outdir . -outdir packages -outfile BTCAuthenticator -srcdir target -srcfiles wallettemplate-app.jar -appclass org.wallet.Main -name "Authenticator Wallet" -title "Authenticator Wallet"
```
<br>
Build (Windows): 
```
c:\<Path to project> "%jh%/javafxpackager.exe" -deploy -v -native -outdir . -outdir packages -outfile BTCAuthenticator -srcdir target -srcfiles wallettemplate-app.jar -appclass org.wallet.Main -name "Authenticator Wallet" -title "Authenticator Wallet"
```

## Development 
- Check our future development proposals [here](https://docs.google.com/spreadsheets/d/1o5ZS_L8OppZJit46SzpauJOthI0ncWuIgmo6ZtPevOU/edit?usp=sharing)

## Testing
- Our UX testing checklist [here](https://docs.google.com/spreadsheets/d/1Tcg6E1ZxlYmg9TjcGjNhZoP0vHRUZT_O_2SzJJVfcKQ/edit?usp=sharing)
- <b> Unit testing coverage is low, any contribution will be much appreciated !</b>

## Contacts
If you have any questions feel free to contact us: 
<br><b>ctpacia@gmail.com (Chris Pacia)
<br>alonmuroch@gmail.com (Alon Muroch)</b>