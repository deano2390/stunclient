# stunclient
STUN client for Android

Forked from https://github.com/mangefoo/stunclient itself based on https://github.com/tking/JSTUN

# Gradle

[![](https://jitpack.io/v/deano2390/stunclient.svg)](https://jitpack.io/#deano2390/stunclient)

Add the jitpack repo to your your project's build.gradle at the end of repositories

/build.gradle
```groovy
allprojects {
	repositories {
		jcenter()
		maven { url "https://jitpack.io" }
	}
}
```

Then add the dependency to your module's build.gradle:

/app/build.gradle
```groovy
	dependencies {
	        compile 'com.github.deano2390:stunclient:0.4'
	}

```
# Usage
So far I have only usd this to obtain my public IP address from a STUN server:

```java
  try {

        String publicIPV4, publicIPV6;
 
        LoggerFactory.setObserver(JoinGridHandler.this);

        //String stunServer = "numb.viagenie.ca"; // server supports IPV6
        String stunServer = "stun.xten.com";

        PublicIPDiscoveryTest test = new PublicIPDiscoveryTest(InetAddress.getByName("0.0.0.0"), stunServer, 3478);
        DiscoveryInfo discoveryInfo = test.test();

          if (discoveryInfo != null) {

            if (discoveryInfo.getPublicIPV4() != null) {
              publicIPV4 = discoveryInfo.getPublicIPV4().getHostAddress();
            }
            if (discoveryInfo.getPublicIPV6() != null) {
              publicIPV6 = discoveryInfo.getPublicIPV6();
            }
          }
  } catch (Exception e) {
    e.printStackTrace();
  }
                
```
